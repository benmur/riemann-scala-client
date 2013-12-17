package net.benmur.riemann.client

import scala.annotation.implicitNotFound

import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite

import EventSenderDSL.event2EventSender
import EventSenderDSL.event2EventSenderOff
import EventSenderDSL.query2QuerySender
import net.benmur.riemann.client.testingsupport.TestingTransportSupport.TestingTransport
import net.benmur.riemann.client.testingsupport.TestingTransportSupport.TestingTransportConnection
import net.benmur.riemann.client.testingsupport.TestingTransportSupport.event
import net.benmur.riemann.client.testingsupport.TestingTransportSupport.timeout

class EventSenderDSLTest extends FunSuite
  with MockFactory
  with testingsupport.ImplicitActorSystem {

  object DestinationOps extends DestinationOps
  import DestinationOps.RiemannDestination
  import EventSenderDSL._

  def makeDestination = {
    val conn = new TestingTransportConnection
    val dest = new DestinationOps.RiemannDestination[TestingTransport](EventPart(), conn)
    (conn, dest)
  }

  test("DSL operator to send an event without expecting a result") {
    val (conn, dest) = makeDestination
    implicit val sender = mock[SendOff[EventPart, TestingTransport]]
    (sender.sendOff _).expects(conn, Write(event)).once()

    event |>> dest
  }

  test("DSL operator to send operator to send an event expecting a status") {
    val (conn, dest) = makeDestination
    implicit val sender = mock[SendAndExpectFeedback[EventPart, Boolean, TestingTransport]]
    (sender.send _).expects(conn, Write(event), timeout, ec).once()

    event |>< dest
  }

  test("DSL operator to send operator to send a query expecting a status") {
    val (conn, dest) = makeDestination
    implicit val sender = mock[SendAndExpectFeedback[Query, Iterable[EventPart], TestingTransport]]

    val q = Query("true")
    (sender.send _).expects(conn, Write(q), timeout, ec).once()
    q |>< dest
  }
}