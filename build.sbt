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
    Test / javaOptions += "-Dlogback.configurationFile=logback-test.xml",
    libraryDependencies ++= Seq(
      "net.logstash.logback" % "logstash-logback-encoder" % "7.4",
      ("com.gu" %% "simple-configuration-ssm" % "1.6.4").cross(CrossVersion.for3Use2_13),
      /* Using Scala 2.13 version of identity-auth-play until a Scala 3 version has been released:
       * https://trello.com/c/5kOc41kD/4669-release-scala-3-version-of-identity-libraries */
      ("com.gu.identity" %% "identity-auth-core" % "4.20")
        .cross(CrossVersion.for3Use2_13)
        exclude ("org.scala-lang.modules", "scala-xml_2.13")
        exclude ("org.scala-lang.modules", "scala-parser-combinators_2.13")
        exclude ("com.fasterxml.jackson.module", "jackson-module-scala_2.13")
        exclude ("com.typesafe", "ssl-config-core_2.13"),
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    ),
    dependencyOverrides ++= Seq(
      // To keep all Jackson dependencies on the same version
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.2" % Runtime,
    ),
    excludeDependencies ++= Seq(
      // As of Play 3.0, groupId has changed to org.playframework; exclude transitive dependencies to the old artifacts
      ExclusionRule(organization = "com.typesafe.play")
    ),
  )
