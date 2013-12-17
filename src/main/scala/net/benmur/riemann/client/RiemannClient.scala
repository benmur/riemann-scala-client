package net.benmur.riemann.client

import scala.annotation.implicitNotFound

import akka.actor.ActorSystem
import akka.util.Timeout

object RiemannClient
  extends EventDSL
  with EventSenderDSL
  with ReliableIO
  with UnreliableIO
  with DestinationOps {

  def riemannConnectAs[T <: TransportType](implicit connectionBuilder: ConnectionBuilder[T], system: ActorSystem, timeout: Timeout): DestinationBuilder[T] =
    new DestinationBuilder[T](connectionBuilder)
}
