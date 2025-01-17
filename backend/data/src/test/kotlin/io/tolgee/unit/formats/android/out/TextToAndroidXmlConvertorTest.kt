package io.tolgee.unit.formats.android.out

import io.tolgee.formats.android.AndroidStringValue
import io.tolgee.formats.android.out.TextToAndroidXmlConvertor
import io.tolgee.testing.assert
import org.assertj.core.api.AbstractStringAssert
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class TextToAndroidXmlConvertorTest {
  @Test
  fun `xml and placeholders is converted to CDATA`() {
    "<b>%s</b>".assertSingleCdataNodeText().isEqualTo("<b>%s</b>")
  }

  @Test
  fun `apostrophe is escaped in the HTML CDATA node`() {
    "<b>%s ' </b>".assertSingleCdataNodeText().isEqualTo("<b>%s \\' </b>")
  }

  @Test
  fun `double quotes and escape chars are escaped in the HTML CDATA node`() {
    "<b>%s \" \\ </b>".assertSingleCdataNodeText().isEqualTo("<b>%s \\\" \\\\ </b>")
  }

  @Test
  fun `more whitespaces are not converted in the HTML CDATA node`() {
    "<b>%s   </b>".assertSingleCdataNodeText().isEqualTo("<b>%s   </b>")
  }

  @Test
  fun `unsupported tags are converted to CDATA nodes`() {
    var nodes =
      "What a <unsupported attr=\"https://example.com\">link ' %% \" </unsupported>."
        .convertedNodes().toList()
    nodes[0].assertTextContent("What a ")
    nodes[1].nodeAssertCdataNodeText(
      "<unsupported attr=\\\"https://example.com\\\">link \\' % \\\" " +
        "</unsupported>",
    )
    nodes[2].assertTextContent(".")

    nodes =
      (
        "What a <unsupported attr=\"https://example.com\">link ' %% %s \"    " +
          "</unsupported>."
      ).convertedNodes().toList()
    nodes[0].assertTextContent("What a ")
    nodes[1].nodeAssertCdataNodeText(
      "<unsupported attr=\\\"https://example.com\\\">link \\' %% %s \\\"    " +
        "</unsupported>",
    )
    nodes[2].assertTextContent(".")
  }

  @Test
  fun `all possible spaces are quoted`() {
    "a\n\t   \u0020 \u2008 \u2003a".assertSingleTextNode("a\"\\n\t   \u0020 \u2008 \u2003\"a")
  }

  @Test
  fun `escapes in text nodes`() {
    val nodes = "'\"  <b></b>\n\n   \u0020\u2008\u2003".convertedNodes().toList()
    nodes[0].textContent.assert.isEqualTo("\\'\\\"\"  \"")
    nodes[1].nodeName.assert.isEqualTo("b")
    nodes[2].textContent.assert.isEqualTo("\"\n\n   \u0020\u2008\u2003\"")
  }

  @Test
  fun `new lines are escaped in cdata string`() {
    val nodes = "\n\n".convertedNodes(isWrappedWithCdata = true)
    nodes.getSingleNode().assertSingleCdataNodeText().isEqualTo("\\n\\n")
  }

  private fun Node.assertTextContent(text: String) {
    this.nodeType.assert.isEqualTo(Node.TEXT_NODE)
    this.textContent.assert.isEqualTo(text)
  }

  private fun String.assertSingleTextNode(text: String) {
    this.assertSingleTextNode().isEqualTo(text)
  }

  private fun String.assertSingleTextNode(): AbstractStringAssert<*> {
    val nodes = this.convertedNodes()
    return nodes.getSingleNode().textContent.assert
  }

  private fun Collection<Node>.getSingleNode(): Node {
    this.assert.hasSize(1)
    return this.single()
  }

  private fun Node.nodeAssertCdataNodeText(text: String) {
    this.nodeType.assert.isEqualTo(Node.CDATA_SECTION_NODE)
    this.textContent.assert.isEqualTo(text)
  }

  private fun String.assertSingleCdataNodeText(): AbstractStringAssert<*> {
    val node = this.convertedNodes().single()
    return node.assertSingleCdataNodeText()
  }

  private fun Node.assertSingleCdataNodeText(): AbstractStringAssert<*> {
    this.nodeType.assert.isEqualTo(Node.CDATA_SECTION_NODE)
    return this.textContent.assert
  }

  private fun String.getConverted(isWrappedWithCdata: Boolean = false) =
    TextToAndroidXmlConvertor(document, AndroidStringValue(this, isWrappedWithCdata)).convert()

  private fun String.convertedNodes(isWrappedWithCdata: Boolean = false): Collection<Node> {
    val result = this.getConverted(isWrappedWithCdata)
    result.text.assert.isNull()
    result.children.assert.isNotNull
    return result.children!!
  }

  private val document: Document by lazy {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    builder.newDocument()
  }
}
