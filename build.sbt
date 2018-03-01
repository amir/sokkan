libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
  "com.google.protobuf" % "protobuf-java" % "3.5.1"
)

PB.targets in Compile := Seq(
  Pbm2SccGenerator -> (sourceManaged in Compile).value
)
