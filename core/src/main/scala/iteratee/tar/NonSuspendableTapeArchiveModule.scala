package sokkan.iteratee.tar

import java.io.{Closeable, File, FileInputStream}
import java.util.zip.GZIPInputStream

import cats.{Eval, MonadError}
import io.iteratee.{Enumerator, Module}
import io.iteratee.internal.Step
import org.kamranzafar.jtar.{TarEntry, TarInputStream}

trait NonSuspendableTapeArchiveModule[F[_]] extends TapeArchiveModule[F] {
  this: Module[F] {type M[f[_]] <: MonadError[f, Throwable]} =>

  private[this] def captureEffect[A](a: => A): Eval[F[A]] = Eval.always(F.catchNonFatal(a))

  def close(c: Closeable): Eval[F[Unit]] = Eval.later(F.catchNonFatal(c.close()))

  override final def readTapeArchiveStreams(file: File): Enumerator[F, (TarEntry, Array[Byte])] =
    Enumerator.liftMEval(captureEffect(new TarInputStream(new GZIPInputStream(new FileInputStream(file)))))(F).flatMap { ti =>
      new TapeArchiveEnumerator(ti).ensureEval(close(ti))(F)
    }(F)

  final class TapeArchiveEnumerator(ti: TarInputStream) extends Enumerator[F, (TarEntry, Array[Byte])] {
    final def apply[A](s: Step[F, (TarEntry, Array[Byte]), A]): F[Step[F, (TarEntry, Array[Byte]), A]] =
      F.tailRecM(s) { step =>
        if (step.isDone) F.pure(Right(step)) else F.flatten(
          F.catchNonFatal {
            val nextEntry = ti.getNextEntry
            if (nextEntry != null) {
              val bufferSize = 8192
              val array = new Array[Byte](bufferSize)
              val bytesRead = ti.read(array, 0, bufferSize)
              val read = if (bytesRead == bufferSize) array else array.slice(0, bytesRead)

              if (bytesRead == -1) F.pure(Right(step)) else F.map(step.feedEl((nextEntry, read)))(Left(_))
            } else F.pure(Right(step))
          }
        )
      }
  }
}

