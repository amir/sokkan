import com.google.protobuf.ByteString
import net.jcazevedo.moultingyaml.{FlowStyle, LineBreak, ScalarStyle, YamlArray, YamlBoolean, YamlNull, YamlNumber, YamlObject, YamlString, YamlValue}
import play.api.libs.json._
import skuber.ObjectResource

class YamlObjectResource[T <: ObjectResource](o: T)(implicit fmt: Format[T]) {
  private def jsValueToYamlValue(json: JsValue): YamlValue = json match {
    case o: JsObject =>
      YamlObject(o.fields.map(f => (YamlString(f._1): YamlValue) -> jsValueToYamlValue(f._2)).toMap)
    case a: JsArray =>
      YamlArray(a.value.map(jsValueToYamlValue).toVector)
    case n: JsNumber => YamlNumber(n.value)
    case s: JsString => YamlString(s.value)
    case b: JsBoolean => YamlBoolean(b.value)
    case _ => YamlNull
  }

  def toYaml: YamlValue = jsValueToYamlValue(Json.toJson(o))

  def toYamlByteString: ByteString = ByteString.copyFrom(
    toYaml.print(
      flowStyle = FlowStyle.DEFAULT,
      scalarStyle = ScalarStyle.DEFAULT,
      lineBreak = LineBreak.DEFAULT).getBytes
  )
}

object YamlObjectResource {
  implicit def objectToYaml[T <: ObjectResource](o: T)(implicit fmt: Format[T]) = new YamlObjectResource[T](o)
}

