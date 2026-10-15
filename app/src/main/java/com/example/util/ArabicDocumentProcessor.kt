package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import com.example.data.ai.GeminiAiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * محرك معالجة واستخراج النصوص العربية المتقدم والتشكيل (Arabic Document Processor & Converter)
 * يدعم التشكيل العربي الكامل (الفتحة، الضمة، الكسرة، الشدة، السكون، التنوين)
 * معالجة أشكال العرض (NFKC) ودعم RTL للـ PDF، DOCX، TXT والصور الممسوحة ضوئياً عبر Gemini OCR.
 */
object ArabicDocumentProcessor {

    enum class ReadingMode {
        AUTO,    // تلقائي (نص مباشر، وOCR بالذكاء الاصطناعي إذا لزم)
        AI       // ذكاء اصطناعي دائماً (أدق للمستندات الممسوحة والصور والتشكيل)
    }

    enum class TargetFormat(val extension: String, val displayName: String, val mimeType: String) {
        DOCX("docx", "Word (DOCX)", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        PDF("pdf", "PDF", "application/pdf"),
        HTML("html", "HTML", "text/html"),
        TXT("txt", "نص (TXT)", "text/plain")
    }

    data class ProcessResult(
        val paragraphs: List<String>,
        val fullText: String,
        val outputFile: File?,
        val previewHtml: String,
        val detectedRtl: Boolean,
        val wasAiUsed: Boolean
    )

    /* ------------------------------ أدوات العربية ------------------------------ */

    private val ARABIC_REGEX = Regex("""[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]""")
    private val PRESENTATION_FORMS_REGEX = Regex("""[\uFB50-\uFDFF\uFE70-\uFEFF]""")

    /**
     * تحويل أشكال العرض المتصلة إلى حروف عربية أساسية مع الحفاظ على التشكيل
     */
    fun fixArabic(s: String): String {
        return if (PRESENTATION_FORMS_REGEX.containsMatchIn(s)) {
            Normalizer.normalize(s, Normalizer.Form.NFKC)
        } else {
            s
        }
    }

    /**
     * التحقق مما إذا كان النص عربياً أو موجهاً من اليمين لليسار (RTL)
     */
    fun isRtl(s: String): Boolean {
        val arabicMatches = ARABIC_REGEX.findAll(s).count()
        val letters = s.filter { it.isLetter() }.length.coerceAtLeast(1)
        return (arabicMatches.toDouble() / letters) > 0.4
    }

    fun escapeHtml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    fun splitIntoParagraphs(text: String, keepLines: Boolean): List<String> {
        val parts = if (keepLines) {
            text.split(Regex("""\n+"""))
        } else {
            text.split(Regex("""\n\s*\n+"""))
        }
        return parts.map { fixArabic(it.trim()) }.filter { it.isNotBlank() }
    }

    /**
     * بناء HTML متكامل مرمز بـ RTL وخط Amiri لسهولة العرض والطباعة كـ PDF
     */
    fun wrapHtml(bodyContent: String, title: String): String {
        return """
<!doctype html>
<html lang="ar" dir="rtl">
<head>
<meta charset="utf-8">
<title>${escapeHtml(title)}</title>
<link href="https://fonts.googleapis.com/css2?family=Amiri:wght@400;700&display=swap" rel="stylesheet">
<style>
body {
    font-family: 'Amiri', 'Traditional Arabic', 'Noto Naskh Arabic', serif;
    font-size: 18px;
    line-height: 2;
    max-width: 800px;
    margin: 30px auto;
    padding: 0 20px;
    color: #1e293b;
    background-color: #ffffff;
}
p, li, td, th, h1, h2, h3, h4, h5, h6 {
    unicode-bidi: plaintext;
    text-align: start;
}
p {
    margin: 0 0 0.8em 0;
    text-align: justify;
}
table {
    border-collapse: collapse;
    width: 100%;
}
td, th {
    border: 1px solid #cbd5e1;
    padding: 8px;
}
img {
    max-width: 100%;
    height: auto;
}
@media print {
    body {
        margin: 0;
        max-width: none;
    }
}
</style>
</head>
<body>
$bodyContent
</body>
</html>
        """.trimIndent()
    }

    fun htmlFromParagraphs(paras: List<String>): String {
        return paras.joinToString("\n") { "<p>${escapeHtml(it)}</p>" }
    }

    /* ------------------------------ قراءة ومعالجة الملفات ------------------------------ */

    suspend fun processAndConvert(
        context: Context,
        fileUri: Uri,
        fileName: String,
        targetFormat: TargetFormat,
        readingMode: ReadingMode,
        keepLines: Boolean,
        onProgress: (String) -> Unit = {}
    ): Result<ProcessResult> = withContext(Dispatchers.IO) {
        try {
            onProgress("جاري فحص وقراءة نوع الملف...")
            val ext = fileName.substringAfterLast(".", "").lowercase()
            val mimeType = context.contentResolver.getType(fileUri) ?: when (ext) {
                "pdf" -> "application/pdf"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "txt" -> "text/plain"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                else -> "application/octet-stream"
            }

            var paras: List<String> = emptyList()
            var srcHtml: String? = null
            var usedAi = false

            when {
                // 1. مستندات Word DOCX
                ext == "docx" -> {
                    onProgress("جاري استخراج النصوص العربية من ملف DOCX...")
                    val rawText = DocxExtractor.extractFullText(context, fileUri)
                    paras = splitIntoParagraphs(rawText, keepLines)
                    srcHtml = htmlFromParagraphs(paras)
                }

                // 2. ملفات النصوص العادية TXT
                ext == "txt" -> {
                    onProgress("جاري قراءة الملف النصي...")
                    val stream = context.contentResolver.openInputStream(fileUri)
                        ?: return@withContext Result.failure(Exception("تعذر فتح الملف النصي"))
                    val rawText = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    paras = splitIntoParagraphs(rawText, keepLines)
                }

                // 3. ملفات PDF
                ext == "pdf" -> {
                    if (readingMode == ReadingMode.AUTO) {
                        onProgress("جاري فحص الطبقة النصية المباشرة في PDF...")
                        val extracted = extractPdfTextDirect(context, fileUri, keepLines)
                        paras = extracted
                    }

                    // إذا طلب المستخدم الذكاء الاصطناعي دائماً أو كان النص المستخرج قليلاً جداً (أقل من 20 حرفاً - ممسوح ضوئياً)
                    if (readingMode == ReadingMode.AI || paras.joinToString("").trim().length < 20) {
                        onProgress("جاري القراءة والتدقيق بالذكاء الاصطناعي (OCR مع التشكيل)...")
                        val base64Data = readUriAsBase64(context, fileUri)
                        if (base64Data != null) {
                            val ocrResult = GeminiAiService.extractDocumentOcr(
                                base64Data = base64Data,
                                mimeType = "application/pdf"
                            )
                            if (ocrResult.isSuccess) {
                                paras = splitIntoParagraphs(ocrResult.getOrThrow(), keepLines)
                                usedAi = true
                            } else if (paras.isEmpty()) {
                                // حاول أيضاً مع تصيير الصفحة الأولى كصورة عالية الدقة للـ OCR
                                val firstPageBmp = renderFirstPdfPageToBitmap(context, fileUri)
                                if (firstPageBmp != null) {
                                    val imgBase64 = bitmapToBase64(firstPageBmp)
                                    val imgOcr = GeminiAiService.extractDocumentOcr(imgBase64, "image/jpeg")
                                    if (imgOcr.isSuccess) {
                                        paras = splitIntoParagraphs(imgOcr.getOrThrow(), keepLines)
                                        usedAi = true
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. الصور الممسوحة ضوئياً
                mimeType.startsWith("image/") || ext in listOf("png", "jpg", "jpeg", "webp") -> {
                    onProgress("جاري قراءة وتفريغ الصورة بالذكاء الاصطناعي (OCR والتشكيل)...")
                    val base64Data = readUriAsBase64(context, fileUri)
                        ?: return@withContext Result.failure(Exception("تعذر قراءة الصورة من الجهاز"))
                    val ocrResult = GeminiAiService.extractDocumentOcr(
                        base64Data = base64Data,
                        mimeType = mimeType
                    )
                    if (ocrResult.isSuccess) {
                        paras = splitIntoParagraphs(ocrResult.getOrThrow(), keepLines)
                        usedAi = true
                    } else {
                        return@withContext Result.failure(
                            ocrResult.exceptionOrNull() ?: Exception("تعذر استخراج النص من الصورة عبر OCR")
                        )
                    }
                }

                else -> {
                    return@withContext Result.failure(Exception("صيغة غير مدعومة. الصيغ المدعومة: PDF, DOCX, TXT, والصور"))
                }
            }

            if (paras.isEmpty()) {
                return@withContext Result.failure(Exception("لم يتم العثور على أي نص في الملف المختار"))
            }

            val fullText = paras.joinToString(if (keepLines) "\n" else "\n\n")
            val baseTitle = fileName.substringBeforeLast(".")
            val htmlContent = wrapHtml(srcHtml ?: htmlFromParagraphs(paras), baseTitle)
            val isRtlDoc = isRtl(fullText)

            onProgress("جاري توليد وإنشاء الملف بصيغة ${targetFormat.displayName}...")

            val cacheFolder = File(context.cacheDir, "arabic_conversions").apply { if (!exists()) mkdirs() }
            val cleanTitle = baseTitle.replace(Regex("""[^\w\u0600-\u06FF]"""), "_")

            val outputFile: File = when (targetFormat) {
                TargetFormat.DOCX -> {
                    val outFile = File(cacheFolder, "${cleanTitle}_katib.docx")
                    buildDocxFile(outFile, cleanTitle, paras)
                    outFile
                }
                TargetFormat.PDF -> {
                    val outFile = File(cacheFolder, "${cleanTitle}_katib.pdf")
                    buildPdfFile(outFile, cleanTitle, paras)
                    outFile
                }
                TargetFormat.HTML -> {
                    val outFile = File(cacheFolder, "${cleanTitle}_katib.html")
                    outFile.writeText(htmlContent, StandardCharsets.UTF_8)
                    outFile
                }
                TargetFormat.TXT -> {
                    val outFile = File(cacheFolder, "${cleanTitle}_katib.txt")
                    // إضافة علامة BOM \uFEFF لحفظ التشكيل والأحرف العربية في برامج تحرير النصوص
                    outFile.writeText("\uFEFF$fullText", StandardCharsets.UTF_8)
                    outFile
                }
            }

            onProgress("تم التحويل بنجاح ✔")

            Result.success(
                ProcessResult(
                    paragraphs = paras,
                    fullText = fullText,
                    outputFile = outputFile,
                    previewHtml = htmlContent,
                    detectedRtl = isRtlDoc,
                    wasAiUsed = usedAi
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /* ------------------------------ توليد ملف DOCX متوافق مع العربية ------------------------------ */

    private fun buildDocxFile(outFile: File, title: String, paragraphs: List<String>) {
        ZipOutputStream(FileOutputStream(outFile)).use { zos ->
            // [Content_Types].xml
            addZipEntry(zos, "[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""")

            // _rels/.rels
            addZipEntry(zos, "_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""")

            // word/document.xml with RTL formatting and Tashkeel retention
            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
            sb.append("<w:body>")

            // Title
            sb.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"36\"/><w:szCs w:val=\"36\"/><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\" w:cs=\"Arial\"/><w:rtl/></w:rPr><w:t>")
            sb.append(escapeXml(title))
            sb.append("</w:t></w:r></w:p>")

            // Subtitle Tag
            sb.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:i/><w:sz w:val=\"20\"/><w:szCs w:val=\"20\"/><w:color w:val=\"64748B\"/><w:rtl/></w:rPr><w:t>")
            sb.append("تم استخراج وتنسيق المستند عبر كاتب (Arabic Tashkeel & Diacritics Engine)")
            sb.append("</w:t></w:r></w:p>")

            // Paragraphs
            for (p in paragraphs) {
                sb.append("<w:p><w:pPr><w:jc w:val=\"both\"/><w:spacing w:line=\"360\" w:lineRule=\"auto\" w:after=\"120\"/><w:bidi/></w:pPr><w:r><w:rPr><w:sz w:val=\"28\"/><w:szCs w:val=\"28\"/><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\" w:cs=\"Arial\"/><w:rtl/></w:rPr><w:t>")
                sb.append(escapeXml(p))
                sb.append("</w:t></w:r></w:p>")
            }

            // Page Setup (A4 with generous margins)
            sb.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\"/></w:sectPr>")
            sb.append("</w:body></w:document>")

            addZipEntry(zos, "word/document.xml", sb.toString())
        }
    }

    private fun addZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /* ------------------------------ توليد ملف PDF متوافق مع العربية ------------------------------ */

    private fun buildPdfFile(outFile: File, title: String, paragraphs: List<String>) {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50
        val contentWidth = pageWidth - (margin * 2)

        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.argb(255, 30, 41, 59)
            textSize = 12f
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = margin.toFloat()

        // Draw title
        val titleLayout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        canvas.save()
        canvas.translate(margin.toFloat(), currentY)
        titleLayout.draw(canvas)
        canvas.restore()
        currentY += titleLayout.height + 25f

        for (p in paragraphs) {
            val layout = StaticLayout.Builder.obtain(p, 0, p.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1.15f)
                .build()

            if (currentY + layout.height > pageHeight - margin) {
                // End current page & start new one
                doc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = doc.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin.toFloat()
            }

            canvas.save()
            canvas.translate(margin.toFloat(), currentY)
            layout.draw(canvas)
            canvas.restore()
            currentY += layout.height + 14f
        }

        doc.finishPage(page)
        FileOutputStream(outFile).use { fos ->
            doc.writeTo(fos)
        }
        doc.close()
    }

    /* ------------------------------ استخراج مباشر للـ PDF ------------------------------ */

    private fun extractPdfTextDirect(context: Context, uri: Uri, keepLines: Boolean): List<String> {
        val out = mutableListOf<String>()
        var pfd: ParcelFileDescriptor? = null
        try {
            pfd = if (uri.scheme == "file") {
                ParcelFileDescriptor.open(File(uri.path ?: ""), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }

            // قراءة البايتات وفك نصوص Tj / TJ كطبقة استخراج مباشر
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val raw = stream.bufferedReader(StandardCharsets.ISO_8859_1).readText()
                val sb = StringBuilder()

                val tjRegex = Regex("""\[(.*?)\]\s*TJ""", RegexOption.DOT_MATCHES_ALL)
                for (tj in tjRegex.findAll(raw)) {
                    val subMatches = Regex("""\(([^)]+)\)""").findAll(tj.groupValues[1])
                    for (sub in subMatches) {
                        sb.append(sub.groupValues[1]).append(" ")
                    }
                }

                val singleTjRegex = Regex("""\(([^)]+)\)\s*Tj""", RegexOption.DOT_MATCHES_ALL)
                for (m in singleTjRegex.findAll(raw)) {
                    val part = m.groupValues[1]
                    if (part.length > 1) {
                        sb.append(part).append(" ")
                    }
                }

                val directText = sb.toString().trim()
                if (directText.isNotBlank()) {
                    out.addAll(splitIntoParagraphs(directText, keepLines))
                }
            }
        } catch (_: Exception) {
        } finally {
            try { pfd?.close() } catch (_: Exception) {}
        }
        return out
    }

    private fun renderFirstPdfPageToBitmap(context: Context, uri: Uri): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = if (uri.scheme == "file") {
                ParcelFileDescriptor.open(File(uri.path ?: ""), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            } ?: return null

            renderer = PdfRenderer(pfd)
            if (renderer.pageCount == 0) return null
            val page = renderer.openPage(0)
            val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return bmp
        } catch (_: Exception) {
            return null
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private fun readUriAsBase64(context: Context, uri: Uri): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) Base64.encodeToString(bytes, Base64.NO_WRAP) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun bitmapToBase64(bmp: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
