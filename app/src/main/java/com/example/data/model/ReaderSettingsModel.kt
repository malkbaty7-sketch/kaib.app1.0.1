package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * ثيمات القراءة المريحة للعين مع دعم التباين العالي والأنماط الكلاسيكية
 */
enum class ReaderTheme(
    val title: String,
    val backgroundColor: Color,
    val textColor: Color,
    val surfaceColor: Color = backgroundColor,
    val borderColor: Color = Color.Transparent
) {
    LIGHT(
        title = "فاتح",
        backgroundColor = Color(0xFFFFFFFF),
        textColor = Color(0xFF1C1B1F),
        surfaceColor = Color(0xFFF8FAFC),
        borderColor = Color(0xFFE0E0E0)
    ),
    DARK(
        title = "داكن",
        backgroundColor = Color(0xFF121212),
        textColor = Color(0xFFE0E0E0),
        surfaceColor = Color(0xFF1E1E1E),
        borderColor = Color(0xFF2C2C2C)
    ),
    SEPIA(
        title = "دافئ",
        backgroundColor = Color(0xFFFBF0D9),
        textColor = Color(0xFF5F4B32),
        surfaceColor = Color(0xFFF3E5C8),
        borderColor = Color(0xFFEADBCE)
    ),
    CALM_GRAY(
        title = "رمادي",
        backgroundColor = Color(0xFFF0F2F5),
        textColor = Color(0xFF263238),
        surfaceColor = Color(0xFFE4E7EB),
        borderColor = Color(0xFFD6DBE1)
    );

    // للتوافق مع الشاشات السابقة
    val displayName: String get() = title
    val bg: Color get() = backgroundColor
    val text: Color get() = textColor
    val surface: Color get() = surfaceColor
}

/**
 * خيارات سمك الخط المتاحة في القارئ
 */
enum class ReaderFontWeightOption(
    val title: String,
    val weight: FontWeight
) {
    NORMAL("عادي", FontWeight.Normal),
    MEDIUM("متوسط", FontWeight.Medium),
    BOLD("عريض", FontWeight.Bold)
}

/**
 * خيارات تباعد الأسطر السريعة
 */
enum class LineSpacingPreset(
    val title: String,
    val multiplier: Float
) {
    COMPACT("مدمج", 1.3f),
    BALANCED("متوازن", 1.6f),
    WIDE("واسع", 1.9f),
    RELAXED("فسيح", 2.2f)
}

/**
 * نموذج حالة إعدادات القراءة الشامل
 */
data class ReaderSettingsState(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val fontSizeSp: Float = 16f,
    val fontWeight: FontWeight = FontWeight.Normal,
    val lineHeightMultiplier: Float = 1.6f,
    val fontFamilyName: String = "افتراضي"
) {
    companion object {
        const val MIN_FONT_SIZE = 12f
        const val MAX_FONT_SIZE = 34f
        const val MIN_LINE_HEIGHT = 1.2f
        const val MAX_LINE_HEIGHT = 2.6f
    }
}
