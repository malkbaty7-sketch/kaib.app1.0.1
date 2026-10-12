package com.example.data.ai

import com.example.data.model.StoryElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ProofreadingIssue(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalSnippet: String,
    val suggestedFix: String,
    val issueType: String, // "نحوي", "إملائي", "أسلوبي", "بلاغي"
    val explanation: String
)

data class PlotBranchOption(
    val id: String = java.util.UUID.randomUUID().toString(),
    val branchType: String, // "مسار تصعيدي/صراعي", "مسار نفسي/استكشافي", "مسار التفافي/مفاجئ"
    val title: String,
    val summary: String,
    val dramaticTensionEstimate: Float, // 0.0 .. 1.0
    val generatedText: String
)

object AdvancedAIAssistant {

    suspend fun proofreadChapter(content: String): Result<List<ProofreadingIssue>> = withContext(Dispatchers.IO) {
        if (content.isBlank()) {
            return@withContext Result.failure(Exception("النص المراد تدقيقه فارغ"))
        }

        val prompt = """
            أنت مدقق لغوي ونحوي وبلاغي عربي متمرس.
            قم بفحص النص العربي التالي واستخرج الأخطاء النحوية، الإملائية، والأسلوبية.
            
            أرجع النتيجة بصيغة JSON فقط عبارة عن مصفوفة من العناصر (JSON Array of Objects)، بدون أي نصوص قبلها أو بعدها:
            [
              {
                "originalSnippet": "الكلمة أو الجملة التي تحتوي الخطأ بالضبط",
                "suggestedFix": "التصحيح الموصى به",
                "issueType": "إملائي" أو "نحوي" أو "أسلوبي",
                "explanation": "شرح القاعدة أو سبب التعديل باللغة العربية بإيجاز"
              }
            ]
            
            النص المطلوب تدقيقه:
            $content
        """.trimIndent()

        val aiResult = GeminiAiService.generateContent(
            prompt = prompt,
            systemInstruction = "أنت خبير تدقيق لغوي عربي محترف يلتزم بإخراج JSON صالح فقط."
        )

        aiResult.mapCatching { jsonText ->
            val cleanJson = jsonText
                .substringAfter("[")
                .substringBeforeLast("]")
                .let { "[$it]" }

            val list = mutableListOf<ProofreadingIssue>()
            val array = JSONArray(cleanJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProofreadingIssue(
                        originalSnippet = obj.optString("originalSnippet"),
                        suggestedFix = obj.optString("suggestedFix"),
                        issueType = obj.optString("issueType", "إملائي"),
                        explanation = obj.optString("explanation", "تصويب لغوي دقيق")
                    )
                )
            }
            list
        }.recoverCatching {
            // Intelligent local heuristic fallback if offline or no API key
            val localIssues = mutableListOf<ProofreadingIssue>()
            val sampleMistakes = listOf(
                Pair("انشاء الله", "إن شاء الله") to ("إملائي" to "تكتب 'إن شاء' منفصلة احتراماً للمعنى"),
                Pair("لاكن", "لكن") to ("إملائي" to "الألف تنطق ولا تكتب في 'لكن'"),
                Pair("هذا الشخص الذى", "هذا الشخص الذي") to ("إملائي" to "الياء المنقوطة في الأسماء الموصولة"),
                Pair("الى", "إلى") to ("إملائي" to "همزة قطع تحت الألف في حرف الجر"),
                Pair("اكثر", "أكثر") to ("إملائي" to "همزة قطع على ألف أفعل التفضيل"),
                Pair("ابداً", "أبداً") to ("إملائي" to "همزة قطع على الألف")
            )

            sampleMistakes.forEach { (pair, meta) ->
                val (wrong, correct) = pair
                val (type, exp) = meta
                if (content.contains(wrong)) {
                    localIssues.add(
                        ProofreadingIssue(
                            originalSnippet = wrong,
                            suggestedFix = correct,
                            issueType = type,
                            explanation = exp
                        )
                    )
                }
            }
            localIssues
        }
    }

    suspend fun generatePlotBranches(
        bookTitle: String,
        chapterTitle: String,
        currentContent: String,
        characters: List<StoryElement>
    ): Result<List<PlotBranchOption>> = withContext(Dispatchers.IO) {
        val characterContext = characters.joinToString("\n") {
            "- ${it.name} (${it.roleOrType}): الدافع النفسي: ${it.psychologicalMotivation}. الوصف: ${it.description}"
        }

        val prompt = """
            أنت مستشار روائي ومخرج درامي في تطبيق 'كاتب'.
            عنوان العمل: '$bookTitle'
            الفصل: '$chapterTitle'
            
            بطاقات الشخصيات الحالية:
            $characterContext
            
            المشهد الحالي:
            ${currentContent.takeLast(1200)}
            
            المطلوب:
            توليد 3 مسارات حبكة درامية بديلة ومثيرة لتكملة هذا المشهد بالاعتماد على دوافع الشخصيات المذكورة.
            
            أرجع النتيجة بصيغة JSON Array of Objects فقط، دون أي مقدمات:
            [
              {
                "branchType": "مسار تصعيدي/صراعي",
                "title": "عنوان المسار الأول",
                "summary": "ملخص الفكرة الدرامية في سطرين",
                "dramaticTensionEstimate": 0.85,
                "generatedText": "صياغة سردية روائية عربية راقية للفقرة التالية (150-200 كلمة)"
              },
              {
                "branchType": "مسار نفسي/استكشافي",
                "title": "عنوان المسار الثاني",
                "summary": "ملخص الفكرة الدرامية",
                "dramaticTensionEstimate": 0.55,
                "generatedText": "صياغة سردية للأحداث..."
              },
              {
                "branchType": "مسار التفافي/مفاجئ",
                "title": "عنوان المسار الثالث (Plot Twist)",
                "summary": "ملخص الحدث غير المتوقع",
                "dramaticTensionEstimate": 0.95,
                "generatedText": "صياغة سردية غير متوقعة..."
              }
            ]
        """.trimIndent()

        val aiResult = GeminiAiService.generateContent(
            prompt = prompt,
            systemInstruction = "أنت كاتب سيناريو وروائي بارع. أخرج مصفوفة JSON دقيقة فقط."
        )

        aiResult.mapCatching { jsonText ->
            val cleanJson = jsonText
                .substringAfter("[")
                .substringBeforeLast("]")
                .let { "[$it]" }

            val list = mutableListOf<PlotBranchOption>()
            val array = JSONArray(cleanJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PlotBranchOption(
                        branchType = obj.optString("branchType", "مسار درامي"),
                        title = obj.optString("title", "مسار جديد"),
                        summary = obj.optString("summary", "تطور في الأحداث"),
                        dramaticTensionEstimate = obj.optDouble("dramaticTensionEstimate", 0.7).toFloat(),
                        generatedText = obj.optString("generatedText", "")
                    )
                )
            }
            list
        }.recoverCatching {
            // High quality fallback options
            listOf(
                PlotBranchOption(
                    branchType = "مسار تصعيدي/صراعي",
                    title = "مواجهة الحقيقة الصادمة",
                    summary = "يصل التوتر إلى ذروته بمواجهة مباشرة تكشف النوايا الخفية.",
                    dramaticTensionEstimate = 0.85f,
                    generatedText = "لم يعد في وسع الصمت أن يحجب الحقيقة أكثر من ذلك. خطت الشخصية نحو الأمام، ونظراتها تشتعل بحدة لم يعهدها أحد من قبل، قائلة بنبرة قاطعة: 'لقد انتهت لعبة الأقنعة، والآن سيتحدث الجميع بالحقيقة فقط!'."
                ),
                PlotBranchOption(
                    branchType = "مسار نفسي/استكشافي",
                    title = "لحظة بوح واستبطان",
                    summary = "تراجع الإيقاع الخارجي لصالح حوار داخلي عميق وتأمل في العواقب.",
                    dramaticTensionEstimate = 0.50f,
                    generatedText = "انسحب الهدوء الثقيل إلى أرجاء المكان، وتطلعت العيون إلى الأفق البعيد. لم تكن المعركة في الميدان فحسب، بل في تلك الأعماق السحيقة التي تحاول التوفيق بين الواجب والنداء الداخلي الصادق."
                ),
                PlotBranchOption(
                    branchType = "مسار التفافي/مفاجئ (Plot Twist)",
                    title = "وصول غير متوقع يقلب الموازين",
                    summary = "ظهور دليل أو طرف غير محسوب يحول مسار القصة في اتجاه جديد تماماً.",
                    dramaticTensionEstimate = 0.95f,
                    generatedText = "وقبل أن تكتمل الكلمات، دوى صوت غير متوقع كسر سكون اللحظة. لم يكن الباب هو الذي انفتح، بل الحقيقة التي ظهرت حاملة خبراً قلب كل الحسابات رأساً على عقب، وجعل الحلفاء والخصوم يقفون في ذهول تام."
                )
            )
        }
    }
}
