package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStore Preferences Singleton extension on Context
private val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_preferences")

/**
 * نموذج بيانات لحفظ موضع القراءة الدقيق لكل كتاب/مستند
 */
data class SavedReadingPosition(
    val bookId: String,
    val chapterIndex: Int = 0,
    val scrollOffset: Int = 0,
    val totalProgressPercentage: Float = 0f,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)

/**
 * مدير تخزين واسترجاع تفضيلات ومواضع القراءة باستخدام Jetpack DataStore Preferences
 */
class ReaderPreferences(private val context: Context) {

    companion object {
        private val KEY_GLOBAL_LAST_CHAPTER_INDEX = intPreferencesKey("global_last_chapter_index")
        private val KEY_GLOBAL_LAST_BOOK_ID = stringPreferencesKey("global_last_book_id")

        // Keys for reader settings
        private val KEY_FONT_SIZE_SP = floatPreferencesKey("reader_font_size_sp")
        private val KEY_FONT_WEIGHT_NAME = stringPreferencesKey("reader_font_weight_name")
        private val KEY_READER_THEME_NAME = stringPreferencesKey("reader_theme_name")
        private val KEY_LINE_HEIGHT_MULTIPLIER = floatPreferencesKey("reader_line_height_multiplier")

        // Helper to sanitize bookId for DataStore keys
        private fun sanitizeKey(bookId: String): String {
            return bookId.replace(Regex("[^a-zA-Z0-9_]"), "_").take(64)
        }
    }

    private val safeDataStore: Flow<Preferences> = context.readerDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    /**
     * استرجاع رقم آخر فصل تم الوصول إليه بشكل عام
     */
    val lastChapterIndex: Flow<Int> = safeDataStore.map { preferences ->
        preferences[KEY_GLOBAL_LAST_CHAPTER_INDEX] ?: 0
    }

    /**
     * حفظ رقم الفصل الأخير العام
     */
    suspend fun saveLastChapterIndex(chapterIndex: Int) {
        context.readerDataStore.edit { preferences ->
            preferences[KEY_GLOBAL_LAST_CHAPTER_INDEX] = chapterIndex
        }
    }

    /**
     * استرجاع معرف آخر كتاب تمت قراءته
     */
    val lastBookId: Flow<String?> = safeDataStore.map { preferences ->
        preferences[KEY_GLOBAL_LAST_BOOK_ID]
    }

    /**
     * حفظ معرف آخر كتاب تمت قراءته
     */
    suspend fun saveLastBookId(bookId: String) {
        context.readerDataStore.edit { preferences ->
            preferences[KEY_GLOBAL_LAST_BOOK_ID] = bookId
        }
    }

    /**
     * استرجاع رقم آخر فصل لكتاب مخصص بناءً على المعرف (bookId / URI / ProjectId)
     */
    fun getLastChapterForBook(bookId: String): Flow<Int> {
        val sanitized = sanitizeKey(bookId)
        val key = intPreferencesKey("last_ch_$sanitized")
        return safeDataStore.map { preferences ->
            preferences[key] ?: 0
        }
    }

    /**
     * حفظ رقم الفصل المخصص لكتاب معين
     */
    suspend fun saveLastChapterForBook(bookId: String, chapterIndex: Int) {
        val sanitized = sanitizeKey(bookId)
        val key = intPreferencesKey("last_ch_$sanitized")
        context.readerDataStore.edit { preferences ->
            preferences[key] = chapterIndex
            preferences[KEY_GLOBAL_LAST_BOOK_ID] = bookId
            preferences[KEY_GLOBAL_LAST_CHAPTER_INDEX] = chapterIndex
        }
    }

    /**
     * استرجاع موضع القراءة الكامل لكتاب معين (رقم الفصل، موضع التمرير، نسبة التقدم، ووقت القراءة)
     */
    fun getReadingPositionForBook(bookId: String): Flow<SavedReadingPosition> {
        val sanitized = sanitizeKey(bookId)
        val chapterKey = intPreferencesKey("pos_ch_$sanitized")
        val scrollKey = intPreferencesKey("pos_scroll_$sanitized")
        val progressKey = floatPreferencesKey("pos_prog_$sanitized")
        val timeKey = longPreferencesKey("pos_time_$sanitized")

        return safeDataStore.map { preferences ->
            SavedReadingPosition(
                bookId = bookId,
                chapterIndex = preferences[chapterKey] ?: preferences[intPreferencesKey("last_ch_$sanitized")] ?: 0,
                scrollOffset = preferences[scrollKey] ?: 0,
                totalProgressPercentage = preferences[progressKey] ?: 0f,
                lastReadTimestamp = preferences[timeKey] ?: System.currentTimeMillis()
            )
        }
    }

    /**
     * حفظ موضع القراءة الكامل (الفصل، التمرير، نسبة التقدم)
     */
    suspend fun saveReadingPosition(
        bookId: String,
        chapterIndex: Int,
        scrollOffset: Int = 0,
        totalProgressPercentage: Float = 0f
    ) {
        val sanitized = sanitizeKey(bookId)
        val chapterKey = intPreferencesKey("pos_ch_$sanitized")
        val fallbackChapterKey = intPreferencesKey("last_ch_$sanitized")
        val scrollKey = intPreferencesKey("pos_scroll_$sanitized")
        val progressKey = floatPreferencesKey("pos_prog_$sanitized")
        val timeKey = longPreferencesKey("pos_time_$sanitized")

        context.readerDataStore.edit { preferences ->
            preferences[chapterKey] = chapterIndex
            preferences[fallbackChapterKey] = chapterIndex
            preferences[scrollKey] = scrollOffset
            preferences[progressKey] = totalProgressPercentage
            preferences[timeKey] = System.currentTimeMillis()

            preferences[KEY_GLOBAL_LAST_BOOK_ID] = bookId
            preferences[KEY_GLOBAL_LAST_CHAPTER_INDEX] = chapterIndex
        }
    }

    suspend fun saveLastReadingPosition(
        bookUri: String,
        chapterIndex: Int,
        scrollOffset: Int = 0
    ) {
        saveReadingPosition(bookId = bookUri, chapterIndex = chapterIndex, scrollOffset = scrollOffset)
    }

    suspend fun getLastReadingPosition(bookUri: String): SavedReadingPosition {
        val sanitized = sanitizeKey(bookUri)
        val chapterKey = intPreferencesKey("pos_ch_$sanitized")
        val scrollKey = intPreferencesKey("pos_scroll_$sanitized")
        val progressKey = floatPreferencesKey("pos_prog_$sanitized")
        val timeKey = longPreferencesKey("pos_time_$sanitized")

        var pos = SavedReadingPosition(bookId = bookUri, chapterIndex = 0)
        try {
            val prefs = safeDataStore.first()
            val ch = prefs[chapterKey] ?: prefs[intPreferencesKey("last_ch_$sanitized")] ?: -1
            val scroll = prefs[scrollKey] ?: 0
            val prog = prefs[progressKey] ?: 0f
            val time = prefs[timeKey] ?: System.currentTimeMillis()
            pos = SavedReadingPosition(bookId = bookUri, chapterIndex = ch, scrollOffset = scroll, totalProgressPercentage = prog, lastReadTimestamp = time)
        } catch (_: Exception) {}
        return pos
    }
}
