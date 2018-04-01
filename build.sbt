organization := "com.inno.sierra"

name := "SierraBot"

version := "0.1"

scalaVersion := "2.12.4"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "info.mukel" %% "telegrambot4s" % "3.0.14",
  "org.squeryl" %% "squeryl" % "0.9.5-7",
  "com.h2database" % "h2" % "1.4.196",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)