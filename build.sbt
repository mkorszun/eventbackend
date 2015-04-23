import com.typesafe.sbt.SbtStartScript

seq(SbtStartScript.startScriptForClassesSettings: _*)

name := """event_backend"""

version := "1.0"

scalaVersion := "2.11.2"

crossPaths := false

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "spray nightlies" at "http://nightlies.spray.io"

resolvers += DefaultMavenRepository

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor"          % "2.3.6",
  "com.typesafe.akka"  %% "akka-slf4j"          % "2.3.6",
  "io.spray"            % "spray-can_2.11"      % "1.3.2",
  "io.spray"            % "spray-routing_2.11"  % "1.3.2",
  "io.spray"            % "spray-json_2.11"     % "1.3.1"
)

libraryDependencies += "org.mongodb" %% "casbah" % "2.7.2"

libraryDependencies += "com.stormpath.sdk" % "stormpath-sdk-api" % "1.0.RC4.2"

libraryDependencies += "com.stormpath.sdk" % "stormpath-sdk-httpclient" % "1.0.RC4.2" exclude("org.apache.httpcomponents","httpclient")

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.4"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)
