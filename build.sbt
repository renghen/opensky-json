name := """opensky-json"""
organization := "com.renghen"
maintainer := "renghen@gmail.com"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.11"

libraryDependencies ++= Seq(
  guice,
  "com.beachape" %% "enumeratum" % "1.7.2",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.15",
  "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "4.0.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
)
