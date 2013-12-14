package net.benmur.riemann.client

import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import RiemannClient.riemannConnectAs

class RiemannClientWithDestinationAPITest extends FunSuite
    with testingsupport.ImplicitActorSystem
    with MockFactory {
  import testingsupport.TestingTransportSupport._

  test("entry point to create a connection (pristine state)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    conn.where should be theSameInstanceAs address
  }

  test("entry point to create a connection (sending an event)") {
    val dest = riemannConnectAs[TestingTransport] to address
    implicit val sender = mock[SendOff[EventPart, TestingTransport]]
    (sender.sendOff _).expects(dest.connection, Write(event)).once()

    dest send event
  }

  test("entry point to create a connection (sending multiple events)") {
    val dest = riemannConnectAs[TestingTransport] to address
    implicit val sender = mock[SendOff[EventSeq, TestingTransport]]
    (sender.sendOff _).expects(dest.connection, Write(EventSeq(event, event2))).once()

    dest send EventSeq(event, event2)
  }

  test("entry point to create a connection (sending an event expecting feedback)") {
    val dest = riemannConnectAs[TestingTransport] to address
    implicit val sender = mock[SendAndExpectFeedback[EventPart, Boolean, TestingTransport]]
    (sender.send _).expects(dest.connection, Write(event)).once()

    dest ask event
  }

  test("entry point to create a connection (sending multiple events expecting feedback)") {
    val dest = riemannConnectAs[TestingTransport] to address
    implicit val sender = mock[SendAndExpectFeedback[EventSeq, Boolean, TestingTransport]]
    (sender.send _).expects(dest.connection, Write(EventSeq(event, event2))).once()

    dest ask EventSeq(event, event2)
  }

  test("entry point to create a connection (sending an query)") {
    val dest = riemannConnectAs[TestingTransport] to address
    implicit val sender = mock[SendAndExpectFeedback[Query, Iterable[EventPart], TestingTransport]]
    (sender.send _).expects(dest.connection, Write(Query("true"))).once()

    dest ask Query("true")
  }

  test("entry point to create a connection (sending an event, combining default EventPart values)") {
    val dest = riemannConnectAs[TestingTransport] to address withValues (EventPart(host = Some("h"), service = Some("s")))
    implicit val sender = mock[SendOff[EventPart, TestingTransport]]
    val ev = EventPart(host = Some("h"), service = Some("s"), state = Some("ok"))
    (sender.sendOff _).expects(dest.connection, Write(ev)).once()

    dest send EventPart(state = Some("ok"))
  }
}