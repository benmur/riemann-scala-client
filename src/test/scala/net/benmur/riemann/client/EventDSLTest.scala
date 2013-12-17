package net.benmur.riemann.client

import org.scalatest.FunSuite

import EventDSL.description
import EventDSL.eventPartToEventPartCombinator
import EventDSL.host
import EventDSL.metric
import EventDSL.oneEvent
import EventDSL.service
import EventDSL.state
import EventDSL.time
import EventDSL.ttl

class EventDSLTest extends FunSuite {
  import EventDSL._

  test("provide an EventPart builder function for host") {
    expectResult(EventPart(host = Some("h"))) {
      host("h")
    }
  }

  test("provide an EventPart builder function for empty events") {
    expectResult(EventPart()) {
      oneEvent()
    }
  }

  test("provide an EventPart builder function for service") {
    expectResult(EventPart(service = Some("se"))) {
      service("se")
    }
  }

  test("provide an EventPart builder function for state") {
    expectResult(EventPart(state = Some("st"))) {
      state("st")
    }
  }

  test("provide an EventPart builder function for time") {
    expectResult(EventPart(time = Some(1234L))) {
      time(1234L)
    }
  }

  test("provide an EventPart builder function for description") {
    expectResult(EventPart(description = Some("d"))) {
      description("d")
    }
  }

  test("provide an EventPart builder function for tags") {
    expectResult(EventPart(tags = Array("t1", "t2"))) {
      EventDSL.tags("t1", "t2")
    }
  }

  test("provide an EventPart builder function for metric (float)") {
    expectResult(EventPart(metric = Some(1.12f))) {
      metric(1.12f)
    }
  }

  test("provide an EventPart builder function for metric (double)") {
    expectResult(EventPart(metric = Some(1.12))) {
      metric(1.12)
    }
  }

  test("provide an EventPart builder function for metric (long)") {
    expectResult(EventPart(metric = Some(112L))) {
      metric(112L)
    }
  }

  test("provide an EventPart builder function for ttl") {
    expectResult(EventPart(ttl = Some(10))) {
      ttl(10)
    }
  }

  test("EventParts combination merges tags") {
    expectResult(EventPart(tags = Array("tag1", "tag2", "tag3"))) {
      EventDSL.tags("tag1", "tag2") | EventDSL.tags() | EventDSL.tags("tag3") | EventDSL.tags("tag1")
    }
  }

  test("provide a method to combine EventParts") {
    val expected = EventPart(host = Some("server"), service = Some("service-name"),
      state = Some("ok"), time = Some(1234L), description = Some("descript"),
      tags = Array("tag1", "tag2"), metric = Some(112L), ttl = Some(10L))

    expectResult(expected) {
      ttl(10) | metric(112) | EventDSL.tags("tag1", "tag2") | description("descript") |
        time(1234L) | state("discarded-state") | state("ok") | service("service-name") |
        host("server")
    }
  }
}