libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.1",
  "org.typelevel" %% "cats-free" % "1.0.1",
  "com.google.protobuf" % "protobuf-java" % "3.5.1",
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "io.iteratee" %% "iteratee-core" % "0.17.0",
  "io.iteratee" %% "iteratee-files" % "0.17.0",

  "org.apache.commons" % "commons-vfs2" % "2.2",
  "org.apache.commons" % "commons-compress" % "1.14",
  "commons-httpclient" % "commons-httpclient" % "3.1",

  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

scalacOptions ++= Seq(
  "-Ypartial-unification",
  "-language:higherKinds"
)
