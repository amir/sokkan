package com.gluegadget.sokkan

import com.gluegadget.sokkan.SokkanOp._
import hapi.services.tiller.tiller.ReleaseServiceGrpc
import hapi.version.version.Version
import io.grpc.{Channel, ManagedChannelBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats.{Id, ~>}
import cats.instances.future._

object Main extends App {
  def program: ReleaseService[Option[Version]] =
    for {
      r <- getVersion()
    } yield r.version

  def blockingGrpcCompiler: ReleaseServiceA ~> Id =
    new (ReleaseServiceA ~> Id) {
      val channel: Channel = ManagedChannelBuilder.forAddress("localhost", 44134).usePlaintext(true).build
      val blockingStub = ReleaseServiceGrpc.blockingStub(channel)

      def apply[A](fa: ReleaseServiceA[A]): Id[A] =
        fa match {
          case GetVersion(req) =>
            blockingStub.getVersion(req)
        }
    }

  def grpcCompiler: ReleaseServiceA ~> Future =
    new (ReleaseServiceA ~> Future) {
      val channel: Channel = ManagedChannelBuilder.forAddress("localhost", 44134).usePlaintext(true).build
      val stub = ReleaseServiceGrpc.stub(channel)

      def apply[A](fa: ReleaseServiceA[A]): Future[A] =
        fa match {
          case GetVersion(req) =>
            stub.getVersion(req)
        }
    }

  val version: Option[Version] = program.foldMap(blockingGrpcCompiler)
  val futureVersion: Future[Option[Version]] = program.foldMap(grpcCompiler)

  println(version)

  futureVersion onComplete println
}