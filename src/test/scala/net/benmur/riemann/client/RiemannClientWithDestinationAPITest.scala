package net.benmur.riemann.client

import org.scalatest.FunSuite
import net.benmur.riemann.client.testingsupport.TestingTransportSupport
import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll
import java.net.InetSocketAddress
import org.scalatest.matchers.ShouldMatchers

class RiemannClientWithDestinationAPITest extends FunSuite with BeforeAndAfterAll with ShouldMatchers {
  import RiemannClient._
  import TestingTransportSupport._

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
  }

  test("entry point to create a connection (sending an event)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest send event
    conn.sentOff should be === Write(protoMsgEvent)
  }

  test("entry point to create a connection (sending multiple events)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest send List(event, event2)
    conn.sentOff should be === Write(protoMsgEvents)
  }

  test("entry point to create a connection (sending an event expecting feedback)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest ask event
    conn.sentExpect should be === Write(protoMsgEvent)
  }

  test("entry point to create a connection (sending multiple events expecting feedback)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest ask List(event, event2)
    conn.sentExpect should be === Write(protoMsgEvents)
  }

  test("entry point to create a connection (sending an query)") {
    val dest = riemannConnectAs[TestingTransport] to address
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest ask Query("true")
    conn.sentExpect should be === Write(protoMsgQuery)
  }

  test("entry point to create a connection (sending an event, combining default EventPart values)") {
    val dest = riemannConnectAs[TestingTransport] to address withValues (host("h") | service("s"))
    val conn = dest.connection.asInstanceOf[TestingTransportConnection]

    dest send state("ok")
    conn.sentOff should be === Write(protoMsgEvent)
  }
}