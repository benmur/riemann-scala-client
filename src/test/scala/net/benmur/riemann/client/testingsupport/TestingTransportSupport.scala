package net.benmur.riemann.client.testingsupport

import java.net.{ InetSocketAddress, SocketAddress }
import com.aphyr.riemann.Proto
import akka.actor.ActorSystem
import akka.dispatch.{ Future, Promise }
import akka.util.Timeout
import akka.util.duration.intToDurationInt
import net.benmur.riemann.client._

trait TestingTransportSupport {
  import RiemannClient._

  implicit val timeout = Timeout(1 millisecond)

  implicit object TestingTransportEventPartSendOff extends SendOff[EventPart, TestingTransport] {
    def sendOff(connection: Connection[TestingTransport], command: Write[EventPart]): Unit = connection match {
      case uc: TestingTransportConnection => uc.sentOff = command
    }
  }

  implicit object TestingTransportEventSeqSendOff extends SendOff[EventSeq, TestingTransport] {
    def sendOff(connection: Connection[TestingTransport], command: Write[EventSeq]): Unit = connection match {
      case uc: TestingTransportConnection => uc.sentOff = command
    }
  }

  implicit object TestingTransportEventPartSendAndExpectFeedback extends SendAndExpectFeedback[EventPart, TestingTransport] {
    def send(connection: Connection[TestingTransport], command: Write[EventPart])(implicit system: ActorSystem, timeout: Timeout): Future[Either[RemoteError, List[EventPart]]] =
      connection match {
        case tc: TestingTransportConnection =>
          tc.sentExpect = command
          Promise.successful(Right(Nil))
        case c => Promise.successful(Left(RemoteError("bad connection type")))
      }
  }

  implicit object TestingTransportEventSeqSendAndExpectFeedback extends SendAndExpectFeedback[EventSeq, TestingTransport] {
    def send(connection: Connection[TestingTransport], command: Write[EventSeq])(implicit system: ActorSystem, timeout: Timeout): Future[Either[RemoteError, List[EventPart]]] =
      connection match {
        case tc: TestingTransportConnection =>
          tc.sentExpect = command
          Promise.successful(Right(Nil))
        case c => Promise.successful(Left(RemoteError("bad connection type")))
      }
  }

  implicit object TestingTransportQuerySendAndExpectFeedback extends SendAndExpectFeedback[Query, TestingTransport] {
    def send(connection: Connection[TestingTransport], command: Write[Query])(implicit system: ActorSystem, timeout: Timeout): Future[Either[RemoteError, List[EventPart]]] =
      connection match {
        case tc: TestingTransportConnection =>
          tc.sentExpect = command
          Promise.successful(Right(Nil))
        case c => Promise.successful(Left(RemoteError("bad connection type")))
      }
  }

  class TestingTransportConnection(val where: SocketAddress = new InetSocketAddress(0)) extends Connection[TestingTransport] {
    var sentOff: Write[_ <: RiemannSendable] = _
    var sentExpect: Write[_ <: RiemannSendable] = _
  }

  implicit object TestingTransportConnectionBuilder extends ConnectionBuilder[TestingTransport] {
    implicit def buildConnection(where: SocketAddress, factory: Option[TestingTransport#SocketFactory] = None, dispatcherId: Option[String])(implicit system: ActorSystem, timeout: Timeout): Connection[TestingTransport] =
      new TestingTransportConnection(where)
  }

  trait TestingTransport extends TransportType {
    type SocketFactory = SocketAddress => Unit
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