name := """confitura-reactive"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  ws,
  "org.webjars" % "jquery" % "2.1.4",
  "org.webjars" % "bootstrap" % "3.3.5",
  "org.webjars.bower" % "FlipClock" % "0.7.7",
  "org.webjars" % "toastr" % "2.1.0",
  "com.typesafe.akka" %% "akka-contrib" % "2.3.11",
  "com.google.guava" % "guava" % "12.0" force()
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

routesGenerator := InjectedRoutesGenerator

scalariformSettings

