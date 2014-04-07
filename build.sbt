name := "playr-swagger"

releaseSettings

organization := "26lights"

resolvers += "26Lights snapshots" at "http://build.26source.org/nexus/content/repositories/public-snapshots"

libraryDependencies ++= Seq (
  "26lights"  %% "playr"  % "0.1.0"
)

play.Project.playScalaSettings
