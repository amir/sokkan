libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "io.circe" %% "circe-generic" % "0.9.3",
  "io.circe" %% "circe-parser" % "0.9.3"
)

scalacOptions ++= Seq(
  "-Ypartial-unification",
  "-language:higherKinds"
)
