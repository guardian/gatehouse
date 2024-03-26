lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JDebPackaging, SystemdPlugin)
  .settings(
    name := "gatehouse",
    version := "0.1.0-SNAPSHOT",
    maintainer := "Guardian Identity team",
    packageSummary := "Gatehouse",
    packageDescription := "Gatehouse.",
//    Debian / linuxPackageMappings += {
//      val file = sourceDirectory.value / ".." / "conf" / "debian.conf"
//      packageMapping((file, "/etc/default/gatehouse")) withPerms "0644"
//    },
    // this is equal to
    // linuxPackageMappings <<= linuxPackageMappings map { mappings => /* stuff */ mappings }
//    linuxPackageMappings := {
//      // first get the current mappings. mapping is of type Seq[LinuxPackageMapping]
//      val mappings = linuxPackageMappings.value
//
//      // map over the mappings if you want to change them
//      mappings map { mapping =>
//
//        // we remove everything besides files that end with ".conf"
//        val filtered = mapping.mappings filter { case (file, name) =>
//          name != "/etc/default/gatehouse" // only elements where this is true are kept
//        }
//        val f2 = filtered :+ {
//          val file = baseDirectory.value / "conf" / "env.conf"
//          (file, "/etc/default/gatehouse")
//        }
//
//        // now we copy the mapping but replace the mappings
//        mapping.copy(mappings = filtered)
//
//      } filter {
//        // only keep those mappings that are nonEmpty (_.mappings.nonEmpty == true)
//        _.mappings.nonEmpty
//      }
//    },
//    linuxPackageMappings += {
//      val file = baseDirectory.value / "conf" / "env.conf"
//      packageMapping((file, "/etc/default/gatehouse")) withPerms "0644"
//    },
    scalaVersion := "3.3.3",
    scalacOptions ++= Seq(
      "-explain",
      "-feature",
      "-Werror",
    ),
    scalafmtOnCompile := true,
    Universal / javaOptions ++= Seq(
      "-javaagent:/opt/aws-opentelemetry-agent/aws-opentelemetry-agent.jar",
      "-Dotel.exporter=otlp",
      "-Dotel.resource.attributes=service.name=Gatehouse,service.namespace=CODE,environment=CODE",
      "-Dotel.javaagent.debug=true",
      "-Dpidfile.path=/dev/null",
      s"-J-Dlogs.home=/var/log/${packageName.value}",
    ),
    Test / javaOptions += "-Dlogback.configurationFile=logback-test.xml",
    libraryDependencies ++= Seq(
      ws,
      "org.playframework" %% "play-slick" % "6.1.0",
      "net.logstash.logback" % "logstash-logback-encoder" % "7.4",
      ("com.gu" %% "simple-configuration-ssm" % "1.7.0").cross(CrossVersion.for3Use2_13),
      /* Using Scala 2.13 version of identity-auth-play until a Scala 3 version has been released:
       * https://trello.com/c/5kOc41kD/4669-release-scala-3-version-of-identity-libraries */
      ("com.gu.identity" %% "identity-auth-core" % "4.23")
        .cross(CrossVersion.for3Use2_13)
        exclude ("org.scala-lang.modules", "scala-xml_2.13")
        exclude ("org.scala-lang.modules", "scala-parser-combinators_2.13")
        exclude ("com.fasterxml.jackson.module", "jackson-module-scala_2.13")
        exclude ("com.typesafe", "ssl-config-core_2.13"),
      "org.postgresql" % "postgresql" % "42.7.3",
      "com.okta.sdk" % "okta-sdk-api" % "15.0.0",
      "com.okta.sdk" % "okta-sdk-impl" % "15.0.0" % Runtime,
      "com.googlecode.libphonenumber" % "libphonenumber" % "8.13.33",
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
