package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.data.model.CustomFontEntity
import java.io.File
import java.io.FileOutputStream

object FontStorageManager {

    val defaultFonts = listOf(
        "Tajawal" to "تجوال (العصري المتوازن)",
        "Amiri" to "الأميري (الكلاسيكي النسخي)",
        "Cairo" to "كايرو (الحديث الجريء)",
        "Almarai" to "المراعي (الواضح للمطالعة)",
        "Noto Naskh" to "نسخ عربي أنيق",
        "System Default" to "خط النظام الافتراضي"
    )

    fun importFontFromUri(context: Context, uri: Uri): CustomFontEntity? {
        return try {
            val contentResolver = context.contentResolver
            var originalName = "custom_font_${System.currentTimeMillis()}.ttf"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val retrievedName = cursor.getString(nameIndex)
                        if (!retrievedName.isNullOrBlank()) {
                            originalName = retrievedName
                        }
                    }
                }
            }

            // Clean font name
            val cleanFontName = originalName
                .substringBeforeLast(".")
                .replace("_", " ")
                .replace("-", " ")
                .trim()
                .ifBlank { "خط مخصص" }

            val fontsDir = File(context.filesDir, "custom_fonts")
            if (!fontsDir.exists()) {
                fontsDir.mkdirs()
            }

            val safeFileName = "font_${System.currentTimeMillis()}_${originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}"
            val destFile = File(fontsDir, safeFileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                CustomFontEntity(
                    fontName = cleanFontName,
                    fileName = originalName,
                    filePath = destFile.absolutePath,
                    isDefault = false,
                    dateAdded = System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFontFamily(fontName: String, customFonts: List<CustomFontEntity>): FontFamily {
        // Check custom fonts first
        val custom = customFonts.firstOrNull { it.fontName.equals(fontName, ignoreCase = true) }
        if (custom != null) {
            val file = File(custom.filePath)
            if (file.exists() && file.canRead()) {
                try {
                    return FontFamily(Font(file))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Return standard font family
        return when (fontName) {
            "Tajawal", "تجوال" -> FontFamily.SansSerif
            "Amiri", "الأميري" -> FontFamily.Serif
            "Cairo", "كايرو" -> FontFamily.SansSerif
            "Almarai", "المراعي" -> FontFamily.SansSerif
            "Noto Naskh", "نسخ" -> FontFamily.Serif
            else -> FontFamily.Default
        }
    }
}
