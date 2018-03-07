package sokkan
package skuber

import cats.free.Free
import cats.free.Free.liftF

sealed abstract class KubernetesOp[A] extends Product with Serializable

object KubernetesOp {

  final case class GetObjectResource[T](name: String) extends KubernetesOp[T]

  type SkuberOpF[A] = Free[KubernetesOp, A]

  def get[T](name: String): SkuberOpF[T] =
    liftF[KubernetesOp, T](GetObjectResource[T](name))
}
