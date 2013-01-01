package net.benmur.riemann.client

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream }
import java.net.{ InetSocketAddress, SocketAddress }

import org.scalamock.ProxyMockFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import org.scalatest.matchers.ShouldMatchers

import com.aphyr.riemann.Proto

import akka.actor.ActorSystem
import akka.dispatch.Await
import akka.testkit.CallingThreadDispatcher
import akka.util.duration.intToDurationInt

class ReliableIOTest extends FunSuite
    with BeforeAndAfterAll
    with MockFactory
    with ProxyMockFactory
    with ShouldMatchers {

  import ReliableIO._
  import testingsupport.TestingTransportSupport._

  implicit val system = ActorSystem()
  val address = new InetSocketAddress(0)

  override def afterAll {
    system.shutdown
  }

  test("sending a protobuf Msg with Event") {
    val in = Array.ofDim[Byte](256)
    val ios = new ByteArrayInputStream(in)
    val oos = new ByteArrayOutputStream()

    val wrapper = mock[ConnectedSocketWrapper]
    wrapper expects 'outputStream returning oos once;
    wrapper expects 'inputStream returning ios once

    val socketFactory = mockFunction[SocketAddress, ConnectedSocketWrapper]
    socketFactory expects address returning wrapper once

    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
    implicitly[SendOff[EventPart, Reliable]].sendOff(conn, Write(event))

    val out = oos.toByteArray
    val outRef = protoMsgEvent.toByteArray
    new DataInputStream(new ByteArrayInputStream(out)).readInt should be === outRef.length
    out.slice(4, out.length) should be === outRef
  }

  test("sending a protobuf Msg with Event, with feedback") {
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
    val respFuture = implicitly[SendAndExpectFeedback[EventPart, Boolean, Reliable]].send(conn, Write(event))

    val out = oos.toByteArray
    val outRef = protoMsgEvent.toByteArray
    new DataInputStream(new ByteArrayInputStream(out)).readInt should be === outRef.length
    out.slice(4, out.length) should be === outRef

    val resp = Await.result(respFuture, 1 second)
    resp should be === true
  }

  test("sending a protobuf Msg with Query, with feedback") {
    val response = Proto.Msg.newBuilder.addEvents(protoEvent).addEvents(protoEvent2).setOk(true).build
    val responseBytes = response.toByteArray

    val responseBuilder = new ByteArrayOutputStream()
    val responseBuilderData = new DataOutputStream(responseBuilder)
    responseBuilderData.writeInt(responseBytes.length)
    responseBuilderData.write(responseBytes)

    val oos = new ByteArrayOutputStream()

    val wrapper = mock[ConnectedSocketWrapper]
    wrapper expects 'outputStream returning oos once;
    wrapper expects 'inputStream returning new ByteArrayInputStream(responseBuilder.toByteArray) once

    val socketFactory = mockFunction[SocketAddress, ConnectedSocketWrapper]
    socketFactory expects address returning wrapper once

    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
    val respFuture = implicitly[SendAndExpectFeedback[Query, Iterable[EventPart], Reliable]].send(conn, Write(Query("true")))

    val out = oos.toByteArray
    val queryData = protoMsgQuery.toByteArray
    new DataInputStream(new ByteArrayInputStream(out)).readInt should be === queryData.length
    out.slice(4, out.length) should be === queryData

    val resp = Await.result(respFuture, 1 second)
    resp should be === Seq(event, event2)
  }
}