package com.example.data.model

data class Emotion(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val description: String,
    val colorHex: String
)

val availableEmotions = listOf(
    Emotion("warmth", "دفء", "☀️", "أجواء دافئة مريحة للنفس", "#FF8C42"),
    Emotion("tenderness", "حنان", "🌸", "مشاعر رقيقة وإنسانية", "#FFB6C1"),
    Emotion("mystery", "غموض", "🔮", "إثارة وتشويق وحبس أنفاس", "#4B0082"),
    Emotion("facts", "حقائق", "📊", "معلومات دقيقة ورصينة", "#4682B4"),
    Emotion("science", "علم وبحث", "🔬", "منهجية علمية وتحليل", "#2E8B57"),
    Emotion("romance", "رومنسية", "❤️", "حب وعاطفة جياشة", "#DC143C"),
    Emotion("motivation", "تحفيز", "⚡", "طاقة وإيجابية وشغف", "#FFD700"),
    Emotion("positive_energy", "طاقة إيجابية", "✨", "تفاؤل وأمل متجدد", "#FFA500"),
    Emotion("sadness", "حزن", "🌧️", "مشاعر عميقة وشجن صادق", "#708090"),
    Emotion("excitement", "حماس", "🔥", "طاقة عالية وتسارع أحداث", "#FF4500"),
    Emotion("calm", "هدوء", "🍃", "استرخاء وسكينة وسلام", "#87CEEB"),
    Emotion("hope", "أمل", "🌱", "تفاؤل بمستقبل مشرق", "#98FB98"),
    Emotion("inspiration", "إلهام", "💡", "إبداع وتفكير عميق", "#9370DB")
)

data class EmotionSuggestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String, // "REORDER", "REWRITE", "RELOCATE"
    val title: String,
    val message: String,
    val suggestedText: String? = null,
    val sourceChapterIndex: Int? = null,
    val targetChapterIndex: Int? = null
)

object EmotionEngine {
    fun generateSuggestions(
        chapters: List<ChapterItem>,
        selectedEmotions: List<String>
    ): List<EmotionSuggestion> {
        val list = mutableListOf<EmotionSuggestion>()

        if (chapters.isEmpty()) return list

        if (selectedEmotions.contains("mystery")) {
            val highTensionIdx = chapters.indexOfFirst { it.tensionLevel > 0.6f }
            if (highTensionIdx in 0 until chapters.size - 1) {
                list.add(
                    EmotionSuggestion(
                        type = "REORDER",
                        title = "اقتراح إعادة ترتيب لتعزيز الغموض",
                        message = "لزيادة عنصر الغموض والتشويق، يُقترح نقل قسم '${chapters[highTensionIdx].title}' إلى نهاية العمل كحبكة مفاجئة.",
                        sourceChapterIndex = highTensionIdx,
                        targetChapterIndex = chapters.lastIndex
                    )
                )
            }
        }

        if (selectedEmotions.contains("motivation")) {
            val firstChapter = chapters.first()
            if (!firstChapter.content.contains("ابدأ") && !firstChapter.content.contains("قوة") && !firstChapter.content.contains("نجاح")) {
                list.add(
                    EmotionSuggestion(
                        type = "REWRITE",
                        title = "اقتراح شحذ النبرة التحفيزية",
                        message = "اخترت مشاعر 'التحفيز'. نقترح إضافة افتتاحية نارية تشعل حماس القارئ وتدفعه للإنجاز.",
                        suggestedText = "كل لحظة تبدأ فيها فصلاً جديداً هي فرصة استثنائية لتعيد رسم مستقبلك. لا تنتظر الإلهام، بل اصنعه بعملك الدؤوب وإيمانك الراسخ.",
                        sourceChapterIndex = 0
                    )
                )
            }
        }

        if (selectedEmotions.contains("calm")) {
            val longChapterIdx = chapters.indexOfFirst { it.content.length > 500 }
            if (longChapterIdx != -1) {
                list.add(
                    EmotionSuggestion(
                        type = "REWRITE",
                        title = "تعزيز السكينة والهدوء الإنشائي",
                        message = "لتحقيق الهدوء النفسي للقارئ، نوصي بتقسيم الفقرات الطويلة في '${chapters[longChapterIdx].title}' واستخدام وقفات تأملية.",
                        sourceChapterIndex = longChapterIdx
                    )
                )
            }
        }

        if (selectedEmotions.contains("warmth")) {
            list.add(
                EmotionSuggestion(
                    type = "REWRITE",
                    title = "إضافة لمسة دفء وجدانية",
                    message = "لتعميق إحساس الدفء الإنساني، أضف عبارة امتنان أو حواراً وجدانياً يعزز الترابط مع القارئ.",
                    suggestedText = "تذكر أنك لست وحدك في هذا الطريق؛ فالأرواح النبيلة تتلاقى دوماً عبر المعاني الصادقة والمحبة التي لا تبلى.",
                    sourceChapterIndex = if (chapters.size > 1) 1 else 0
                )
            )
        }

        if (selectedEmotions.contains("facts") || selectedEmotions.contains("science")) {
            list.add(
                EmotionSuggestion(
                    type = "REWRITE",
                    title = "توثيق الشواهد والمصادر",
                    message = "لتعزيز النزاهة العلمية المرجوة، تذكر إضافة جدول إحصائي أو ذكر المراجع الدقيقة لكل مقولة.",
                    sourceChapterIndex = 0
                )
            )
        }

        return list
    }
}
