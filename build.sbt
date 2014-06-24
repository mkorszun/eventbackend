import com.typesafe.sbt.SbtStartScript

seq(SbtStartScript.startScriptForClassesSettings: _*)

name := """event_backend"""

version := "1.0"

scalaVersion := "2.10.0"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "spray nightlies" at "http://nightlies.spray.io"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor"       % "2.2.3",
  "com.typesafe.akka"  %% "akka-slf4j"       % "2.2.3",
  "ch.qos.logback"      % "logback-classic"  % "1.0.13",
  "io.spray"            % "spray-can"        % "1.2-RC2",
  "io.spray"            % "spray-routing"    % "1.2-RC2",
  "io.spray"           %% "spray-json"       % "1.2.3",
  "org.specs2"         %% "specs2"           % "1.14"         % "test",
  "io.spray"            % "spray-testkit"    % "1.2-RC2" % "test",
  "com.typesafe.akka"  %% "akka-testkit"     % "2.2.3"        % "test",
  "com.novocode"        % "junit-interface"  % "0.7"          %
"test->default"
)

libraryDependencies += "org.mongodb" %% "casbah" % "2.7.2"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
