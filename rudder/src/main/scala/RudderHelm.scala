package sokkan
package rudder

import CirceSupport._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import cats.~>
import hapi.services.tiller.tiller.{InstallReleaseResponse, ListReleasesResponse}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import scala.concurrent.Future
import sokkan.SokkanOp.{InstallRelease, ListReleases}
import sokkan.rudder.model.RudderInstallReleaseRequest

final class RudderHelm(host: String, port: Int) extends (ReleaseServiceA ~> Future) {

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def apply[A](fa: ReleaseServiceA[A]): Future[A] =
    fa match {
      case ListReleases(req) =>
        for {
          r <- Http().singleRequest(
            HttpRequest(uri = s"http://$host:$port/api/v1/releases")
          )
          rr <- Unmarshal(r.entity).to[ListReleasesResponse]
        } yield rr

      case InstallRelease(req) =>
        val metadata = for {
          chart <- req.chart
          metadata <- chart.metadata
        } yield metadata

        metadata match {
          case Some(m) =>
            val e = RudderInstallReleaseRequest(req.name, req.namespace, "stable", m.name, m.version)

            for {
              d <- Marshal(e).to[RequestEntity]
              r <- Http().singleRequest(
                HttpRequest(
                  uri = s"http://$host:$port/api/v1/releases",
                  method = HttpMethods.POST,
                  entity = d
                )
              )
              irr <- Unmarshal(r.entity).to[InstallReleaseResponse]
            } yield irr

          case None => Future.failed(new IllegalArgumentException("Chart metadata is required"))
        }
    }
}
