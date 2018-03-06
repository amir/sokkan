import cats.{Id, ~>}
import hapi.chart.chart.Chart
import hapi.release.release.Release
import hapi.services.tiller.tiller._
import org.scalatest.{FlatSpec, Matchers}
import sokkan.{ReleaseServiceA, SokkanOp}
import sokkan.SokkanOp._

class ReleaseServiceStateTests extends FlatSpec with Matchers {

  val interpreter: ReleaseServiceA ~> Id = new (ReleaseServiceA ~> Id) {
    val state = scala.collection.mutable.Map.empty[String, Option[Chart]]

    def apply[A](fa: ReleaseServiceA[A]): Id[A] =
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
          InstallReleaseResponse(Some(Release(name = req.name, version = v)))

        case UpdateRelease(req: UpdateReleaseRequest) =>
          val rs = state.keySet.filter(_.startsWith(s"${req.name}.v")).toList
            .sortBy(- _.replace(s"${req.name}.v", "").toInt)
          val newV = rs.head.replace(s"${req.name}.v", "").toInt + 1

          state(s"${req.name}.v${newV}") = req.chart

          UpdateReleaseResponse(Some(Release(name = req.name, version = newV)))

        case GetReleaseContent(req: GetReleaseContentRequest) =>
          GetReleaseContentResponse(release = state.get(s"${req.name}.v${req.version}").map(c => Release(chart = c)))

      }
  }

  val name = "test"

  val p: ReleaseService[Option[Release]] = for {
    _ <- SokkanOp.install(InstallReleaseRequest(name = name))
    u <- SokkanOp.update(UpdateReleaseRequest(name = name))
  } yield u.release

  sokkan.run(interpreter, p) should matchPattern { case Some(Release(name, _, _, _, _, _, 2, _)) => }
}
