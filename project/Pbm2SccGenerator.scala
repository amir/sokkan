import com.google.protobuf.Descriptors.{EnumDescriptor, FileDescriptor}
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}

import scalapb.compiler.{DescriptorPimps, FunctionalPrinter}
import scala.collection.JavaConverters._
import scalapb.options.compiler.Scalapb

object Pbm2SccGenerator extends protocbridge.ProtocCodeGenerator with DescriptorPimps {
  override def params = scalapb.compiler.GeneratorParams()

  override def run(input: Array[Byte]): Array[Byte] = {
    val registry = ExtensionRegistry.newInstance()
    Scalapb.registerAllExtensions(registry)

    val request = CodeGeneratorRequest.parseFrom(input, registry)
    val b = CodeGeneratorResponse.newBuilder

    val fileDescByName: Map[String, FileDescriptor] =
      request.getProtoFileList.asScala.foldLeft[Map[String, FileDescriptor]](Map.empty) {
        case (acc, fp) =>
          val deps = fp.getDependencyList.asScala.map(acc)
          acc + (fp.getName -> FileDescriptor.buildFrom(fp, deps.toArray))
      }

    request.getFileToGenerateList.asScala.foreach {
      name =>
        val fileDesc = fileDescByName(name)
        val responseFile = generateFile(fileDesc)
        b.addFile(responseFile)
    }

    b.build.toByteArray
  }

  def printEnum(printer: FunctionalPrinter, e: EnumDescriptor): FunctionalPrinter = {
    val name = e.nameSymbol
    printer
      .add(s"sealed trait $name")
      .print(e.getValues.asScala) {
        case (p, m) =>
          p.add(s"case object $m extends $name")
      }
  }

  def generateFile(fileDesc: FileDescriptor): CodeGeneratorResponse.File = {
    val b = CodeGeneratorResponse.File.newBuilder
    b.setName(s"${fileDesc.scalaDirectory}/${fileDesc.fileDescriptorObjectName}.scala")
    val fp = FunctionalPrinter()
      .add(s"package ${fileDesc.scalaPackageName}")
      .add("")
      .print(fileDesc.getMessageTypes.asScala) {
        case (p, m) =>
          val ps = m.getFields.asScala.map { f =>
            s"${f.scalaName.asSymbol}: ${f.scalaTypeName}"
          }
          p.add(s"final case class ${m.getName}(")
            .indent
            .add(ps.mkString(",\n  "))
            .outdent
            .add(")")
            .add(s"object ${m.getName} {")
            .indent
            .print(m.getEnumTypes.asScala)(printEnum)
            .outdent
            .add("}")
            .add("")
      }

    b.setContent(fp.result())
    b.build()
  }

}
