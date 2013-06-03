name := "riemann-scala-client"

organization := "net.benmur"

version := "0.2"

scalaVersion := "2.9.2"

scalacOptions += "-deprecation"

resolvers += "Clojars" at "http://clojars.org/repo"

resolvers += "Akka" at "http://repo.akka.io/releases"

libraryDependencies += "com.aphyr" % "riemann-java-client" % "0.0.6"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.4"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "latest.integration" % "test"

libraryDependencies += "com.typesafe.akka" % "akka-testkit" % "2.0.4" % "test"

publishMavenStyle := true

publishArtifact in Test := false

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/benmur/riemann-scala-client/</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>http://opensource.org/licenses/MIT</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:benmur/riemann-scala-client.git</url>
    <connection>scm:git:git@github.com:benmur/riemann-scala-client.git</connection>
  </scm>
  <developers>
    <developer>
      <id>benmur</id>
      <name>Rached Ben Mustapha</name>
      <url>http://benmur.net/</url>
    </developer>
  </developers>)

