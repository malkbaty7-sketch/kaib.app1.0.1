package com.example.data.local

import androidx.room.*
import com.example.data.model.SavedBook
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) for SavedBook operations.
 */
@Dao
interface SavedBookDao {
    @Query("SELECT * FROM SavedBooks ORDER BY savedDate DESC")
    fun getAllSavedBooks(): Flow<List<SavedBook>>

    @Query("SELECT * FROM SavedBooks WHERE isFavorite = 1 ORDER BY savedDate DESC")
    fun getFavoriteBooks(): Flow<List<SavedBook>>

    @Query("SELECT * FROM SavedBooks WHERE readingStatus = :status ORDER BY savedDate DESC")
    fun getBooksByStatus(status: String): Flow<List<SavedBook>>

    @Query("SELECT * FROM SavedBooks WHERE genre = :genre ORDER BY savedDate DESC")
    fun getBooksByGenre(genre: String): Flow<List<SavedBook>>

    @Query("SELECT * FROM SavedBooks WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: Int): SavedBook?

    @Query("SELECT * FROM SavedBooks WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY savedDate DESC")
    fun searchBooks(query: String): Flow<List<SavedBook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: SavedBook): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<SavedBook>)

    @Update
    suspend fun updateBook(book: SavedBook)

    @Delete
    suspend fun deleteBook(book: SavedBook)

    @Query("DELETE FROM SavedBooks WHERE id = :id")
    suspend fun deleteBookById(id: Int)

    @Query("UPDATE SavedBooks SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Int)

    @Query("UPDATE SavedBooks SET readingStatus = :newStatus WHERE id = :id")
    suspend fun updateReadingStatus(id: Int, newStatus: String)

    @Query("SELECT COUNT(*) FROM SavedBooks")
    suspend fun getBooksCount(): Int
}
