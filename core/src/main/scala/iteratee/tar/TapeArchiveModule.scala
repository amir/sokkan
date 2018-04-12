package sokkan.iteratee.tar

import io.iteratee.Enumerator
import java.io.{File, InputStream}
import java.net.URL

trait TapeArchiveModule[F[_]] {
  def readTapeArchiveStreams(file: File): Enumerator[F, (String, InputStream)]
  def readTapeArchiveStreamsFromUrl(url: URL): Enumerator[F, (String, InputStream)]
}
