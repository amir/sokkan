libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.1",
  "org.typelevel" %% "cats-free" % "1.0.1",
  "org.typelevel" %% "cats-effect" % "0.9",
  "io.skuber" %% "skuber" % "2.0.4",
  "com.typesafe.akka" %% "akka-http" % "10.0.10"
)

scalacOptions += "-Ypartial-unification"
