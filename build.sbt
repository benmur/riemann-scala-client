name := "riemann-scala-client"

version := "0.1"

scalaVersion := "2.9.2"

scalacOptions += "-deprecation"

resolvers += "Clojars" at "http://clojars.org/repo"

resolvers += "Akka" at "http://repo.akka.io/releases"

libraryDependencies += "com.aphyr" % "riemann-java-client" % "0.0.6"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.4"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "latest.integration"

libraryDependencies += "com.typesafe.akka" % "akka-testkit" % "2.0.4" % "test"
