package net.benmur.riemann.client

import scala.collection.JavaConversions.asJavaIterable
import org.scalatest.{ FunSuite}
import com.aphyr.riemann.Proto
import org.scalatest.matchers.ShouldMatchers

class SerializersTest extends FunSuite with ShouldMatchers {
  object Serializers extends Serializers
  import Serializers._
  import testingsupport.SerializersFixture._

  test("out: convert a full EventPart to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(protobufEvent1).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(event1)
    }
  }

  test("out: convert an empty EventPart to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart())
    }
  }

  test("out: convert an EventPart with only host to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setHost("host")).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart(host = Some("host")))
    }
  }

  test("out: convert an EventPart with only service to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setService("service")).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart(service = Some("service")))
    }
  }

  test("out: convert an EventPart with only state to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setState("state")).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart(state = Some("state")))
    }
  }

  test("out: convert an EventPart with only time to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setTime(1234L)).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart(time = Some(1234L)))
    }
  }

  test("out: convert an EventPart with only description to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setDescription("description")).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart(description = Some("description")))
    }
  }

  test("out: convert an EventPart with only tags to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.addAllTags(List("tag1"))).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart(tags = List("tag1")))
    }
  }

  test("out: convert an EventPart with only metric (long) to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setMetricSint64(1234L)).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart(metric = Some(1234L)))
    }
  }

  test("out: convert an EventPart with only metric (double) to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setMetricD(1234.9)).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart(metric = Some(1234.9)))
    }
  }

  test("out: convert an EventPart with only metric (float) to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setMetricF(1234.9f)).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart(metric = Some(1234.9f)))
    }
  }

  test("out: convert an EventPart with only ttl to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(
      Proto.Event.newBuilder.setTtl(1234L)).build

    expectResult(expected) {
      serializeEventPartToProtoMsg(EventPart(ttl = Some(1234L)))
    }
  }

  test("out: convert an Iterable of full EventParts to a protobuf Msg") {
    val expected = Proto.Msg.newBuilder.addEvents(protobufEvent1).addEvents(protobufEvent2).build

    expectResult(expected) {
      serializeEventPartsToProtoMsg(EventSeq(event1, event2))
    }
  }

  test("out: convert a Query to a protobuf Msg") {
    expectResult(Proto.Msg.newBuilder.setQuery(Proto.Query.newBuilder.setString("true")).build) {
      serializeQueryToProtoMsg(Query("true"))
    }
  }

  test("in: convert a protobuf Msg response with an ok status") {
    expectResult(Nil) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).build)
    }
  }

  test("in: convert a protobuf Msg response with a non-ok status and an error message") {
    val ex = intercept[RemoteError] {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(false).setError("meh").build)
    }
    ex.message should be === "meh"
  }

  test("in: convert a failed Query result from a protobuf Msg with events") {
    val ex = intercept[RemoteError] {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(false).setError("meh").addEvents(protobufEvent1).build)
    }
    ex.message should be === "meh"
  }

  test("in: convert a successful Query result from a protobuf Msg to multiple EventParts") {
    expectResult(List(event1)) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(protobufEvent1).build)
    }
  }

  test("in: convert a Query result with missing ok from a protobuf Msg to RemoteError") {
    val ex = intercept[RemoteError] {
      unserializeProtoMsg(Proto.Msg.newBuilder.addEvents(protobufEvent1).build)
    }
    ex.message should be === "Response has no status"
  }

  test("in: convert a protobuf Msg with empty Event to a List(EventPart)") {
    expectResult(List(EventPart())) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only host to a List(EventPart)") {
    expectResult(List(EventPart(host = Some("host")))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setHost("host")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only service to a List(EventPart)") {
    expectResult(List(EventPart(service = Some("service")))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setService("service")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only state to a List(EventPart)") {
    expectResult(List(EventPart(state = Some("state")))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setState("state")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only time to a List(EventPart)") {
    expectResult(List(EventPart(time = Some(1234L)))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setTime(1234L)).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only description to a List(EventPart)") {
    expectResult(List(EventPart(description = Some("description")))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setDescription("description")).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only tags to a List(EventPart)") {
    expectResult(List(EventPart(tags = List("tag1", "tag2")))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.addAllTags(List("tag1", "tag2"))).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only metric (long) to a List(EventPart)") {
    expectResult(List(EventPart(metric = Some(1234L)))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setMetricSint64(1234L)).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only metric (float) to a List(EventPart)") {
    expectResult(List(EventPart(metric = Some(1234.0f)))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setMetricF(1234.0f)).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only metric (double) to a List(EventPart)") {
    expectResult(List(EventPart(metric = Some(1234.1: Double)))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setMetricD(1234.1: Double)).build)
    }
  }

  test("in: convert a protobuf Msg with Event with only ttl to a List(EventPart)") {
    expectResult(List(EventPart(ttl = Some(1234L)))) {
      unserializeProtoMsg(Proto.Msg.newBuilder.setOk(true).addEvents(
        Proto.Event.newBuilder.setTtl(1234L)).build)
    }
  }
}