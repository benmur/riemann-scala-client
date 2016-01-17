# riemann-scala-client

Scala client library for sending events to [Riemann](http://riemann.io/), featuring strong typing, asynchronous API (using [Akka](http://akka.io/) under the hood) and a DSL to avoid cluttering the application codebase with metrics-related code.

[![Build Status](https://api.travis-ci.org/benmur/riemann-scala-client.png)](https://travis-ci.org/benmur/riemann-scala-client/)

* Current Stable version: 0.4.0 for scala 2.11 and scala 2.10, on master.
* Old Stable version: 0.3.4 for scala 2.11 and scala 2.10, on master.
* Current Stable version: 0.3.2 for scala 2.10 on the v0.3-scala210 branch.
* Previous Stable version: 0.2.1 for scala 2.9 on the v0.2-scala29 branch.

## Usage

### Build System
In build.sbt (scala 2.11.7):
```
# riemann-java-client comes from clojars.org
resolvers += "clojars.org" at "http://clojars.org/repo"

libraryDependencies += "net.benmur" %% "riemann-scala-client" % "0.4.0"
```

Or in pom.xml if you are using maven:
```xml
<dependency>
  <groupId>net.benmur</groupId>
  <artifactId>riemann-scala-client_2.11</artifactId>
  <version>0.4.0</version>
</dependency>
```
```xml
<dependency>
  <groupId>net.benmur</groupId>
  <artifactId>riemann-scala-client_2.10</artifactId>
  <version>0.4.0</version>
</dependency>
```

### Minimum viable use case
The imports list is somewhat longer than it used to be, because Scala 2.11 became more picky about choosing implicits (having both `Reliable` and `Unreliable` variants of `SendOff` in scope makes it unable to choose either one).

Issue #10 is open about simplifying imports again.

```scala
import net.benmur.riemann.client.RiemannClient.{riemannConnectAs, Unreliable}
import net.benmur.riemann.client.UnreliableIO._
import net.benmur.riemann.client.EventSenderDSL._
import net.benmur.riemann.client.EventDSL._

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import java.net.InetSocketAddress

object RiemannSendTest extends App {
    implicit val system = ActorSystem()
    implicit val timeout = Timeout(5.seconds)

    val metrics = riemannConnectAs[Unreliable] to new InetSocketAddress("localhost", 5555)
    service("service name") | state("warning") |>> metrics
}
```

Change `Unreliable` to `Reliable` and `UnreliableIO` to `ReliableIO` as needed, which will make the `|><` sending operation (returning a Future) available.

### Connecting

```scala
val tcpDestination = riemannConnectAs[Reliable] to new InetSocketAddress("localhost", 5555)
val udpDestination = riemannConnectAs[Unreliable] to new InetSocketAddress("localhost", 5555)
```

Please note that operations returning a Future won't compile if the connection is created with an `Unreliable` type parameter, this is intentional. (Well, it will compile if you have an implicit in scope implementing `SendAndExpectFeedback[_, _, Unreliable]`).

### Building events

Building an event is done by combining event parts. Each part is optional, as per the [Protocol Buffers definition](https://github.com/aphyr/riemann-java-client/blob/master/src/main/proto/riemann/proto.proto). Here is how to build a completely populated event:
```scala
val event = oneEvent() | host("hostname") | service("service xyz") | state("warning") | time(1234L) | 
            description("metric is way too high") | tags("performance", "slow", "provider-xyz") | 
            metric(0.8f) | ttl(120L)

// which is the same as (but with less intermediate objects instanciated):
val event2 = EventPart(host=Some("hostname"), service=Some("service xyz"), state=Some("warning"),
             description=Some("metric is way too high"), time=Some(1234L),
             tags=Seq("performance", "slow", "provider-xyz"), metric=Some(0.8f), ttl=Some(120L))
```

### Setting default event values
```scala
// given these declarations:
val destination = riemannConnectAs[Reliable] to new InetSocketAddress("localhost", 5555)
val destinationWithDefaults = destination withValues(host("host") | service("service response time"))

// this:
state("warning") | metric(0.5) |>> destinationWithDefaults
// is the same as:
host("host") | service("service response time") | state("warning") | metric(0.5) |>> destination

// implementing a counter or heartbeat is easy:
val signupCounter = destination withValues(service("Successful user registration"))
oneEvent() |>> signupCounter
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

- [Akka](http://akka.io/) 2.3.11
- [riemann-java-client](https://github.com/aphyr/riemann-java-client) for the Protocol Buffers implementation only

## Status

Pull requests are very welcome.

This version is intended to work with Scala 2.11 and Akka 2.3.

Care has been taken to be as reliable as possible, because sending metrics should not impact your application's stability. In particular:
- Unit test coverage is fairly good. No metrics are available yet, but the only code not tested is the actual socket code (which amounts to a total of 5 lines), for which the different conditions are mocked.
- All API-visible data structures are immutable and concurrency-friendly
- Network writes are serialized through Akka actors
- Exceptions are ignored silently (only logged to the akka event bus)
- Failed connections are retried at most twice per second

Please see next milestone's [open issues list](https://github.com/benmur/riemann-scala-client/issues?milestone=1&state=open) for items pending implementation.

## Authors/Licensing

- (c) 2012-2015 Rached Ben Mustapha <rached@benmur.net>
- licensed under the MIT license, please see the LICENSE file for details.
- thanks to Kyle Kingsbury for Riemann and riemann-java-client
- thanks to Pavel Minchenkov who started the scala 2.10 port
- thanks to Michael Allman for the scala 2.11 port, TCP reconnection improvements, float values roundtripping fixes and an UDP connection fix
- thanks to Matt Sullivan who started the akka 2.3 move
- thanks to github.com/janlisse for event attributes support
