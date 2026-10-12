package com.example.util

import android.content.Context
import android.graphics.BitmapFactory
import com.example.data.model.ChapterItem
import com.example.data.model.ExportAdvancedMetadata
import com.example.data.model.Project
import java.io.File

enum class IssueSeverity {
    PASS,
    WARNING,
    ERROR
}

data class PreflightIssue(
    val category: String, // "METADATA", "IMAGES", "LAYOUT", "STRUCTURE"
    val severity: IssueSeverity,
    val title: String,
    val message: String,
    val fixHint: String? = null
)

data class PreflightReport(
    val scorePercentage: Int, // 0..100
    val isReadyForKdp: Boolean,
    val issues: List<PreflightIssue>
)

object PreflightValidator {

    fun validateProject(
        context: Context,
        project: Project,
        chapters: List<ChapterItem>,
        metadata: ExportAdvancedMetadata,
        frontCoverJson: String?
    ): PreflightReport {
        val issues = mutableListOf<PreflightIssue>()

        // 1. Metadata Validation
        if (project.projectName.isBlank() || project.projectName == "كتاب جديد") {
            issues.add(
                PreflightIssue(
                    category = "METADATA",
                    severity = IssueSeverity.ERROR,
                    title = "عنوان الكتاب مفقود أو افتراضي",
                    message = "يجب تحديد عنوان رسمي للكتاب قبل الرفع إلى منصات النشر.",
                    fixHint = "قم بتعديل اسم المشروع من إعدادات الغلاف أو المحرر."
                )
            )
        } else {
            issues.add(
                PreflightIssue(
                    category = "METADATA",
                    severity = IssueSeverity.PASS,
                    title = "عنوان الكتاب معتمد",
                    message = "العنوان: \"${project.projectName}\""
                )
            )
        }

        // ISBN Check
        val cleanIsbn = metadata.isbn.replace("-", "").trim()
        if (cleanIsbn.length !in listOf(10, 13) || !cleanIsbn.all { it.isDigit() || it == 'X' }) {
            issues.add(
                PreflightIssue(
                    category = "METADATA",
                    severity = IssueSeverity.WARNING,
                    title = "تنسيق الـ ISBN غير مكتمل",
                    message = "الرقم الدولي المعياري للكتاب (ISBN) يجب أن يتكون من 10 أو 13 خانة رقمية معتمدة للنشر الورقي.",
                    fixHint = "يمكنك الحصول على ISBN مجاني من Amazon KDP أو إدخال رقمك المعتمد في خيارات التصدير."
                )
            )
        } else {
            issues.add(
                PreflightIssue(
                    category = "METADATA",
                    severity = IssueSeverity.PASS,
                    title = "رقم الـ ISBN سليم",
                    message = "الرقم المسجل: ${metadata.isbn}"
                )
            )
        }

        // Publisher & Rights
        if (metadata.publisher.isBlank()) {
            issues.add(
                PreflightIssue(
                    category = "METADATA",
                    severity = IssueSeverity.WARNING,
                    title = "دار النشر غير محددة",
                    message = "يوصى بذكر اسم دار النشر أو 'نشر ذاتي - Self-Published' للامتثال للمعايير الدولية."
                )
            )
        } else {
            issues.add(
                PreflightIssue(
                    category = "METADATA",
                    severity = IssueSeverity.PASS,
                    title = "دار النشر وحقوق الملكية",
                    message = "الناشر: ${metadata.publisher}"
                )
            )
        }

        // 2. Cover & Image DPI Check
        var hasValidCover = false
        if (!project.coverImagePath.isNullOrBlank()) {
            val coverFile = File(project.coverImagePath)
            if (coverFile.exists() && coverFile.canRead()) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(coverFile.absolutePath, options)
                val width = options.outWidth
                val height = options.outHeight

                if (width < 1000 || height < 1600) {
                    issues.add(
                        PreflightIssue(
                            category = "IMAGES",
                            severity = IssueSeverity.WARNING,
                            title = "دقة صورة الغلاف أقل من الموصى به لـ KDP",
                            message = "أبعاد الغلاف الحالية ($width x $height). توصي أمازون KDP بأبعاد 1600x2560 بكسل على الأقل (300 DPI) لجودة طباعة مثالية.",
                            fixHint = "يمكنك استخدام أداة توليد الصور بالذكاء الاصطناعي بنمط 9:16 أو رفع صورة عالية الدقة."
                        )
                    )
                } else {
                    issues.add(
                        PreflightIssue(
                            category = "IMAGES",
                            severity = IssueSeverity.PASS,
                            title = "دقة الغلاف مطابقة للمواصفات العالمية",
                            message = "الأبعاد ($width x $height px) متوافقة مع معايير الطباعة 300 DPI."
                        )
                    )
                }
                hasValidCover = true
            }
        }

        if (!hasValidCover && frontCoverJson.isNullOrBlank()) {
            issues.add(
                PreflightIssue(
                    category = "IMAGES",
                    severity = IssueSeverity.ERROR,
                    title = "غلاف الكتاب غير محدد",
                    message = "تتطلب منصات KDP و Apple Books وجود غلاف أساسي مدمج في ملف التصدير.",
                    fixHint = "افتح استوديو الأغلفة أو مولد الصور AI لإنشاء وتعيين غلاف الكتاب."
                )
            )
        } else if (!hasValidCover) {
            issues.add(
                PreflightIssue(
                    category = "IMAGES",
                    severity = IssueSeverity.PASS,
                    title = "غلاف الكتاب مصمم ومجهز",
                    message = "تم تهيئة غلاف رقمي عالي الجودة للكتاب."
                )
            )
        }

        // 3. Layout & Margins (Mirror Margins & Spine)
        if (!metadata.mirrorMargins) {
            issues.add(
                PreflightIssue(
                    category = "LAYOUT",
                    severity = IssueSeverity.WARNING,
                    title = "الهوامش الحية (Mirror Margins) غير مفعلة",
                    message = "في الطباعة الورقية على الوجهين، تتطلب المطابع توسيع حاشية كعب التجليد الداخلية لتجنب اختفاء النصوص في ثنية الكتاب.",
                    fixHint = "فعّل خيار 'الهوامش الحية' في نافذة التصدير."
                )
            )
        } else {
            issues.add(
                PreflightIssue(
                    category = "LAYOUT",
                    severity = IssueSeverity.PASS,
                    title = "الهوامش الحية وكعب الكتاب جاهز",
                    message = "حاشية كعب التجليد (56pt) معدة للطباعة المزدوجة."
                )
            )
        }

        // 4. Structure & Chapters Check
        val emptyChapters = chapters.filter { it.content.isBlank() && it.sectionType == "CHAPTER" }
        if (emptyChapters.isNotEmpty()) {
            issues.add(
                PreflightIssue(
                    category = "STRUCTURE",
                    severity = IssueSeverity.WARNING,
                    title = "توجد فصول فارغة بدون محتوى",
                    message = "تم رصد ${emptyChapters.size} فصول خالية من النصوص (${emptyChapters.take(2).joinToString { it.title }}). ترفض بعض المنصات الكتب التي تحتوي على فصول فارغة.",
                    fixHint = "أضف نصوصاً للفصول أو احذف التبويبات غير المستخدمة."
                )
            )
        } else {
            issues.add(
                PreflightIssue(
                    category = "STRUCTURE",
                    severity = IssueSeverity.PASS,
                    title = "بنية الفصول متناسقة",
                    message = "جميع الفصول الـ ${chapters.size} تحتوي على نصوص ومجهزة للنشر."
                )
            )
        }

        // Calculate Score
        val total = issues.size
        val passes = issues.count { it.severity == IssueSeverity.PASS }
        val errors = issues.count { it.severity == IssueSeverity.ERROR }
        val score = if (total > 0) ((passes.toFloat() / total.toFloat()) * 100).toInt() else 100

        return PreflightReport(
            scorePercentage = score,
            isReadyForKdp = errors == 0,
            issues = issues
        )
    }
}
