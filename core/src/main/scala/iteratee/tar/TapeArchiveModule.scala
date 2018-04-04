package sokkan.iteratee.tar

import java.io.File

import io.iteratee.Enumerator
import org.kamranzafar.jtar.TarEntry

trait TapeArchiveModule[F[_]] {
  def readTapeArchiveStreams(file: File): Enumerator[F, (TarEntry, Array[Byte])]
}
