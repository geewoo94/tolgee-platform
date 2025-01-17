package io.tolgee.formats.xliff.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.xliff.model.XliffFile
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.formats.xliff.model.XliffTransUnit
import io.tolgee.model.ILanguage
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class XliffFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
  baseTranslationsProvider: () -> List<ExportTranslationView>,
  val baseLanguage: ILanguage,
  val convertMessage: (message: String?, isPlural: Boolean) -> String? = { message, _ -> message },
) : FileExporter {
  override val fileExtension: String = ExportFormat.XLIFF.extension

  /**
   * Path -> Xliff Model
   */
  val models = mutableMapOf<String, XliffModel>()
  private val baseTranslations by lazy {
    baseTranslationsProvider().associateBy { it.key.namespace to it.key.name }
  }

  override fun produceFiles(): Map<String, InputStream> {
    prepare()
    return models.asSequence().map { (fileName, resultItem) ->
      fileName to XliffFileWriter(xliffModel = resultItem, enableXmlContent = true).produceFiles()
    }.toMap()
  }

  private fun prepare() {
    translations.forEach { translation ->
      val resultItem = getResultXliffFile(translation)
      addTranslation(resultItem, translation)
    }
  }

  private fun addTranslation(
    resultItem: XliffFile,
    translation: ExportTranslationView,
  ) {
    resultItem.transUnits.add(
      XliffTransUnit().apply {
        this.id = translation.key.name
        this.source =
          convertMessage(
            baseTranslations[translation.key.namespace to translation.key.name]?.text,
            translation.key.isPlural,
          )
        this.target = convertMessage(translation.text, translation.key.isPlural)
        this.note = translation.key.description
      },
    )
  }

  private fun getResultXliffFile(translation: ExportTranslationView): XliffFile {
    val absolutePath = translation.getFilePath()
    return models.computeIfAbsent(absolutePath) {
      XliffModel().apply {
        files.add(
          XliffFile().apply {
            this.sourceLanguage = baseLanguage.tag
            this.targetLanguage = translation.languageTag
          },
        )
      }
    }.files.first()
  }
}
