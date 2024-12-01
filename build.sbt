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
//      "-Werror",
    ),
    scalafmtOnCompile := true,
    Universal / javaOptions ++= Seq(
      "-javaagent:/opt/aws-opentelemetry-agent/aws-opentelemetry-agent.jar",
      "-Dotel.service.name=Gatehouse",
//      "-Dotel.resource.providers.aws.enabled=true",
//      "-Dotel.instrumentation.common.experimental.controller-telemetry.enabled=true",
      "-Dotel.traces.sampler=xray",
      "-Dotel.traces.exporter=logging,otlp",
      "-Dotel.metrics.exporter=none",
      "-Dotel.logs.exporter=none",
      "-Dotel.javaagent.debug=true",
//      s"-Dotel.javaagent.configuration-file=${baseDirectory.value}/conf/telemetry.conf",
      "-Dpidfile.path=/dev/null",
      s"-Dlogs.home=/var/log/${packageName.value}",
    ),
    Test / javaOptions += "-Dlogback.configurationFile=logback-test.xml",
    libraryDependencies ++= Seq(
      ws,
      "org.playframework" %% "play-slick" % "6.1.0",
      "net.logstash.logback" % "logstash-logback-encoder" % "7.4",
      ("com.gu" %% "simple-configuration-ssm" % "2.0.0").cross(CrossVersion.for3Use2_13),
      /* Using Scala 2.13 version of identity-auth-play until a Scala 3 version has been released:
       * https://trello.com/c/5kOc41kD/4669-release-scala-3-version-of-identity-libraries */
      ("com.gu.identity" %% "identity-auth-core" % "4.25")
        .cross(CrossVersion.for3Use2_13)
        exclude ("org.scala-lang.modules", "scala-xml_2.13")
        exclude ("org.scala-lang.modules", "scala-parser-combinators_2.13")
        exclude ("com.fasterxml.jackson.module", "jackson-module-scala_2.13")
        exclude ("com.typesafe", "ssl-config-core_2.13"),
      "org.postgresql" % "postgresql" % "42.7.3",
      "com.okta.sdk" % "okta-sdk-api" % "15.0.0",
      "com.okta.sdk" % "okta-sdk-impl" % "15.0.0" % Runtime,
      "com.googlecode.libphonenumber" % "libphonenumber" % "8.13.34",
      "io.opentelemetry" % "opentelemetry-api" % "1.37.0",
      "io.opentelemetry" % "opentelemetry-sdk" % "1.37.0",
      "io.opentelemetry" % "opentelemetry-exporter-otlp" % "1.37.0",
      "io.opentelemetry.semconv" % "opentelemetry-semconv" % "1.25.0-alpha",
      "io.opentelemetry" % "opentelemetry-extension-aws" % "1.20.1" % Runtime,
      "io.opentelemetry" % "opentelemetry-sdk-extension-aws" % "1.19.0" % Runtime,
      "io.opentelemetry.contrib" % "opentelemetry-aws-xray" % "1.35.0",
      "io.opentelemetry.contrib" % "opentelemetry-aws-xray-propagator" % "1.35.0-alpha",
      "io.opentelemetry.contrib" % "opentelemetry-aws-resources" % "1.35.0-alpha",
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
