package net.benmur.riemann.client

import java.net.SocketAddress

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.util.Timeout

trait DestinationOps {
  class DestinationBuilder[T <: TransportType](connectionBuilder: ConnectionBuilder[T])(implicit system: ActorSystem, timeout: Timeout) {
    def to(where: SocketAddress): RiemannDestination[T] = {
      implicit val context = system.dispatcher
      new RiemannDestination[T](EventPart(), connectionBuilder.buildConnection(where))
    }
  }

  class RiemannDestination[T <: TransportType](baseEvent: EventPart, val connection: T#Connection)(implicit timeout: Timeout, context: ExecutionContext)
    extends Destination[T] {

    def send(event: EventPart)(implicit messenger: SendOff[EventPart, T]): Unit =
      messenger.sendOff(connection, Write(EventDSL.mergeEvents(baseEvent, event)))

    def ask(event: EventPart)(implicit messenger: SendAndExpectFeedback[EventPart, Boolean, T]): Future[Boolean] =
      messenger.send(connection, Write(EventDSL.mergeEvents(baseEvent, event)), timeout, context)

    def send(events: EventSeq)(implicit messenger: SendOff[EventSeq, T]): Unit =
      messenger.sendOff(connection, Write(EventSeq(events.events map (EventDSL.mergeEvents(baseEvent, _)): _*)))

    def ask(events: EventSeq)(implicit messenger: SendAndExpectFeedback[EventSeq, Boolean, T]): Future[Boolean] =
      messenger.send(connection,
        Write(EventSeq(events.events map (EventDSL.mergeEvents(baseEvent, _)): _*)),
        timeout,
        context)

    def ask(query: Query)(implicit messenger: SendAndExpectFeedback[Query, Iterable[EventPart], T]): Future[Iterable[EventPart]] =
      messenger.send(connection, Write(query), timeout, context)

    def withValues(event: EventPart): RiemannDestination[T] =
      new RiemannDestination[T](EventDSL.mergeEvents(baseEvent, event), connection)
  }
}
