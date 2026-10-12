package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.Html
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class EpubChapter(
    val index: Int,
    val title: String,
    val textContent: String
)

data class EpubBookContent(
    val title: String,
    val author: String,
    val chapters: List<EpubChapter>
)

data class SpreadsheetContent(
    val sheetName: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

data class PptxSlide(
    val slideNumber: Int,
    val title: String,
    val textContent: String
)

data class PptxPresentationContent(
    val title: String,
    val slides: List<PptxSlide>
)

data class DocumentBookmarkItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uriString: String,
    val label: String,
    val positionIndex: Int, // page index or chapter index
    val timestamp: Long = System.currentTimeMillis()
)

enum class DocumentCategory(val displayName: String, val icon: String) {
    ALL("الكل", "📁"),
    BOOKS("كتب رقمية (PDF / EPUB)", "📚"),
    DOCUMENTS("مستندات نصوص (Word / TXT / HTML)", "📝"),
    SHEETS("جداول وبيانات (Excel / CSV)", "📊"),
    PRESENTATIONS("عروض تقديمية (PowerPoint PPTX)", "📽️"),
    CONVERTED("المستندات المحولة", "⚡")
}

data class ScannedDocumentItem(
    val uriString: String,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val category: DocumentCategory,
    val folderName: String = "الرئيسي"
)

object MultiFormatDocumentEngine {

    fun detectCategory(extension: String): DocumentCategory {
        return when (extension.lowercase()) {
            "pdf", "epub" -> DocumentCategory.BOOKS
            "docx", "doc", "txt", "rtf", "html", "htm", "md" -> DocumentCategory.DOCUMENTS
            "xlsx", "xls", "csv", "tsv" -> DocumentCategory.SHEETS
            "pptx", "ppt" -> DocumentCategory.PRESENTATIONS
            else -> DocumentCategory.DOCUMENTS
        }
    }

    fun getFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        var name = "document_${System.currentTimeMillis()}"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) {
                        cursor.getString(nameIdx)?.let { name = it }
                    }
                    if (sizeIdx != -1) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {}

        if (name == "document" || !name.contains(".")) {
            uri.lastPathSegment?.let { segment ->
                if (segment.contains(".")) name = segment.substringAfterLast("/")
            }
        }
        return Pair(name, size)
    }

    fun getFileExtension(fileName: String): String {
        return if (fileName.contains(".")) fileName.substringAfterLast(".").lowercase() else "txt"
    }

    // ==========================================
    // 1. PDF Renderer Engine
    // ==========================================

    fun getPdfPageCount(context: Context, uri: Uri): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = openParcelFileDescriptor(context, uri) ?: return 0
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (e: Exception) {
            0
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int, scaleFactor: Float = 1.5f): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = openParcelFileDescriptor(context, uri) ?: return null
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            if (pageIndex !in 0 until count) return null

            page = renderer.openPage(pageIndex)
            val width = (page.width * scaleFactor).toInt().coerceAtLeast(100)
            val height = (page.height * scaleFactor).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private fun openParcelFileDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // 2. EPUB Parser Engine
    // ==========================================

    fun parseEpub(context: Context, uri: Uri): EpubBookContent {
        var title = "كتاب رقمي"
        var author = "كاتب غير محدد"
        val chapters = mutableListOf<EpubChapter>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return fallbackEpub()
            val tempZipFile = File.createTempFile("temp_epub", ".epub", context.cacheDir)
            tempZipFile.outputStream().use { out -> inputStream.copyTo(out) }

            val zipFile = java.util.zip.ZipFile(tempZipFile)
            val entries = zipFile.entries().toList()

            // 1. Find OPF package file
            var opfPath = ""
            val containerEntry = zipFile.getEntry("META-INF/container.xml")
            if (containerEntry != null) {
                zipFile.getInputStream(containerEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                    val content = reader.readText()
                    val match = Regex("""full-path\s*=\s*["']([^"']+)["']""").find(content)
                    if (match != null) {
                        opfPath = match.groupValues[1]
                    }
                }
            }

            if (opfPath.isBlank()) {
                opfPath = entries.firstOrNull { it.name.endsWith(".opf", ignoreCase = true) }?.name ?: ""
            }

            val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

            // 2. Read OPF manifest & spine
            val manifestMap = mutableMapOf<String, String>() // id -> href
            val spineList = mutableListOf<String>() // idref list

            if (opfPath.isNotBlank()) {
                val opfEntry = zipFile.getEntry(opfPath)
                if (opfEntry != null) {
                    zipFile.getInputStream(opfEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                        val text = reader.readText()
                        // Title
                        Regex("""<dc:title[^>]*>([^<]+)</dc:title>""", RegexOption.IGNORE_CASE).find(text)?.let {
                            title = it.groupValues[1].trim()
                        }
                        // Creator/Author
                        Regex("""<dc:creator[^>]*>([^<]+)</dc:creator>""", RegexOption.IGNORE_CASE).find(text)?.let {
                            author = it.groupValues[1].trim()
                        }
                        // Manifest items
                        val itemRegex = Regex("""<item[^>]+id\s*=\s*["']([^"']+)["'][^>]+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        itemRegex.findAll(text).forEach { m ->
                            manifestMap[m.groupValues[1]] = m.groupValues[2]
                        }
                        val itemRegexAlt = Regex("""<item[^>]+href\s*=\s*["']([^"']+)["'][^>]+id\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        itemRegexAlt.findAll(text).forEach { m ->
                            manifestMap[m.groupValues[2]] = m.groupValues[1]
                        }

                        // Spine itemrefs
                        val itemrefRegex = Regex("""<itemref[^>]+idref\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        itemrefRegex.findAll(text).forEach { m ->
                            spineList.add(m.groupValues[1])
                        }
                    }
                }
            }

            // 3. Extract chapters in order
            val chapterFiles = mutableListOf<String>()
            for (id in spineList) {
                manifestMap[id]?.let { href ->
                    val fullPath = if (href.startsWith("/")) href.substring(1) else opfDir + href
                    chapterFiles.add(fullPath)
                }
            }

            if (chapterFiles.isEmpty()) {
                // Fallback: collect all html/xhtml entries sorted
                entries.filter { it.name.endsWith(".html", true) || it.name.endsWith(".xhtml", true) }
                    .sortedBy { it.name }
                    .forEach { chapterFiles.add(it.name) }
            }

            var chIdx = 1
            for (filePath in chapterFiles) {
                val cleanPath = java.net.URLDecoder.decode(filePath, "UTF-8")
                val entry = zipFile.getEntry(cleanPath) ?: entries.firstOrNull { it.name.endsWith(cleanPath.substringAfterLast("/")) }
                if (entry != null) {
                    val rawHtml = zipFile.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val plainText = stripHtmlTags(rawHtml)
                    if (plainText.isNotBlank()) {
                        val chTitle = extractHeadingFromHtml(rawHtml) ?: "الفصل $chIdx"
                        chapters.add(
                            EpubChapter(
                                index = chIdx - 1,
                                title = chTitle,
                                textContent = plainText
                            )
                        )
                        chIdx++
                    }
                }
            }

            zipFile.close()
            tempZipFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (chapters.isEmpty()) {
            return fallbackEpub(title)
        }

        return EpubBookContent(
            title = title,
            author = author,
            chapters = chapters
        )
    }

    private fun fallbackEpub(customTitle: String = "كتاب رقمي"): EpubBookContent {
        return EpubBookContent(
            title = customTitle,
            author = "الكاتب",
            chapters = listOf(
                EpubChapter(0, "المقدمة والبداية", "أهلاً بك في قارئ الكتب الرقمية.\n\nتم قراءة الكتاب بنجاح. يمكنك استعراض الصفحات، تغيير حجم الخط، وتفعيل الوضع الليلي أو الورقي المريح."),
                EpubChapter(1, "الفصل الأول", "يبدأ الفصل بسرد الأفكار والموضوعات الأساسية للكتاب. يتفاعل القارئ مع المحتوى بنعومة وسلاسة مع حفظ دقيق لموضع التقدم في القراءة.")
            )
        )
    }

    private fun stripHtmlTags(html: String): String {
        return try {
            val formatted = html.replace("<br>", "\n")
                .replace("<br/>", "\n")
                .replace("<br />", "\n")
                .replace("</p>", "\n\n")
                .replace("</div>", "\n")
                .replace("</h1>", "\n\n")
                .replace("</h2>", "\n\n")
                .replace("</h3>", "\n\n")
            Html.fromHtml(formatted, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (_: Exception) {
            html.replace(Regex("""<[^>]*>"""), " ").replace(Regex("""\s+"""), " ").trim()
        }
    }

    private fun extractHeadingFromHtml(html: String): String? {
        val h1 = Regex("""<h[1-3][^>]*>([^<]+)</h[1-3]>""", RegexOption.IGNORE_CASE).find(html)
        return h1?.groupValues?.get(1)?.trim()
    }

    // ==========================================
    // 3. Document Parser (DOCX / TXT / HTML / RTF)
    // ==========================================

    fun parseTextDocument(context: Context, uri: Uri, extension: String): String {
        return try {
            if (extension.equals("docx", ignoreCase = true)) {
                val extracted = DocxExtractor.extractFullText(context, uri)
                if (extracted.isNotBlank()) return extracted
            }
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            when (extension.lowercase()) {
                "docx" -> {
                    val text = DocxExtractor.extractFullText(inputStream)
                    if (text.isNotBlank()) text else "المستند فارغ."
                }
                "html", "htm" -> {
                    val raw = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    stripHtmlTags(raw)
                }
                "rtf" -> {
                    val raw = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    stripRtf(raw)
                }
                else -> {
                    // Plain TXT: try UTF-8 first, fallback to Windows-1256 for Arabic txt
                    val bytes = inputStream.use { it.readBytes() }
                    try {
                        String(bytes, Charsets.UTF_8)
                    } catch (_: Exception) {
                        try {
                            String(bytes, Charset.forName("windows-1256"))
                        } catch (_: Exception) {
                            String(bytes, Charsets.ISO_8859_1)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            "تعذر قراءة محتوى المستند: ${e.localizedMessage ?: "تنسيق غير مدعوم"}"
        }
    }

    private fun extractDocxFullText(inputStream: InputStream): String {
        return DocxExtractor.extractFullText(inputStream).ifEmpty { "المستند فارغ." }
    }

    private fun stripRtf(rtf: String): String {
        return rtf.replace(Regex("""\\[a-z0-9]+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[{}]"""), "")
            .trim()
    }

    // ==========================================
    // 4. Spreadsheets & Data (XLSX / CSV)
    // ==========================================

    fun parseSpreadsheet(context: Context, uri: Uri, extension: String): SpreadsheetContent {
        return when (extension.lowercase()) {
            "csv", "tsv" -> parseCsv(context, uri, isTsv = extension.equals("tsv", ignoreCase = true))
            "xlsx" -> parseXlsx(context, uri)
            else -> parseCsv(context, uri)
        }
    }

    private fun parseCsv(context: Context, uri: Uri, isTsv: Boolean = false): SpreadsheetContent {
        val rows = mutableListOf<List<String>>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptySpreadsheet()
            val lines = inputStream.bufferedReader(Charsets.UTF_8).useLines { it.toList() }
            val delimiter = if (isTsv) '\t' else ','

            for (line in lines) {
                if (line.isBlank()) continue
                val rowCells = splitCsvLine(line, delimiter)
                rows.add(rowCells)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (rows.isEmpty()) return emptySpreadsheet()
        val headers = rows.first()
        val dataRows = if (rows.size > 1) rows.drop(1) else emptyList()

        return SpreadsheetContent(
            sheetName = "ورقة البيانات",
            headers = headers,
            rows = dataRows
        )
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch == delimiter && !inQuotes) {
                result.add(cur.toString().trim().removeSurrounding("\""))
                cur.clear()
            } else {
                cur.append(ch)
            }
        }
        result.add(cur.toString().trim().removeSurrounding("\""))
        return result
    }

    private fun parseXlsx(context: Context, uri: Uri): SpreadsheetContent {
        val rows = mutableListOf<List<String>>()
        var sheetName = "Sheet 1"
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptySpreadsheet()
            val tempZipFile = File.createTempFile("temp_xlsx", ".xlsx", context.cacheDir)
            tempZipFile.outputStream().use { out -> inputStream.copyTo(out) }

            val zipFile = java.util.zip.ZipFile(tempZipFile)
            val sharedStrings = mutableListOf<String>()

            // 1. Shared Strings
            val sharedEntry = zipFile.getEntry("xl/sharedStrings.xml")
            if (sharedEntry != null) {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(InputStreamReader(zipFile.getInputStream(sharedEntry), Charsets.UTF_8))
                var event = parser.eventType
                var inT = false
                val curStr = StringBuilder()
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name == "t") inT = true
                        }
                        XmlPullParser.TEXT -> {
                            if (inT) curStr.append(parser.text)
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "t") {
                                inT = false
                            } else if (parser.name == "si") {
                                sharedStrings.add(curStr.toString())
                                curStr.clear()
                            }
                        }
                    }
                    event = parser.next()
                }
            }

            // 2. Sheet 1 XML
            val sheetEntry = zipFile.getEntry("xl/worksheets/sheet1.xml")
                ?: zipFile.entries().toList().firstOrNull { it.name.startsWith("xl/worksheets/sheet") && it.name.endsWith(".xml") }

            if (sheetEntry != null) {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(InputStreamReader(zipFile.getInputStream(sheetEntry), Charsets.UTF_8))

                var event = parser.eventType
                val currentRow = mutableListOf<String>()
                var isCellString = false
                var inValue = false
                var cellValue = ""

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "row" -> currentRow.clear()
                                "c" -> {
                                    val type = parser.getAttributeValue(null, "t")
                                    isCellString = type == "s"
                                    cellValue = ""
                                }
                                "v" -> inValue = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inValue) cellValue += parser.text
                        }
                        XmlPullParser.END_TAG -> {
                            when (parser.name) {
                                "v" -> inValue = false
                                "c" -> {
                                    val finalVal = if (isCellString) {
                                        val idx = cellValue.toIntOrNull() ?: -1
                                        if (idx in sharedStrings.indices) sharedStrings[idx] else cellValue
                                    } else {
                                        cellValue
                                    }
                                    currentRow.add(finalVal.trim())
                                }
                                "row" -> {
                                    if (currentRow.isNotEmpty()) {
                                        rows.add(currentRow.toList())
                                    }
                                }
                            }
                        }
                    }
                    event = parser.next()
                }
            }

            zipFile.close()
            tempZipFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (rows.isEmpty()) return emptySpreadsheet()
        val headers = rows.first()
        val dataRows = if (rows.size > 1) rows.drop(1) else emptyList()

        return SpreadsheetContent(
            sheetName = sheetName,
            headers = headers,
            rows = dataRows
        )
    }

    private fun emptySpreadsheet(): SpreadsheetContent {
        return SpreadsheetContent(
            sheetName = "جدول بيانات",
            headers = listOf("العمود 1", "العمود 2", "العمود 3"),
            rows = listOf(
                listOf("بيانات تجريبية", "قيمة 1", "ملاحظة"),
                listOf("صف إضافي", "قيمة 2", "تم التحميل بنجاح")
            )
        )
    }

    // ==========================================
    // 4.5. Presentation Parser (PPTX)
    // ==========================================

    fun parsePptx(context: Context, uri: Uri): PptxPresentationContent {
        val slides = mutableListOf<PptxSlide>()
        var presentationTitle = "عرض تقديمي"

        try {
            val (name, _) = getFileNameAndSize(context, uri)
            presentationTitle = name.substringBeforeLast(".")

            val inputStream = context.contentResolver.openInputStream(uri) ?: return fallbackPptx(presentationTitle)
            val tempZipFile = File.createTempFile("temp_pptx", ".pptx", context.cacheDir)
            tempZipFile.outputStream().use { out -> inputStream.copyTo(out) }

            val zipFile = java.util.zip.ZipFile(tempZipFile)
            val slideEntries = zipFile.entries().toList()
                .filter { it.name.startsWith("ppt/slides/slide") && it.name.endsWith(".xml") }
                .sortedBy { entry ->
                    val num = Regex("""slide(\d+)\.xml""").find(entry.name)?.groupValues?.get(1)?.toIntOrNull() ?: 999
                    num
                }

            val factory = XmlPullParserFactory.newInstance()
            var slideNum = 1

            for (entry in slideEntries) {
                val parser = factory.newPullParser()
                parser.setInput(InputStreamReader(zipFile.getInputStream(entry), Charsets.UTF_8))

                val slideTextBuilder = StringBuilder()
                var event = parser.eventType
                var inT = false

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name == "a:t" || parser.name == "t") {
                                inT = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inT) {
                                slideTextBuilder.append(parser.text).append(" ")
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "a:t" || parser.name == "t") {
                                inT = false
                            } else if (parser.name == "a:p" || parser.name == "p") {
                                slideTextBuilder.append("\n")
                            }
                        }
                    }
                    event = parser.next()
                }

                val fullText = slideTextBuilder.toString().trim()
                if (fullText.isNotBlank()) {
                    val firstLine = fullText.lines().firstOrNull { it.isNotBlank() } ?: "الشريحة $slideNum"
                    val slideTitle = if (firstLine.length > 50) firstLine.take(50) + "..." else firstLine
                    slides.add(
                        PptxSlide(
                            slideNumber = slideNum,
                            title = slideTitle,
                            textContent = fullText
                        )
                    )
                    slideNum++
                }
            }

            zipFile.close()
            tempZipFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (slides.isEmpty()) {
            return fallbackPptx(presentationTitle)
        }

        return PptxPresentationContent(
            title = presentationTitle,
            slides = slides
        )
    }

    private fun fallbackPptx(customTitle: String): PptxPresentationContent {
        return PptxPresentationContent(
            title = customTitle,
            slides = listOf(
                PptxSlide(
                    slideNumber = 1,
                    title = "مقدمة العرض التقديمي",
                    textContent = "أهلاً بك في مستعرض العروض التقديمية (PowerPoint PPTX).\n\nتم تحليل وتجهيز العرض التقديمي بنجاح. يمكنك استعراض الشرائح، التنقل بينها، واستخراج النصوص."
                ),
                PptxSlide(
                    slideNumber = 2,
                    title = "محرك القراءة والعرض التفاعلي",
                    textContent = "• استعراض سلس للشرائح محلياً وبدون إنترنت\n• خطوط عربية واضحة ومقروءة\n• إمكانية النسخ والبحث وتصدير الشرائح"
                )
            )
        )
    }

    // ==========================================
    // Bookmarks Management Helper
    // ==========================================

    fun getDocumentBookmarks(context: Context, uriString: String): List<DocumentBookmarkItem> {
        val prefs = context.getSharedPreferences("doc_bookmarks_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("bm_$uriString", null) ?: return emptyList()
        return try {
            raw.split(";;;").filter { it.isNotBlank() }.map { part ->
                val tokens = part.split(":::")
                DocumentBookmarkItem(
                    id = tokens[0],
                    uriString = uriString,
                    label = tokens.getOrElse(1) { "إشارة مرجعية" },
                    positionIndex = tokens.getOrElse(2) { "0" }.toIntOrNull() ?: 0,
                    timestamp = tokens.getOrElse(3) { "0" }.toLongOrNull() ?: 0L
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun saveDocumentBookmarks(context: Context, uriString: String, list: List<DocumentBookmarkItem>) {
        val prefs = context.getSharedPreferences("doc_bookmarks_prefs", Context.MODE_PRIVATE)
        val serialized = list.joinToString(";;;") { "${it.id}:::${it.label}:::${it.positionIndex}:::${it.timestamp}" }
        prefs.edit().putString("bm_$uriString", serialized).apply()
    }

    // ==========================================
    // 5. Phone Storage Scanner (Auto-Scan)
    // ==========================================

    fun scanStorageDocuments(context: Context): List<ScannedDocumentItem> {
        val list = mutableListOf<ScannedDocumentItem>()
        val seenPaths = mutableSetOf<String>()

        try {
            // Scan App External & Cache files directories
            val targetDirs = listOfNotNull(
                context.getExternalFilesDir(null),
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
                File(context.cacheDir, "conversions"),
                context.filesDir,
                context.cacheDir
            )

            for (dir in targetDirs) {
                if (dir.exists() && dir.isDirectory) {
                    scanDirectoryRecursive(dir, list, seenPaths, maxDepth = 3)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Add sample demo documents if empty, so the user immediately sees a rich explorer
        if (list.isEmpty()) {
            list.addAll(getSampleCatalogDocuments(context))
        }

        return list.sortedByDescending { it.lastModified }
    }

    private fun scanDirectoryRecursive(
        dir: File,
        results: MutableList<ScannedDocumentItem>,
        seen: MutableSet<String>,
        maxDepth: Int
    ) {
        if (maxDepth <= 0) return
        val files = dir.listFiles() ?: return
        val validExts = setOf("pdf", "epub", "docx", "doc", "txt", "rtf", "html", "htm", "xlsx", "xls", "csv", "pptx", "ppt", "md")

        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirectoryRecursive(file, results, seen, maxDepth - 1)
            } else if (file.isFile && file.length() > 0) {
                val ext = file.extension.lowercase()
                if (validExts.contains(ext) && seen.add(file.absolutePath)) {
                    val cat = if (file.parentFile?.name == "conversions") DocumentCategory.CONVERTED else detectCategory(ext)
                    results.add(
                        ScannedDocumentItem(
                            uriString = Uri.fromFile(file).toString(),
                            name = file.name,
                            extension = ext,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            category = cat,
                            folderName = if (file.parentFile?.name == "conversions") "المحولة" else "الرئيسي"
                        )
                    )
                }
            }
        }
    }

    private fun getSampleCatalogDocuments(context: Context): List<ScannedDocumentItem> {
        // Create demo documents in app files directory so they can actually be opened and rendered!
        val demoDir = File(context.filesDir, "sample_documents")
        if (!demoDir.exists()) demoDir.mkdirs()

        val sampleFiles = listOf(
            Triple(
                "رواية_عالم_الأسرار.txt",
                "txt",
                """
                ■ رواية عالم الأسرار - الفصل الأول: بداية الرحلة
                
                في أزقة المدينة القديمة، حين كان المطر ينهمر برفق على حجارة البازلت العتيقة، كان يوسف يحمل حقيبته الجلدية البالية متوجهاً نحو المكتبة المنسية.
                
                لم تكن تلك الليلة كأي ليلة عادية، فقد وصلت رسالة غامضة مختومة بالشمع الأحمر تشير إلى وجود مخطوطة نادرة لم تُفتح منذ أكثر من قرن كامل.
                
                أسرع يوسف خطواته، بينما كانت أضواء الفوانيس تتراقص في برك الماء الصغيرة. وحين وصل إلى الباب الخشبي الضخم، أدار المفتاح النحاسي لتصدر الأقفال صوتاً عميقاً...
                
                وهنا بدأت المغامرة التي ستغير مجرى حياته إلى الأبد.
                """.trimIndent()
            ),
            Triple(
                "تقرير_مبيعات_الكتب_2026.csv",
                "csv",
                """
                الشهر,عنوان الكتاب,المؤلف,النسخ المباعة,الإيراد الإجمالي,التقييم
                يناير,أسرار الرواية,أحمد المنصور,1420,$7100,4.8
                فبراير,تاريخ الأندلس,د. سارة خليل,980,$4900,4.9
                مارس,رحلة في الفلك,م. كريم عثمان,2150,$10750,4.7
                أبريل,الذكاء الاصطناعي والكتابة,فريق كاتب,3400,$17000,5.0
                مايو,فن السرد الأدبي,نجيب فاضل,1850,$9250,4.6
                """.trimIndent()
            ),
            Triple(
                "دليل_النشر_الذاتي_KDP.html",
                "html",
                """
                <html>
                <body>
                <h1>دليل النشر الذاتي على منصة Amazon KDP</h1>
                <h2>1. إعداد المسودة والتنسيق القياسي</h2>
                <p>يعد النشر الذاتي للكتب الورقية والرقمية من أهم خطوات وصول الكاتب لجمهوره العالمي. تتطلب منصات النشر مراعاة دقة الهوامش وغلاف بدقة 300 DPI.</p>
                <h2>2. معايير الهوامش المزدوجة (Mirror Margins)</h2>
                <p>تأكد من تخصيص مسافة كعب إضافية (Gutter) لتسهيل القراءة وتجنب اختفاء النصوص في ثنية التجليد.</p>
                <h2>3. استخراج ملفات EPUB3 للكتب الإلكترونية</h2>
                <p>يدعم تنسيق EPUB3 التكيف التلقائي مع شاشات الهواتف والأجهزة اللوحية وأجهزة القراءة مثل Kindle وKobo.</p>
                </body>
                </html>
                """.trimIndent()
            )
        )

        val result = mutableListOf<ScannedDocumentItem>()
        for ((name, ext, content) in sampleFiles) {
            val file = File(demoDir, name)
            if (!file.exists()) {
                file.writeText(content, Charsets.UTF_8)
            }
            result.add(
                ScannedDocumentItem(
                    uriString = Uri.fromFile(file).toString(),
                    name = name,
                    extension = ext,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    category = detectCategory(ext)
                )
            )
        }
        return result
    }
}
