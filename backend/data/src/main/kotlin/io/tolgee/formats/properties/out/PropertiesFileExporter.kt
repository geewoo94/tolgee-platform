package io.tolgee.formats.properties.out

import io.tolgee.dtos.IExportParams
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.PropertiesConfiguration.DefaultIOFactory
import org.apache.commons.configuration2.PropertiesConfiguration.PropertiesWriter
import org.apache.commons.configuration2.convert.ListDelimiterHandler
import org.apache.commons.configuration2.convert.ValueTransformer
import org.apache.commons.text.translate.AggregateTranslator
import org.apache.commons.text.translate.EntityArrays
import org.apache.commons.text.translate.LookupTranslator
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringWriter
import java.io.Writer

class PropertiesFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
  val convertMessage: (message: String?, isPlural: Boolean) -> String? = { message, _ -> message },
) : FileExporter {
  override val fileExtension: String = "properties"

  val result: MutableMap<String, PropertiesConfiguration> = mutableMapOf()

  private fun prepare() {
    translations.forEach { translation ->
      val fileName = computeFileName(translation)
      val keyName = translation.key.name
      val value = convertMessage(translation.text, translation.key.isPlural)
      val properties = result.getOrPut(fileName) { PropertiesConfiguration() }
      properties.setProperty(keyName, value)
      properties.layout.setComment(keyName, translation.key.description)
    }
  }

  override fun produceFiles(): Map<String, InputStream> {
    prepare()
    return result.asSequence().map { (fileName, properties) ->
      // convert properties to bytes
      val bytes = properties.asByteArray()
      fileName to ByteArrayInputStream(bytes)
    }.toMap()
  }

  private fun PropertiesConfiguration.asByteArray(): ByteArray {
    val writer = StringWriter()
    this.ioFactory = Utf8IoFactory()
    this.write(writer)
    return writer.toString().toByteArray()
  }

  private fun computeFileName(translation: ExportTranslationView): String {
    return translation.getFilePath()
  }
}

/**
 * This class is custom implementation of the [ValueTransformer] that prevents escaping of UTF-8 characters
 * UTF-8 is supported in properties files by Java 9
 */
private class Utf8ValueTransformer : ValueTransformer {
  companion object {
    private val charsEscape = mapOf<CharSequence, CharSequence>("\\" to "\\\\")
    private val escapeProperties =
      AggregateTranslator(
        LookupTranslator(charsEscape),
        LookupTranslator(
          EntityArrays.JAVA_CTRL_CHARS_ESCAPE,
        ),
      )
  }

  override fun transformValue(p0: Any?): Any {
    val strVal = p0.toString()
    return escapeProperties.translate(strVal)
  }
}

private class Utf8IoFactory : DefaultIOFactory() {
  companion object {
    private val valueTransformer = Utf8ValueTransformer()
  }

  override fun createPropertiesWriter(
    out: Writer?,
    handler: ListDelimiterHandler?,
  ): PropertiesWriter {
    return PropertiesWriter(out, handler, valueTransformer)
  }
}
