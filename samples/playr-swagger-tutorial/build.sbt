name := "playr-swagger-tutorial"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions += "-language:reflectiveCalls"

libraryDependencies += cache

lazy val playrSwagger = RootProject(file("../.."))

lazy val playrTutorial = project in file(".") dependsOn playrSwagger enablePlugins PlayScala

