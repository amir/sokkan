organization in Global := "com.gluegadget.sokkan"

crossScalaVersions in Global := Seq("2.12.4", "2.11.12")

scalaVersion in Global := crossScalaVersions.value.head

lazy val sokkan = project.in(file(".")).aggregate(core, grpc, skuber, app, rudder)

lazy val core = project

lazy val grpc = project dependsOn core

lazy val skuber = project

lazy val rudder = project dependsOn core

lazy val app = project dependsOn (skuber, grpc)
