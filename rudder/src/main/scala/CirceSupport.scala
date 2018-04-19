package sokkan.rudder

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import scala.concurrent.Future

object CirceSupport {

  implicit val decodeByteString: Decoder[com.google.protobuf.ByteString] = (c: HCursor) => for {
    v <- c.value.as[String]
  } yield com.google.protobuf.ByteString.copyFromUtf8(v)

  implicit final def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] =
    Unmarshaller.withMaterializer {
      implicit ex => implicit mat => entity: HttpEntity =>
        entity.dataBytes
          .runFold(ByteString.empty)(_ ++ _)
          .flatMap(s => decode[A](s.utf8String).fold(Future.failed, Future.successful))
    }

  implicit final def marshaller[A: Encoder]: ToEntityMarshaller[A] = {
    Marshaller.withFixedContentType(`application/json`) { a =>
      HttpEntity(`application/json`, a.asJson.noSpaces)
    }
  }
}

