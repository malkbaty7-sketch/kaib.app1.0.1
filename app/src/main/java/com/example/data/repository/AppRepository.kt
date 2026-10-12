package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class AppRepository(private val database: AppDatabase) {
    val allFiles: Flow<List<AppFile>> = database.appFileDao().getAllFiles()
    val allProjects: Flow<List<Project>> = database.projectDao().getAllProjects()
    val allExtractedTexts: Flow<List<ExtractedText>> = database.extractedTextDao().getAllExtractedTexts()

    suspend fun getFileById(id: Int): AppFile? = database.appFileDao().getFileById(id)

    suspend fun insertFile(file: AppFile): Long = database.appFileDao().insertFile(file)

    suspend fun deleteFile(id: Int) = database.appFileDao().deleteFileById(id)

    suspend fun insertExtractedText(extractedText: ExtractedText): Long =
        database.extractedTextDao().insertExtractedText(extractedText)

    suspend fun deleteExtractedText(id: Int) = database.extractedTextDao().deleteExtractedTextById(id)

    suspend fun getProjectById(id: Int): Project? = database.projectDao().getProjectById(id)

    fun getProjectByIdFlow(id: Int): Flow<Project?> = database.projectDao().getProjectByIdFlow(id)

    suspend fun insertProject(project: Project): Long = database.projectDao().insertProject(project)

    suspend fun updateProject(project: Project) = database.projectDao().updateProject(project)

    suspend fun deleteProject(id: Int) = database.projectDao().deleteProjectById(id)

    // Story Elements
    fun getStoryElementsByProject(projectId: Int): Flow<List<StoryElement>> =
        database.storyElementDao().getElementsByProjectId(projectId)

    suspend fun insertStoryElement(element: StoryElement): Long =
        database.storyElementDao().insertElement(element)

    suspend fun updateStoryElement(element: StoryElement) =
        database.storyElementDao().updateElement(element)

    suspend fun deleteStoryElement(element: StoryElement) =
        database.storyElementDao().deleteElement(element)

    // Project Snapshots (Version History)
    fun getSnapshotsByProject(projectId: Int): Flow<List<ProjectSnapshot>> =
        database.projectSnapshotDao().getSnapshotsByProjectId(projectId)

    suspend fun insertSnapshot(snapshot: ProjectSnapshot): Long =
        database.projectSnapshotDao().insertSnapshot(snapshot)

    suspend fun deleteSnapshot(id: Int) =
        database.projectSnapshotDao().deleteSnapshotById(id)

    // Saved Books
    val allSavedBooks: Flow<List<SavedBook>> = database.savedBookDao().getAllSavedBooks()
    val favoriteSavedBooks: Flow<List<SavedBook>> = database.savedBookDao().getFavoriteBooks()

    suspend fun getSavedBookById(id: Int): SavedBook? = database.savedBookDao().getBookById(id)

    suspend fun insertSavedBook(book: SavedBook): Long = database.savedBookDao().insertBook(book)

    suspend fun insertSavedBooks(books: List<SavedBook>) = database.savedBookDao().insertBooks(books)

    suspend fun updateSavedBook(book: SavedBook) = database.savedBookDao().updateBook(book)

    suspend fun deleteSavedBook(book: SavedBook) = database.savedBookDao().deleteBook(book)

    suspend fun deleteSavedBookById(id: Int) = database.savedBookDao().deleteBookById(id)

    suspend fun toggleSavedBookFavorite(id: Int) = database.savedBookDao().toggleFavorite(id)

    suspend fun updateSavedBookReadingStatus(id: Int, newStatus: String) =
        database.savedBookDao().updateReadingStatus(id, newStatus)

    suspend fun getSavedBooksCount(): Int = database.savedBookDao().getBooksCount()

    fun searchSavedBooks(query: String): Flow<List<SavedBook>> =
        database.savedBookDao().searchBooks(query)

    // Detailed Snapshots (Recovery System)
    fun getDetailedSnapshotsForChapter(chapterId: String): Flow<List<SnapshotEntity>> =
        database.snapshotDao().getSnapshotsForChapter(chapterId)

    fun getAllDetailedSnapshots(): Flow<List<SnapshotEntity>> =
        database.snapshotDao().getAllSnapshots()

    suspend fun insertDetailedSnapshot(snapshot: SnapshotEntity): Long =
        database.snapshotDao().insertSnapshot(snapshot)

    suspend fun deleteDetailedSnapshot(id: Long) =
        database.snapshotDao().deleteSnapshotById(id)

    suspend fun deleteDetailedSnapshotsByChapter(chapterId: String) =
        database.snapshotDao().deleteSnapshotsByChapter(chapterId)

    // Custom Fonts Registry
    val allCustomFonts: Flow<List<CustomFontEntity>> = database.customFontDao().getAllFonts()

    suspend fun insertCustomFont(font: CustomFontEntity): Long =
        database.customFontDao().insertFont(font)

    suspend fun deleteCustomFont(id: Long) =
        database.customFontDao().deleteFontById(id)

    // Chapter Comments & Beta Reader Feedback (Task 3.1)
    fun getCommentsForChapter(chapterId: String): Flow<List<com.example.data.model.CommentEntity>> =
        database.commentDao().getCommentsForChapter(chapterId)

    fun getAllComments(): Flow<List<com.example.data.model.CommentEntity>> =
        database.commentDao().getAllComments()

    suspend fun insertComment(comment: com.example.data.model.CommentEntity): Long =
        database.commentDao().insertComment(comment)

    suspend fun setCommentResolved(id: Long, resolved: Boolean) =
        database.commentDao().setCommentResolved(id, resolved)

    suspend fun replyToComment(id: Long, reply: String) =
        database.commentDao().replyToComment(id, reply)

    suspend fun deleteComment(id: Long) =
        database.commentDao().deleteComment(id)

    // Document Progress & Reading Position Persistence (Task 3)
    val allDocumentProgress: Flow<List<DocumentProgressEntity>> = database.documentProgressDao().getAllProgress()

    fun getDocumentProgress(uri: String): Flow<DocumentProgressEntity?> =
        database.documentProgressDao().getProgressForUri(uri)

    suspend fun getDocumentProgressSync(uri: String): DocumentProgressEntity? =
        database.documentProgressDao().getProgressSync(uri)

    suspend fun saveDocumentProgress(progress: DocumentProgressEntity) =
        database.documentProgressDao().insertOrUpdate(progress)

    suspend fun deleteDocumentProgress(uri: String) =
        database.documentProgressDao().deleteProgress(uri)
}

