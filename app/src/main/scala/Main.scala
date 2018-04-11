import java.net.URL

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.EitherK
import cats.free.Free
import _root_.skuber.ConfigMap
import sokkan.ReleaseServiceA
import sokkan.SokkanOp.ReleaseServiceI
import sokkan.skuber.KubernetesOp
import sokkan.skuber.KubernetesOp.KubernetesI
import _root_.skuber.k8sInit
import _root_.skuber.json.format.configMapFmt
import cats.~>
import sokkan.grpc.GrpcHelmClient

import scala.concurrent.ExecutionContext.Implicits.global
import cats.instances.future._
import hapi.chart.config.Config
import hapi.services.tiller.tiller.{InstallReleaseRequest, UninstallReleaseRequest}

import scala.concurrent.Future

object Main extends App {
  type App[A] = EitherK[ReleaseServiceA, KubernetesOp, A]

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val dispatcher = system.dispatcher
  val k8s = k8sInit

  def program(implicit R: ReleaseServiceI[App], K: KubernetesI[App]): Free[App, ConfigMap] = {
    import R._, K._

    val releaseName = "test"
    val chartUrl = new URL("https://kubernetes-charts.storage.googleapis.com/wordpress-0.8.7.tgz")

    for {
      chart <- chartFromTapeArchiveUrl(chartUrl)
      _ <- install(InstallReleaseRequest(name = releaseName, values = Some(Config()), namespace = "default", chart = chart))
      cm <- get[ConfigMap](s"$releaseName-mariadb")
      _ <- uninstall(UninstallReleaseRequest(name = releaseName, purge = true))
    } yield cm
  }

  val helm = new GrpcHelmClient("localhost", 44134, Some("2.8.2"))
  val kubernetes = new SkuberKubernetesClient(k8s)

  val interpreter: App ~> Future = helm or kubernetes

  program.foldMap(interpreter).onComplete(println)
}
