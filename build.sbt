name := "riemann-scala-client"

organization := "net.benmur"

version := "0.3.0"

scalaVersion := "2.10.3"

scalacOptions ++= List("-deprecation", "-feature", "-unchecked")

resolvers += "Clojars" at "http://clojars.org/repo"

resolvers += "Akka" at "http://repo.akka.io/releases"

libraryDependencies += "com.aphyr" % "riemann-java-client" % "0.2.8"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.3"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0.M5b" % "test"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test"

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

