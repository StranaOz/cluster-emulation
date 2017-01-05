name := "cluster-emulation"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {

  Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.14",
    "com.typesafe.akka" %% "akka-http" % "10.0.0",
    "com.iheart" %% "ficus" % "1.4.0",
    "io.dropwizard.metrics" % "metrics-core" % "3.1.2",
    "com.typesafe.akka" %% "akka-remote" % "2.4.16"
  )
}
