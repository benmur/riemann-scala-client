package net.benmur.riemann.client

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream }
import java.net.SocketAddress
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FunSuite}
import org.scalatest.matchers.ShouldMatchers
import com.aphyr.riemann.Proto
import akka.actor.ActorSystem
import akka.testkit.CallingThreadDispatcher
import java.io.InputStream
import java.net.SocketException
import scala.concurrent.duration._
import scala.concurrent.Await

class ReliableIOTest extends FunSuite
    with testingsupport.ImplicitActorSystem
    with MockFactory
    with ShouldMatchers {

  import ReliableIO._
  import testingsupport.TestingTransportSupport._

  test("sending a protobuf Msg with Event") {
    val in = Array.ofDim[Byte](256)
    val ios = new ByteArrayInputStream(in)
    val oos = new ByteArrayOutputStream()

    val wrapper = mock[Reliable.SocketWrapper]
    (wrapper.outputStream _).expects().returning(oos).once()
    (wrapper.inputStream _).expects().returning(ios).once()

    val socketFactory = mockFunction[SocketAddress, Reliable.SocketWrapper]
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

    val wrapper = mock[Reliable.SocketWrapper]
    (wrapper.outputStream _).expects().returning(oos).once()
    (wrapper.inputStream _).expects().returning(new ByteArrayInputStream(outBuilder.toByteArray)).once()

    val socketFactory = mockFunction[SocketAddress, Reliable.SocketWrapper]
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

    val wrapper = mock[Reliable.SocketWrapper]
    (wrapper.outputStream _).expects().returning(oos).once()
    (wrapper.inputStream _).expects().returning(new ByteArrayInputStream(responseBuilder.toByteArray)).once()

    val socketFactory = mockFunction[SocketAddress, Reliable.SocketWrapper]
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

  test("reconnect in case of SocketException while reading") {
    val inputStream = new InputStream {
      override def read = throw new SocketException
    }

    val os = new ByteArrayOutputStream

    val wrapper = mock[Reliable.SocketWrapper]
    (wrapper.inputStream _).expects().returning(inputStream).twice()
    (wrapper.outputStream _).expects().returning(os).twice()

    val socketFactory = mockFunction[SocketAddress, Reliable.SocketWrapper]
    socketFactory expects address returning wrapper twice

    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))

    // TODO need to test that automatic resending works (two messages should be sent instead of one)
    implicitly[SendAndExpectFeedback[Query, Iterable[EventPart], Reliable]].send(conn, Write(Query("true")))

    implicitly[SendAndExpectFeedback[Query, Iterable[EventPart], Reliable]].send(conn, Write(Query("true")))

    val queryData = protoMsgQuery.toByteArray
    val out = os.toByteArray
    val is = new ByteArrayInputStream(out)
    val dis = new DataInputStream(is)

    dis.readInt should be === queryData.length
    val msg1 = Array.ofDim[Byte](queryData.length)
    dis.readFully(msg1)
    msg1 should be === queryData

    // 2 messages were written because it crashed during the 1st response read, after writing
    dis.readInt should be === queryData.length
    val msg2 = Array.ofDim[Byte](queryData.length)
    dis.readFully(msg2)
    msg2 should be === queryData
  }

//  test("reconnect in case of SocketException while connecting") {
//    val wrapper = mock[Reliable.SocketWrapper]
//    val socketFactory = mockFunction[SocketAddress, Reliable.SocketWrapper]
//
//    val os = new ByteArrayOutputStream
//
//    socketFactory expects address throwing new SocketException once()
//    socketFactory expects address returning wrapper once()
//    (wrapper.inputStream _).expects().returning(new ByteArrayInputStream(Array.ofDim[Byte](0))).once()
//    (wrapper.outputStream _).expects().returning(os).once()
//
//    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
//
//    // TODO need to test that automatic resending works (two messages should be sent instead of one)
//    implicitly[SendAndExpectFeedback[Query, Iterable[EventPart], Reliable]].send(conn, Write(Query("true")))
//
//    implicitly[SendAndExpectFeedback[Query, Iterable[EventPart], Reliable]].send(conn, Write(Query("true")))
//
//    val out = os.toByteArray
//    val queryData = protoMsgQuery.toByteArray
//    new DataInputStream(new ByteArrayInputStream(out)).readInt should be === queryData.length
//    out.slice(4, out.length) should be === queryData
//  }
}