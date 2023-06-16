name := """opensky-json"""
organization := "com.renghen"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.11"

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.15",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.renghen.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.renghen.binders._"
