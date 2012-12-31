package net.benmur.riemann.client.testingsupport
import scala.collection.JavaConversions.asJavaIterable

import com.aphyr.riemann.Proto

import net.benmur.riemann.client.EventPart

object SerializersFixture {
  val event1 = EventPart(host = Some("server"), service = Some("service-name"),
    state = Some("ok"), time = Some(1234L), description = Some("descript"),
    tags = Array("tag1", "tag2"), metric = Some(112L), ttl = Some(10L))

  val event2 = EventPart(host = Some("server2"), service = Some("service-name2"),
    state = Some("crit"), time = Some(12340L), description = Some("descript2"),
    tags = Array("tag3"), metric = Some(1120L), ttl = Some(100L))

  val protobufEvent1 = Proto.Event.newBuilder
    .setHost("server")
    .setService("service-name")
    .setState("ok")
    .setTime(1234L)
    .addAllTags(Seq("tag1", "tag2"))
    .setTtl(10)
    .setMetricSint64(112)
    .setDescription("descript")

  val protobufEvent2 = Proto.Event.newBuilder
    .setHost("server2")
    .setService("service-name2")
    .setState("crit")
    .setTime(12340L)
    .addAllTags(Seq("tag3"))
    .setTtl(100)
    .setMetricSint64(1120)
    .setDescription("descript2")
}