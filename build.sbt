organization := "com.inno.sierra"

name := "SierraBot"

version := "0.1"

scalaVersion := "2.12.4"

herokuAppName in Compile := "innosierrabot"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "info.mukel" %% "telegrambot4s" % "3.0.14",
  "org.squeryl" %% "squeryl" % "0.9.5-7",
  "com.h2database" % "h2" % "1.4.196",
  "org.postgresql" % "postgresql" % "42.2.2",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe" % "config" % "1.3.2",
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
)

enablePlugins(JavaAppPackaging)
