package com.gluegadget.sokkan

import com.gluegadget.sokkan.SokkanOp._
import hapi.services.tiller.tiller.{ListReleasesResponse, ReleaseServiceGrpc}
import io.grpc.{Channel, ManagedChannelBuilder, Metadata}

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.~>
import cats.instances.future._
import io.grpc.stub.{MetadataUtils, StreamObserver}

import scala.util.{Failure, Success}

object Main extends App {
  def program: ReleaseService[ListReleasesResponse] =
    for {
      rs <- listAll()
    } yield rs

  def grpcCompiler: ReleaseServiceA ~> Future =
    new (ReleaseServiceA ~> Future) {
      val header: Metadata = new Metadata()
      val key: Metadata.Key[String] = Metadata.Key.of("x-helm-api-client", Metadata.ASCII_STRING_MARSHALLER)
      header.put(key, "2.8.1")

      val channel: Channel = ManagedChannelBuilder.forAddress("localhost", 44134).usePlaintext(true).build
      var stub = ReleaseServiceGrpc.stub(channel)
      stub = MetadataUtils.attachHeaders(stub, header)

      def apply[A](fa: ReleaseServiceA[A]): Future[A] =
        fa match {
          case GetVersion(req) =>
            stub.getVersion(req)
          case GetStatus(req) =>
            stub.getReleaseStatus(req)
          case ListReleases(req) =>
            val releasesPromise: Promise[ListReleasesResponse] = Promise()
            val buffer = scala.collection.mutable.ListBuffer.empty[ListReleasesResponse]
            val listReleasesResponseObserver = new StreamObserver[ListReleasesResponse] {
              override def onNext(value: ListReleasesResponse): Unit = buffer += value

              override def onError(t: Throwable): Unit = releasesPromise.failure(t)

              override def onCompleted(): Unit = {
                val all = buffer.foldLeft(ListReleasesResponse()) { (o, p) =>
                  ListReleasesResponse(
                    count = p.count,
                    next = p.next,
                    total = p.total,
                    releases = o.releases ++ p.releases)
                }
                releasesPromise.success(all)
              }
            }
            stub.listReleases(req, listReleasesResponseObserver)
            releasesPromise.future
        }
    }

  val releases = program.foldMap(grpcCompiler)
  releases onComplete {
    case Success(s) => s.releases.foreach(r => println(r.name))
    case Failure(e) => println(e)
  }

  Await.ready(releases, 5 minutes)
}