package net.benmur.riemann.client
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import net.benmur.riemann.client.testingsupport.TestingTransportSupport
import org.scalamock.CallHandler2
import net.benmur.riemann.client.ReliableIO._
import net.benmur.riemann.client.Write
import net.benmur.riemann.client.EventPart
import net.benmur.riemann.client.Query


class EventSenderDSLTest extends FunSuite
    with MockFactory
    with testingsupport.ImplicitActorSystem      {

  object DestinationOps extends DestinationOps
  import DestinationOps.RiemannDestination
  import TestingTransportSupport._
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
    (sender.send _).expects(conn, Write(event)).once()

    event |>< dest
  }

  test("DSL operator to send operator to send a query expecting a status") {
    val (conn, dest) = makeDestination
    implicit val sender = mock[SendAndExpectFeedback[Query, Iterable[EventPart], TestingTransport]]

    val q = Query("true")
    (sender.send _).expects(conn, Write(q)).once()
    q |>< dest
  }
}