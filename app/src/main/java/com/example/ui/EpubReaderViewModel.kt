package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.ReaderPreferences
import com.example.data.preferences.SavedReadingPosition
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel لإدارة وحفظ تفضيلات القراءة والفصول في كتب EPUB والمستندات عبر DataStore
 */
class EpubReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val readerPrefs = ReaderPreferences(application)

    // استمرار مراقبة رقم آخر فصل تم الوصول إليه عامة كـ StateFlow
    val lastChapterIndex: StateFlow<Int> = readerPrefs.lastChapterIndex
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val lastBookId: StateFlow<String?> = readerPrefs.lastBookId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * تُستدعى عند تغيير الفصل أو مغادرة القارئ
     */
    fun onChapterChanged(newIndex: Int) {
        viewModelScope.launch {
            readerPrefs.saveLastChapterIndex(newIndex)
        }
    }

    /**
     * حفظ واسترجاع مخصص حسب id الكتاب أو المسار
     */
    fun getBookLastChapter(bookId: String): StateFlow<Int> {
        return readerPrefs.getLastChapterForBook(bookId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )
    }

    /**
     * حفظ رقم الفصل لكتاب معين
     */
    fun saveBookChapter(bookId: String, chapterIndex: Int) {
        viewModelScope.launch {
            readerPrefs.saveLastChapterForBook(bookId, chapterIndex)
        }
    }

    /**
     * استرجاع موضع القراءة الكامل (الفصل وموضع التمرير والنسبة المئوية)
     */
    fun getReadingPosition(bookId: String): StateFlow<SavedReadingPosition> {
        return readerPrefs.getReadingPositionForBook(bookId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SavedReadingPosition(bookId = bookId)
            )
    }

    /**
     * حفظ موضع القراءة الكامل
     */
    fun saveReadingPosition(
        bookId: String,
        chapterIndex: Int,
        scrollOffset: Int = 0,
        totalProgressPercentage: Float = 0f
    ) {
        viewModelScope.launch {
            readerPrefs.saveReadingPosition(
                bookId = bookId,
                chapterIndex = chapterIndex,
                scrollOffset = scrollOffset,
                totalProgressPercentage = totalProgressPercentage
            )
        }
    }
}
