package com.example.util

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

data class DocxParagraph(
    val text: String,
    val isHeading: Boolean = false
)

object DocxExtractor {

    /**
     * استخراج كل الفقرات والنصوص من ملف DOCX عبر Uri الخاص بـ Storage Access Framework
     */
    fun extractParagraphs(context: Context, fileUri: Uri): List<DocxParagraph> {
        val paragraphs = mutableListOf<DocxParagraph>()

        try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                paragraphs.addAll(extractParagraphs(inputStream))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return paragraphs
    }

    /**
     * استخراج كل الفقرات والنصوص من InputStream لملف DOCX مضغوط
     */
    fun extractParagraphs(inputStream: InputStream): List<DocxParagraph> {
        val paragraphs = mutableListOf<DocxParagraph>()

        try {
            val documentXmlStream = getDocumentXmlStream(inputStream)
            if (documentXmlStream != null) {
                paragraphs.addAll(parseDocumentXml(documentXmlStream))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return paragraphs
    }

    /**
     * استخراج النص الكامل مجمعاً ومفصولاً بأسطر بين الفقرات
     */
    fun extractFullText(context: Context, fileUri: Uri): String {
        val paragraphs = extractParagraphs(context, fileUri)
        return if (paragraphs.isEmpty()) "" else paragraphs.joinToString("\n\n") { it.text }
    }

    /**
     * استخراج النص الكامل مجمعاً من InputStream
     */
    fun extractFullText(inputStream: InputStream): String {
        val paragraphs = extractParagraphs(inputStream)
        return if (paragraphs.isEmpty()) "" else paragraphs.joinToString("\n\n") { it.text }
    }

    /**
     * 1. فك ضغط أرشيف ZIP والبحث عن ملف word/document.xml المخصص لمحتوى المستند
     */
    private fun getDocumentXmlStream(zipInput: InputStream): InputStream? {
        val zipStream = ZipInputStream(zipInput)
        var entry = zipStream.nextEntry

        while (entry != null) {
            if (entry.name == "word/document.xml") {
                return zipStream // إرجاع مجرى الملف عند العثور عليه
            }
            entry = zipStream.nextEntry
        }
        return null
    }

    /**
     * 2. تحليل ملف XML واستخراج فقرات <w:p> والنصوص <w:t> باستخدام XmlPullParser
     */
    private fun parseDocumentXml(xmlStream: InputStream): List<DocxParagraph> {
        val paragraphs = mutableListOf<DocxParagraph>()

        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val parser = factory.newPullParser().apply {
            setInput(xmlStream, "UTF-8")
        }

        var eventType = parser.eventType
        val currentParagraphBuilder = StringBuilder()
        var isInParagraph = false
        var isInText = false
        var isHeading = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName == "p") { // بداية فقرة جديدة <w:p>
                        isInParagraph = true
                        isHeading = false
                        currentParagraphBuilder.clear()
                    } else if (tagName == "pStyle" && isInParagraph) {
                        // فحص إذا كانت الفقرة عنواناً (Heading / Title)
                        for (i in 0 until parser.attributeCount) {
                            val attrVal = parser.getAttributeValue(i)
                            if (attrVal != null && (
                                    attrVal.contains("Heading", ignoreCase = true) ||
                                    attrVal.contains("Title", ignoreCase = true) ||
                                    attrVal.contains("عنوان", ignoreCase = true)
                                )) {
                                isHeading = true
                                break
                            }
                        }
                    } else if (tagName == "t" && isInParagraph) { // بداية نص <w:t>
                        isInText = true
                    }
                }

                XmlPullParser.TEXT -> {
                    if (isInText) {
                        currentParagraphBuilder.append(parser.text)
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (tagName == "t") {
                        isInText = false
                    } else if (tagName == "p") { // نهاية الفقرة <w:p>
                        isInParagraph = false
                        val paragraphText = currentParagraphBuilder.toString().trim()
                        if (paragraphText.isNotEmpty()) {
                            paragraphs.add(DocxParagraph(text = paragraphText, isHeading = isHeading))
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return paragraphs
    }
}
