name := "playr-swagger"

organization := "26lights"

scalaVersion := "2.11.12"

resolvers += "local-repo" at sys.env.get("DIST_PATH").map(file).getOrElse(target.value / "dist").toURI.toASCIIString

libraryDependencies ++= Seq(
  "26lights" %% "playr" % "0.9.0-SNAPSHOT",
  "org.webjars" % "swagger-ui" % "2.0.24"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
