import java.io.File

import cats.{Id, ~>}
import hapi.chart.chart.Chart
import hapi.chart.config.Config
import cats.instances.try_._
import hapi.release.release.Release
import hapi.services.tiller.tiller._
import io.iteratee.modules.{IdModule, TryModule}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.TryValues._
import sokkan.{ReleaseServiceA, RootChart, SokkanOp}
import sokkan.SokkanOp._
import sokkan.iteratee.chart.ChartModule
import sokkan.iteratee.tar.NonSuspendableTapeArchiveModule

import scala.util.{Success, Try}

class ReleaseServiceStateTests extends FlatSpec with Matchers {

  val interpreter: ReleaseServiceA ~> Try = new (ReleaseServiceA ~> Try) {
    val state = scala.collection.mutable.Map.empty[String, Option[Chart]]

    trait TryChartModule extends TryModule with NonSuspendableTapeArchiveModule[Try] with ChartModule[Try]
    final object tryCharts extends TryChartModule

    def apply[A](fa: ReleaseServiceA[A]): Try[A] =
      fa match {
        case InstallRelease(req: InstallReleaseRequest) =>
          def in(n: String, idx: Int): Int = {
            state.get(s"${n}.v${idx}") match {
              case Some(_) => in(n, idx+1)
              case None =>
                state(s"${n}.v${idx}") = req.chart
                idx
            }
          }
          val v = in(req.name, 1)
          Success(InstallReleaseResponse(Some(Release(name = req.name, version = v))))

        case UpdateRelease(req: UpdateReleaseRequest) =>
          val rs = state.keySet.filter(_.startsWith(s"${req.name}.v")).toList
            .sortBy(- _.replace(s"${req.name}.v", "").toInt)
          val newV = rs.head.replace(s"${req.name}.v", "").toInt + 1

          state(s"${req.name}.v${newV}") = req.chart

          Success(UpdateReleaseResponse(Some(Release(name = req.name, version = newV))))

        case GetReleaseContent(req: GetReleaseContentRequest) =>
          Success(GetReleaseContentResponse(release = state.get(s"${req.name}.v${req.version}").map(c => Release(chart = c))))

        case GetChartFromTapeArchive(file: File) =>
          tryCharts.chartFromFiles(tryCharts.readTapeArchiveStreams(file)).run.map { s =>
            s.get(RootChart).map { rootChart =>
              val deps = (s - RootChart).values.toList
              rootChart.copy(dependencies = deps)
            }
          }


      }
  }

  val name = "test"

  val p: ReleaseService[Option[Release]] = for {
    _ <- SokkanOp.install(InstallReleaseRequest(name = name))
    u <- SokkanOp.update(UpdateReleaseRequest(name = name))
  } yield u.release

  sokkan.run(interpreter, p).success.value should matchPattern { case Some(Release(name, _, _, _, _, _, 2, _)) => }

  val p2: ReleaseService[Option[Chart]] =
    SokkanOp.chartFromTapeArchive(new File(getClass.getResource("mychart-0.1.0.tgz").getPath))

  val chart = sokkan.run(interpreter, p2)

  chart.success.value shouldBe defined
}
