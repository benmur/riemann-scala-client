package net.benmur.riemann.client

trait EventDSL {
  def mergeEvents(e: EventPart, overlay: EventPart) = EventPart(
    overlay.host orElse e.host,
    overlay.service orElse e.service,
    overlay.state orElse e.state,
    overlay.time orElse e.time,
    overlay.description orElse e.description,
    (overlay.tags.toSet ++ e.tags).toSeq.sorted,
    overlay.attributes ++ e.attributes,
    overlay.metric orElse e.metric,
    overlay.ttl orElse e.ttl)

  implicit class EventPartCombinator(e: EventPart) {
    def |(overlay: EventPart) = mergeEvents(e, overlay)
  }

  def oneEvent() = EventPart()
  def host(s: String) = EventPart(host = Some(s))
  def service(s: String) = EventPart(service = Some(s))
  def state(s: String) = EventPart(state = Some(s))
  def time(l: Long) = EventPart(time = Some(l))
  def description(s: String) = EventPart(description = Some(s))
  def tags(s: String*) = EventPart(tags = s)
  def attributes(a: (String, String)*) = EventPart(attributes = a.toMap)
  def metric(m: Long) = EventPart(metric = Some(m))
  def metric(m: Float) = EventPart(metric = Some(m))
  def metric(m: Double) = EventPart(metric = Some(m))
  def ttl(f: Float) = EventPart(ttl = Some(f))
}

object EventDSL extends EventDSL
