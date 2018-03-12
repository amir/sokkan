import sokkan.skuber.KubernetesOp
import cats.~>
import sokkan.skuber.KubernetesOp.{GetObjectResource, RestrictedFunctionK}

import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import _root_.skuber._

class SkuberKubernetesClient(implicit system: ActorSystem,
                             materializer: ActorMaterializer,
                             dispatcher: ExecutionContextExecutor)
  extends (KubernetesOp ~> Future) {

  val k8s = k8sInit

  def apply[A](fa: KubernetesOp[A]): Future[A] = {
    fa match {
      case g: GetObjectResource[A] => {
        val f = new RestrictedFunctionK[ObjectResource, GetObjectResource, Future] {
          def apply[X <: ObjectResource](g: GetObjectResource[X]): Future[X] =
            k8s.get(g.name)(g.fmt, g.rd)
        }
        g.accept(f)
      }
    }
  }
}

