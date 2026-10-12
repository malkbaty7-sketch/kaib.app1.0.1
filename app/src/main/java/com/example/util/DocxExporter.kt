package com.example.util

import android.content.Context
import com.example.data.model.ChapterItem
import com.example.data.model.Project
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxExporter {

    fun exportProjectToDocx(
        context: Context,
        project: Project,
        chapters: List<ChapterItem>
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeFileName = project.projectName.replace(Regex("""[^\w\u0600-\u06FF]"""), "_")
        val outputFile = File(exportDir, "${safeFileName}_${System.currentTimeMillis()}.docx")

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // 1. [Content_Types].xml
            addZipEntry(zos, "[Content_Types].xml", getContentTypesXml())

            // 2. _rels/.rels
            addZipEntry(zos, "_rels/.rels", getRelsXml())

            // 3. word/_rels/document.xml.rels
            addZipEntry(zos, "word/_rels/document.xml.rels", getDocRelsXml())

            // 4. word/document.xml
            val docXml = buildDocumentXml(project, chapters)
            addZipEntry(zos, "word/document.xml", docXml)
        }

        return outputFile
    }

    private fun addZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun getContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

    private fun getRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private fun getDocRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>"""

    private fun buildDocumentXml(project: Project, chapters: List<ChapterItem>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        sb.append("<w:body>")

        // Document Title
        sb.append("<w:p>")
        sb.append("<w:pPr><w:jc w:val=\"center\"/><w:bidi/></w:pPr>")
        sb.append("<w:r><w:rPr><w:rFonts w:ascii=\"Arial\" w:cs=\"Arial\"/><w:b/><w:sz w:val=\"48\"/><w:rtl/></w:rPr>")
        sb.append("<w:t>${escapeXml(project.projectName)}</w:t></w:r>")
        sb.append("</w:p>")

        // Subtitle
        sb.append("<w:p>")
        sb.append("<w:pPr><w:jc w:val=\"center\"/><w:bidi/></w:pPr>")
        sb.append("<w:r><w:rPr><w:rFonts w:ascii=\"Arial\" w:cs=\"Arial\"/><w:i/><w:sz w:val=\"24\"/><w:color w:val=\"666666\"/><w:rtl/></w:rPr>")
        sb.append("<w:t>${escapeXml("تطبيق خطة • ${project.projectType}")}</w:t></w:r>")
        sb.append("</w:p>")

        sb.append("<w:p><w:pPr><w:bidi/></w:pPr></w:p>") // Spacer

        // Chapters
        for (chapter in chapters) {
            // Chapter Title Heading 1
            sb.append("<w:p>")
            sb.append("<w:pPr><w:bidi/><w:pStyle w:val=\"Heading1\"/></w:pPr>")
            sb.append("<w:r><w:rPr><w:rFonts w:ascii=\"Arial\" w:cs=\"Arial\"/><w:b/><w:sz w:val=\"36\"/><w:color w:val=\"D2691E\"/><w:rtl/></w:rPr>")
            sb.append("<w:t>${escapeXml(chapter.title)}</w:t></w:r>")
            sb.append("</w:p>")

            // Paragraphs
            val paragraphs = chapter.content.split("\n\n")
            for (para in paragraphs) {
                if (para.isNotBlank()) {
                    sb.append("<w:p>")
                    sb.append("<w:pPr><w:bidi/><w:spacing w:line=\"360\" w:lineRule=\"auto\"/></w:pPr>")
                    sb.append("<w:r><w:rPr><w:rFonts w:ascii=\"Arial\" w:cs=\"Arial\"/><w:sz w:val=\"26\"/><w:rtl/></w:rPr>")
                    sb.append("<w:t>${escapeXml(para.trim())}</w:t></w:r>")
                    sb.append("</w:p>")
                }
            }

            // Chapter separator
            sb.append("<w:p><w:pPr><w:bidi/></w:pPr></w:p>")
        }

        sb.append("</w:body></w:document>")
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
