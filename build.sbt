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
      "net.logstash.logback" % "logstash-logback-encoder" % "7.4",
      ("com.gu" %% "simple-configuration-ssm" % "1.6.4").cross(CrossVersion.for3Use2_13),
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    ),
    dependencyOverrides ++= Seq(
      // To keep all Jackson dependencies on the same version
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.2" % Runtime,
    )
  )
