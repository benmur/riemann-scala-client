package net.benmur.riemann.client

import java.net.{ DatagramPacket, DatagramSocket, SocketAddress }

import scala.annotation.implicitNotFound
import scala.collection.mutable.WrappedArray

import akka.actor.{ Actor, ActorSystem, Props }
import akka.util.Timeout

trait UnreliableIO {
  class UnreliableConnection(where: SocketAddress, factory: Unreliable#SocketFactory, dispatcherId: Option[String] = None)(implicit system: ActorSystem) extends Connection[Unreliable] {
    val props = {
      val p = Props(new UnconnectedConnectionActor(where, factory))
      if (dispatcherId.isEmpty) p else p.withDispatcher(dispatcherId.get)
    }
    val ioActor = system.actorOf(props, io.IO.clientName("riemann-udp-client-"))
  }

  implicit object UnreliableSendOff extends SendOff[EventPart, Unreliable] with Serializers {
    def sendOff(connection: Connection[Unreliable], command: Write[EventPart]): Unit = connection match {
      case uc: UnreliableConnection =>
        val data = serializeEventPartToProtoMsg(command.m).toByteArray
        uc.ioActor tell WriteBinary(data)
      case c => System.err.println("don't know how to send data to " + c.getClass.getName)
    }
  }

  private[this] class UnconnectedConnectionActor(where: SocketAddress, factory: Unreliable#SocketFactory) extends Actor {
    val connection = factory(where)
    def receive = {
      case WriteBinary(data) => connection send data
    }
  }

  val makeUdpConnection: Unreliable#SocketFactory = (addr: SocketAddress) => {
    val dest = new DatagramSocket(addr)
    new UnconnectedSocketWrapper {
      override def send(data: WrappedArray[Byte]) = dest send new DatagramPacket(data.array, data.length)
    }
  }

  implicit object OneWayConnectionBuilder extends ConnectionBuilder[Unreliable] {
    implicit def buildConnection(where: SocketAddress, factory: Option[Unreliable#SocketFactory], dispatcherId: Option[String])(implicit system: ActorSystem, timeout: Timeout): Connection[Unreliable] =
      new UnreliableConnection(where, factory getOrElse makeUdpConnection, dispatcherId)
  }
}

object UnreliableIO extends UnreliableIO
