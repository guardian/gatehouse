lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JDebPackaging, SystemdPlugin)
  .settings(
    name := "gatehouse",
    version := "0.1.0-SNAPSHOT",
    maintainer := "Guardian Identity team",
    scalaVersion := "3.3.3",
    scalacOptions ++= Seq(
      "-explain",
      "-feature",
      "-Werror",
    ),
    scalafmtOnCompile := true,
    Universal / javaOptions ++= Seq(
      "-javaagent:/opt/aws-opentelemetry-agent/aws-opentelemetry-agent.jar",
      "-Dotel.service.name=Gatehouse",
      "-Dotel.exporter=otlp",
      "-Dotel.traces.sampler=xray",
//      "-Dotel.javaagent.debug=true",
      "-Dpidfile.path=/dev/null",
      s"-J-Dlogs.home=/var/log/${packageName.value}",
    ),
    Test / javaOptions += "-Dlogback.configurationFile=logback-test.xml",
    libraryDependencies ++= Seq(
      ws,
      "org.playframework" %% "play-slick" % "6.1.0",
      "net.logstash.logback" % "logstash-logback-encoder" % "7.4",
      ("com.gu" %% "simple-configuration-ssm" % "6.0.0").cross(CrossVersion.for3Use2_13),
      /* Using Scala 2.13 version of identity-auth-play until a Scala 3 version has been released:
       * https://trello.com/c/5kOc41kD/4669-release-scala-3-version-of-identity-libraries */
      ("com.gu.identity" %% "identity-auth-core" % "4.37.0")
        .cross(CrossVersion.for3Use2_13)
        exclude ("org.scala-lang.modules", "scala-xml_2.13")
        exclude ("org.scala-lang.modules", "scala-parser-combinators_2.13")
        exclude ("com.fasterxml.jackson.module", "jackson-module-scala_2.13")
        exclude ("com.typesafe", "ssl-config-core_2.13"),
      "org.postgresql" % "postgresql" % "42.7.3",
      "com.okta.sdk" % "okta-sdk-api" % "15.0.0",
      "com.okta.sdk" % "okta-sdk-impl" % "15.0.0" % Runtime,
      "com.googlecode.libphonenumber" % "libphonenumber" % "8.13.35",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    ),
    dependencyOverrides ++= {
      val jacksonVersion = "2.17.0"
      Seq(
        // To keep all Jackson dependencies on the same version
        "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion % Runtime,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion % Runtime,
        "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % jacksonVersion % Runtime,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion % Runtime,
      )
    },
    excludeDependencies ++= Seq(
      // As of Play 3.0, groupId has changed to org.playframework; exclude transitive dependencies to the old artifacts
      ExclusionRule(organization = "com.typesafe.play")
    ),
  )
