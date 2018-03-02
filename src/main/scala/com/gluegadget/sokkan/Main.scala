package com.gluegadget.sokkan

import com.gluegadget.sokkan.SokkanOp._
import hapi.services.tiller.tiller.{GetReleaseStatusRequest, ListReleasesRequest, ListReleasesResponse, ReleaseServiceGrpc}
import hapi.version.version.Version
import io.grpc.{Channel, ManagedChannelBuilder, Metadata}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.{Id, ~>}
import cats.instances.future._
import io.grpc.stub.MetadataUtils

object Main extends App {
  def program: ReleaseService[Option[Version]] =
    for {
      r <- getVersion()
    } yield r.version

  def program2: ReleaseService[Iterator[ListReleasesResponse]] =
    for {
      rs <- lazyList(ListReleasesRequest())
    } yield rs

  def blockingGrpcCompiler: ReleaseServiceA ~> Id =
    new (ReleaseServiceA ~> Id) {
      val header: Metadata = new Metadata()
      val key: Metadata.Key[String] = Metadata.Key.of("x-helm-api-client", Metadata.ASCII_STRING_MARSHALLER)
      header.put(key, "2.8.1")

      val channel: Channel = ManagedChannelBuilder.forAddress("localhost", 44134).usePlaintext(true).build
      var blockingStub = ReleaseServiceGrpc.blockingStub(channel)
      blockingStub = MetadataUtils.attachHeaders(blockingStub, header)

      def apply[A](fa: ReleaseServiceA[A]): Id[A] =
        fa match {
          case GetVersion(req) =>
            blockingStub.getVersion(req)
          case ListReleases(req) =>
            blockingStub.listReleases(req).toList
          case LazyListReleases(req) =>
            blockingStub.listReleases(req)
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

  val releases = program2.foldMap(blockingGrpcCompiler)
  releases.toList.foreach(println)
}