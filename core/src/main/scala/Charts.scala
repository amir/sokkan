package sokkan

sealed trait ChartLevel
case object RootChart extends ChartLevel
case class SubChart(chartName: String) extends ChartLevel

object Charts {
  private val chartNamepattern = "^.+/charts/([^/]+).*$".r
  private val templateFileNamePattern = "^.+/(templates/.+)$".r
  private val subChartFileNamePattern = "^.+/charts/([^._][^/]+/?(.*))$".r
  private val ordinaryFileNamePattern = "^/*[^/]+(?!.*/(?:charts|templates)/)/(.+)$".r
  private val subChartsPattern = ".*?/charts/[^/]+".r

  def getChartName(chartPath: String): Option[String] = {
    chartNamepattern.findFirstMatchIn(chartPath).map(_.group(1))
  }

  def getTemplateFileName(path: String): Option[String] = {
    templateFileNamePattern.findFirstMatchIn(path).map(_.group(1))
  }

  def getSubChartFileName(path: String): Option[String] = {
    subChartFileNamePattern.findFirstMatchIn(path).map { m =>
      val group2 = m.group(2)
      if (group2.isEmpty) m.group(1) else group2
    }
  }

  def getOrdinaryFileName(path: String): Option[String] = {
    ordinaryFileNamePattern.findFirstMatchIn(path).map(_.group(1))
  }

  def toSubCharts(chartPath: String): List[String] = {
    subChartsPattern.findAllMatchIn(chartPath).map(x => chartPath.substring(0, x.end)).toList
  }
}
