package net.benmur.riemann.client

import java.net.SocketAddress

import scala.collection.mutable.WrappedArray

import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite

import akka.actor.ActorSystem
import akka.testkit.CallingThreadDispatcher

class UnreliableIOTest extends FunSuite
    with testingsupport.ImplicitActorSystem
    with MockFactory {

  import UnreliableIO._
  import testingsupport.TestingTransportSupport._

  test("send a protobuf Msg") {
    val socket = mock[Unreliable.SocketWrapper]
    (socket.send _).expects(WrappedArray.make(protoMsgEvent.toByteArray)).once()

    val socketFactory = mockFunction[SocketAddress, Unreliable.SocketWrapper]
    socketFactory expects address returning socket once

    val conn = implicitly[ConnectionBuilder[Unreliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
    implicitly[SendOff[EventPart, Unreliable]].sendOff(conn, Write(event))
  }
}