package sokkan.iteratee.vfs

import java.io.{BufferedReader, Closeable, InputStream, InputStreamReader}

import cats.{Eval, MonadError}
import io.iteratee.internal.Step
import io.iteratee.{Enumerator, Module}
import org.apache.commons.vfs2.{FileContent, FileObject, VFS}

trait NonSuspendableVFSModule[F[_]] extends VFSModule[F] {
  this: Module[F] { type M[f[_]] <: MonadError[f, Throwable] } =>

  private[this] def captureEffect[A](a: => A): Eval[F[A]] = Eval.always(F.catchNonFatal(a))
  private[this] def close(c: Closeable): Eval[F[Unit]] = Eval.later(F.catchNonFatal(c.close()))

  private[this] def newFileContent(file: FileObject): F[FileContent] = F.catchNonFatal(file.getContent)
  private[this] def newBufferedReader(content: FileContent): F[BufferedReader] =
    F.catchNonFatal(new BufferedReader(new InputStreamReader(content.getInputStream, "UTF-8")))

  private[this] def newInputStream(content: FileContent): F[InputStream] = F.catchNonFatal(content.getInputStream)

  final def readLines(file: FileObject): Enumerator[F, String] = Enumerator.liftMEval(
    Eval.always(
      bracket(newFileContent(file))(newBufferedReader)(F)
    )
  )(F).flatMap(reader => new LineEnumerator(reader).ensureEval(close(reader))(F))(F)

  final def inputStream(file: FileObject): Enumerator[F, (FileObject, InputStream)] =
    Enumerator.liftMEval(
      Eval.always(
        bracket(newFileContent(file))(newInputStream)(F)
      )
    )(F).map(f => (file, f))(F)

  final def readFileObjectStreams(name: String): Enumerator[F, (FileObject, InputStream)] =
    Enumerator.liftMEval(captureEffect(VFS.getManager.resolveFile(name)))(F)
      .flatMap(listFilesRec)(F)
      .flatMap(inputStream)(F)

  final def listFiles(dir: FileObject): Enumerator[F, FileObject] =
    Enumerator.liftMEval(captureEffect(dir.getChildren))(F).flatMap {
      case null => Enumerator.empty[F, FileObject](F)
      case files => Enumerator.enumVector(files.toVector)(F)
    }(F)

  final def listFilesRec(dir: FileObject): Enumerator[F, FileObject] = listFiles(dir).flatMap {
    case item if item.isFolder => listFilesRec(item)
    case item => Enumerator.enumOne(item)(F)
  }(F)

  private[this] final class LineEnumerator(reader: BufferedReader) extends Enumerator[F, String] {
    final def apply[A](s: Step[F, String, A]): F[Step[F, String, A]] = F.tailRecM(s) { step =>
      if (step.isDone) F.pure(Right(step)) else F.flatMap(F.catchNonFatal(reader.readLine())) {
        case null => F.pure(Right(step))
        case line => F.map(step.feedEl(line))(Left(_))
      }
    }
  }
}
