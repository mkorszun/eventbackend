import com.typesafe.sbt.SbtStartScript
import sbt.Keys._

seq(SbtStartScript.startScriptForClassesSettings: _*)

name := """event_backend"""

version := "1.1"

scalaVersion := "2.11.2"

crossPaths := false

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "spray nightlies" at "http://nightlies.spray.io"

resolvers += DefaultMavenRepository

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.6",
    "com.typesafe.akka" %% "akka-slf4j" % "2.3.6",
    "io.spray" % "spray-can_2.11" % "1.3.2",
    "io.spray" % "spray-routing_2.11" % "1.3.2",
    "io.spray" % "spray-json_2.11" % "1.3.1",
    "ch.qos.logback" % "logback-classic" % "1.0.9"
)

libraryDependencies += "io.spray" % "spray-client_2.11" % "1.3.1"

libraryDependencies += "org.mongodb" %% "casbah" % "2.7.2"

libraryDependencies += "com.gettyimages" %% "spray-swagger" % "0.5.1"

libraryDependencies += "com.github.seratch" %% "awscala" % "0.5.+"

scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-Ywarn-dead-code",
    "-language:_",
    "-target:jvm-1.7",
    "-encoding", "UTF-8"
)
