package io.tolgee.formats.android.`in`

import AndroidStringsXmlParser
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.ImportMessageConvertorType
import io.tolgee.formats.StringWrapper
import io.tolgee.formats.android.ANDROID_CDATA_CUSTOM_KEY
import io.tolgee.formats.android.AndroidStringValue
import io.tolgee.formats.android.PluralUnit
import io.tolgee.formats.android.StringArrayUnit
import io.tolgee.formats.android.StringUnit
import io.tolgee.service.dataImport.processors.FileProcessorContext
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class AndroidStringsXmlProcessor(override val context: FileProcessorContext) : ImportFileProcessor() {
  override fun process() {
    val parsed = AndroidStringsXmlParser(xmlEventReader).parse()

    parsed.items.forEach { (keyName, item) ->
      when (item) {
        is StringUnit -> handleString(keyName, item)
        is PluralUnit -> handlePlural(keyName, item)
        is StringArrayUnit -> handleStringsArray(keyName, item)
        else -> {}
      }
    }
  }

  private fun handleString(
    keyName: String,
    it: StringUnit,
  ) {
    if (keyName.isBlank()) {
      return
    }
    val text = it.value?.string
    context.addTranslation(
      keyName,
      guessedLanguage,
      convertMessage(text),
      forceIsPlural = false,
      rawData = StringWrapper(text),
      convertedBy = ImportMessageConvertorType.ANDROID_XML,
    )
    setCustomWrappedWithCdata(keyName, it.value)
  }

  private fun setCustomWrappedWithCdata(
    keyName: String,
    value: Collection<AndroidStringValue>,
  ) {
    val isWrappedCdata = value.any { it.isWrappedCdata }
    if (isWrappedCdata) {
      context.setCustom(keyName, ANDROID_CDATA_CUSTOM_KEY, true)
    }
  }

  private fun setCustomWrappedWithCdata(
    keyName: String,
    value: AndroidStringValue?,
  ) {
    setCustomWrappedWithCdata(keyName, value?.let { listOf(it) } ?: emptyList())
  }

  private fun handlePlural(
    keyName: String,
    it: PluralUnit,
  ) {
    if (keyName.isBlank()) {
      return
    }

    val rawData = it.items.map { it.key to it.value.string }.toMap()

    val converted =
      AndroidToIcuMessageConvertor().convert(
        rawData = rawData,
        languageTag = guessedLanguage,
        convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
        context.projectIcuPlaceholdersEnabled,
      )

    context.addTranslation(
      keyName,
      guessedLanguage,
      converted.message,
      forceIsPlural = true,
      rawData = rawData,
      convertedBy = ImportMessageConvertorType.ANDROID_XML,
    )

    setCustomWrappedWithCdata(keyName, it.items.map { it.value })
  }

  private fun handleStringsArray(
    keyName: String,
    arrayUnit: StringArrayUnit,
  ) {
    if (keyName.isBlank()) {
      return
    }
    arrayUnit.items.forEachIndexed { index, item ->
      val keyNameWithIndex = "$keyName[$index]"

      val text = item.value?.string
      context.addTranslation(
        keyNameWithIndex,
        guessedLanguage,
        convertMessage(text),
        forceIsPlural = false,
        rawData = StringWrapper(text),
        convertedBy = ImportMessageConvertorType.ANDROID_XML,
      )
      setCustomWrappedWithCdata(keyNameWithIndex, item.value)
    }
  }

  private val guessedLanguage: String by lazy {
    val matchResult = LANGUAGE_GUESS_REGEX.find(context.file.name) ?: return@lazy "unknown"
    val language = matchResult.groups["language"]!!.value
    val region = matchResult.groups["region"]?.value
    val regionString = region?.let { "-$it" } ?: ""
    "$language$regionString"
  }

  private val xmlEventReader: XMLEventReader by lazy {
    val inputFactory: XMLInputFactory = XMLInputFactory.newInstance()
    inputFactory.createXMLEventReader(context.file.data.inputStream())
  }

  private fun convertMessage(message: String?): String? {
    if (message == null) {
      return null
    }
    return AndroidToIcuMessageConvertor().convert(
      message,
      guessedLanguage,
      context.importSettings.convertPlaceholdersToIcu,
      context.projectIcuPlaceholdersEnabled,
    ).message
  }

  companion object {
    val LANGUAGE_GUESS_REGEX = Regex("values-(?<language>[a-zA-Z]{2,3})(-r(?<region>[a-zA-Z]{2,3}))?")
  }
}
