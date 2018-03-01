addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.17")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.7.0",
  "com.google.protobuf" % "protobuf-java" % "3.5.1"
)