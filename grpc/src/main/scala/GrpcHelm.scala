package sokkan
package grpc

import cats.instances.future._
import cats.~>
import hapi.services.tiller.tiller.{ListReleasesResponse, ReleaseServiceGrpc, TestReleaseResponse}
import io.grpc.{Channel, ManagedChannelBuilder, Metadata}
import io.grpc.stub.{MetadataUtils, StreamObserver}
import io.iteratee.modules.FutureModule
import scala.concurrent.{ExecutionContext, Future, Promise}
import sokkan.SokkanOp._
import sokkan.iteratee.chart.ChartModule
import sokkan.iteratee.tar.NonSuspendableTapeArchiveModule

final class GrpcHelmClient(
                            host: String,
                            port: Int,
                            helmApiClientVersion: Option[String])(implicit ec: ExecutionContext)
  extends (ReleaseServiceA ~> Future) {

  private val channel: Channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build
  private val stub = helmApiClientVersion.fold(ReleaseServiceGrpc.stub(channel)) { v =>
      val header: Metadata = new Metadata()
      val key: Metadata.Key[String] = Metadata.Key.of("x-helm-api-client", Metadata.ASCII_STRING_MARSHALLER)
      header.put(key, v)
      var stub = ReleaseServiceGrpc.stub(channel)
      stub = MetadataUtils.attachHeaders(stub, header)

      stub
  }

  private trait FutureChartModule extends FutureModule
    with NonSuspendableTapeArchiveModule[Future] with ChartModule[Future]

  private def futureCharts(implicit ec0: ExecutionContext) = new FutureChartModule {
    override protected def ec: ExecutionContext = ec0
  }

  // scalastyle:off method.length
  def apply[A](fa: ReleaseServiceA[A]): Future[A] =
    fa match {
      case GetVersion(req) =>
        stub.getVersion(req)
      case GetReleaseStatus(req) =>
        stub.getReleaseStatus(req)
      case GetReleaseContent(req) =>
        stub.getReleaseContent(req)
      case UpdateRelease(req) =>
        stub.updateRelease(req)
      case InstallRelease(req) =>
        stub.installRelease(req)
      case UninstallRelease(req) =>
        stub.uninstallRelease(req)
      case RollbackRelease(req) =>
        stub.rollbackRelease(req)
      case GetHistory(req) =>
        stub.getHistory(req)
      case ListReleases(req) =>
        val releasesPromise: Promise[ListReleasesResponse] = Promise()
        val buffer = scala.collection.mutable.ListBuffer.empty[ListReleasesResponse]
        val observer = new StreamObserver[ListReleasesResponse] {
          override def onNext(value: ListReleasesResponse): Unit = buffer += value

          override def onError(t: Throwable): Unit = releasesPromise.failure(t)

          override def onCompleted(): Unit = {
            val all = buffer.foldLeft(ListReleasesResponse()) { (o, p) =>
              ListReleasesResponse(
                count = p.count,
                next = p.next,
                total = p.total,
                releases = o.releases ++ p.releases
              )
            }
            releasesPromise.success(all)
          }
        }
        stub.listReleases(req, observer)
        releasesPromise.future

      case RunReleaseTest(req) =>
        val testPromise: Promise[List[TestReleaseResponse]] = Promise()
        val buffer = scala.collection.mutable.ListBuffer.empty[TestReleaseResponse]
        val observer = new StreamObserver[TestReleaseResponse] {
          override def onNext(value: TestReleaseResponse): Unit = buffer += value

          override def onError(t: Throwable): Unit = testPromise.failure(t)

          override def onCompleted(): Unit = testPromise.success(buffer.toList)
        }
        stub.runReleaseTest(req, observer)
        testPromise.future

      case GetChartFromTapeArchive(file) =>
        futureCharts.chartFromFiles(futureCharts.readTapeArchiveStreams(file)).run.map { s =>
          s.get(RootChart).map { rootChart =>
            val deps = (s - RootChart).values.toList
            rootChart.copy(dependencies = deps)
          }
        }

      case GetChartFromTapeArchiveUrl(url) =>
        futureCharts.chartFromFiles(futureCharts.readTapeArchiveStreamsFromUrl(url)).run.map { s =>
          s.get(RootChart).map { rootChart =>
            val deps = (s - RootChart).values.toList
            rootChart.copy(dependencies = deps)
          }
        }


    }
  // scalastyle:on method.length
}
