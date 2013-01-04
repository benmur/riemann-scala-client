package net.benmur.riemann.client

import java.net.SocketAddress

import scala.collection.mutable.WrappedArray

import org.scalamock.ProxyMockFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import akka.actor.ActorSystem
import akka.testkit.CallingThreadDispatcher

class UnreliableIOTest extends FunSuite
    with testingsupport.ImplicitActorSystem
    with MockFactory
    with ProxyMockFactory
    with ShouldMatchers {

  import UnreliableIO._
  import testingsupport.TestingTransportSupport._

  test("send a protobuf Msg") {
    val socket = mock[UnconnectedSocketWrapper]
    socket expects 'send withArguments (WrappedArray.make(protoMsgEvent.toByteArray)) once

    val socketFactory = mockFunction[SocketAddress, UnconnectedSocketWrapper]
    socketFactory expects address returning socket once

    val conn = implicitly[ConnectionBuilder[Unreliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
    implicitly[SendOff[EventPart, Unreliable]].sendOff(conn, Write(event))
  }
}