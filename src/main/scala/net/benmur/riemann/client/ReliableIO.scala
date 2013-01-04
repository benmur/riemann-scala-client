package net.benmur.riemann.client

import java.io.{ DataInputStream, DataOutputStream }
import java.net.{ Socket, SocketAddress, SocketException }
import java.util.concurrent.atomic.AtomicLong
import scala.annotation.implicitNotFound
import com.aphyr.riemann.Proto
import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy, Props }
import akka.actor.SupervisorStrategy._
import akka.actor.actorRef2Scala
import akka.dispatch.{ Future, Promise }
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import akka.actor.ActorInitializationException

trait ReliableIO {
  private val nClients = new AtomicLong(0L) // FIXME this should be more global

  private[this] class ReliableConnectionActor(where: SocketAddress, factory: Reliable#SocketFactory, dispatcherId: Option[String])(implicit system: ActorSystem) extends Actor {
    override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 2, withinTimeRange = 1 second) { // This needs to be more reasonable
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

  implicit object ReliableEventPartSendAndExpectFeedback extends SendAndExpectFeedback[EventPart, Boolean, Reliable] with Serializers {
    def send(connection: Connection[Reliable], command: Write[EventPart])(implicit system: ActorSystem, timeout: Timeout): Future[Boolean] =
      connection match {
        case rc: ReliableConnection =>
          val data = serializeEventPartToProtoMsg(command.m).toByteArray
          (rc.ioActor ask WriteBinary(data)).mapTo[Proto.Msg] map (_.getOk)

        case c =>
          Promise.failed(RemoteError("don't know how to send data to " + c.getClass.getName))
      }
  }

  implicit object ReliableQuerySendAndExpectFeedback extends SendAndExpectFeedback[Query, Iterable[EventPart], Reliable] with Serializers {
    def send(connection: Connection[Reliable], command: Write[Query])(implicit system: ActorSystem, timeout: Timeout): Future[Iterable[EventPart]] =
      connection match {
        case rc: ReliableConnection =>
          val data = serializeQueryToProtoMsg(command.m).toByteArray
          (rc.ioActor ask WriteBinary(data)).mapTo[Proto.Msg] map (unserializeProtoMsg(_))

        case c =>
          Promise.failed(RemoteError("don't know how to send data to " + c.getClass.getName))
      }
  }

  implicit object ReliableSendOff extends SendOff[EventPart, Reliable] with Serializers {
    def sendOff(connection: Connection[Reliable], command: Write[EventPart]): Unit = connection match {
      case rc: ReliableConnection =>
        rc.ioActor tell WriteBinary(serializeEventPartToProtoMsg(command.m).toByteArray)
      case c =>
        System.err.println(
          "don't know how to send data to " + c.getClass.getName)
    }
  }

  class TcpConnectionActor(where: SocketAddress, factory: Reliable#SocketFactory) extends Actor with ActorLogging {
    lazy val connection = factory(where)
    lazy val outputStream = new DataOutputStream(connection.outputStream)
    lazy val inputStream = new DataInputStream(connection.inputStream)

    def receive = {
      case WriteBinary(ab) =>
        try {
          outputStream writeInt ab.length
          outputStream write ab
          outputStream.flush
          val buf = Array.ofDim[Byte](inputStream.readInt())
          inputStream.readFully(buf)
          sender ! Proto.Msg.parseFrom(buf)
        } catch {
          case e: SocketException => throw e
          case exception =>
            log.error(exception, "could not send or receive data")
            val message = Option(exception.getMessage) getOrElse "(no message)"
            sender ! Proto.Msg.newBuilder.setError(message).setOk(false).build
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
