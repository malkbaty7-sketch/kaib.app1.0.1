package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.ChapterItem
import com.example.data.model.ExportAdvancedMetadata
import com.example.data.model.Project
import com.example.data.model.formatPageNumber
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfExporter {

    fun calculateSpineThicknessMm(pageCount: Int, paperWeightGsm: Int = 80): Float {
        val sheetsCount = (pageCount + 1) / 2
        // Caliper in mm: 80gsm is ~0.105mm, 70gsm is ~0.092mm, 100gsm is ~0.13mm
        val sheetThicknessMm = when (paperWeightGsm) {
            70 -> 0.092f
            100 -> 0.130f
            else -> 0.105f // Standard 80gsm book paper
        }
        return (sheetsCount * sheetThicknessMm)
    }

    fun exportProjectToPdf(
        context: Context,
        project: Project,
        chapters: List<ChapterItem>,
        metadata: ExportAdvancedMetadata = ExportAdvancedMetadata()
    ): File {
        val pdfDocument = PdfDocument()
        
        // Standard Book Trim Sizes (Task 3.4)
        val (pageWidth, pageHeight) = when (metadata.trimSize.uppercase()) {
            "A5" -> Pair(420, 595) // Standard A5 (148 x 210 mm)
            "B5" -> Pair(499, 709) // Standard B5 (176 x 250 mm)
            "ROYAL_OCTAVO", "ROYAL OCTAVO", "ROYAL" -> Pair(432, 648) // Royal Octavo 6x9 in (152 x 229 mm)
            "DIGEST" -> Pair(396, 612) // Digest 5.5x8.5 in (140 x 216 mm)
            else -> when (project.paperSize.uppercase()) {
                "A5" -> Pair(420, 595)
                "B5" -> Pair(499, 709)
                "ROYAL_OCTAVO", "ROYAL OCTAVO" -> Pair(432, 648)
                "DIGEST" -> Pair(396, 612)
                else -> Pair(595, 842) // Standard A4 (210 x 297 mm)
            }
        }

        // Mirror margins settings
        val innerGutter = if (metadata.mirrorMargins) 56f else 48f
        val outerMargin = if (metadata.mirrorMargins) 36f else 48f
        val topMargin = 48f
        val bottomMargin = 48f
        val contentWidth = (pageWidth - innerGutter - outerMargin).toInt()

        val totalEstimatedPages = chapters.size + 2
        val spineThicknessMm = calculateSpineThicknessMm(totalEstimatedPages, metadata.paperWeightGsm)

        // Background color
        val bgColorInt = try {
            Color.parseColor(project.paperColor)
        } catch (_: Exception) {
            Color.WHITE
        }

        val bgPaint = Paint().apply {
            color = bgColorInt
            style = Paint.Style.FILL
        }

        val titlePaint = TextPaint().apply {
            color = Color.rgb(35, 35, 35)
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerPaint = TextPaint().apply {
            color = Color.rgb(180, 80, 20)
            textSize = 17f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.rgb(45, 45, 45)
            textSize = 12f
            isAntiAlias = true
        }

        val metaPaint = TextPaint().apply {
            color = Color.rgb(110, 110, 110)
            textSize = 10f
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(200, 160, 110)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }

        var pageNumber = 1

        // 1. Cover / Title Page
        val coverPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val coverPage = pdfDocument.startPage(coverPageInfo)
        val coverCanvas: Canvas = coverPage.canvas
        coverCanvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Decorative outer border
        coverCanvas.drawRect(innerGutter - 12, topMargin - 12, pageWidth - outerMargin + 12, pageHeight - bottomMargin + 12, borderPaint)

        // Book title centered
        val coverTitleLayout = StaticLayout.Builder.obtain(
            project.projectName,
            0,
            project.projectName.length,
            titlePaint,
            contentWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        coverCanvas.save()
        coverCanvas.translate(innerGutter, pageHeight / 3.2f)
        coverTitleLayout.draw(coverCanvas)
        coverCanvas.restore()

        // Metadata block on Cover
        val metaSummaryText = "الناشر: ${metadata.publisher}\nالرقم المعياري (ISBN): ${metadata.isbn}\n${metadata.rights}\nسنة النشر: ${metadata.publicationYear}"
        val coverMetaLayout = StaticLayout.Builder.obtain(
            metaSummaryText,
            0,
            metaSummaryText.length,
            metaPaint,
            contentWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        coverCanvas.save()
        coverCanvas.translate(innerGutter, (pageHeight / 3.2f) + coverTitleLayout.height + 26)
        coverMetaLayout.draw(coverCanvas)
        coverCanvas.restore()

        pdfDocument.finishPage(coverPage)
        pageNumber++

        // 2. Printing Specifications & Spine Blueprint Page
        val specPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val specPage = pdfDocument.startPage(specPageInfo)
        val specCanvas = specPage.canvas
        specCanvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        val specTitle = "معايير الطباعة الاحترافية وحساب كعب الكتاب"
        val specTitleLayout = StaticLayout.Builder.obtain(
            specTitle, 0, specTitle.length, headerPaint, contentWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

        specCanvas.save()
        specCanvas.translate(innerGutter, topMargin + 20)
        specTitleLayout.draw(specCanvas)
        specCanvas.restore()

        val spineText = String.format(
            Locale.US,
            """
■ مواصفات الطباعة والقطع (Trim Size):
- مقاس الكتاب: %s (%dx%d pt)
- سُمك الكعب المقدر: %.2f ملم (mm)
- وزن الورق المستخدم: %d جم/م² (Standard Book Paper)
- عدد الفصول والصفحات المقدر: %d صفحة

■ إعدادات الهوامش الحية (Mirror Margins):
- الهامش الداخلي (كعب التجليد Gutter): %.0f نقطة (Inner Margin)
- الهامش الخارجي (حاشية القطع): %.0f نقطة (Outer Margin)
- نظام الطباعة: مجهز ومعد للطباعة المزدوجة على الوجهين (Duplex Ready)

■ معايير النشر الرقمي الدولية:
- ISBN: %s
- دار النشر: %s
- لغة الوثيقة: %s
            """.trimIndent(),
            metadata.trimSize,
            pageWidth,
            pageHeight,
            spineThicknessMm,
            metadata.paperWeightGsm,
            totalEstimatedPages,
            innerGutter,
            outerMargin,
            metadata.isbn,
            metadata.publisher,
            metadata.language
        )

        val specBodyLayout = StaticLayout.Builder.obtain(
            spineText, 0, spineText.length, bodyPaint, contentWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

        specCanvas.save()
        specCanvas.translate(innerGutter, topMargin + 70)
        specBodyLayout.draw(specCanvas)
        specCanvas.restore()

        // Draw illustrative spine box
        val boxPaint = Paint().apply {
            color = Color.rgb(220, 220, 220)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val spineBoxTop = topMargin + 260f
        specCanvas.drawRoundRect(innerGutter, spineBoxTop, innerGutter + 200f, spineBoxTop + 60f, 8f, 8f, boxPaint)
        specCanvas.drawText("مخطط كعب الكتاب: ${String.format(Locale.US, "%.2f", spineThicknessMm)} mm", innerGutter + 12, spineBoxTop + 36, metaPaint)

        pdfDocument.finishPage(specPage)
        pageNumber++

        // 3. Chapter Pages with Alternate Mirror Margins
        for (chapter in chapters) {
            val isOddPage = (pageNumber % 2 != 0)
            // Arabic / RTL: Odd (Recto / Right page) -> Inner gutter on Right
            // Even (Verso / Left page) -> Inner gutter on Left
            val leftMargin = if (metadata.mirrorMargins) {
                if (isOddPage) outerMargin else innerGutter
            } else innerGutter

            val rightMargin = if (metadata.mirrorMargins) {
                if (isOddPage) innerGutter else outerMargin
            } else outerMargin

            val actualContentWidth = (pageWidth - leftMargin - rightMargin).toInt()

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            var currentY = topMargin

            // Running header
            val runningHeader = project.projectName
            canvas.drawText(runningHeader, leftMargin, currentY, metaPaint)
            canvas.drawLine(leftMargin, currentY + 6, pageWidth - rightMargin, currentY + 6, borderPaint)
            currentY += 28f

            // Chapter Title
            val chapterTitleLayout = StaticLayout.Builder.obtain(
                chapter.title,
                0,
                chapter.title.length,
                headerPaint,
                actualContentWidth
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

            canvas.save()
            canvas.translate(leftMargin, currentY)
            chapterTitleLayout.draw(canvas)
            canvas.restore()

            currentY += chapterTitleLayout.height + 16f

            // Chapter Content
            val safeContent = if (chapter.content.isNotBlank()) chapter.content else "(فصل بدون نصوص)"
            val contentLayout = StaticLayout.Builder.obtain(
                safeContent,
                0,
                safeContent.length,
                bodyPaint,
                actualContentWidth
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

            canvas.save()
            canvas.translate(leftMargin, currentY)
            contentLayout.draw(canvas)
            canvas.restore()

            // Page numbering footer
            val pageNumText = formatPageNumber(pageNumber - 2, project.pageNumberStyle)
            val pageNumWidth = metaPaint.measureText(pageNumText)
            val xPos = when (project.pageNumberPosition) {
                "BOTTOM_RIGHT" -> pageWidth - rightMargin - pageNumWidth
                "BOTTOM_LEFT" -> leftMargin
                else -> (pageWidth - pageNumWidth) / 2f
            }
            canvas.drawText(pageNumText, xPos, pageHeight - bottomMargin + 10, metaPaint)

            pdfDocument.finishPage(page)
            pageNumber++
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeFileName = project.projectName.replace(Regex("""[^\w\u0600-\u06FF]"""), "_")
        val outputFile = File(exportDir, "${safeFileName}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }
}
