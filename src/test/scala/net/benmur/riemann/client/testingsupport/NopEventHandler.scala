package net.benmur.riemann.client.testingsupport

import akka.actor.Actor
import akka.actor.actorRef2Scala
import akka.event.Logging.InitializeLogger
import akka.event.Logging.LoggerInitialized

class NopEventHandler extends Actor {
  def receive: Receive = {
    case InitializeLogger(_) => sender ! LoggerInitialized
  }
}