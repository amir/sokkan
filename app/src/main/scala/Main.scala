import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.EitherK
import cats.free.Free
import _root_.skuber.{ConfigMap, ObjectMeta}
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
import hapi.chart.chart.Chart
import hapi.chart.metadata.Metadata
import hapi.chart.template.Template
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
    import YamlObjectResource._

    val cm: ConfigMap = ConfigMap(
      metadata = ObjectMeta(name = "mychart-configmap"),
      data = Map("myvalue" -> "Hello World"))
    val configMap = cm.toYamlByteString

    val metadata = new Metadata(tillerVersion = "2.8.1")
    val template = new Template(data = configMap)
    val chart = new Chart(metadata = Some(metadata), templates = Seq(template))

    val releaseName = "test"

    for {
      _ <- install(InstallReleaseRequest(name = releaseName, namespace = "default", chart = Some(chart)))
      cm <- get[ConfigMap]("mychart-configmap")
      _ <- uninstall(UninstallReleaseRequest(name = releaseName, purge = true))
    } yield cm
  }

  val helm = new GrpcHelmClient("localhost", 44134, Some("2.8.1"))
  val kubernetes = new SkuberKubernetesClient(k8s)

  val interpreter: App ~> Future = helm or kubernetes

  program.foldMap(interpreter).onComplete(println)
}
