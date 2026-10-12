package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.ChapterItem
import com.example.data.model.Project
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object BookExporter {

    fun exportToTxt(context: Context, project: Project, chapters: List<ChapterItem>): File {
        val fileName = "${project.projectName.replace("[^a-zA-Z0-9ء-ي]".toRegex(), "_")}_book.txt"
        val cacheFile = File(context.cacheDir, fileName)

        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("         ${project.projectName}\n")
        sb.append("=========================================\n\n")

        chapters.forEachIndexed { idx, ch ->
            if (ch.sectionType == "FRONT_COVER") {
                sb.append("--- [الغلاف الأمامي] ---\n\n")
            } else if (ch.sectionType == "BACK_COVER") {
                sb.append("--- [الغلاف الخلفي] ---\n\n")
            } else {
                sb.append("الفصل ${idx + 1}: ${ch.title}\n")
                sb.append("-----------------------------------------\n")
            }
            sb.append(ch.content)
            sb.append("\n\n\n")
        }

        sb.append("=========================================\n")
        sb.append("تم إعداد هذا الكتاب بواسطة تطبيق كاتب (Kateeb)\n")

        cacheFile.writeText(sb.toString(), Charsets.UTF_8)
        saveToPublicDownloadsIfPossible(context, fileName, "text/plain", cacheFile.readBytes())
        return cacheFile
    }

    fun exportToHtml(context: Context, project: Project, chapters: List<ChapterItem>): File {
        val fileName = "${project.projectName.replace("[^a-zA-Z0-9ء-ي]".toRegex(), "_")}_book.html"
        val cacheFile = File(context.cacheDir, fileName)

        val sb = StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${project.projectName}</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background-color: #f8fafc;
                        color: #1e293b;
                        margin: 0;
                        padding: 20px;
                        line-height: 1.8;
                    }
                    .book-container {
                        max-width: 800px;
                        margin: 0 auto;
                        background: #fff;
                        padding: 40px;
                        border-radius: 12px;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.08);
                    }
                    .book-header {
                        text-align: center;
                        border-bottom: 2px solid #e2e8f0;
                        padding-bottom: 30px;
                        margin-bottom: 40px;
                    }
                    h1 { color: #0f172a; font-size: 2.2em; margin-bottom: 10px; }
                    .author-tag { color: #0284c7; font-size: 1.2em; font-weight: bold; }
                    .chapter-title {
                        color: #0369a1;
                        border-right: 4px solid #0284c7;
                        padding-right: 14px;
                        margin-top: 40px;
                    }
                    .chapter-content {
                        white-space: pre-wrap;
                        font-size: 1.1em;
                        color: #334155;
                        margin-bottom: 30px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 50px;
                        font-size: 0.9em;
                        color: #94a3b8;
                        border-top: 1px solid #e2e8f0;
                        padding-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="book-container">
                    <div class="book-header">
                        <h1>${project.projectName}</h1>
                        <div class="author-tag">كتاب إلكتروني متكامل</div>
                    </div>
        """.trimIndent())

        chapters.forEachIndexed { idx, ch ->
            sb.append("<div class='chapter-section'>")
            if (ch.sectionType == "FRONT_COVER") {
                sb.append("<h2 class='chapter-title'>الغلاف الأمامي</h2>")
            } else if (ch.sectionType == "BACK_COVER") {
                sb.append("<h2 class='chapter-title'>الغلاف الخلفي والنبذة</h2>")
            } else {
                sb.append("<h2 class='chapter-title'>الفصل ${idx + 1}: ${ch.title}</h2>")
            }
            sb.append("<div class='chapter-content'>${escapeHtml(ch.content)}</div>")
            sb.append("</div>")
        }

        sb.append("""
                    <div class="footer">
                        تم تأليف وتنسيق هذا المستند بواسطة تطبيق كاتب (Kateeb)
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent())

        cacheFile.writeText(sb.toString(), Charsets.UTF_8)
        saveToPublicDownloadsIfPossible(context, fileName, "text/html", cacheFile.readBytes())
        return cacheFile
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /**
     * Attempts to save a copy into the device's public Downloads directory using MediaStore on Android 10+
     */
    private fun saveToPublicDownloadsIfPossible(context: Context, fileName: String, mimeType: String, bytes: ByteArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KateebBooks")
                }
                val uri: Uri? = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os: OutputStream ->
                        os.write(bytes)
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { it.write(bytes) }
            }
        } catch (e: Exception) {
            // Ignore errors in public downloads; cacheFile is still valid and shareable
        }
    }
}
