package net.benmur.riemann.client.testingsupport

import scala.concurrent.ExecutionContext

import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem

trait ImplicitActorSystem extends BeforeAndAfterEach {
  self: Suite =>
  implicit var system: ActorSystem = _
  implicit var ec: ExecutionContext = _

  override def beforeEach = {
    super.beforeEach
    system = ActorSystem("test", config = ConfigFactory.parseString(
      """akka.loggers = [ "net.benmur.riemann.client.testingsupport.NopEventHandler" ]"""))
    ec = system.dispatcher
  }
}