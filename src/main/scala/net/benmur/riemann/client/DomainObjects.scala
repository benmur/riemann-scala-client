package net.benmur.riemann.client

import java.io.{ InputStream, OutputStream }
import java.net.SocketAddress

import scala.annotation.implicitNotFound
import scala.collection.mutable.WrappedArray

import akka.actor.ActorSystem
import akka.dispatch.Future
import akka.util.Timeout

sealed trait RiemannSendable

case class EventPart(
  host: Option[String] = None,
  service: Option[String] = None,
  state: Option[String] = None,
  time: Option[Long] = None,
  description: Option[String] = None,
  tags: Iterable[String] = Nil,
  metric: Option[AnyVal] = None,
  ttl: Option[Float] = None) extends RiemannSendable

case class EventSeq(events: EventPart*) extends RiemannSendable

case class Query(q: String) extends RiemannSendable

case class Write[T <: RiemannSendable](m: T)

case class WriteBinary(data: Array[Byte])

case class RemoteError(message: String) extends Throwable

trait TransportType {
  type SocketFactory
}
trait Reliable extends TransportType {
  type SocketFactory = SocketAddress => ConnectedSocketWrapper
}
trait Unreliable extends TransportType {
  type SocketFactory = SocketAddress => UnconnectedSocketWrapper
}

trait Connection[T <: TransportType]

trait ConnectedSocketWrapper {
  def inputStream: InputStream
  def outputStream: OutputStream
}

trait UnconnectedSocketWrapper {
  def send(data: WrappedArray[Byte]): Unit
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
  def buildConnection(where: SocketAddress, factory: Option[T#SocketFactory] = None, dispatcherId: Option[String] = None)(implicit system: ActorSystem, timeout: Timeout): Connection[T]
}

@implicitNotFound(msg = "Connection type ${T} does not allow sending to ${S} Riemann because there is no implicit in scope returning a implementation of SendOff[${S}, ${T}].")
trait SendOff[S <: RiemannSendable, T <: TransportType] {
  def sendOff(connection: Connection[T], command: Write[S]): Unit
}

@implicitNotFound(msg = "Connection type ${T} does not allow getting feedback from Riemann after sending ${S} because there is no implicit in scope returning a implementation of SendAndExpectFeedback[${S}, ${T}].")
trait SendAndExpectFeedback[S <: RiemannSendable, R, T <: TransportType] {
  def send(connection: Connection[T], command: Write[S])(implicit system: ActorSystem, timeout: Timeout): Future[R]
}
