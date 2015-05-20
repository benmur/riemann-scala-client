package net.benmur.riemann.client

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.net.SocketAddress
import java.net.SocketException

import scala.annotation.implicitNotFound
import scala.collection.mutable.WrappedArray
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import org.scalatest.Matchers

import com.aphyr.riemann.Proto

import ReliableIO.Reliable
import ReliableIO.ReliableEventPartSendAndExpectFeedback
import ReliableIO.ReliableQuerySendAndExpectFeedback
import ReliableIO.ReliableSendOff
import ReliableIO.TwoWayConnectionBuilder
import akka.testkit.CallingThreadDispatcher
import testingsupport.TestingTransportSupport.address
import testingsupport.TestingTransportSupport.event
import testingsupport.TestingTransportSupport.event2
import testingsupport.TestingTransportSupport.protoEvent
import testingsupport.TestingTransportSupport.protoEvent2
import testingsupport.TestingTransportSupport.protoMsgEvent
import testingsupport.TestingTransportSupport.protoMsgQuery
import testingsupport.TestingTransportSupport.timeout

class ReliableIOTest extends FunSuite
  with testingsupport.ImplicitActorSystem
  with MockFactory
  with Matchers {

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
    socketFactory.expects(address).returning(wrapper).once()

    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
    implicitly[SendOff[EventPart, Reliable]].sendOff(conn, Write(event))
    system.shutdown
    system.awaitTermination

    val out = oos.toByteArray
    val outRef = protoMsgEvent.toByteArray
    new DataInputStream(new ByteArrayInputStream(out)).readInt should === (outRef.length)
    out.slice(4, out.length) should === (outRef)
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
    socketFactory.expects(address).returning(wrapper).once()

    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
    val respFuture = implicitly[SendAndExpectFeedback[EventPart, Boolean, Reliable]].
      send(conn, Write(event), timeout, ec)
    system.shutdown
    system.awaitTermination

    val out = oos.toByteArray
    val outRef = protoMsgEvent.toByteArray
    new DataInputStream(new ByteArrayInputStream(out)).readInt should === (outRef.length)
    out.slice(4, out.length) should === (outRef)

    val resp = Await.result(respFuture, 1.second)
    resp should === (true)
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
    socketFactory.expects(address).returning(wrapper).once()

    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))
    val respFuture = implicitly[SendAndExpectFeedback[Query, Iterable[EventPart], Reliable]].
      send(conn, Write(Query("true")), timeout, ec)
    system.shutdown
    system.awaitTermination

    val out = oos.toByteArray
    val queryData = protoMsgQuery.toByteArray
    new DataInputStream(new ByteArrayInputStream(out)).readInt should === (queryData.length)
    out.slice(4, out.length) should === (queryData)

    val resp = Await.result(respFuture, 1.second)
    resp should === (Seq(event, event2))
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
    socketFactory.expects(address).returning(wrapper).twice()

    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))

    // TODO need to test that automatic resending works (two messages should be sent instead of one)
    implicitly[SendAndExpectFeedback[Query, Iterable[EventPart], Reliable]].
      send(conn, Write(Query("true")), timeout, ec)

    implicitly[SendAndExpectFeedback[Query, Iterable[EventPart], Reliable]].
      send(conn, Write(Query("true")), timeout, ec)
    system.shutdown
    system.awaitTermination

    val queryData = protoMsgQuery.toByteArray
    val out = os.toByteArray
    val is = new ByteArrayInputStream(out)
    val dis = new DataInputStream(is)

    dis.readInt should === (queryData.length)
    val msg1 = Array.ofDim[Byte](queryData.length)
    dis.readFully(msg1)
    msg1 should === (queryData)

    // 2 messages were written because it crashed during the 1st response read, after writing
    dis.readInt should === (queryData.length)
    val msg2 = Array.ofDim[Byte](queryData.length)
    dis.readFully(msg2)
    msg2 should === (queryData)
  }

  test("reconnect in case of SocketException while connecting") {
    val wrapper = mock[Reliable.SocketWrapper]
    val socketFactory = mockFunction[SocketAddress, Reliable.SocketWrapper]

    val os = new ByteArrayOutputStream

    socketFactory expects address throwing new SocketException once ()
    socketFactory expects address returning wrapper once ()
    (wrapper.inputStream _).expects().returning(new ByteArrayInputStream(Array.ofDim[Byte](0))).once()
    (wrapper.outputStream _).expects().returning(os).once()

    val conn = implicitly[ConnectionBuilder[Reliable]].buildConnection(address, Some(socketFactory), Some(CallingThreadDispatcher.Id))

    // TODO need to test that automatic resending works (two messages should be sent instead of one)
    implicitly[SendAndExpectFeedback[Query, Iterable[EventPart], Reliable]].
      send(conn, Write(Query("true")), timeout, ec)

    implicitly[SendAndExpectFeedback[Query, Iterable[EventPart], Reliable]].
      send(conn, Write(Query("true")), timeout, ec)

    system.shutdown
    system.awaitTermination

    val out = os.toByteArray
    val queryData = protoMsgQuery.toByteArray
    new DataInputStream(new ByteArrayInputStream(out)).readInt should === (queryData.length)
    out.slice(4, out.length) should === (queryData)
  }
}
