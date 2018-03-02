package com.gluegadget.sokkan

import cats.free.Free
import cats.free.Free.liftF
import hapi.services.tiller.tiller._

sealed trait ReleaseServiceA[A]

object SokkanOp {
  case class ListReleases(req: ListReleasesRequest) extends ReleaseServiceA[List[ListReleasesResponse]]
  case class LazyListReleases(req: ListReleasesRequest) extends ReleaseServiceA[Iterator[ListReleasesResponse]]
  case class GetVersion(req: GetVersionRequest) extends ReleaseServiceA[GetVersionResponse]
  case class GetStatus(req: GetReleaseStatusRequest) extends ReleaseServiceA[GetReleaseStatusResponse]

  type ReleaseService[A] = Free[ReleaseServiceA, A]

  def list(req: ListReleasesRequest): ReleaseService[List[ListReleasesResponse]] =
    liftF[ReleaseServiceA, List[ListReleasesResponse]](ListReleases(req))

  def lazyList(req: ListReleasesRequest): ReleaseService[Iterator[ListReleasesResponse]] =
    liftF[ReleaseServiceA, Iterator[ListReleasesResponse]](LazyListReleases(req))

  def getStatus(req: GetReleaseStatusRequest): ReleaseService[GetReleaseStatusResponse] =
    liftF[ReleaseServiceA, GetReleaseStatusResponse](GetStatus(req))

  def getVersion(req: GetVersionRequest = GetVersionRequest()): ReleaseService[GetVersionResponse] =
    liftF[ReleaseServiceA, GetVersionResponse](GetVersion(req))
}
