package com.example.data.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class CoverTextLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "نص جديد",
    val xRatio: Float = 0.5f, // 0..1 relative to canvas width
    val yRatio: Float = 0.5f, // 0..1 relative to canvas height
    val fontSize: Float = 20f,
    val fontFamily: String = "Amiri",
    val color: String = "#FFFFFF",
    val rotation: Float = 0f, // 0..360 degrees
    val scale: Float = 1f,
    val hasShadow: Boolean = true,
    val shadowColor: String = "#000000",
    val hasStroke: Boolean = false,
    val strokeColor: String = "#000000",
    val strokeWidth: Float = 2f,
    val isBold: Boolean = true,
    val isItalic: Boolean = false,
    val letterSpacing: Float = 0f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("text", text)
        put("xRatio", xRatio.toDouble())
        put("yRatio", yRatio.toDouble())
        put("fontSize", fontSize.toDouble())
        put("fontFamily", fontFamily)
        put("color", color)
        put("rotation", rotation.toDouble())
        put("scale", scale.toDouble())
        put("hasShadow", hasShadow)
        put("shadowColor", shadowColor)
        put("hasStroke", hasStroke)
        put("strokeColor", strokeColor)
        put("strokeWidth", strokeWidth.toDouble())
        put("isBold", isBold)
        put("isItalic", isItalic)
        put("letterSpacing", letterSpacing.toDouble())
    }

    companion object {
        fun fromJson(obj: JSONObject): CoverTextLayer = CoverTextLayer(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            text = obj.optString("text", "نص"),
            xRatio = obj.optDouble("xRatio", 0.5).toFloat(),
            yRatio = obj.optDouble("yRatio", 0.5).toFloat(),
            fontSize = obj.optDouble("fontSize", 20.0).toFloat(),
            fontFamily = obj.optString("fontFamily", "Amiri"),
            color = obj.optString("color", "#FFFFFF"),
            rotation = obj.optDouble("rotation", 0.0).toFloat(),
            scale = obj.optDouble("scale", 1.0).toFloat(),
            hasShadow = obj.optBoolean("hasShadow", true),
            shadowColor = obj.optString("shadowColor", "#000000"),
            hasStroke = obj.optBoolean("hasStroke", false),
            strokeColor = obj.optString("strokeColor", "#000000"),
            strokeWidth = obj.optDouble("strokeWidth", 2.0).toFloat(),
            isBold = obj.optBoolean("isBold", true),
            isItalic = obj.optBoolean("isItalic", false),
            letterSpacing = obj.optDouble("letterSpacing", 0.0).toFloat()
        )
    }
}

@Immutable
data class CoverStickerLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String = "BEST_SELLER", // BEST_SELLER, AWARD, SPECIAL_EDITION, ISLAMIC_ORNAMENT, GEOMETRIC_STAR, BARCODE, CROWN, RIBBON
    val label: String = "الأكثر مبيعاً",
    val xRatio: Float = 0.5f,
    val yRatio: Float = 0.85f,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val color: String = "#F59E0B",
    val opacity: Float = 1f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("label", label)
        put("xRatio", xRatio.toDouble())
        put("yRatio", yRatio.toDouble())
        put("rotation", rotation.toDouble())
        put("scale", scale.toDouble())
        put("color", color)
        put("opacity", opacity.toDouble())
    }

    companion object {
        fun fromJson(obj: JSONObject): CoverStickerLayer = CoverStickerLayer(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            type = obj.optString("type", "BEST_SELLER"),
            label = obj.optString("label", "الأكثر مبيعاً"),
            xRatio = obj.optDouble("xRatio", 0.5).toFloat(),
            yRatio = obj.optDouble("yRatio", 0.85).toFloat(),
            rotation = obj.optDouble("rotation", 0.0).toFloat(),
            scale = obj.optDouble("scale", 1.0).toFloat(),
            color = obj.optString("color", "#F59E0B"),
            opacity = obj.optDouble("opacity", 1.0).toFloat()
        )
    }
}

@Immutable
data class BookCoverTemplate(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val category: String = "عام", // "أدبي", "أكاديمي", "تراثي", "تجريدي", "تقني", "خاص بي"
    val backgroundType: String = "GRADIENT", // "GRADIENT", "IMAGE_URI", "SOLID_COLOR"
    val backgroundColors: List<String> = listOf("#0F766E", "#115E59"),
    val gradientAngle: Float = 90f, // 0..360 degrees
    val imageUri: String? = null,
    val imageOpacity: Float = 1.0f, // 0..1
    val imageBlur: Float = 0f, // 0..25 dp
    val bookTitle: String = "عنوان الكتاب",
    val subtitle: String = "العنوان الفرعي أو عبارة تعريفية",
    val authorName: String = "اسم الكاتب",
    val publisherName: String = "دار النشر • الطبعة الأولى",
    val titleColor: String = "#FFFFFF",
    val subtitleColor: String = "#E2E8F0",
    val authorColor: String = "#F8FAFC",
    val accentColor: String = "#F59E0B",
    val fontFamily: String = "Amiri",
    val isUserCustom: Boolean = false,
    val backSynopsis: String = "نبذة موجزة وشاملة عن موضوع الكتاب والرسائل الأساسية التي يتناولها العمل...",
    val backAuthorBio: String = "عن الكاتب: كاتب ومؤلف متخصص.",
    val textLayers: List<CoverTextLayer> = emptyList(),
    val stickerLayers: List<CoverStickerLayer> = emptyList(),
    val is3DView: Boolean = false,
    val createdDate: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        val obj = JSONObject().apply {
            put("id", id)
            put("title", title)
            put("category", category)
            put("backgroundType", backgroundType)
            put("backgroundColors", JSONArray(backgroundColors))
            put("gradientAngle", gradientAngle.toDouble())
            put("imageUri", imageUri ?: "")
            put("imageOpacity", imageOpacity.toDouble())
            put("imageBlur", imageBlur.toDouble())
            put("bookTitle", bookTitle)
            put("subtitle", subtitle)
            put("authorName", authorName)
            put("publisherName", publisherName)
            put("titleColor", titleColor)
            put("subtitleColor", subtitleColor)
            put("authorColor", authorColor)
            put("accentColor", accentColor)
            put("fontFamily", fontFamily)
            put("isUserCustom", isUserCustom)
            put("backSynopsis", backSynopsis)
            put("backAuthorBio", backAuthorBio)
            put("is3DView", is3DView)
            put("createdDate", createdDate)

            val textsArr = JSONArray()
            textLayers.forEach { textsArr.put(it.toJson()) }
            put("textLayers", textsArr)

            val stickersArr = JSONArray()
            stickerLayers.forEach { stickersArr.put(it.toJson()) }
            put("stickerLayers", stickersArr)
        }
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): BookCoverTemplate? {
            return try {
                val obj = JSONObject(jsonStr)
                val colorsArray = obj.optJSONArray("backgroundColors")
                val colors = mutableListOf<String>()
                if (colorsArray != null) {
                    for (i in 0 until colorsArray.length()) {
                        colors.add(colorsArray.getString(i))
                    }
                }
                if (colors.isEmpty()) colors.add("#1E293B")

                val textsList = mutableListOf<CoverTextLayer>()
                val textsArr = obj.optJSONArray("textLayers")
                if (textsArr != null) {
                    for (i in 0 until textsArr.length()) {
                        textsList.add(CoverTextLayer.fromJson(textsArr.getJSONObject(i)))
                    }
                }

                val stickersList = mutableListOf<CoverStickerLayer>()
                val stickersArr = obj.optJSONArray("stickerLayers")
                if (stickersArr != null) {
                    for (i in 0 until stickersArr.length()) {
                        stickersList.add(CoverStickerLayer.fromJson(stickersArr.getJSONObject(i)))
                    }
                }

                BookCoverTemplate(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.optString("title", "نموذج غلاف"),
                    category = obj.optString("category", "خاص بي"),
                    backgroundType = obj.optString("backgroundType", "GRADIENT"),
                    backgroundColors = colors,
                    gradientAngle = obj.optDouble("gradientAngle", 90.0).toFloat(),
                    imageUri = obj.optString("imageUri").takeIf { it.isNotBlank() },
                    imageOpacity = obj.optDouble("imageOpacity", 1.0).toFloat(),
                    imageBlur = obj.optDouble("imageBlur", 0.0).toFloat(),
                    bookTitle = obj.optString("bookTitle", "عنوان الكتاب"),
                    subtitle = obj.optString("subtitle", ""),
                    authorName = obj.optString("authorName", "اسم الكاتب"),
                    publisherName = obj.optString("publisherName", ""),
                    titleColor = obj.optString("titleColor", "#FFFFFF"),
                    subtitleColor = obj.optString("subtitleColor", "#E2E8F0"),
                    authorColor = obj.optString("authorColor", "#F8FAFC"),
                    accentColor = obj.optString("accentColor", "#F59E0B"),
                    fontFamily = obj.optString("fontFamily", "Amiri"),
                    isUserCustom = obj.optBoolean("isUserCustom", true),
                    backSynopsis = obj.optString("backSynopsis", ""),
                    backAuthorBio = obj.optString("backAuthorBio", ""),
                    textLayers = textsList,
                    stickerLayers = stickersList,
                    is3DView = obj.optBoolean("is3DView", false),
                    createdDate = obj.optLong("createdDate", System.currentTimeMillis())
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
