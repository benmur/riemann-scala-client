package net.benmur.riemann.client

import java.io.InputStream
import java.io.OutputStream
import java.net.SocketAddress

import scala.annotation.implicitNotFound
import scala.collection.mutable.WrappedArray
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.util.Timeout

sealed trait RiemannSendable

case class EventPart(
  host: Option[String] = None,
  service: Option[String] = None,
  state: Option[String] = None,
  time: Option[Long] = None,
  description: Option[String] = None,
  tags: Iterable[String] = Nil,
  attributes: Map[String, String] = Map.empty,
  metric: Option[AnyVal] = None,
  ttl: Option[Float] = None) extends RiemannSendable

case class EventSeq(events: EventPart*) extends RiemannSendable

case class Query(q: String) extends RiemannSendable

case class Write[T <: RiemannSendable](m: T)

case class WriteBinary(data: Array[Byte])

case class RemoteError(message: String) extends Throwable

trait TransportType {
  type Connection
  type SocketWrapper
  type SocketFactory = SocketAddress => SocketWrapper
}

object Reliable extends TransportType {
  type SocketWrapper = ConnectedSocketWrapper
  type Connection = TcpActorConnectionHandle

  trait ConnectedSocketWrapper {
    def inputStream: InputStream
    def outputStream: OutputStream
  }

  trait TcpActorConnectionHandle {
    val ioActor: ActorRef
  }
}

object Unreliable extends TransportType {
  type SocketWrapper = UnconnectedSocketWrapper
  type Connection = UdpActorConnectionHandle

  trait UnconnectedSocketWrapper {
    def send(data: WrappedArray[Byte]): Unit
  }

  trait UdpActorConnectionHandle {
    val ioActor: ActorRef
  }
}

trait Destination[T <: TransportType] {
  def send(event: EventPart)(implicit messenger: SendOff[EventPart, T]): Unit
  def ask(event: EventPart)(implicit messenger: SendAndExpectFeedback[EventPart, Boolean, T]): Future[Boolean]
  def send(events: EventSeq)(implicit messenger: SendOff[EventSeq, T]): Unit
  def ask(events: EventSeq)(implicit messenger: SendAndExpectFeedback[EventSeq, Boolean, T]): Future[Boolean]
  def ask(query: Query)(implicit messenger: SendAndExpectFeedback[Query, Iterable[EventPart], T]): Future[Iterable[EventPart]]
  def withValues(event: EventPart): Destination[T]
}

@implicitNotFound(msg = "No way of building a connection to Riemann of type ${T}.")
trait ConnectionBuilder[T <: TransportType] {
  def buildConnection(where: SocketAddress, factory: Option[T#SocketFactory] = None, dispatcherId: Option[String] = None)(implicit system: ActorSystem, timeout: Timeout): T#Connection
}

@implicitNotFound(msg = "Connection type ${T} does not allow sending to ${S} Riemann because there is no implicit in scope returning a implementation of SendOff[${S}, ${T}].")
trait SendOff[S <: RiemannSendable, T <: TransportType] {
  def sendOff(connection: T#Connection, command: Write[S]): Unit
}

@implicitNotFound(msg = "Connection type ${T} does not allow getting feedback from Riemann after sending ${S} because there is no implicit in scope returning a implementation of SendAndExpectFeedback[${S}, ${T}].")
trait SendAndExpectFeedback[S <: RiemannSendable, R, T <: TransportType] {
  def send(connection: T#Connection, command: Write[S], timeout: Timeout, context: ExecutionContext): Future[R]
}
