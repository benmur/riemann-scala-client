package net.benmur.riemann.client

import scala.annotation.implicitNotFound

trait EventSenderDSL {
  implicit class EventSenderOff[T <: TransportType](e: EventPart)(implicit messenger: SendOff[EventPart, T]) {
    def |>>(d: Destination[T]) = d send e
  }

  implicit class EventSender[T <: TransportType](e: EventPart)(implicit messenger: SendAndExpectFeedback[EventPart, Boolean, T]) {
    def |><(d: Destination[T]) = d ask e
  }

  implicit class QuerySender[T <: TransportType](q: Query)(implicit messenger: SendAndExpectFeedback[Query, Iterable[EventPart], T]) {
    def |><(d: Destination[T]) = d ask q
  }
}

object EventSenderDSL extends EventSenderDSL
