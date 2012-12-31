# riemann-scala-client

Scala client for sending events to [Riemann](http://aphyr.github.com/riemann/).

## Usage

### Fire-and-forget mode (over UDP)
```scala
import RiemannClient._
val metricsDestination = riemannConnectAs[Unreliable] to new
  InetSocketAddress("localhost", 5555) withValues(host("myhost") | service("myservice response time"))

state("warning") | metric(0.5) |>> metricsDestination
```

### Fire-and-forget mode (over TCP)
```scala
import RiemannClient._
val metricsDestination = riemannConnectAs[Reliable] to new
  InetSocketAddress("localhost", 5555) withValues(host("myhost") | service("myservice response time"))

state("warning") | metric(0.5) |>> metricsDestination
```

### Sending and waiting for a Future (over TCP)
```scala
import RiemannClient._
val metricsDestination = riemannConnectAs[Reliable] to new
  InetSocketAddress("localhost", 5555) withValues(host("myhost") | service("myservice response time"))

state("warning") | metric(0.5) |>< metricsDestination onComplete {
  case Left(RemoteError(message)) => println("error: " + message)
  case Right(_) => println("sent ok")
}
```

### Sending a text query (over TCP)
```scala
import RiemannClient._
val metricsDestination = riemannConnectAs[Reliable] to new
  InetSocketAddress("localhost", 5555) withValues(host("myhost") | service("myservice response time"))

Query("true") |>< metricsDestination onComplete {
  case Left(RemoteError(message)) => println("error: " + message)
  case Right(events) => events foreach println
}
```

Please note that operations returning a Future won't compile if the you create the connection with an `Unreliable` type parameter, this is intentional. (Well, it will compile if you have an implicit in scope implementing `SendAndExpectFeedback[Unreliable]`).

## Dependencies

- Akka 2.0.4
- [riemann-java-client](https://github.com/aphyr/riemann-java-client) for the Protocol Buffers implementation only

## Status

This version is intended to work with Scala 2.9 and Akka 2.0. Support will be added for Scala 2.10 and Akka 2.1 when Scala 2.10 final is released.

Care has been taken to be as reliable as possible, because sending metrics should not impact your application’s stability. In particular:
- Unit test coverage is fairly good. No metrics are available yet, but the only code not tested is the actual socket code, for which the different conditions are mocked.
-	All API-visible data structures are immutable and concurrency-friendly
- Network writes are serialized through Akka actors
-	Exceptions are ignored silently (only logged to the akka event bus)

Remaining items include :
- Add explicit unit tests for TCP reconnections (which already work thanks to Akka automatically respawning failed actors).
-	Hybrid tcp/udp connection mode
- Retrying failed Writes after reconnecting (with a counter).

## Authors/Licensing

- (c) 2012 Rached Ben Mustapha <rached@benmur.net>
- licensed under the MIT license, please see the LICENSE file for details.
- thanks to Kyle Kingsbury for Riemann and riemann-java-client

