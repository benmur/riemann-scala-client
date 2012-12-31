package net.benmur.riemann.client

import java.io.{ DataInputStream, DataOutputStream }
import java.net.{ Socket, SocketAddress }
import java.util.concurrent.atomic.AtomicLong
import com.aphyr.riemann.Proto
import akka.actor.{ Actor, ActorLogging, ActorSystem, Props, actorRef2Scala }
import akka.dispatch.Future
import akka.pattern.ask
import akka.util.Timeout
import akka.dispatch.Promise
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import akka.util.duration._
import akka.actor.ActorRef
import java.net.SocketException

trait ReliableIO {
  private val nClients = new AtomicLong(0L) // FIXME this should be more global

  private[this] class ReliableConnectionActor(where: SocketAddress, factory: Reliable#SocketFactory, dispatcherId: Option[String])(implicit system: ActorSystem) extends Actor {
    override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 36000, withinTimeRange = 1 hour) { // This needs to be more reasonable
      case _ => Restart
    }

    val props = {
      val p = Props(new TcpConnectionActor(where, factory))
      if (dispatcherId.isEmpty) p else p.withDispatcher(dispatcherId.get)
    }

    val ioActor = context.actorOf(props, "io")

    def receive = {
      case message => ioActor forward message
    }
  }

  implicit object ReliableSendAndExpectFeedback extends SendAndExpectFeedback[Reliable] {
    def send(connection: Connection[Reliable], command: Write)(implicit system: ActorSystem, timeout: Timeout): Future[Either[RemoteError, List[EventPart]]] =
      connection match {
        case rc: ReliableConnection =>
          (rc.ioActor ask command).mapTo[Either[RemoteError, List[EventPart]]]
        case c =>
          Promise.successful(Left(RemoteError(
            "don't know how to send data to " + c.getClass.getName)))
      }
  }

  implicit object ReliableSendOff extends SendOff[Reliable] {
    def sendOff(connection: Connection[Reliable], command: Write): Unit = connection match {
      case rc: ReliableConnection =>
        rc.ioActor tell command
      case c =>
        System.err.println(
          "don't know how to send data to " + c.getClass.getName)
    }
  }

  class TcpConnectionActor(where: SocketAddress, factory: Reliable#SocketFactory) extends Actor with ActorLogging {
    val connection = factory(where)
    val outputStream = new DataOutputStream(connection.outputStream)
    val inputStream = new DataInputStream(connection.inputStream)
    println("actor init")
    def receive = {
      case Write(msg) =>
        try {
          val ab = msg.toByteArray
          outputStream writeInt ab.length
          outputStream write ab
          outputStream.flush
          val buf = Array.ofDim[Byte](inputStream.readInt())
          inputStream.readFully(buf)
          sender ! Serializers.unserializeProtoMsg(Proto.Msg.parseFrom(buf))
        } catch {
          case e: SocketException => throw e
          case exception =>
            log.error(exception, "could not send or receive data")
            sender ! Left(RemoteError(exception.getMessage()))
        }
    }
  }

  val makeTcpConnection: Reliable#SocketFactory = (addr) => {
    val socket = new Socket()
    socket.connect(addr)
    new ConnectedSocketWrapper {
      override def outputStream = socket.getOutputStream()
      override def inputStream = socket.getInputStream()
    }
  }

  class ReliableConnection(val ioActor: ActorRef) extends Connection[Reliable]

  implicit object TwoWayConnectionBuilder extends ConnectionBuilder[Reliable] {
    def buildConnection(where: SocketAddress, factory: Option[Reliable#SocketFactory] = None, dispatcherId: Option[String])(implicit system: ActorSystem, timeout: Timeout): Connection[Reliable] = {
      val props = {
        val p = Props(new ReliableConnectionActor(where, factory getOrElse makeTcpConnection, dispatcherId))
        if (dispatcherId.isEmpty) p else p.withDispatcher(dispatcherId.get)
      }
      new ReliableConnection(system.actorOf(props, "riemann-tcp-client-" + nClients.incrementAndGet))
    }
  }
}

object ReliableIO extends ReliableIO
