package com.example.data.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Immutable
data class PageImageItem(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: String,
    val title: String = "",
    val caption: String = "",
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val widthDp: Float = 220f,
    val heightDp: Float = 160f,
    val rotationDeg: Float = 0f,
    val marginHorizontalDp: Float = 8f,
    val marginVerticalDp: Float = 8f,
    val cornerRadiusDp: Float = 10f,
    val borderThicknessDp: Float = 0f,
    val borderColorHex: String = "#333333",
    val elevationDp: Float = 4f,
    val opacity: Float = 1f,
    val alignment: String = "CENTER", // "START", "CENTER", "END", "FREE"
    val zIndex: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val blendMode: String = "NORMAL", // "NORMAL", "MULTIPLY", "SCREEN", "OVERLAY", "DARKEN", "LIGHTEN"
    val filterType: String = "NONE", // "NONE", "GRAYSCALE", "SEPIA", "VINTAGE_COMIC", "HIGH_CONTRAST", "WARM", "COOL"
    val speechBubbleText: String = "",
    val speechBubbleStyle: String = "NONE", // "NONE", "SPEECH", "THOUGHT", "SHOUT", "CAPTION"
    val speechBubblePosition: String = "TOP", // "TOP", "BOTTOM", "LEFT", "RIGHT"
    val comicPanelBorder: Boolean = false,
    val comicPanelPaddingDp: Float = 0f
)

object PageImageSerializer {
    fun toJson(images: List<PageImageItem>): String {
        val array = JSONArray()
        images.forEach { img ->
            val obj = JSONObject().apply {
                put("id", img.id)
                put("sourceUri", img.sourceUri)
                put("title", img.title)
                put("caption", img.caption)
                put("offsetX", img.offsetX.toDouble())
                put("offsetY", img.offsetY.toDouble())
                put("widthDp", img.widthDp.toDouble())
                put("heightDp", img.heightDp.toDouble())
                put("rotationDeg", img.rotationDeg.toDouble())
                put("marginHorizontalDp", img.marginHorizontalDp.toDouble())
                put("marginVerticalDp", img.marginVerticalDp.toDouble())
                put("cornerRadiusDp", img.cornerRadiusDp.toDouble())
                put("borderThicknessDp", img.borderThicknessDp.toDouble())
                put("borderColorHex", img.borderColorHex)
                put("elevationDp", img.elevationDp.toDouble())
                put("opacity", img.opacity.toDouble())
                put("alignment", img.alignment)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String?): List<PageImageItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<PageImageItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PageImageItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        sourceUri = obj.optString("sourceUri", ""),
                        title = obj.optString("title", ""),
                        caption = obj.optString("caption", ""),
                        offsetX = obj.optDouble("offsetX", 0.0).toFloat(),
                        offsetY = obj.optDouble("offsetY", 0.0).toFloat(),
                        widthDp = obj.optDouble("widthDp", 220.0).toFloat(),
                        heightDp = obj.optDouble("heightDp", 160.0).toFloat(),
                        rotationDeg = obj.optDouble("rotationDeg", 0.0).toFloat(),
                        marginHorizontalDp = obj.optDouble("marginHorizontalDp", 8.0).toFloat(),
                        marginVerticalDp = obj.optDouble("marginVerticalDp", 8.0).toFloat(),
                        cornerRadiusDp = obj.optDouble("cornerRadiusDp", 10.0).toFloat(),
                        borderThicknessDp = obj.optDouble("borderThicknessDp", 0.0).toFloat(),
                        borderColorHex = obj.optString("borderColorHex", "#333333"),
                        elevationDp = obj.optDouble("elevationDp", 4.0).toFloat(),
                        opacity = obj.optDouble("opacity", 1.0).toFloat(),
                        alignment = obj.optString("alignment", "CENTER")
                    )
                )
            }
        } catch (e: Exception) {
            // ignore malformed json
        }
        return list
    }
}
