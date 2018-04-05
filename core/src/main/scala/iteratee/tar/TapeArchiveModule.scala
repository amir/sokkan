package sokkan.iteratee.tar

import java.io.File

import io.iteratee.Enumerator

trait TapeArchiveModule[F[_]] {
  def readTapeArchiveStreams(file: File): Enumerator[F, (String, Array[Byte])]
}
