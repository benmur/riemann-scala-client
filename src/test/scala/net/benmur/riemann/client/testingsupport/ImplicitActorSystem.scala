package net.benmur.riemann.client.testingsupport

import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import org.scalatest.Suite

trait ImplicitActorSystem extends BeforeAndAfterAll {
  self: Suite =>

  implicit val system = ActorSystem()

  override def afterAll {
    super.afterAll
    system.shutdown
  }
}