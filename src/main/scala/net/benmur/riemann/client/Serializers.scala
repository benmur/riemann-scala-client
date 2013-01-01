package net.benmur.riemann.client

import scala.collection.JavaConversions.{ asJavaIterable, iterableAsScalaIterable }

import com.aphyr.riemann.Proto

trait Serializers {
  def serializeQueryToProtoMsg(q: Query) = Proto.Msg.newBuilder
    .setQuery(Proto.Query.newBuilder().setString(q.q))
    .build

  def serializeEventPartToProtoMsg(e: EventPart) = serializeEventPartsToProtoMsg(EventSeq(e))

  def serializeEventPartsToProtoMsg(ei: EventSeq) = Proto.Msg.newBuilder
    .addAllEvents(ei.events map convertOneEventPart)
    .build

  def unserializeProtoMsg(m: Proto.Msg): Iterable[EventPart] = m.hasOk match {
    case true if m.getOk => m.getEventsList map convertProtoEventToEventPart toList
    case true            => throw RemoteError(m.getError)
    case false           => throw RemoteError("Response has no status")
  }

  private def convertOneEventPart(e: EventPart) = {
    val b = Proto.Event.newBuilder
    e.host foreach (b.setHost(_))
    e.service foreach (b.setService(_))
    e.state foreach (b.setState(_))
    e.time foreach (b.setTime(_))
    e.description foreach (b.setDescription(_))
    e.tags foreach (b.addTags(_))
    e.metric foreach (_ match {
      case value: Long   => b.setMetricSint64(value)
      case value: Double => b.setMetricD(value)
      case value: Float  => b.setMetricF(value)
      case v             => System.err.println("Warning: don't know what to do with value " + v)
    })
    e.ttl foreach (b.setTtl(_))
    b.build
  }

  private def convertProtoEventToEventPart(e: Proto.Event) = EventPart(
    host = if (e.hasHost) Some(e.getHost()) else None,
    service = if (e.hasService) Some(e.getService) else None,
    state = if (e.hasState) Some(e.getState) else None,
    time = if (e.hasTime) Some(e.getTime) else None,
    description = if (e.hasDescription) Some(e.getDescription) else None,
    tags = if (e.getTagsList.isEmpty) List() else e.getTagsList.toList,
    metric = extractMetric(e),
    ttl = if (e.hasTtl) Some(e.getTtl) else None)

  private def extractMetric(e: Proto.Event) =
    if (e.hasMetricD)
      Some(e.getMetricD)
    else if (e.hasMetricF)
      Some(e.getMetricF)
    else if (e.hasMetricSint64)
      Some(e.getMetricSint64)
    else None
}

object Serializers extends Serializers
