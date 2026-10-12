package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

/**
 * نموذج بيانات يمثل منطقة التظليل الملون على صفحة الـ PDF
 */
data class PdfHighlightArea(
    val rect: RectF,
    val color: Int = Color.parseColor("#80FFEB3B") // أصفر شفاف افتراضي
)

/**
 * محرك PdfAnnotator المدمج:
 * يوفر معالجة خفيفة وعالية الدقة لملفات PDF دون الحاجة لمكتبات خارجية:
 * 1. التقاط التظليلات الملونة والتوقيع الرقمي (Signature Path).
 * 2. تطبيق التعديلات فوق الـ Bitmap الأصلية لكل صفحة.
 * 3. كتابة وإخراج مستند PDF نهائي جديد عبر android.graphics.pdf.PdfDocument.
 */
object PdfAnnotator {

    /**
     * معالجة وحفظ صفحة PDF محددة مع التظليلات والتوقيع كملف PDF مخرَج.
     */
    fun annotateAndSavePdf(
        context: Context,
        inputPdfUri: Uri,
        outputFile: File,
        pageIndex: Int = 0,
        highlights: List<PdfHighlightArea>,
        signaturePath: Path?
    ): Boolean {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        return try {
            pfd = tryOpenPfd(context, inputPdfUri) ?: return false
            renderer = PdfRenderer(pfd)

            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return false

            // 1. فتح الصفحة واختيار الأبعاد
            val page = renderer.openPage(pageIndex)
            val pageWidth = page.width
            val pageHeight = page.height

            // 2. تصيير الصفحة الأصلية على Bitmap
            val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            // 3. إنشاء Canvas للرسم والتعديل فوق صورة الصفحة
            val canvas = Canvas(bitmap)

            // أ) إضافة التظليلات الملونة (Highlighter)
            val highlightPaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            highlights.forEach { highlight ->
                highlightPaint.color = highlight.color
                canvas.drawRect(highlight.rect, highlightPaint)
            }

            // ب) إضافة التوقيع الرقمي (Digital Signature Path)
            if (signaturePath != null) {
                val signaturePaint = Paint().apply {
                    color = Color.BLACK // لون التوقيع
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                canvas.drawPath(signaturePath, signaturePaint)
            }

            // 4. إنشاء صفحة PDF جديدة داخل PdfDocument وإسقاط Bitmap التعديلات عليها
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val newPage = pdfDocument.startPage(pageInfo)

            val pdfCanvas = newPage.canvas
            pdfCanvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(newPage)

            // 5. كتابة الملف المعدل وحفظه
            if (!outputFile.parentFile?.exists()!!) {
                outputFile.parentFile?.mkdirs()
            }
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
            renderer?.close()
            pfd?.close()
        }
    }

    /**
     * معالجة كامل ملف الـ PDF وحفظه مع تظليلات متعددة الصفحات وتوقيع في صفحة محددة.
     */
    fun annotateAndSaveFullPdf(
        context: Context,
        inputPdfUri: Uri,
        outputFile: File,
        highlightsByPage: Map<Int, List<PdfHighlightArea>>,
        signatureByPage: Map<Int, Path>
    ): Boolean {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        return try {
            pfd = tryOpenPfd(context, inputPdfUri) ?: return false
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            if (totalPages == 0) return false

            val highlightPaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val signaturePaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

            for (i in 0 until totalPages) {
                val page = renderer.openPage(i)
                val width = page.width
                val height = page.height

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val canvas = Canvas(bitmap)

                // رسم التظليلات الخاصة بهذه الصفحة إن وجدت
                val pageHighlights = highlightsByPage[i].orEmpty()
                pageHighlights.forEach { h ->
                    highlightPaint.color = h.color
                    canvas.drawRect(h.rect, highlightPaint)
                }

                // رسم التوقيع الخاص بهذه الصفحة إن وجد
                val pageSignature = signatureByPage[i]
                if (pageSignature != null) {
                    canvas.drawPath(pageSignature, signaturePaint)
                }

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                val newPage = pdfDocument.startPage(pageInfo)
                newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(newPage)
            }

            if (outputFile.parentFile?.exists() == false) {
                outputFile.parentFile?.mkdirs()
            }
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
            renderer?.close()
            pfd?.close()
        }
    }

    /**
     * حفظ وتصدير المستند بالكامل مع التظليلات والتواقيع مباشرة إلى OutputStream (مثل SAF Uri عبر ContentResolver).
     */
    fun annotateAndSaveFullPdfToUri(
        context: Context,
        inputPdfUri: Uri,
        targetUri: Uri,
        highlightsByPage: Map<Int, List<PdfHighlightArea>> = emptyMap(),
        signatureByPage: Map<Int, Path> = emptyMap()
    ): Boolean {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        return try {
            pfd = tryOpenPfd(context, inputPdfUri) ?: return false
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            if (totalPages <= 0) return false

            val highlightPaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val signaturePaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

            for (i in 0 until totalPages) {
                val page = renderer.openPage(i)
                val width = page.width
                val height = page.height

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val canvas = Canvas(bitmap)

                // رسم التظليلات الخاصة بهذه الصفحة إن وجدت
                val pageHighlights = highlightsByPage[i].orEmpty()
                pageHighlights.forEach { h ->
                    highlightPaint.color = h.color
                    canvas.drawRect(h.rect, highlightPaint)
                }

                // رسم التوقيع الخاص بهذه الصفحة إن وجد
                val pageSignature = signatureByPage[i]
                if (pageSignature != null) {
                    canvas.drawPath(pageSignature, signaturePaint)
                }

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                val newPage = pdfDocument.startPage(pageInfo)
                newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(newPage)
            }

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                pdfDocument.writeTo(out)
            } ?: return false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
            renderer?.close()
            pfd?.close()
        }
    }

    private fun tryOpenPfd(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            try {
                // محاولة النسخ المؤقت إذا تعذر فتح الـ Uri مباشرة
                val temp = File(context.cacheDir, "pdf_annotator_${System.currentTimeMillis()}.tmp")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(temp).use { output ->
                        input.copyTo(output)
                    }
                }
                if (temp.exists()) {
                    ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
                } else null
            } catch (ex: Exception) {
                null
            }
        }
    }
}
