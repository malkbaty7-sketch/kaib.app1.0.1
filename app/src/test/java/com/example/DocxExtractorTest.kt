package com.example

import com.example.util.DocxExtractor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocxExtractorTest {

    @Test
    fun testDocxExtractionZeroDependency() {
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:pPr>
                            <w:pStyle w:val="Heading1"/>
                        </w:pPr>
                        <w:r>
                            <w:t>الفصل الأول: البداية</w:t>
                        </w:r>
                    </w:p>
                    <w:p>
                        <w:r>
                            <w:t>هذا نص الفقرة الأولى لاختبار القراءة السريعة الخفيفة بدون مكتبات إضافية.</w:t>
                        </w:r>
                    </w:p>
                    <w:p>
                        <w:r>
                            <w:t>فقرة ثانية داعمة للغة العربية.</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(xmlContent.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        val zipBytes = byteOut.toByteArray()
        val inputStream = ByteArrayInputStream(zipBytes)

        val paragraphs = DocxExtractor.extractParagraphs(inputStream)
        println("Extracted paragraphs count: ${paragraphs.size}")
        paragraphs.forEach { println("PARAGRAPH: isHeading=${it.isHeading}, text=${it.text}") }

        assertEquals(3, paragraphs.size)
        assertTrue(paragraphs[0].isHeading)
        assertEquals("الفصل الأول: البداية", paragraphs[0].text)
        assertFalse(paragraphs[1].isHeading)
        assertEquals("هذا نص الفقرة الأولى لاختبار القراءة السريعة الخفيفة بدون مكتبات إضافية.", paragraphs[1].text)
        assertEquals("فقرة ثانية داعمة للغة العربية.", paragraphs[2].text)

        val fullText = DocxExtractor.extractFullText(ByteArrayInputStream(zipBytes))
        assertTrue(fullText.contains("الفصل الأول: البداية"))
        assertTrue(fullText.contains("فقرة ثانية داعمة للغة العربية."))
    }
}
