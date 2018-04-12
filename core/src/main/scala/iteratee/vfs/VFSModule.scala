package sokkan.iteratee.vfs

import cats.MonadError
import io.iteratee.Enumerator
import java.io.{Closeable, InputStream}
import org.apache.commons.vfs2.FileObject
import scala.util.control.NonFatal

trait VFSModule[F[_]] {
  def readLines(file: FileObject): Enumerator[F, String]
  def listFiles(file: FileObject): Enumerator[F, FileObject]
  def listFilesRec(file: FileObject): Enumerator[F, FileObject]
  def readFileObjectStreams(name: String): Enumerator[F, (FileObject, InputStream)]

  protected final def bracket[R <: Closeable, A](fr: F[R])(f: R => F[A])(implicit F: MonadError[F, Throwable]): F[A] =
    F.flatMap(fr) { r =>
      F.handleErrorWith(f(r)) {
        case NonFatal(e) =>
          try r.close() catch {
            case NonFatal(_) =>
          }
          F.raiseError(e)
      }
    }
}
