package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAiService {
    private const val TAG = "GeminiAiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("لم يتم تكوين مفتاح GEMINI_API_KEY في لوحة أسرار AI Studio.")
            )
        }

        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val jsonBody = JSONObject()

            // contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject().apply {
                put("text", prompt)
            }
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            jsonBody.put("contents", contentsArray)

            // systemInstruction if provided
            if (!systemInstruction.isNullOrBlank()) {
                val sysContent = JSONObject().apply {
                    val sysParts = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    }
                    put("parts", sysParts)
                }
                jsonBody.put("systemInstruction", sysContent)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error (${response.code}): $responseBody")
                return@withContext Result.failure(
                    Exception("خطأ في الاتصال بنموذج Gemini (${response.code})")
                )
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    return@withContext Result.success(text.trim())
                }
            }

            Result.failure(Exception("لم يُرجع النموذج أي استجابة صالحة"))
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call exception", e)
            Result.failure(e)
        }
    }

    // High level helper prompts
    suspend fun refineText(originalText: String, style: String = "بلاغي فصيح"): Result<String> {
        val sysPrompt = "أنت كاتب ومحرر لغوي عربي محترف وخبير في البلاغة العربية وتدقيق الأسلوب الأدبي والأكاديمي."
        val prompt = """
            قم بتنقيح وإعادة صياغة النص التالي بأسلوب $style:
            - حافظ على المعنى والمقصد الأساسي.
            - حسّن ترابط الجمل والبلاغة والتراكيب العربية السليمة.
            - أعطني النص المنقح مباشرة دون مقدمات أو شروحات إضافية.
            
            النص الأصلي:
            $originalText
        """.trimIndent()
        return generateContent(prompt, sysPrompt)
    }

    suspend fun expandSection(topic: String, contextText: String): Result<String> {
        val sysPrompt = "أنت مساعد كتابة محترف متخصص في صياغة الفصول والكتب العربية."
        val prompt = """
            اكتب توسعاً غنياً ومتسقاً للفقرة/الفصل التالي حول موضوع: '$topic'.
            السياق الحالي:
            $contextText
            
            المطلوب:
            صياغة فقرات أدبية/فكرية متقنة ومترابطة تنسجم مع السياق، دون تكرار مبتذل. أعطني المحتوى المكتوب مباشرة.
        """.trimIndent()
        return generateContent(prompt, sysPrompt)
    }

    suspend fun summarizeAndExtract(sourceText: String, topic: String): Result<String> {
        val sysPrompt = "أنت محلل نصوص محترف يستخلص الجواهر والأفكار المحورية من المراجع."
        val prompt = """
            من النص المرجعي أدناه، استخلص أهم الفقرات والأفكار العميقة المتعلقة بموضوع: '$topic'.
            
            النص المرجعي:
            $sourceText
            
            المطلوب:
            استخلاص فقرات واضحة ومركزة مقتبسة ومصاغة بأسلوب سلس مع الحفاظ على دقة المعنى.
        """.trimIndent()
        return generateContent(prompt, sysPrompt)
    }

    const val DEFAULT_ARABIC_TASHKEEL_PROMPT =
        "انسخ كل النص الموجود في هذا المستند كما هو بالضبط. حافظ على كل التشكيل العربي " +
        "(الفتحة والضمة والكسرة والشدة والسكون والتنوين) وعلامات الترقيم والأرقام وترتيب الأسطر والفقرات. " +
        "لا تترجم ولا تصحح ولا تلخص ولا تضف أي تعليق. افصل الفقرات بسطر فارغ. أخرج النص فقط."

    suspend fun extractDocumentOcr(
        base64Data: String,
        mimeType: String,
        customPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("لم يتم تكوين مفتاح GEMINI_API_KEY في لوحة أسرار AI Studio لاستخدام ميزة OCR الذكية.")
            )
        }

        try {
            // Use gemini-2.5-flash-image for image tasks per skill, or gemini-3.5-flash
            val model = if (mimeType.startsWith("image/")) "gemini-2.5-flash-image" else "gemini-3.5-flash"
            val url = "$BASE_URL/$model:generateContent?key=$apiKey"

            val promptText = customPrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_ARABIC_TASHKEEL_PROMPT

            val jsonBody = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // 1. Inline data part
            val inlineDataObj = JSONObject().apply {
                put("mime_type", mimeType)
                put("data", base64Data)
            }
            val dataPart = JSONObject().apply {
                put("inline_data", inlineDataObj)
            }
            partsArray.put(dataPart)

            // 2. Text prompt part
            val textPart = JSONObject().apply {
                put("text", promptText)
            }
            partsArray.put(textPart)

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            jsonBody.put("contents", contentsArray)

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini OCR API error (${response.code}): $responseBody")
                return@withContext Result.failure(
                    Exception("خطأ في الاتصال بنموذج Gemini للاستخراج الذكي (${response.code})")
                )
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    return@withContext Result.success(text.trim())
                }
            }

            Result.failure(Exception("لم يُرجع نموذج الذكاء الاصطناعي أي نص"))
        } catch (e: Exception) {
            Log.e(TAG, "Gemini OCR exception", e)
            Result.failure(e)
        }
    }
}

