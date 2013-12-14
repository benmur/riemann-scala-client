package net.benmur.riemann.client.testingsupport

import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import org.scalatest.Suite
import com.typesafe.config.ConfigFactory

trait ImplicitActorSystem extends BeforeAndAfterAll {
  self: Suite =>

  implicit val system = ActorSystem("test")

  implicit val context = system.dispatcher

  override def afterAll {
    super.afterAll
    system.shutdown
  }
}