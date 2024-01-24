lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "gatehouse",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "3.3.1",
    scalacOptions ++= Seq(
      "-explain",
      "-feature",
      "-Werror",
    ),
    scalafmtOnCompile := true,
    libraryDependencies ++= Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test,
    ),
  )
