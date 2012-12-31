package net.benmur.riemann.client

import java.io.{ InputStream, OutputStream }
import java.net.SocketAddress
import scala.annotation.implicitNotFound
import com.aphyr.riemann.Proto
import akka.actor.ActorSystem
import akka.dispatch.Future
import akka.util.Timeout
import scala.collection.mutable.WrappedArray

case class EventPart(
  host: Option[String] = None,
  service: Option[String] = None,
  state: Option[String] = None,
  time: Option[Long] = None,
  description: Option[String] = None,
  tags: Iterable[String] = Nil,
  metric: Option[AnyVal] = None,
  ttl: Option[Float] = None)

case class Query(q: String)

case class Write(m: Proto.Msg)

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
  def send(event: EventPart)(implicit messenger: SendOff[T]): Unit
  def ask(event: EventPart)(implicit messenger: SendAndExpectFeedback[T]): Future[Either[RemoteError, List[EventPart]]]
  def send(events: Iterable[EventPart])(implicit messenger: SendOff[T]): Unit
  def ask(events: Iterable[EventPart])(implicit messenger: SendAndExpectFeedback[T]): Future[Either[RemoteError, List[EventPart]]]
  def ask(query: Query)(implicit messenger: SendAndExpectFeedback[T]): Future[Either[RemoteError, List[EventPart]]]
  def withValues(event: EventPart): Destination[T]
}

@implicitNotFound(msg = "No way of building a connection to Riemann of type ${T}.")
trait ConnectionBuilder[T <: TransportType] {
  def buildConnection(where: SocketAddress, factory: Option[T#SocketFactory] = None, dispatcherId: Option[String] = None)(implicit system: ActorSystem, timeout: Timeout): Connection[T]
}

@implicitNotFound(msg = "Connection type ${T} does not allow sending to Riemann because there is no implicit in scope returning a implementation of SendOff[${T}].")
trait SendOff[T <: TransportType] {
  def sendOff(connection: Connection[T], command: Write): Unit
}

@implicitNotFound(msg = "Connection type ${T} does not allow getting feedback from Riemann.")
trait SendAndExpectFeedback[T <: TransportType] {
  def send(connection: Connection[T], command: Write)(implicit system: ActorSystem, timeout: Timeout): Future[Either[RemoteError, List[EventPart]]]
}
