package com.example.data.local

import androidx.room.*
import com.example.data.model.AppFile
import com.example.data.model.ExtractedText
import com.example.data.model.Project
import com.example.data.model.ProjectSnapshot
import com.example.data.model.SavedBook
import com.example.data.model.StoryElement
import kotlinx.coroutines.flow.Flow

@Dao
interface AppFileDao {
    @Query("SELECT * FROM Files ORDER BY uploadDate DESC")
    fun getAllFiles(): Flow<List<AppFile>>

    @Query("SELECT * FROM Files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Int): AppFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: AppFile): Long

    @Query("DELETE FROM Files WHERE id = :id")
    suspend fun deleteFileById(id: Int)
}

@Dao
interface ExtractedTextDao {
    @Query("SELECT * FROM ExtractedTexts ORDER BY extractionDate DESC")
    fun getAllExtractedTexts(): Flow<List<ExtractedText>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtractedText(extractedText: ExtractedText): Long

    @Query("DELETE FROM ExtractedTexts WHERE id = :id")
    suspend fun deleteExtractedTextById(id: Int)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM Projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM Projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): Project?

    @Query("SELECT * FROM Projects WHERE id = :id")
    fun getProjectByIdFlow(id: Int): Flow<Project?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Query("DELETE FROM Projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

@Dao
interface StoryElementDao {
    @Query("SELECT * FROM StoryElements WHERE projectId = :projectId ORDER BY id ASC")
    fun getElementsByProjectId(projectId: Int): Flow<List<StoryElement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElement(element: StoryElement): Long

    @Update
    suspend fun updateElement(element: StoryElement)

    @Delete
    suspend fun deleteElement(element: StoryElement)

    @Query("DELETE FROM StoryElements WHERE projectId = :projectId")
    suspend fun deleteElementsByProjectId(projectId: Int)
}

@Dao
interface ProjectSnapshotDao {
    @Query("SELECT * FROM ProjectSnapshots WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getSnapshotsByProjectId(projectId: Int): Flow<List<ProjectSnapshot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: ProjectSnapshot): Long

    @Query("DELETE FROM ProjectSnapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: Int)
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM DetailedSnapshots WHERE chapterId = :chapterId ORDER BY timestamp DESC")
    fun getSnapshotsForChapter(chapterId: String): Flow<List<com.example.data.model.SnapshotEntity>>

    @Query("SELECT * FROM DetailedSnapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<com.example.data.model.SnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: com.example.data.model.SnapshotEntity): Long

    @Query("DELETE FROM DetailedSnapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: Long)

    @Query("DELETE FROM DetailedSnapshots WHERE chapterId = :chapterId")
    suspend fun deleteSnapshotsByChapter(chapterId: String)
}

@Dao
interface CustomFontDao {
    @Query("SELECT * FROM CustomFonts ORDER BY dateAdded DESC")
    fun getAllFonts(): Flow<List<com.example.data.model.CustomFontEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFont(font: com.example.data.model.CustomFontEntity): Long

    @Query("DELETE FROM CustomFonts WHERE id = :id")
    suspend fun deleteFontById(id: Long)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM ChapterComments WHERE chapterId = :chapterId ORDER BY timestamp DESC")
    fun getCommentsForChapter(chapterId: String): Flow<List<com.example.data.model.CommentEntity>>

    @Query("SELECT * FROM ChapterComments ORDER BY timestamp DESC")
    fun getAllComments(): Flow<List<com.example.data.model.CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: com.example.data.model.CommentEntity): Long

    @Update
    suspend fun updateComment(comment: com.example.data.model.CommentEntity)

    @Query("UPDATE ChapterComments SET isResolved = :resolved WHERE id = :id")
    suspend fun setCommentResolved(id: Long, resolved: Boolean)

    @Query("UPDATE ChapterComments SET replyText = :reply WHERE id = :id")
    suspend fun replyToComment(id: Long, reply: String)

    @Query("DELETE FROM ChapterComments WHERE id = :id")
    suspend fun deleteComment(id: Long)
}

@Dao
interface DailyWritingAnalyticsDao {
    @Query("SELECT * FROM DailyWritingAnalytics ORDER BY dateKey DESC")
    fun getAllDailyAnalytics(): Flow<List<com.example.data.model.DailyWritingAnalyticsEntity>>

    @Query("SELECT * FROM DailyWritingAnalytics WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getAnalyticsForDate(dateKey: String): com.example.data.model.DailyWritingAnalyticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(analytics: com.example.data.model.DailyWritingAnalyticsEntity)
}

@Dao
interface DocumentProgressDao {
    @Query("SELECT * FROM document_progress ORDER BY lastReadTimestamp DESC")
    fun getAllProgress(): Flow<List<com.example.data.model.DocumentProgressEntity>>

    @Query("SELECT * FROM document_progress WHERE fileUri = :uri LIMIT 1")
    fun getProgressForUri(uri: String): Flow<com.example.data.model.DocumentProgressEntity?>

    @Query("SELECT * FROM document_progress WHERE fileUri = :uri LIMIT 1")
    suspend fun getProgressSync(uri: String): com.example.data.model.DocumentProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: com.example.data.model.DocumentProgressEntity)

    @Query("DELETE FROM document_progress WHERE fileUri = :uri")
    suspend fun deleteProgress(uri: String)
}

@Database(
    entities = [
        AppFile::class,
        ExtractedText::class,
        Project::class,
        StoryElement::class,
        ProjectSnapshot::class,
        SavedBook::class,
        com.example.data.model.SnapshotEntity::class,
        com.example.data.model.CustomFontEntity::class,
        com.example.data.model.CommentEntity::class,
        com.example.data.model.DailyWritingAnalyticsEntity::class,
        com.example.data.model.DocumentProgressEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appFileDao(): AppFileDao
    abstract fun extractedTextDao(): ExtractedTextDao
    abstract fun projectDao(): ProjectDao
    abstract fun storyElementDao(): StoryElementDao
    abstract fun projectSnapshotDao(): ProjectSnapshotDao
    abstract fun savedBookDao(): SavedBookDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun customFontDao(): CustomFontDao
    abstract fun commentDao(): CommentDao
    abstract fun dailyWritingAnalyticsDao(): DailyWritingAnalyticsDao
    abstract fun documentProgressDao(): DocumentProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "khetta_app_database.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

