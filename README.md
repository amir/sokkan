```scala
import sokkan._
import sokkan.grpc._

import hapi.services.tiller.tiller.GetVersionRequest
import hapi.version.version.Version
import cats.instances.future._

import scala.concurrent.ExecutionContext.Implicits.global
```

```scala
val v: SokkanOp.ReleaseService[Option[Version]] = for {
  v <- SokkanOp.getVersion(GetVersionRequest())
} yield v.version

val interpreter = new GrpcHelmClient("localhost", 44134, Some("2.8.1"))

sokkan.run(interpreter, v).onComplete(println)
```
