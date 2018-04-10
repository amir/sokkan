package sokkan.iteratee.tar

import java.io._
import java.util.zip.GZIPInputStream

import cats.{Eval, MonadError}
import io.iteratee.{Enumerator, Module}
import io.iteratee.internal.Step
import org.kamranzafar.jtar.TarInputStream

trait NonSuspendableTapeArchiveModule[F[_]] extends TapeArchiveModule[F] {
  this: Module[F] {type M[f[_]] <: MonadError[f, Throwable]} =>

  private[this] def captureEffect[A](a: => A): Eval[F[A]] = Eval.always(F.catchNonFatal(a))

  def close(c: Closeable): Eval[F[Unit]] = Eval.later(F.catchNonFatal(c.close()))

  override final def readTapeArchiveStreams(file: File): Enumerator[F, (String, InputStream)] =
    Enumerator.liftMEval(captureEffect(new TarInputStream(new GZIPInputStream(new FileInputStream(file)))))(F).flatMap { ti =>
      new TapeArchiveEnumerator(ti).ensureEval(close(ti))(F)
    }(F)

  final class TapeArchiveEnumerator(ti: TarInputStream) extends Enumerator[F, (String, InputStream)] {
    final def apply[A](s: Step[F, (String, InputStream), A]): F[Step[F, (String, InputStream), A]] =
      F.tailRecM(s) { step =>
        if (step.isDone) F.pure(Right(step)) else F.flatten(
          F.catchNonFatal {
            val nextEntry = ti.getNextEntry
            if (nextEntry != null) {
              val baos = new ByteArrayOutputStream()
              val bufferSize = 32768
              val bytes = new Array[Byte](bufferSize)
              def go() {
                val bytesRead = ti.read(bytes)
                if (bytesRead >= 0) {
                  baos.write(bytes, 0, bytesRead)
                  go()
                }
              }
              go()
              baos.flush()

              F.map(step.feedEl((nextEntry.getName, new ByteArrayInputStream(baos.toByteArray))))(Left(_))
            } else F.pure(Right(step))
          }
        )
      }
  }
}

