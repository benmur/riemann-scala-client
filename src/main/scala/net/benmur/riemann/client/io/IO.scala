package net.benmur.riemann.client.io

import java.util.concurrent.atomic.AtomicLong

object IO {
  private val nClients = new AtomicLong(0L)
  def clientName(actorNamePrefix: String) = actorNamePrefix + nClients.incrementAndGet
}