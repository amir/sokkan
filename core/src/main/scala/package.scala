import cats.{~>, Monad}

package object sokkan {
  def run[F[_]:Monad, A](interpreter: ReleaseServiceA ~> F, op: SokkanOp.ReleaseService[A]): F[A] =
    op.foldMap(interpreter)
}
