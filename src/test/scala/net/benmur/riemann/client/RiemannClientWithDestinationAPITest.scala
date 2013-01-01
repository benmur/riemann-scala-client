package net.benmur.riemann.client

import java.net.InetSocketAddress

import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import org.scalatest.matchers.ShouldMatchers

import RiemannClient.{ host, riemannConnectAs, service, state }
import akka.actor.ActorSystem

class RiemannClientWithDestinationAPITest extends FunSuite with BeforeAndAfterAll with ShouldMatchers {
  import RiemannClient._
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
    conn.sentOff should be === null
    conn.sentExpect should be === null
  }

  test("entry point to create a connection (sending an event)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest send event
    conn.sentOff should be === Write(event)
  }

  test("entry point to create a connection (sending multiple events)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest send EventSeq(event, event2)
    conn.sentOff should be === Write(EventSeq(event, event2))
  }

  test("entry point to create a connection (sending an event expecting feedback)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest ask event
    conn.sentExpect should be === Write(event)
  }

  test("entry point to create a connection (sending multiple events expecting feedback)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest ask EventSeq(event, event2)
    conn.sentExpect should be === Write(EventSeq(event, event2))
  }

  test("entry point to create a connection (sending an query)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest ask Query("true")
    conn.sentExpect should be === Write(Query("true"))
  }

  test("entry point to create a connection (sending an event, combining default EventPart values)") {
    val dest = riemannConnectAs[TestingTransport] to address withValues (host("h") | service("s"))
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest send state("ok")
    conn.sentOff should be === Write(host("h") | service("s") | state("ok"))
  }
}