import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.EitherK
import cats.free.Free
import skuber.ConfigMap
import sokkan.ReleaseServiceA
import sokkan.SokkanOp.ReleaseServiceI
import sokkan.skuber.KubernetesOp
import sokkan.skuber.KubernetesOp.KubernetesI
import _root_.skuber.json.format.configMapFmt
import cats.~>
import sokkan.grpc.GrpcHelmClient

import scala.concurrent.ExecutionContext.Implicits.global
import cats.instances.future._
import com.google.protobuf.ByteString
import hapi.chart.chart.Chart
import hapi.chart.metadata.Metadata
import hapi.chart.template.Template
import hapi.services.tiller.tiller.InstallReleaseRequest

import scala.concurrent.Future

object Main extends App {
  type App[A] = EitherK[ReleaseServiceA, KubernetesOp, A]

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val dispatcher = system.dispatcher

  def program(implicit R: ReleaseServiceI[App], K: KubernetesI[App]): Free[App, ConfigMap] = {
    import R._, K._

    val configMap =
      """
        |apiVersion: v1
        |kind: ConfigMap
        |metadata:
        |  name: mychart-configmap
        |data:
        |  myvalue: "Hello World"
      """.stripMargin

    val metadata = new Metadata(tillerVersion = "2.8.1")
    val template = new Template(data = ByteString.copyFrom(configMap.getBytes))
    val chart = new Chart(metadata = Some(metadata), templates = Seq(template))

    for {
      _ <- install(InstallReleaseRequest(name = "test", namespace = "default", chart = Some(chart)))
      cm <- get[ConfigMap]("mychart-configmap")
    } yield cm
  }

  val helm = new GrpcHelmClient("localhost", 44134, Some("2.8.1"))
  val kubernetes = new SkuberKubernetesClient()

  val interpreter: App ~> Future = helm or kubernetes

  program.foldMap(interpreter).onComplete(println)
}
