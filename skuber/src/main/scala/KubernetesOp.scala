package sokkan
package skuber

import _root_.skuber.{ObjectResource, ResourceDefinition}
import cats.InjectK
import cats.free.Free
import cats.free.Free.liftF
import play.api.libs.json.Format

sealed abstract class KubernetesOp[A] extends Product with Serializable

object KubernetesOp {

  trait RestrictedFunctionK[R, F[_ <: R], G[_]] {
    def apply[A <: R](fa: F[A]): G[A]
  }

  final case class GetObjectResource[T <: ObjectResource](name: String, fmt: Format[T], rd: ResourceDefinition[T])
    extends KubernetesOp[T] {
    def accept[G[_]](f: RestrictedFunctionK[ObjectResource, GetObjectResource, G]): G[T] = f(this)
  }

  type SkuberOpF[A] = Free[KubernetesOp, A]

  def get[T <: ObjectResource](name: String)(implicit fmt: Format[T], rd: ResourceDefinition[T]): SkuberOpF[T] =
    liftF[KubernetesOp, T](GetObjectResource[T](name, fmt, rd))

  class KubernetesI[F[_]](implicit I: InjectK[KubernetesOp, F]) {
    def get[T <: ObjectResource](name: String)(implicit fmt: Format[T], rd: ResourceDefinition[T]): Free[F, T] =
      Free.inject[KubernetesOp, F](GetObjectResource[T](name, fmt, rd))
  }

  object KubernetesI {
    implicit def kubernetesI[F[_]](implicit I: InjectK[KubernetesOp, F]): KubernetesI[F] = new KubernetesI[F]
  }
}
