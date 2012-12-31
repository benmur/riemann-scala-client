package net.benmur.riemann.client

import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import com.aphyr.riemann.Proto
import akka.actor.ActorSystem
import net.benmur.riemann.client.testingsupport.TestingTransportSupport

class EventSenderDSLTest extends FunSuite with BeforeAndAfterAll {
  import TestingTransportSupport._
  import EventSenderDSL._

  implicit val system = ActorSystem()

  override def afterAll {
    system.shutdown
  }

  def makeDestination = {
    val conn = new TestingTransportConnection
    val dest = new RiemannDestination[TestingTransport](EventPart(), conn)
    (conn, dest)
  }

  test("DSL operator to send an event without expecting a result") {
    val (conn, dest) = makeDestination

    expect(Write(protoMsgEvent)) {
      event |>> dest
      conn.sentOff
    }
  }

  test("DSL operator to send operator to send an event expecting a status") {
    val (conn, dest) = makeDestination

    expect(Write(protoMsgEvent)) {
      event |>< dest
      conn.sentExpect
    }
  }

  test("DSL operator to send operator to send a query expecting a status") {
    val (conn, dest) = makeDestination

    expect(Write(protoMsgQuery)) {
      Query("true") |>< dest
      conn.sentExpect
    }
  }
}