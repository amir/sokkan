import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.EitherK
import cats.free.Free
import skuber.ConfigMap
import sokkan.{ReleaseServiceA, SokkanOp}
import sokkan.SokkanOp.ReleaseServiceI
import sokkan.skuber.KubernetesOp
import sokkan.skuber.KubernetesOp.KubernetesI
import _root_.skuber.json.format.configMapFmt
import cats.~>
import sokkan.grpc.GrpcHelmClient

import scala.concurrent.ExecutionContext.Implicits.global
import cats.instances.future._
import hapi.version.version.Version

import scala.concurrent.Future

object Main extends App {
  type App[A] = EitherK[ReleaseServiceA, KubernetesOp, A]

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val dispatcher = system.dispatcher

  def program(implicit R: ReleaseServiceI[App], K: KubernetesI[App]): Free[App, (ConfigMap, Option[Version])] = {
    import R._, K._

    for {
      cm <- get[ConfigMap]("mychart-configmap")
      v <- getVersion()
    } yield (cm, v.version)
  }

  val interpreter1 = new GrpcHelmClient("localhost", 44134, Some("2.8.1"))
  val interpreter2 = new SkuberKubernetesClient()

  val interpreter: App ~> Future = interpreter1 or interpreter2

  val evaled = program.foldMap(interpreter)

  evaled.onComplete(println)
}