# Sokkan
Sokkan (Persian: سُکّان) means helm. Sokkan is a [Scala](https://scala-lang.org/) client for interacting with [Helm](https://helm.sh/).

## Getting Started

### Imports
```scala
import sokkan._
import sokkan.grpc._

import cats.instances.future._
import scala.concurrent.ExecutionContext.Implicits.global
```

### Algebra
```scala
import hapi.version.version.Version
import hapi.services.tiller.tiller.GetVersionRequest

val v: SokkanOp.ReleaseService[Option[Version]] = for {
  v <- SokkanOp.getVersion(GetVersionRequest())
} yield v.version
```

### The GRPC interpreter
```scala
val interpreter = new GrpcHelmClient("localhost", 44134, Some("2.8.1"))

sokkan.run(interpreter, v).onComplete(println)
```
