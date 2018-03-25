organization := "com.inno.sierra"

name := "SierraBot"

version := "0.1"

scalaVersion := "2.12.4"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "info.mukel" %% "telegrambot4s" % "3.0.14",
  "org.squeryl" %% "squeryl" % "0.9.5-7",
  "com.h2database" % "h2" % "1.4.196"
)

// Create a default Scala style task to run with tests
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := scalastyle.in(Test).toTask("").value
(test in Test) := ((test in Test) dependsOn testScalastyle).value
(scalastyleConfig in Test) := baseDirectory.value / "project" /"scalastyle-test-config.xml"