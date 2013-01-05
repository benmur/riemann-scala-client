package net.benmur.riemann.client

import java.net.{ DatagramPacket, DatagramSocket, SocketAddress }

import scala.collection.mutable.WrappedArray

import akka.actor.{ Actor, ActorSystem, Props }
import akka.util.Timeout

trait UnreliableIO {
  private type ImplementedTransport = Unreliable.type
  type Unreliable = ImplementedTransport

  class UnreliableConnection(where: SocketAddress, factory: ImplementedTransport#SocketFactory, dispatcherId: Option[String] = None)(implicit system: ActorSystem)
      extends ImplementedTransport#Connection {
    val props = {
      val p = Props(new UnconnectedConnectionActor(where, factory))
      if (dispatcherId.isEmpty) p else p.withDispatcher(dispatcherId.get)
    }
    val ioActor = system.actorOf(props, io.IO.clientName("riemann-udp-client-"))
  }

  implicit object UnreliableSendOff extends SendOff[EventPart, ImplementedTransport] with Serializers {
    def sendOff(connection: ImplementedTransport#Connection, command: Write[EventPart]): Unit = {
      val data = serializeEventPartToProtoMsg(command.m).toByteArray
      connection.ioActor tell WriteBinary(data)
    }
  }

  private[this] class UnconnectedConnectionActor(where: SocketAddress, factory: ImplementedTransport#SocketFactory) extends Actor {
    val connection = factory(where)
    def receive = {
      case WriteBinary(data) => connection send data
    }
  }

  val makeUdpConnection: ImplementedTransport#SocketFactory = (addr: SocketAddress) => {
    new ImplementedTransport#SocketWrapper {
      val dest = new DatagramSocket(addr)
      override def send(data: WrappedArray[Byte]) = dest send new DatagramPacket(data.array, data.length)
    }
  }

  implicit object OneWayConnectionBuilder extends ConnectionBuilder[ImplementedTransport] {
    implicit def buildConnection(where: SocketAddress, factory: Option[ImplementedTransport#SocketFactory], dispatcherId: Option[String])(implicit system: ActorSystem, timeout: Timeout): ImplementedTransport#Connection =
      new UnreliableConnection(where, factory getOrElse makeUdpConnection, dispatcherId)
  }
}

object UnreliableIO extends UnreliableIO
