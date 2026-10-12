package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.BookCoverTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class BookCoverRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("book_cover_templates_prefs", Context.MODE_PRIVATE)
    private val coversDir = File(context.filesDir, "custom_book_covers").apply { mkdirs() }

    private val _customTemplates = MutableStateFlow<List<BookCoverTemplate>>(emptyList())
    val customTemplates: StateFlow<List<BookCoverTemplate>> = _customTemplates.asStateFlow()

    init {
        loadCustomTemplates()
    }

    val builtInTemplates: List<BookCoverTemplate> = listOf(
        BookCoverTemplate(
            id = "template_classic_leather",
            title = "الغلاف الكلاسيكي الفاخر",
            category = "أدبي",
            backgroundType = "GRADIENT",
            backgroundColors = listOf("#4A1515", "#1F0808"),
            bookTitle = "ملحمة الشرق والزمن",
            subtitle = "دراسة أدبية وسردية في التاريخ والذاكرة",
            authorName = "د. طارق المنصور",
            publisherName = "دار الحكمة للنشر • طبعة خاصة",
            titleColor = "#F59E0B",
            subtitleColor = "#FEF3C7",
            authorColor = "#FFFFFF",
            accentColor = "#D97706",
            fontFamily = "Amiri",
            backSynopsis = "عمل فكري وأدبي رائد يستعرض مراحل التطور الإنساني وجماليات التعبير عبر العصور.",
            backAuthorBio = "المؤلف: باحث ومفكر حاصل على جوائز في الدراسات الأدبية والإنسانية."
        ),
        BookCoverTemplate(
            id = "template_emerald_luxury",
            title = "الزمرد الملكي العصري",
            category = "فكر وتاريخ",
            backgroundType = "GRADIENT",
            backgroundColors = listOf("#064E3B", "#022C22"),
            bookTitle = "أسرار الفكر والنهضة",
            subtitle = "أبعاد الرؤية المعاصرة وتحديات المستقبل",
            authorName = "أحمد رضوان العباسي",
            publisherName = "منشورات التنوير والمعرفة",
            titleColor = "#E2E8F0",
            subtitleColor = "#A7F3D0",
            authorColor = "#34D399",
            accentColor = "#10B981",
            fontFamily = "Cairo",
            backSynopsis = "استكشاف عميق لمسارات النهضة الفكرية وتجارب الحضارات في بناء الوعي المجتمعي.",
            backAuthorBio = "أحمد رضوان: كاتب ومحلل استراتيجي في الفكر المعاصر."
        ),
        BookCoverTemplate(
            id = "template_sapphire_academic",
            title = "الأزرق الأكاديمي والبحثي",
            category = "أكاديمي",
            backgroundType = "GRADIENT",
            backgroundColors = listOf("#1E3A8A", "#0F172A"),
            bookTitle = "منهجية البحث العلمي المتقدم",
            subtitle = "دليل الباحثين وطلاب الدراسات العليا",
            authorName = "أ.د. عبد الله الشمري",
            publisherName = "جامعة المعارف • الطبعة الجامعية الأولى",
            titleColor = "#FFFFFF",
            subtitleColor = "#93C5FD",
            authorColor = "#60A5FA",
            accentColor = "#3B82F6",
            fontFamily = "Tajawal",
            backSynopsis = "مرجع أكاديمي شامل يقدم أحدث المنهجيات والتقنيات في التحليل والتوثيق والبحث.",
            backAuthorBio = "الأستاذ الدكتور عبد الله الشمري: أستاذ مناهج البحث والمشرف على العديد من الأطروحات العلمية."
        ),
        BookCoverTemplate(
            id = "template_heritage_islamic",
            title = "التراث الأندلسي الذهبي",
            category = "تراثي",
            backgroundType = "GRADIENT",
            backgroundColors = listOf("#78350F", "#451A03"),
            bookTitle = "ديوان البلاغة والحكمة",
            subtitle = "مختارات من روائع الفكر والبيان العربي",
            authorName = "تحقيق: محمد بن سالم الزاهي",
            publisherName = "مطبعة التراث العربي الأصيل",
            titleColor = "#FDE68A",
            subtitleColor = "#FEF3C7",
            authorColor = "#F59E0B",
            accentColor = "#FBBF24",
            fontFamily = "Amiri",
            backSynopsis = "مختارات من عيون الأدب العربي ونفائس الحكم والمواعظ والقصائد الخالدة.",
            backAuthorBio = "محمد بن سالم الزاهي: محقق وباحث في التراث والمخطوطات العربية القديمة."
        ),
        BookCoverTemplate(
            id = "template_minimalist_dark",
            title = "الأسود الفخم التجريدي (Noir)",
            category = "عصري وتجريدي",
            backgroundType = "GRADIENT",
            backgroundColors = listOf("#1E293B", "#0F172A"),
            bookTitle = "ما وراء الصمت",
            subtitle = "رواية نفسية وتأملات في الوجود",
            authorName = "سارة عبد الرحمن",
            publisherName = "دار أفق للروايات والآداب",
            titleColor = "#F8FAFC",
            subtitleColor = "#94A3B8",
            authorColor = "#CBD5E1",
            accentColor = "#EF4444",
            fontFamily = "Cairo",
            backSynopsis = "رحلة سردية تأخذ القارئ إلى أعماق النفس البشرية وصراعات الهوية والذاكرة.",
            backAuthorBio = "سارة عبد الرحمن: روائية وكاتبة سيناريو حازت أعمالها على اهتمام نقدي واسع."
        ),
        BookCoverTemplate(
            id = "template_cosmic_scifi",
            title = "الخيال العلمي والكوني",
            category = "أدبي",
            backgroundType = "GRADIENT",
            backgroundColors = listOf("#312E81", "#1E1B4B"),
            bookTitle = "بوابة النجوم والأبعاد",
            subtitle = "رواية من أدب الخيال العلمي واستشراف الغد",
            authorName = "عمر الهاشمي",
            publisherName = "آفاق المستقبل للنشر العلمي",
            titleColor = "#C7D2FE",
            subtitleColor = "#A5B4FC",
            authorColor = "#E0E7FF",
            accentColor = "#818CF8",
            fontFamily = "Tajawal",
            backSynopsis = "سفينة فضائية تبحر نحو أقصى المجرة وتكتشف حقائق مذهلة عن الزمن والوجود.",
            backAuthorBio = "عمر الهاشمي: مؤلف متخصص في أدب الخيال العلمي ومقالات الفضاء."
        ),
        BookCoverTemplate(
            id = "template_terracotta_poetry",
            title = "التيراكوتا والشعر الدافئ",
            category = "أدبي",
            backgroundType = "GRADIENT",
            backgroundColors = listOf("#9A3412", "#7C2D12"),
            bookTitle = "أوراق الخريف والوجد",
            subtitle = "مجموعة شعرية ونصوص وجدانية",
            authorName = "ليلى الكرمي",
            publisherName = "دار الكلمة الراقية",
            titleColor = "#FFEDD5",
            subtitleColor = "#FED7AA",
            authorColor = "#FDBA74",
            accentColor = "#FB923C",
            fontFamily = "Amiri",
            backSynopsis = "قصائد تحاكي نبضات القلب وعذوبة الحنين بلغة بصرية آسرة وأوزان عذبة.",
            backAuthorBio = "ليلى الكرمي: شاعرة وكاتبة نشرت العديد من الدواوين الشعرية المميزة."
        )
    )

    fun getAllTemplates(): List<BookCoverTemplate> {
        return _customTemplates.value + builtInTemplates
    }

    /**
     * Copies an image from a content Uri (picked by user) into internal storage,
     * and creates a new custom BookCoverTemplate for the user.
     */
    fun saveUserCoverFromUri(
        sourceUri: Uri,
        customTitle: String = "غلافي المخصص",
        bookTitle: String = "عنوان الكتاب",
        subtitle: String = "",
        authorName: String = "اسم الكاتب"
    ): BookCoverTemplate? {
        return try {
            val fileName = "cover_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val destFile = File(coversDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input: InputStream ->
                FileOutputStream(destFile).use { output: FileOutputStream ->
                    input.copyTo(output)
                }
            }

            val template = BookCoverTemplate(
                id = UUID.randomUUID().toString(),
                title = customTitle,
                category = "خاص بي",
                backgroundType = "IMAGE_URI",
                backgroundColors = listOf("#000000", "#1E293B"),
                imageUri = destFile.absolutePath,
                bookTitle = bookTitle,
                subtitle = subtitle,
                authorName = authorName,
                isUserCustom = true
            )

            addCustomTemplate(template)
            template
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun addCustomTemplate(template: BookCoverTemplate) {
        val updated = listOf(template) + _customTemplates.value.filterNot { it.id == template.id }
        _customTemplates.value = updated
        saveCustomTemplatesToPrefs(updated)
    }

    fun deleteCustomTemplate(templateId: String) {
        val toDelete = _customTemplates.value.firstOrNull { it.id == templateId }
        if (toDelete != null && toDelete.imageUri != null) {
            try {
                File(toDelete.imageUri).delete()
            } catch (e: Exception) {
                // Ignore file deletion errors
            }
        }
        val updated = _customTemplates.value.filterNot { it.id == templateId }
        _customTemplates.value = updated
        saveCustomTemplatesToPrefs(updated)
    }

    private fun loadCustomTemplates() {
        val jsonStr = prefs.getString("user_covers_json", null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<BookCoverTemplate>()
            for (i in 0 until arr.length()) {
                val itemJson = arr.getString(i)
                val template = BookCoverTemplate.fromJson(itemJson)
                if (template != null) {
                    list.add(template)
                }
            }
            _customTemplates.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCustomTemplatesToPrefs(list: List<BookCoverTemplate>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(t.toJson())
        }
        prefs.edit().putString("user_covers_json", arr.toString()).apply()
    }
}
