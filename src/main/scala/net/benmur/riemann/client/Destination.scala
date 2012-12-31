package net.benmur.riemann.client

import java.net.SocketAddress

import scala.annotation.implicitNotFound
import scala.collection.JavaConversions.iterableAsScalaIterable

import akka.actor.ActorSystem
import akka.dispatch.Future
import akka.util.Timeout

trait DestinationOps {
  class DestinationBuilder[T <: TransportType](connectionBuilder: ConnectionBuilder[T])(implicit system: ActorSystem, timeout: Timeout) {
    def to(where: SocketAddress): RiemannDestination[T] =
      new RiemannDestination[T](EventPart(), connectionBuilder.buildConnection(where))
  }

  class RiemannDestination[T <: TransportType](baseEvent: EventPart, val connection: Connection[T])(implicit system: ActorSystem, timeout: Timeout)
      extends Destination[T] {
    
    def send(event: EventPart)(implicit messenger: SendOff[T]): Unit =
      messenger.sendOff(connection, Write(
        Serializers.serializeEventPartToProtoMsg(EventDSL.mergeEvents(baseEvent, event))))

    def ask(event: EventPart)(implicit messenger: SendAndExpectFeedback[T]): Future[Either[RemoteError, List[EventPart]]] =
      messenger.send(connection, Write(
        Serializers.serializeEventPartToProtoMsg(EventDSL.mergeEvents(baseEvent, event))))

    def send(events: Iterable[EventPart])(implicit messenger: SendOff[T]): Unit =
      messenger.sendOff(connection, Write(
        Serializers.serializeEventPartsToProtoMsg(events map (EventDSL.mergeEvents(baseEvent, _)))))

    def ask(events: Iterable[EventPart])(implicit messenger: SendAndExpectFeedback[T]): Future[Either[RemoteError, List[EventPart]]] =
      messenger.send(connection, Write(
        Serializers.serializeEventPartsToProtoMsg(events map (EventDSL.mergeEvents(baseEvent, _)))))

    def ask(query: Query)(implicit messenger: SendAndExpectFeedback[T]): Future[Either[RemoteError, List[EventPart]]] =
      messenger.send(connection, Write(
        Serializers.serializeQueryToProtoMsg(query)))

    def withValues(event: EventPart): RiemannDestination[T] =
      new RiemannDestination[T](EventDSL.mergeEvents(baseEvent, event), connection)
  }
}
