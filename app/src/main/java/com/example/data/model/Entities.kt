package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "Files")
data class AppFile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fileName: String,
    val filePath: String,
    val fileType: String,
    val uploadDate: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val wordCount: Int = 0,
    val extractedText: String = ""
)

@Immutable
@Entity(tableName = "ExtractedTexts")
data class ExtractedText(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fileId: Int? = null,
    val topicQuery: String,
    val extractedContent: String,
    val extractionDate: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "Projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val projectName: String,
    val projectType: String = "E_BOOK", // E_BOOK, RESEARCH_PAPER, GENERAL_DOC
    val writingTemplateId: String = "novel_action",
    val themeId: String = "warmSunset",
    val paperColor: String = "#FFFDD0", // Cream
    val paperSize: String = "A4",
    val pageViewMode: String = "SINGLE_PAGE", // SINGLE_PAGE, DOUBLE_PAGE
    val pageNumberStyle: Int = 0, // 0..5
    val pageNumberPosition: String = "BOTTOM_CENTER",
    val selectedFont: String = "Tajawal",
    val coverImagePath: String? = null,
    val emotionProfileJson: String = "[\"warmth\",\"motivation\"]",
    val emotionSuggestionsActive: Boolean = true,
    val createdDate: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val contentJson: String = "[]", // JSON array of ChapterItem
    val targetWordCount: Int = 0
)

@Immutable
data class Scene(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chapterId: String = "",
    val title: String = "مشهد جديد",
    val content: String = "",
    val orderIndex: Int = 0,
    val tensionLevel: Float = 0.5f,
    val summary: String = ""
)

object SceneSerializer {
    fun fromJson(jsonStr: String?): List<Scene> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<Scene>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Scene(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        chapterId = obj.optString("chapterId", ""),
                        title = obj.optString("title", "مشهد"),
                        content = obj.optString("content", ""),
                        orderIndex = obj.optInt("orderIndex", i),
                        tensionLevel = obj.optDouble("tensionLevel", 0.5).toFloat(),
                        summary = obj.optString("summary", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedBy { it.orderIndex }
    }

    fun toJson(scenes: List<Scene>): String {
        val array = org.json.JSONArray()
        scenes.forEachIndexed { idx, scene ->
            val obj = org.json.JSONObject().apply {
                put("id", scene.id)
                put("chapterId", scene.chapterId)
                put("title", scene.title)
                put("content", scene.content)
                put("orderIndex", idx)
                put("tensionLevel", scene.tensionLevel.toDouble())
                put("summary", scene.summary)
            }
            array.put(obj)
        }
        return array.toString()
    }
}

@Immutable
data class ChapterItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val tabName: String = title,
    val content: String = "",
    val tensionLevel: Float = 0.5f,
    val sectionType: String = "CHAPTER",
    val imagesJson: String = "[]",
    val scenesJson: String = "[]"
)

@Immutable
@Entity(tableName = "StoryElements")
data class StoryElement(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val projectId: Int,
    val name: String,
    val roleOrType: String = "CHARACTER", // "CHARACTER", "LOCATION", "LORE", "PLOT_DEVICE"
    val description: String = "",
    val psychologicalMotivation: String = "",
    val colorTag: String = "#D2691E",
    val createdDate: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "ProjectSnapshots")
data class ProjectSnapshot(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val projectId: Int,
    val snapshotLabel: String,
    val contentJson: String,
    val wordCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "DetailedSnapshots")
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val chapterId: String,
    val contentHash: String,
    val deltaContent: String,
    val wordCount: Int = 0,
    val chapterTitle: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "CustomFonts")
data class CustomFontEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fontName: String,
    val fileName: String,
    val filePath: String,
    val isDefault: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "ChapterComments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val chapterId: String,
    val selectedTextRange: String = "", // Quotation or substring range e.g. "0-40"
    val authorName: String = "قارئ تجريبي",
    val commentText: String,
    val replyText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)

@Immutable
data class CorkboardCard(
    val id: String = java.util.UUID.randomUUID().toString(),
    val targetId: String = "", // chapterId, sceneId, or storyElementId
    val cardType: String = "SCENE", // "CHAPTER", "SCENE", "CHARACTER", "PLOT"
    val title: String = "",
    val content: String = "",
    val posX: Float = 100f,
    val posY: Float = 100f,
    val colorHex: String = "#FFF9C4",
    val connectedCardIds: List<String> = emptyList(),
    val tag: String = ""
)

@Immutable
data class ExportAdvancedMetadata(
    val isbn: String = "978-3-16-148410-0",
    val publisher: String = "دار كاتب للنشر والتوزيع",
    val language: String = "ar",
    val rights: String = "جميع الحقوق محفوظة للمؤلف © 2026",
    val authorBio: String = "",
    val publicationYear: String = "2026",
    val mirrorMargins: Boolean = true,
    val paperWeightGsm: Int = 80, // 80 g/m² standard book paper
    val trimSize: String = "A5" // "A5", "B5", "ROYAL_OCTAVO", "DIGEST", "A4"
)

@Immutable
@Entity(tableName = "DailyWritingAnalytics")
data class DailyWritingAnalyticsEntity(
    @PrimaryKey
    val dateKey: String, // "YYYY-MM-DD"
    val wordsAdded: Int = 0,
    val wordsDeleted: Int = 0,
    val netWords: Int = 0,
    val sessionMinutes: Int = 0,
    val wordsPerMinute: Int = 0,
    val targetWordGoal: Int = 2000,
    val peakHour: Int = 20, // 8 PM
    val streakDays: Int = 1
)

@Immutable
@Entity(tableName = "document_progress")
data class DocumentProgressEntity(
    @PrimaryKey val fileUri: String, // رابط الملف الفريد
    val fileName: String,
    val fileType: String,
    val lastPageIndex: Int = 0, // رقم الصفحة للـ PDF/EPUB
    val scrollOffset: Int = 0, // موضع التمرير العمودي للنصوص والوورد
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val totalProgressPercentage: Float = 0f
)

