package net.benmur.riemann.client

import java.net.{ InetSocketAddress, SocketAddress }

import scala.annotation.implicitNotFound
import scala.collection.mutable.WrappedArray

import org.scalamock.ProxyMockFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import org.scalatest.matchers.ShouldMatchers

import akka.actor.ActorSystem
import akka.testkit.CallingThreadDispatcher
import net.benmur.riemann.client.testingsupport.TestingTransportSupport

class UnreliableIOTest extends FunSuite
    with BeforeAndAfterAll
    with MockFactory
    with ProxyMockFactory
    with ShouldMatchers {

  import UnreliableIO._
  import TestingTransportSupport._

  implicit val system = ActorSystem()
  val address = new InetSocketAddress(0)

  override def afterAll {
    system.shutdown
  }

  test("send a protobuf Msg") {
    val socket = mock[UnconnectedSocketWrapper]
    socket expects 'send withArguments (WrappedArray.make(protoMsgEvent.toByteArray)) once

    val socketFactory = mockFunction[SocketAddress, UnconnectedSocketWrapper]
    socketFactory expects address returning socket once

    val conn = implicitly[ConnectionBuilder[Unreliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
    implicitly[SendOff[Unreliable]].sendOff(conn, Write(protoMsgEvent))
  }
}