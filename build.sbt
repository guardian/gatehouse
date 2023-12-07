name := "gatehouse"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.3.1"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
