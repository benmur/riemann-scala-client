package net.benmur.riemann.client

import java.net.InetSocketAddress
import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import org.scalatest.matchers.ShouldMatchers
import RiemannClient.{ host, riemannConnectAs, service, state }
import akka.actor.ActorSystem
import org.scalamock.ProxyMockFactory
import org.scalamock.scalatest.MockFactory

class RiemannClientWithDestinationAPITest extends FunSuite
    with BeforeAndAfterAll
    with ShouldMatchers
    with MockFactory
    with ProxyMockFactory {
  import testingsupport.TestingTransportSupport._

  implicit val system = ActorSystem()
  val address = new InetSocketAddress(0)

  override def afterAll {
    system.shutdown
  }

  test("entry point to create a connection (pristine state)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    conn.where should be theSameInstanceAs address
  }

  test("entry point to create a connection (sending an event)") {
    val dest = riemannConnectAs[TestingTransport] to address
    implicit val sender = mock[SendOff[EventPart, TestingTransport]]
    sender expects 'sendOff withArguments (dest.connection, Write(event)) once

    dest send event
  }

  test("entry point to create a connection (sending multiple events)") {
    val dest = riemannConnectAs[TestingTransport] to address
    implicit val sender = mock[SendOff[EventSeq, TestingTransport]]
    sender expects 'sendOff withArguments (dest.connection, Write(EventSeq(event, event2))) once

    dest send EventSeq(event, event2)
  }

  test("entry point to create a connection (sending an event expecting feedback)") {
    val dest = riemannConnectAs[TestingTransport] to address
    implicit val sender = mock[SendAndExpectFeedback[EventPart, Boolean, TestingTransport]]
    sender expects 'send withArguments (dest.connection, Write(event), system, timeout) once

    dest ask event
  }

  test("entry point to create a connection (sending multiple events expecting feedback)") {
    val dest = riemannConnectAs[TestingTransport] to address
    implicit val sender = mock[SendAndExpectFeedback[EventSeq, Boolean, TestingTransport]]
    sender expects 'send withArguments (dest.connection, Write(EventSeq(event, event2)), system, timeout) once

    dest ask EventSeq(event, event2)
  }

  test("entry point to create a connection (sending an query)") {
    val dest = riemannConnectAs[TestingTransport] to address
    implicit val sender = mock[SendAndExpectFeedback[Query, Iterable[EventPart], TestingTransport]]
    sender expects 'send withArguments (dest.connection, Write(Query("true")), system, timeout) once

    dest ask Query("true")
  }

  test("entry point to create a connection (sending an event, combining default EventPart values)") {
    val dest = riemannConnectAs[TestingTransport] to address withValues (EventPart(host = Some("h"), service = Some("s")))
    implicit val sender = mock[SendOff[EventPart, TestingTransport]]
    val ev = EventPart(host = Some("h"), service = Some("s"), state = Some("ok"))
    sender expects 'sendOff withArguments (dest.connection, Write(ev)) once

    dest send state("ok")
  }
}