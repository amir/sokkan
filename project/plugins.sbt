addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.17")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.7.0",
  "com.google.protobuf" % "protobuf-java" % "3.5.1"
)
