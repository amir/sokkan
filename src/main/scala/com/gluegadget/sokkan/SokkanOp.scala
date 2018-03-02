package com.gluegadget.sokkan

import cats.free.Free
import cats.free.Free.liftF
import hapi.services.tiller.tiller._

sealed trait ReleaseServiceA[A]

object SokkanOp {
  case class GetVersion(req: GetVersionRequest) extends ReleaseServiceA[GetVersionResponse]
  case class GetStatus(req: GetReleaseStatusRequest) extends ReleaseServiceA[GetReleaseStatusResponse]

  type ReleaseService[A] = Free[ReleaseServiceA, A]

  def getStatus(req: GetReleaseStatusRequest): ReleaseService[GetReleaseStatusResponse] =
    liftF[ReleaseServiceA, GetReleaseStatusResponse](GetStatus(req))

  def getVersion(req: GetVersionRequest = GetVersionRequest()): ReleaseService[GetVersionResponse] =
    liftF[ReleaseServiceA, GetVersionResponse](GetVersion(req))
}
