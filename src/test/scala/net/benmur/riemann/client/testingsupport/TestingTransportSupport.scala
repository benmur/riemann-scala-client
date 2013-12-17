package net.benmur.riemann.client.testingsupport

import java.net.InetSocketAddress
import java.net.SocketAddress

import scala.annotation.implicitNotFound
import scala.concurrent.duration.DurationInt

import com.aphyr.riemann.Proto

import akka.actor.ActorSystem
import akka.util.Timeout
import net.benmur.riemann.client.ConnectionBuilder
import net.benmur.riemann.client.DestinationOps
import net.benmur.riemann.client.EventPart
import net.benmur.riemann.client.TransportType

trait TestingTransportSupport {
  import net.benmur.riemann.client.RiemannClient._

  implicit val timeout = Timeout(10.seconds)
  val address = new InetSocketAddress(0)

  class TestingTransportConnection(val where: SocketAddress = new InetSocketAddress(0))

  implicit object TestingTransportConnectionBuilder extends ConnectionBuilder[TestingTransport] {
    implicit def buildConnection(where: SocketAddress, factory: Option[TestingTransport#SocketFactory] = None, dispatcherId: Option[String])(implicit system: ActorSystem, timeout: Timeout): TestingTransport#Connection =
      new TestingTransportConnection(where)
  }

  trait TestingTransport extends TransportType {
    type SocketWrapper = Unit
    type Connection = TestingTransportConnection
  }

  val event = EventPart(host = Some("h"), service = Some("s"), state = Some("ok"))
  val event2 = EventPart(host = Some("h"), service = Some("s2"), state = Some("ok"))
  val protoQuery = Proto.Query.newBuilder().setString("true")
  val protoEvent = Proto.Event.newBuilder().setHost("h").setService("s").setState("ok")
  val protoEvent2 = Proto.Event.newBuilder().setHost("h").setService("s2").setState("ok")
  val protoMsgEvent = Proto.Msg.newBuilder.addEvents(protoEvent).build
  val protoMsgEvents = Proto.Msg.newBuilder.addEvents(protoEvent).addEvents(protoEvent2).build
  val protoMsgQuery = Proto.Msg.newBuilder.setQuery(protoQuery).build
}

object TestingTransportSupport extends TestingTransportSupport with DestinationOps
