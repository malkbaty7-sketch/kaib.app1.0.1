package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single quote or excerpt saved from a book.
 */
data class BookQuote(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val pageNumber: Int? = null,
    val thought: String = "",
    val dateAdded: Long = System.currentTimeMillis()
)

object BookQuoteSerializer {
    fun toJson(quotes: List<BookQuote>): String {
        val array = JSONArray()
        for (q in quotes) {
            val obj = JSONObject()
            obj.put("id", q.id)
            obj.put("text", q.text)
            if (q.pageNumber != null) obj.put("pageNumber", q.pageNumber)
            obj.put("thought", q.thought)
            obj.put("dateAdded", q.dateAdded)
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String?): List<BookQuote> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<BookQuote>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val text = obj.optString("text", "")
                val pageNumber = if (obj.has("pageNumber") && !obj.isNull("pageNumber")) obj.getInt("pageNumber") else null
                val thought = obj.optString("thought", "")
                val dateAdded = obj.optLong("dateAdded", System.currentTimeMillis())
                if (text.isNotBlank()) {
                    list.add(BookQuote(id, text, pageNumber, thought, dateAdded))
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }
}

/**
 * Room Entity representing a saved book in the user's personal library.
 */
@Entity(tableName = "SavedBooks")
data class SavedBook(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val author: String,
    val description: String = "",
    val genre: String = "عام", // أدب وروايات، علوم وتكنولوجيا، فكر وفلسفة، تاريخ، شعر وبلاغة، عام
    val pageCount: Int = 0,
    val currentPage: Int = 0, // صفحة القراءة الحالية لتتبع نسبة الإنجاز
    val rating: Float = 5.0f,
    val readingStatus: String = "WANT_TO_READ", // WANT_TO_READ, READING, COMPLETED
    val isFavorite: Boolean = false,
    val coverColorHex: String = "#1E293B",
    val savedDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val quotesJson: String = "[]", // سجل الاقتباسات والفوائد
    val tags: String = "", // وسوم وتصنيفات مخصصة مفصولة بفواصل
    val format: String = "BOOK", // BOOK (ورقي), EBOOK (إلكتروني), AUDIO (صوتي)
    val isbnOrSource: String = "",
    val completionDate: Long? = null // تاريخ إتمام القراءة
) {
    val progressPercent: Int
        get() {
            if (pageCount <= 0) return if (readingStatus == "COMPLETED") 100 else 0
            if (readingStatus == "COMPLETED") return 100
            val p = ((currentPage.toFloat() / pageCount.toFloat()) * 100).toInt()
            return p.coerceIn(0, 100)
        }

    val quotesList: List<BookQuote>
        get() = BookQuoteSerializer.fromJson(quotesJson)
}

