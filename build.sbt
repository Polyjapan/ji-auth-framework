name := "JIAuthFramework"
organization := "ch.japanimpact"

version := "0.1-SNAPSHOT"

publishArtifact in (Compile, packageDoc) := false
publishArtifact in (Compile, packageSrc) := false
publishArtifact in (Compile, packageBin) := true

scalaVersion := "2.12.8"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.7.1"
libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.7.1"
