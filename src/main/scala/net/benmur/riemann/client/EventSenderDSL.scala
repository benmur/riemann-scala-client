package net.benmur.riemann.client

trait EventSenderDSL {
  class EventSenderOff[T <: TransportType](e: EventPart)(implicit messenger: SendOff[EventPart, T]) {
    def |>>(d: Destination[T]) = d send e
  }

  class EventSender[T <: TransportType](e: EventPart)(implicit messenger: SendAndExpectFeedback[EventPart, T]) {
    def |><(d: Destination[T]) = d ask e
  }

  class QuerySender[T <: TransportType](q: Query)(implicit messenger: SendAndExpectFeedback[Query, T]) {
    def |><(d: Destination[T]) = d ask q
  }

  implicit def event2EventSenderOff[T <: TransportType](e: EventPart)(implicit messenger: SendOff[EventPart, T]) = new EventSenderOff[T](e)
  implicit def event2EventSender[T <: TransportType](e: EventPart)(implicit messenger: SendAndExpectFeedback[EventPart, T]) = new EventSender[T](e)
  implicit def query2QuerySender[T <: TransportType](q: Query)(implicit messenger: SendAndExpectFeedback[Query, T]) = new QuerySender[T](q)
}

object EventSenderDSL extends EventSenderDSL
