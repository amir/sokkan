package sokkan

import cats.InjectK
import cats.free.Free
import cats.free.Free.liftF
import hapi.chart.chart.Chart
import hapi.release.release.Release
import hapi.services.tiller.tiller._
import java.io.File
import java.net.URL

sealed trait ReleaseServiceA[A]

object SokkanOp {
  case class ListReleases(req: ListReleasesRequest) extends ReleaseServiceA[ListReleasesResponse]
  case class GetReleaseStatus(req: GetReleaseStatusRequest) extends ReleaseServiceA[GetReleaseStatusResponse]
  case class GetReleaseContent(req: GetReleaseContentRequest) extends ReleaseServiceA[GetReleaseContentResponse]
  case class UpdateRelease(req: UpdateReleaseRequest) extends ReleaseServiceA[UpdateReleaseResponse]
  case class InstallRelease(req: InstallReleaseRequest) extends ReleaseServiceA[InstallReleaseResponse]
  case class UninstallRelease(req: UninstallReleaseRequest) extends ReleaseServiceA[UninstallReleaseResponse]
  case class GetVersion(req: GetVersionRequest) extends ReleaseServiceA[GetVersionResponse]
  case class RollbackRelease(req: RollbackReleaseRequest) extends ReleaseServiceA[RollbackReleaseResponse]
  case class GetHistory(req: GetHistoryRequest) extends ReleaseServiceA[GetHistoryResponse]
  case class RunReleaseTest(req: TestReleaseRequest) extends ReleaseServiceA[List[TestReleaseResponse]]
  case class GetChartFromTapeArchive(file: File) extends ReleaseServiceA[Option[Chart]]
  case class GetChartFromTapeArchiveUrl(url: URL) extends ReleaseServiceA[Option[Chart]]

  type ReleaseService[A] = Free[ReleaseServiceA, A]

  def list(req: ListReleasesRequest): ReleaseService[ListReleasesResponse] =
    liftF[ReleaseServiceA, ListReleasesResponse](ListReleases(req))

  def listAll(): ReleaseService[ListReleasesResponse] = list(ListReleasesRequest())

  def getStatus(req: GetReleaseStatusRequest): ReleaseService[GetReleaseStatusResponse] =
    liftF[ReleaseServiceA, GetReleaseStatusResponse](GetReleaseStatus(req))

  def getContent(req: GetReleaseContentRequest): ReleaseService[GetReleaseContentResponse] =
    liftF[ReleaseServiceA, GetReleaseContentResponse](GetReleaseContent(req))

  def update(req: UpdateReleaseRequest): ReleaseService[UpdateReleaseResponse] =
    liftF[ReleaseServiceA, UpdateReleaseResponse](UpdateRelease(req))

  def install(req: InstallReleaseRequest): ReleaseService[InstallReleaseResponse] =
    liftF[ReleaseServiceA, InstallReleaseResponse](InstallRelease(req))

  def uninstall(req: UninstallReleaseRequest): ReleaseService[UninstallReleaseResponse] =
    liftF[ReleaseServiceA, UninstallReleaseResponse](UninstallRelease(req))

  def getVersion(req: GetVersionRequest = GetVersionRequest()): ReleaseService[GetVersionResponse] =
    liftF[ReleaseServiceA, GetVersionResponse](GetVersion(req))

  def testRelease(req: TestReleaseRequest): ReleaseService[List[TestReleaseResponse]] =
    liftF[ReleaseServiceA, List[TestReleaseResponse]](RunReleaseTest(req))

  def getRelease(name: String, version: Int): ReleaseService[Option[Release]] = for {
    r <- getContent(GetReleaseContentRequest(name, version))
  } yield r.release

  def chartFromTapeArchive(file: File): ReleaseService[Option[Chart]] =
    liftF[ReleaseServiceA, Option[Chart]](GetChartFromTapeArchive(file))

  def chartFromTapeArchiveUrl(url: URL): ReleaseService[Option[Chart]] =
    liftF[ReleaseServiceA, Option[Chart]](GetChartFromTapeArchiveUrl(url))

  class ReleaseServiceI[F[_]](implicit I: InjectK[ReleaseServiceA, F]) {
    def list(req: ListReleasesRequest): Free[F, ListReleasesResponse] =
      Free.inject[ReleaseServiceA, F](ListReleases(req))

    def getVersion(req: GetVersionRequest = GetVersionRequest()): Free[F, GetVersionResponse] =
      Free.inject[ReleaseServiceA, F](GetVersion(req))

    def install(req: InstallReleaseRequest): Free[F, InstallReleaseResponse] =
      Free.inject[ReleaseServiceA, F](InstallRelease(req))

    def uninstall(req: UninstallReleaseRequest): Free[F, UninstallReleaseResponse] =
      Free.inject[ReleaseServiceA, F](UninstallRelease(req))

    def chartFromTapeArchiveUrl(url: URL): Free[F, Option[Chart]] =
      Free.inject[ReleaseServiceA, F](GetChartFromTapeArchiveUrl(url))
  }

  object ReleaseServiceI {
    implicit def releaseServiceI[F[_]](implicit I: InjectK[ReleaseServiceA, F]): ReleaseServiceI[F] =
      new ReleaseServiceI[F]
  }
}

