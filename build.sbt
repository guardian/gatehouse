lazy val root = (project in file("."))
  .settings(
      name := "gatehouse",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := "3.3.3",
      scalacOptions ++= Seq(),
      libraryDependencies ++= Seq(),
  )
