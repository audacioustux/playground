lazy val root = project
  .in(file("."))
  .enablePlugins(JmhPlugin)
  .settings(
    name := "bloom",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    Compile / mainClass := Some("bench.Polyglot.AgentBenchJMH"),
    Compile / run / fork := true,
    nativeImageGraalHome := file(sys.env("GRAALVM_HOME")).toPath,
    //    nativeImageOptions += s"-H:ReflectionConfigurationFiles=${target.value / "native-image-configs" / "reflect-config.json"}",
    //    nativeImageOptions += s"-H:ConfigurationFileDirectories=${target.value / "native-image-configs"}",
    // nativeImageOptions += s"-H:+PrintClassInitialization",
    //    nativeImageOptions += "-H:+JNI",
    nativeImageOptions ++=
      Seq(
        "--no-fallback",
        "--no-server",
        "--language:wasm",
        // "--language:js",
        // "--language:python",
        //        "--initialize-at-build-time",
        //        "--pgo-instrument"
        "--pgo=/Users/tanjimhossain/Bytes/poc-wormhole/bloom/default.iprof"
      ),
    javaOptions ++= Seq(
      "-Xmx16G"
      // "-truffle"
      // "--cpusampler"
    ),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "io.jvm.uuid" %% "scala-uuid" % "0.3.1"
    ).map(_.cross(CrossVersion.for3Use2_13)),
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "ch.qos.logback" % "logback-classic" % "1.3.0-alpha12",
      "org.graalvm.sdk" % "graal-sdk" % "22.3.0"
    )
  )
  .enablePlugins(NativeImagePlugin)
val scala3Version = "3.2.1"
val AkkaVersion = "2.6.20"
