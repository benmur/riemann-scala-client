package net.benmur.riemann.client

import scala.collection.JavaConversions.asJavaIterable

import org.scalatest.FunSuite

import com.aphyr.riemann.Proto

class SerializersTest extends FunSuite {
  import Serializers._
  import testingsupport.SerializersFixture._

  test("out: convert a full EventPart to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(protobufEvent1).build

    expect(expected) {
      serializeEventPartToProtoMsg(event1)
    }
  }

  test("out: convert an empty EventPart to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart())
    }
  }

  test("out: convert an EventPart with only host to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setHost("host")).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart(host = Some("host")))
    }
  }

  test("out: convert an EventPart with only service to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setService("service")).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart(service = Some("service")))
    }
  }

  test("out: convert an EventPart with only state to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setState("state")).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart(state = Some("state")))
    }
  }

  test("out: convert an EventPart with only time to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setTime(1234L)).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart(time = Some(1234L)))
    }
  }

  test("out: convert an EventPart with only description to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setDescription("description")).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart(description = Some("description")))
    }
  }

  test("out: convert an EventPart with only tags to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.addAllTags(List("tag1"))).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart(tags = List("tag1")))
    }
  }

  test("out: convert an EventPart with only metric (long) to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setMetricSint64(1234L)).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart(metric = Some(1234L)))
    }
  }

  test("out: convert an EventPart with only metric (double) to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setMetricD(1234.9)).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart(metric = Some(1234.9)))
    }
  }

  test("out: convert an EventPart with only metric (float) to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setMetricF(1234.9f)).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart(metric = Some(1234.9f)))
    }
  }

  test("out: convert an EventPart with only ttl to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setTtl(1234L)).build

    expect(expected) {
      serializeEventPartToProtoMsg(EventPart(ttl = Some(1234L)))
    }
  }

  test("out: convert an Iterable of full EventParts to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(protobufEvent1).addEvents(protobufEvent2).build

    expect(expected) {
      serializeEventPartsToProtoMsg(EventSeq(event1, event2))
    }
  }

  test("out: convert a Query to a protobuf Msg") {
    expect(Proto.Msg.newBuilder.setQuery(Proto.Query.newBuilder.setString("true")).build) {
      serializeQueryToProtoMsg(Query("true"))
    }
  }

  test("in: convert a protobuf Msg response with an ok status") {
    expect(Right(Nil)) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).build)
    }
  }

  test("in: convert a protobuf Msg response with a non-ok status and an error message") {
    expect(Left(RemoteError("meh"))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(false).setError("meh").build)
    }
  }

  test("in: convert a failed Query result from a protobuf Msg with events") {
    expect(Left(RemoteError("meh"))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(false).setError("meh").addEvents(protobufEvent1).build)
    }
  }

  test("in: convert a successful Query result from a protobuf Msg to multiple EventParts") {
    expect(Right(List(event1))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(protobufEvent1).build)
    }
  }

  test("in: convert Query result with missing ok from a protobuf Msg to RemoteError") {
    expect(Left(RemoteError("Response has no status"))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.addEvents(protobufEvent1).build)
    }
  }

  test("in: convert a protobuf Msg with empty Event to a Right(List(EventPart))") {
    expect(Right(List(EventPart()))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only host to a Right(List(EventPart))") {
    expect(Right(List(EventPart(host = Some("host"))))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setHost("host")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only service to a Right(List(EventPart))") {
    expect(Right(List(EventPart(service = Some("service"))))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setService("service")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only state to a Right(List(EventPart))") {
    expect(Right(List(EventPart(state = Some("state"))))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setState("state")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only time to a Right(List(EventPart))") {
    expect(Right(List(EventPart(time = Some(1234L))))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setTime(1234L)).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only description to a Right(List(EventPart))") {
    expect(Right(List(EventPart(description = Some("description"))))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setDescription("description")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only tags to a Right(List(EventPart))") {
    expect(Right(List(EventPart(tags = List("tag1", "tag2"))))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.addAllTags(List("tag1", "tag2"))).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only metric (long) to a Right(List(EventPart))") {
    expect(Right(List(EventPart(metric = Some(1234L))))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setMetricSint64(1234L)).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only metric (float) to a Right(List(EventPart))") {
    expect(Right(List(EventPart(metric = Some(1234.0f))))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setMetricF(1234.0f)).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only metric (double) to a Right(List(EventPart))") {
    expect(Right(List(EventPart(metric = Some(1234.1: Double))))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setMetricD(1234.1: Double)).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only ttl to a Right(List(EventPart))") {
    expect(Right(List(EventPart(ttl = Some(1234L))))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setTtl(1234L)).build)
    }
  }
}