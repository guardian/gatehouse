lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JDebPackaging, SystemdPlugin)
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
    Universal / javaOptions ++= Seq(
      s"-Dpidfile.path=/dev/null",
      s"-J-Dlogs.home=/var/log/${packageName.value}",
    ),
    libraryDependencies ++= Seq(
      "software.amazon.awssdk" % "ssm" % "2.23.10",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    ),
  )
