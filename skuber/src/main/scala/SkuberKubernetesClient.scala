import _root_.skuber.{K8SRequestContext, ObjectResource}
import cats.~>
import scala.concurrent.Future
import sokkan.skuber.KubernetesOp
import sokkan.skuber.KubernetesOp.{GetObjectResource, RestrictedFunctionK}

class SkuberKubernetesClient(client: K8SRequestContext) extends (KubernetesOp ~> Future) {
  def apply[A](fa: KubernetesOp[A]): Future[A] = {
    fa match {
      case g: GetObjectResource[A] => {
        val f = new RestrictedFunctionK[ObjectResource, GetObjectResource, Future] {
          def apply[X <: ObjectResource](g: GetObjectResource[X]): Future[X] =
            client.get(g.name)(g.fmt, g.rd)
        }
        g.accept(f)
      }
    }
  }
}

