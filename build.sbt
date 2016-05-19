organization := "com.productfoundry"

name := "akka-persistence-couchbase"

version := "0.3.3"

scalaVersion := "2.11.8"

fork in Test := true

javaOptions in Test += "-Xmx512M"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

parallelExecution in Test := false

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization := Some("productfoundry")

val akkaVer = "2.4.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence" % akkaVer,
  "com.couchbase.client" % "java-client" % "2.2.6",
  "commons-codec" % "commons-codec" % "1.10",
  "com.typesafe.akka" %% "akka-persistence-tck" % akkaVer % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
