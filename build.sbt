name := "playr-swagger"

organization := "26lights"

scalaVersion := "2.11.2"

resolvers += "26Lights snapshots" at "http://build.26source.org/nexus/content/repositories/public-snapshots"

resolvers += "26Lights releases" at "http://build.26source.org/nexus/content/repositories/public-releases"

libraryDependencies ++= Seq (
  "26lights"    %% "playr"      % "0.4.2",
  "org.webjars"  % "swagger-ui" % "2.0.22"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
