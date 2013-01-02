# riemann-scala-client

Scala client library for sending events to [Riemann](http://riemann.io/), featuring strong typing, asynchronous API (using [Akka](http://akka.io/) under the hood) and a DSL to avoid cluttering the application codebase with metrics-related code.

[![Build Status](https://api.travis-ci.org/benmur/riemann-scala-client.png)](https://travis-ci.org/benmur/riemann-scala-client/)

## Usage

### Minimum viable use case
```scala
import net.benmur.riemann.client._
import RiemannClient._

val metrics = riemannConnectAs[Unreliable] to new InetSocketAddress("localhost", 5555)
service("service name") | state("warning") |>> metrics
```

### Imports

The client relies heavily on implicits, here are the needed ones in scope:
```scala
import net.benmur.riemann.client._
import RiemannClient._

implicit val system = ActorSystem()
implicit val timeout = Timeout(5 seconds)
```

### Connecting

```scala
val tcpDestination = riemannConnectAs[Reliable] to new InetSocketAddress("localhost", 5555)
val udpDestination = riemannConnectAs[Unreliable] to new InetSocketAddress("localhost", 5555)
```

Please note that operations returning a Future won't compile if the connection is created with an `Unreliable` type parameter, this is intentional. (Well, it will compile if you have an implicit in scope implementing `SendAndExpectFeedback[_, _, Unreliable]`).

### Building events

Building an event is done by combining event parts. Each part is optional, as per the [Protocol Buffers definition](https://github.com/aphyr/riemann-java-client/blob/master/src/main/proto/riemann/proto.proto). Here is how to build a completely populated event:
```scala
val event = host("hostname") | service("service xyz") | state("warning") | time(1234L) | 
            description("metric is way too high") | tags("performance", "slow", "provider-xyz") | 
            metric(0.8f) | ttl(120L)

// which is the same as (but with less intermediate objects instanciated):
val event2 = EventPart(host=Some("hostname"), service=Some("service xyz"), state=Some("warning"),
             description=Some("metric is way too high"), time=Some(1234L),
             tags=Seq("performance", "slow", "provider-xyz"), metric=Some(0.8f), ttl=Some(120L))
```

### Settings default event values
```scala
// given these declarations:
val destination = riemannConnectAs[Reliable] to new InetSocketAddress("localhost", 5555)
val destinationWithDefaults = destination withValues(host("host") | service("service response time"))

// this:
state("warning") | metric(0.5) |>> destinationWithDefaults
// is the same as:
host("host") | service("service response time") | state("warning") | metric(0.5) |>> destination
```

### Sending events: fire-and-forget mode (over UDP)
```scala
val metricsDestination = riemannConnectAs[Unreliable] to new
  InetSocketAddress("localhost", 5555) withValues(host("host") | service("service response time"))

state("warning") | metric(0.5) |>> metricsDestination
```

### Sending events: fire-and-forget mode (over TCP)
```scala
val metricsDestination = riemannConnectAs[Reliable] to new
  InetSocketAddress("localhost", 5555) withValues(host("host") | service("service response time"))

state("warning") | metric(0.5) |>> metricsDestination
```

### Sending events and waiting for a Future (over TCP)
```scala
val metricsDestination = riemannConnectAs[Reliable] to new
  InetSocketAddress("localhost", 5555) withValues(host("host") | service("service response time"))

state("warning") | metric(0.5) |>< metricsDestination onComplete {
  case Left(exception) => // ...
  case Right(false)    => println("not sent ok!!")
  case Right(true)     => println("sent ok")
}
```

### Sending a text query (over TCP)
```scala
val metricsDestination = riemannConnectAs[Reliable] to new InetSocketAddress("localhost", 5555)

Query("tagged \"slow\"") |>< metricsDestination onComplete {
  case Left(exception) => // ...
  case Right(events)   => events foreach println
}
```

Please note that operations returning a Future won't compile if the connection is created with an `Unreliable` type parameter, this is intentional. (Well, it will compile if you have an implicit in scope implementing `SendAndExpectFeedback[_, _, Unreliable]`).

## Dependencies

- [Akka](http://akka.io/) 2.0.4
- [riemann-java-client](https://github.com/aphyr/riemann-java-client) for the Protocol Buffers implementation only

## Status

Pull requests are very welcome.

This version is intended to work with Scala 2.9 and Akka 2.0. Support will be added for Scala 2.10 and Akka 2.1 when Scala 2.10 final is released.

Care has been taken to be as reliable as possible, because sending metrics should not impact your application's stability. In particular:
- Unit test coverage is fairly good. No metrics are available yet, but the only code not tested is the actual socket code (which amounts to a total of 5 lines), for which the different conditions are mocked.
- All API-visible data structures are immutable and concurrency-friendly
- Network writes are serialized through Akka actors
- Exceptions are ignored silently (only logged to the akka event bus)

Remaining items include :
- Add explicit unit tests for TCP reconnections (which already work thanks to Akka automatically respawning failed actors).
- Hybrid tcp/udp connection mode
- Retrying failed Writes after reconnecting (with a counter)
- Shutdown/closing

## Authors/Licensing

- (c) 2012 Rached Ben Mustapha <rached@benmur.net>
- licensed under the MIT license, please see the LICENSE file for details.
- thanks to Kyle Kingsbury for Riemann and riemann-java-client

