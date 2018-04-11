package sokkan.iteratee.tar

import java.io.{File, InputStream}
import java.net.URL

import io.iteratee.Enumerator

trait TapeArchiveModule[F[_]] {
  def readTapeArchiveStreams(file: File): Enumerator[F, (String, InputStream)]
  def readTapeArchiveStreamsFromUrl(url: URL): Enumerator[F, (String, InputStream)]
}
