package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.ReaderPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ReaderPreferencesTest {

    private lateinit var context: Context
    private lateinit var readerPreferences: ReaderPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        readerPreferences = ReaderPreferences(context)
    }

    @Test
    fun testDefaultChapterIndexIsZero() = runBlocking {
        val initialChapter = readerPreferences.lastChapterIndex.first()
        assertEquals(0, initialChapter)
    }

    @Test
    fun testSaveAndRetrieveGlobalChapterIndex() = runBlocking {
        readerPreferences.saveLastChapterIndex(5)
        val chapter = readerPreferences.lastChapterIndex.first()
        assertEquals(5, chapter)
    }

    @Test
    fun testSaveAndRetrieveSpecificBookChapter() = runBlocking {
        val bookId = "my_custom_book_42"
        val initial = readerPreferences.getLastChapterForBook(bookId).first()
        assertEquals(0, initial)

        readerPreferences.saveLastChapterForBook(bookId, 7)
        val saved = readerPreferences.getLastChapterForBook(bookId).first()
        assertEquals(7, saved)
    }

    @Test
    fun testSaveAndRetrieveReadingPosition() = runBlocking {
        val bookId = "novel_arabic_101"
        readerPreferences.saveReadingPosition(
            bookId = bookId,
            chapterIndex = 3,
            scrollOffset = 250,
            totalProgressPercentage = 0.45f
        )

        val position = readerPreferences.getReadingPositionForBook(bookId).first()
        assertNotNull(position)
        assertEquals(bookId, position.bookId)
        assertEquals(3, position.chapterIndex)
        assertEquals(250, position.scrollOffset)
        assertEquals(0.45f, position.totalProgressPercentage, 0.01f)
    }
}
