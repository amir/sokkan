package sokkan
package iteratee.chart

import cats.MonadError
import cats.syntax.either._
import com.google.protobuf.ByteString
import com.google.protobuf.any.Any
import hapi.chart.chart.Chart
import hapi.chart.config.Config
import hapi.chart.metadata.Metadata
import hapi.chart.template.Template
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import io.circe.yaml
import io.iteratee.{Iteratee, Module}
import java.io.{InputStream, InputStreamReader}

trait ChartModule[F[_]] {
  this: Module[F] {type M[f[_]] <: MonadError[f, Throwable]} =>

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  final def chartFromFiles: Iteratee[F, (String, InputStream), Map[ChartLevel, Chart]] =
    Iteratee.foldM[F, (String, InputStream), Map[ChartLevel, Chart]](Map.empty[ChartLevel, Chart]) { (k, v) =>
      val (entry, bytes) = v
      Charts.toSubCharts(entry) match {
        case (head: String) :: _ => Charts.getChartName(head) match {
          case Some(cn) => F.pure(addEntry(entry, bytes, SubChart(cn), k))
          case _ => F.pure(k)
        }
        case _ =>
          F.pure(addEntry(entry, bytes, RootChart, k))
      }
    }(F)

  private def addEntry(file: String, inputStream: InputStream, chartLevel: ChartLevel, map: Map[ChartLevel, Chart]):
    Map[ChartLevel, Chart] = {
    Charts.getTemplateFileName(file) match {
      case Some(t) =>
        val data = ByteString.readFrom(inputStream)
        val c = map.getOrElse(chartLevel, Chart())
        map + (chartLevel -> c.copy(templates = c.templates :+ Template(t, data)))

      case None =>
        Charts.getSubChartFileName(file).fold(Charts.getOrdinaryFileName(file))(Some(_)).map {
          case "Chart.yaml" =>
            val o = yaml.parser.parse(new InputStreamReader(inputStream))
            val metadata = o.flatMap(_.as[Metadata]).toOption
            val c = map.getOrElse(chartLevel, Chart())
            map + (chartLevel -> c.copy(metadata = metadata) )
          case "values.yaml" =>
            val c = map.getOrElse(chartLevel, Chart())
            val rawBytes = ByteString.readFrom(inputStream)
            map + (chartLevel -> c.copy(values = Some(Config(raw = rawBytes.toString("UTF-8")))))
          case otherwise =>
            val data = ByteString.readFrom(inputStream)
            val c = map.getOrElse(chartLevel, Chart())
            val file = new Any(otherwise, data)
            map + (chartLevel -> c.copy(files = c.files :+ file))
        }.getOrElse(map)
    }
  }
}
