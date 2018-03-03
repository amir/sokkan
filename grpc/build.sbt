libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.1",
  "org.typelevel" %% "cats-free" % "1.0.1",
  "org.typelevel" %% "cats-effect" % "0.9",
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
)

scalacOptions += "-Ypartial-unification"
