package net.benmur.riemann.client

import org.scalatest.FunSuite
import net.benmur.riemann.client.testingsupport.TestingTransportSupport
import akka.actor.ActorSystem
import java.net.InetSocketAddress
import java.net.Socket
import org.scalatest.BeforeAndAfterAll
import org.scalamock.scalatest.MockFactory
import org.scalamock.ProxyMockFactory
import org.scalatest.matchers.ShouldMatchers
import java.net.SocketAddress
import akka.testkit.CallingThreadDispatcher
import org.scalamock.annotation.mock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import com.aphyr.riemann.Proto
import akka.dispatch.Await
import akka.util.duration._

class ReliableIOTest extends FunSuite
    with BeforeAndAfterAll
    with MockFactory
    with ProxyMockFactory
    with ShouldMatchers {

  import ReliableIO._
  import TestingTransportSupport._

  implicit val system = ActorSystem()
  val address = new InetSocketAddress(0)

  override def afterAll {
    system.shutdown
  }

  test("sending a protobuf Msg") {
    val in = Array.ofDim[Byte](256)
    val ios = new ByteArrayInputStream(in)
    val oos = new ByteArrayOutputStream()

    val wrapper = mock[ConnectedSocketWrapper]
    wrapper expects 'outputStream returning oos once;
    wrapper expects 'inputStream returning ios once

    val socketFactory = mockFunction[SocketAddress, ConnectedSocketWrapper]
    socketFactory expects address returning wrapper once

    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
    implicitly[SendOff[Reliable]].sendOff(conn, Write(protoMsgEvent))

    val out = oos.toByteArray
    val outRef = protoMsgEvent.toByteArray
    new DataInputStream(new ByteArrayInputStream(out)).readInt should be === outRef.length
    out.slice(4, out.length) should be === outRef
  }

  test("sending a protobuf Msg, with feedback") {
    val response = Proto.Msg.newBuilder.setOk(true).build
    val responseBytes = response.toByteArray

    val outBuilder = new ByteArrayOutputStream()
    val outBuilderData = new DataOutputStream(outBuilder)
    outBuilderData.writeInt(responseBytes.length)
    outBuilderData.write(responseBytes)

    val oos = new ByteArrayOutputStream()

    val wrapper = mock[ConnectedSocketWrapper]
    wrapper expects 'outputStream returning oos once;
    wrapper expects 'inputStream returning new ByteArrayInputStream(outBuilder.toByteArray) once

    val socketFactory = mockFunction[SocketAddress, ConnectedSocketWrapper]
    socketFactory expects address returning wrapper once

    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
    val respFuture = implicitly[SendAndExpectFeedback[Reliable]].send(conn, Write(protoMsgEvent))

    val out = oos.toByteArray
    val outRef = protoMsgEvent.toByteArray
    new DataInputStream(new ByteArrayInputStream(out)).readInt should be === outRef.length
    out.slice(4, out.length) should be === outRef

    val resp = Await.result(respFuture, 1 second)
    resp should be === Right(Nil)
  }
}