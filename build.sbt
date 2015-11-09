name := "riemann-scala-client"

organization := "net.benmur"

version := "0.4.0-SNAPSHOT"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.6", "2.11.7")

scalacOptions ++= List("-deprecation", "-feature", "-unchecked")

resolvers += "Clojars" at "http://clojars.org/repo"

resolvers += Resolver.typesafeRepo("releases")

val akkaVersion = "2.3.11"

libraryDependencies += "com.aphyr" % "riemann-java-client" % "0.2.9"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"

parallelExecution in Test := false

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

