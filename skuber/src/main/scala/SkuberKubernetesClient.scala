import SkuberKubernetesClient.Result
import sokkan.skuber.KubernetesOp
import cats.~>
import sokkan.skuber.KubernetesOp.GetObjectResource

import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import _root_.skuber._
import cats.data.ReaderT
import play.api.libs.json.Format

class SkuberKubernetesClient(implicit system: ActorSystem,
                             materializer: ActorMaterializer,
                             dispatcher: ExecutionContextExecutor)
  extends (KubernetesOp ~> Result) {

  val k8s = k8sInit

  def apply[A](fa: KubernetesOp[A]): Result[A] = {
    fa match {
      case GetObjectResource(name: String) =>
        ReaderT {
          case (format, definition) =>
            k8s.get(name)(format, definition): Future[A]
        }
    }
  }
}

object SkuberKubernetesClient {
  type Result[A] = ReaderT[
    Future,
    (Format[A with ObjectResource], ResourceDefinition[A with ObjectResource]),
    A,
    ]
}
