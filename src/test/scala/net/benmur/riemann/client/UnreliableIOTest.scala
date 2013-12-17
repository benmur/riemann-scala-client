package net.benmur.riemann.client

import java.net.SocketAddress

import scala.annotation.implicitNotFound
import scala.collection.mutable.WrappedArray

import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite

import UnreliableIO.OneWayConnectionBuilder
import UnreliableIO.Unreliable
import UnreliableIO.UnreliableSendOff
import akka.testkit.CallingThreadDispatcher
import testingsupport.TestingTransportSupport.address
import testingsupport.TestingTransportSupport.event
import testingsupport.TestingTransportSupport.protoMsgEvent
import testingsupport.TestingTransportSupport.timeout

class UnreliableIOTest extends FunSuite
  with testingsupport.ImplicitActorSystem
  with MockFactory {

  import UnreliableIO._
  import testingsupport.TestingTransportSupport._

  test("send a protobuf Msg") {
    val socket = mock[Unreliable.SocketWrapper]
    val wa: WrappedArray[Byte] = WrappedArray.make(protoMsgEvent.toByteArray())
    (socket.send _).expects(wa).once()

    val socketFactory = mockFunction[SocketAddress, Unreliable.SocketWrapper]
    socketFactory.expects(address).returning(socket).once()

    val conn = implicitly[ConnectionBuilder[Unreliable]].buildConnection(
      address,
      Some(socketFactory),
      Some(CallingThreadDispatcher.Id))

    implicitly[SendOff[EventPart, Unreliable]].sendOff(conn, Write(event))
    system.shutdown
    system.awaitTermination
  }
}