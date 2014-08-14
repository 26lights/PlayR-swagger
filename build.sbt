name := "playr-swagger"

scalaVersion := "2.11.2"

releaseSettings

organization := "26lights"

resolvers += "26Lights snapshots" at "http://build.26source.org/nexus/content/repositories/public-snapshots"

resolvers += "26Lights releases" at "http://build.26source.org/nexus/content/repositories/public-releases"

libraryDependencies ++= Seq (
  "26lights"  %% "playr"  % "0.4.0-SNAPSHOT"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
