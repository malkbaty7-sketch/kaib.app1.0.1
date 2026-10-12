package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BookQuote
import com.example.data.model.BookQuoteSerializer
import com.example.data.model.SavedBook
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SavedBooksStats(
    val totalCount: Int = 0,
    val readingCount: Int = 0,
    val completedCount: Int = 0,
    val wantToReadCount: Int = 0,
    val favoriteCount: Int = 0,
    val totalPagesRead: Int = 0,
    val totalQuotesCount: Int = 0,
    val completedThisYear: Int = 0,
    val yearlyGoal: Int = 12
)

data class FilterParams(
    val query: String,
    val genre: String,
    val statusFilter: String,
    val formatFilter: String,
    val sort: String
)

class SavedBooksViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: AppRepository = AppRepository(AppDatabase.getDatabase(application))

    val allBooks: StateFlow<List<SavedBook>> = repository.allSavedBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedGenre = MutableStateFlow("ALL") // "ALL", "أدب وروايات", "علوم وتكنولوجيا", "فكر وفلسفة", "تاريخ وحضارة", "شعر وبلاغة", "عام"
    val selectedReadingStatus = MutableStateFlow("ALL") // "ALL", "FAVORITE", "READING", "COMPLETED", "WANT_TO_READ"
    val selectedFormat = MutableStateFlow("ALL") // "ALL", "BOOK", "EBOOK", "AUDIO"
    val sortBy = MutableStateFlow("DATE_DESC") // "DATE_DESC", "TITLE_ASC", "RATING_DESC", "PAGES_DESC", "PROGRESS_DESC"
    val viewMode = MutableStateFlow("LIST") // "LIST" or "GRID"
    val yearlyReadingGoal = MutableStateFlow(12)

    // Toast or message notification
    val toastMessage = MutableStateFlow<String?>(null)

    // Filtered & Sorted books for the LazyColumn list
    private val filterCriteria = combine(
        searchQuery,
        selectedGenre,
        selectedReadingStatus,
        selectedFormat,
        sortBy
    ) { query, genre, statusFilter, formatFilter, sort ->
        FilterParams(query, genre, statusFilter, formatFilter, sort)
    }

    val filteredBooks: StateFlow<List<SavedBook>> = combine(
        allBooks,
        filterCriteria
    ) { books, filter ->
        var result = books

        // 1. Text Query Filter
        if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.genre.lowercase().contains(q) ||
                it.tags.lowercase().contains(q)
            }
        }

        // 2. Genre Filter
        if (filter.genre != "ALL") {
            result = result.filter { it.genre == filter.genre }
        }

        // 3. Status or Favorite Filter
        if (filter.statusFilter == "FAVORITE") {
            result = result.filter { it.isFavorite }
        } else if (filter.statusFilter != "ALL") {
            result = result.filter { it.readingStatus == filter.statusFilter }
        }

        // 4. Format Filter
        if (filter.formatFilter != "ALL") {
            result = result.filter { it.format == filter.formatFilter }
        }

        // 5. Sorting
        when (filter.sort) {
            "TITLE_ASC" -> result.sortedBy { it.title }
            "RATING_DESC" -> result.sortedByDescending { it.rating }
            "PAGES_DESC" -> result.sortedByDescending { it.pageCount }
            "PROGRESS_DESC" -> result.sortedByDescending { it.progressPercent }
            else -> result.sortedByDescending { it.savedDate }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics Flow
    val stats: StateFlow<SavedBooksStats> = combine(allBooks, yearlyReadingGoal) { list, goal ->
        var totalPages = 0
        var totalQuotes = 0
        var completedYear = 0

        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        list.forEach { book ->
            totalPages += when (book.readingStatus) {
                "COMPLETED" -> book.pageCount
                else -> book.currentPage.coerceAtMost(book.pageCount)
            }
            totalQuotes += book.quotesList.size
            if (book.readingStatus == "COMPLETED") {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = book.completionDate ?: book.savedDate
                if (cal.get(java.util.Calendar.YEAR) == currentYear) {
                    completedYear++
                }
            }
        }

        SavedBooksStats(
            totalCount = list.size,
            readingCount = list.count { it.readingStatus == "READING" },
            completedCount = list.count { it.readingStatus == "COMPLETED" },
            wantToReadCount = list.count { it.readingStatus == "WANT_TO_READ" },
            favoriteCount = list.count { it.isFavorite },
            totalPagesRead = totalPages,
            totalQuotesCount = totalQuotes,
            completedThisYear = completedYear,
            yearlyGoal = goal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavedBooksStats())

    init {
        seedInitialBooksIfEmpty()
    }

    private fun seedInitialBooksIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getSavedBooksCount()
            if (count == 0) {
                val quote1 = listOf(
                    BookQuote(
                        text = "إن التاريخ في ظاهره لا يزيد عن الإخبار، ولكن في باطنه نظر وتحقيق، وتعليل للكائنات ومباديها دقيق.",
                        pageNumber = 12,
                        thought = "فلسفة نقدية عميقة للتحقق من الأخبار التاريخية وربط الأسباب بالمسببات."
                    ),
                    BookQuote(
                        text = "الظلم مؤذن بخراب العمران.",
                        pageNumber = 287,
                        thought = "قاعدة سوسيولوجية وسياسية خالدة تربط العدالة بازدهار الأمم."
                    )
                )

                val quote2 = listOf(
                    BookQuote(
                        text = "كان هذا اليوم لا يُنسى، لم يذق فيه الصبي طعماً للنوم، كانت عيناه مغلقتين ولكن بصيرته متقدة.",
                        pageNumber = 45,
                        thought = "تصوير عميق لمعاناة الطفل وإصراره على المعرفة."
                    )
                )

                val seedList = listOf(
                    SavedBook(
                        title = "مقدمة ابن خلدون",
                        author = "عبد الرحمن بن خلدون",
                        description = "كتاب موسوعي يبحث في علم الاجتماع وفلسفة التاريخ والعمران البشري وطبائع الدول والقبائل.",
                        genre = "تاريخ وحضارة",
                        pageCount = 680,
                        currentPage = 680,
                        rating = 5.0f,
                        readingStatus = "COMPLETED",
                        isFavorite = true,
                        coverColorHex = "#0F766E", // Deep Teal
                        notes = "من أعظم ما كُتب في التحليل السوسيولوجي وحركات التاريخ وتطور المجتمعات.",
                        quotesJson = BookQuoteSerializer.toJson(quote1),
                        tags = "تاريخ, علم الاجتماع, فلسفة, عمران",
                        format = "BOOK",
                        completionDate = System.currentTimeMillis() - 86400000L * 15
                    ),
                    SavedBook(
                        title = "الأيام",
                        author = "د. طه حسين",
                        description = "سيرة ذاتية ملهمة لعميد الأدب العربي تجسد رحلته من القرية إلى الأزهر ومن ثم باريس متغلباً على فقدان البصر.",
                        genre = "أدب وروايات",
                        pageCount = 320,
                        currentPage = 195,
                        rating = 4.8f,
                        readingStatus = "READING",
                        isFavorite = true,
                        coverColorHex = "#B45309", // Warm Amber
                        notes = "أسلوب أدبي بديع بصيغة الغائب واستخدام فريد للحواس البديلة.",
                        quotesJson = BookQuoteSerializer.toJson(quote2),
                        tags = "سيرة ذاتية, أدب عربي, إلهام",
                        format = "BOOK"
                    ),
                    SavedBook(
                        title = "طوق الحمامة في الألفة والأُلاّف",
                        author = "ابن حزم الأندلسي",
                        description = "دراسة نفسية وأدبية عميقة لطبيعة المشاعر الإنسانية والحب والوفاء في الحضارة الأندلسية.",
                        genre = "فكر وفلسفة",
                        pageCount = 240,
                        currentPage = 0,
                        rating = 4.7f,
                        readingStatus = "WANT_TO_READ",
                        isFavorite = false,
                        coverColorHex = "#831843", // Burgundy Rose
                        notes = "تحفة أدبية تتضمن استشهادات شعرية ورؤية فلسفية ثاقبة.",
                        tags = "أندلسيات, فلسفة المشاعر, شعر",
                        format = "EBOOK"
                    ),
                    SavedBook(
                        title = "كليلة ودمنة",
                        author = "عبد الله بن المقفع",
                        description = "حكايات رمزية فلسفية على ألسنة الحيوانات والطيور تحمل حكماً سياسية وأخلاقية خالدة.",
                        genre = "أدب وروايات",
                        pageCount = 290,
                        currentPage = 290,
                        rating = 4.9f,
                        readingStatus = "COMPLETED",
                        isFavorite = true,
                        coverColorHex = "#1E3A8A", // Indigo Navy
                        notes = "مرجع بليغ في النثر الفني العربي وعلم السياسة الأخلاقية.",
                        tags = "أمثال وحكم, حكايات رمزية, سياسة",
                        format = "BOOK",
                        completionDate = System.currentTimeMillis() - 86400000L * 40
                    ),
                    SavedBook(
                        title = "البؤساء (Les Misérables)",
                        author = "فيكتور هوجو",
                        description = "ملحمة إنسانية خالدة تتناول قضايا العدالة والرحمة والفقر والخلاص في المجتمع الفرنسي.",
                        genre = "أدب وروايات",
                        pageCount = 850,
                        currentPage = 420,
                        rating = 5.0f,
                        readingStatus = "READING",
                        isFavorite = false,
                        coverColorHex = "#374151", // Slate Charcoal
                        notes = "جان فالجان وجافير - صراع القانون والضمير الإنساني.",
                        tags = "أدب عالمي, رواية كلاسيكية, عدالة",
                        format = "EBOOK"
                    ),
                    SavedBook(
                        title = "فيزياء المستحيل",
                        author = "ميتشيو كاكو",
                        description = "استكشاف علمي شيق لإمكانية تحقيق تقنيات الخيال العلمي كالقفز الزمكاني والتخفي عن الأنظار.",
                        genre = "علوم وتكنولوجيا",
                        pageCount = 360,
                        currentPage = 50,
                        rating = 4.6f,
                        readingStatus = "READING",
                        isFavorite = false,
                        coverColorHex = "#0369A1", // Ocean Sky
                        notes = "شرح مبسط لقوانين الديناميكا الحرارية والفيزياء الكمية وتطبيقاتها المستقبلية.",
                        tags = "فيزياء, فضاء, تكنولوجيا, خيال علمي",
                        format = "AUDIO"
                    )
                )
                repository.insertSavedBooks(seedList)
            }
        }
    }

    fun addBook(
        title: String,
        author: String,
        description: String,
        genre: String,
        pageCount: Int,
        currentPage: Int = 0,
        rating: Float,
        readingStatus: String,
        coverColorHex: String,
        notes: String,
        tags: String = "",
        format: String = "BOOK"
    ) {
        if (title.isBlank()) {
            toastMessage.value = "يرجى كتابة عنوان الكتاب"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val safePageCount = pageCount.coerceAtLeast(0)
            val safeCurrentPage = currentPage.coerceIn(0, if (safePageCount > 0) safePageCount else 99999)
            val finalStatus = if (safePageCount > 0 && safeCurrentPage >= safePageCount) "COMPLETED" else readingStatus

            val book = SavedBook(
                title = title.trim(),
                author = if (author.isBlank()) "مؤلف مجهول" else author.trim(),
                description = description.trim(),
                genre = if (genre.isBlank()) "عام" else genre,
                pageCount = safePageCount,
                currentPage = safeCurrentPage,
                rating = rating.coerceIn(0.5f, 5.0f),
                readingStatus = finalStatus,
                coverColorHex = coverColorHex,
                notes = notes.trim(),
                tags = tags.trim(),
                format = format,
                completionDate = if (finalStatus == "COMPLETED") System.currentTimeMillis() else null
            )
            repository.insertSavedBook(book)
            toastMessage.value = "تمت إضافة كتاب «$title» إلى مكتبتك بنجاح!"
        }
    }

    fun updateBook(book: SavedBook) {
        viewModelScope.launch(Dispatchers.IO) {
            val safePageCount = book.pageCount.coerceAtLeast(0)
            val safeCurrentPage = book.currentPage.coerceIn(0, if (safePageCount > 0) safePageCount else 99999)
            val isNowCompleted = (safePageCount > 0 && safeCurrentPage >= safePageCount) || book.readingStatus == "COMPLETED"

            val updated = book.copy(
                pageCount = safePageCount,
                currentPage = safeCurrentPage,
                readingStatus = if (isNowCompleted) "COMPLETED" else book.readingStatus,
                completionDate = if (isNowCompleted && book.completionDate == null) System.currentTimeMillis() else book.completionDate
            )
            repository.updateSavedBook(updated)
            toastMessage.value = "تم تحديث بيانات كتاب «${book.title}»"
        }
    }

    fun updateReadingProgress(book: SavedBook, newCurrentPage: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxPages = if (book.pageCount > 0) book.pageCount else 99999
            val safePage = newCurrentPage.coerceIn(0, maxPages)
            val isFinished = book.pageCount > 0 && safePage >= book.pageCount

            val updated = book.copy(
                currentPage = safePage,
                readingStatus = if (isFinished) "COMPLETED" else if (safePage > 0) "READING" else book.readingStatus,
                completionDate = if (isFinished && book.completionDate == null) System.currentTimeMillis() else book.completionDate
            )
            repository.updateSavedBook(updated)
            if (isFinished) {
                toastMessage.value = "تهانينا! لقد أتممت قراءة كتاب «${book.title}» 🎉"
            } else {
                toastMessage.value = "تم تحديث التقدم إلى الصفحة $safePage (${updated.progressPercent}%)"
            }
        }
    }

    fun addQuote(book: SavedBook, quoteText: String, page: Int?, thought: String) {
        if (quoteText.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val currentQuotes = book.quotesList.toMutableList()
            currentQuotes.add(
                BookQuote(
                    text = quoteText.trim(),
                    pageNumber = page,
                    thought = thought.trim(),
                    dateAdded = System.currentTimeMillis()
                )
            )
            val updated = book.copy(quotesJson = BookQuoteSerializer.toJson(currentQuotes))
            repository.updateSavedBook(updated)
            toastMessage.value = "تم حفظ الاقتباس في سجل كتاب «${book.title}»"
        }
    }

    fun deleteQuote(book: SavedBook, quoteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentQuotes = book.quotesList.filterNot { it.id == quoteId }
            val updated = book.copy(quotesJson = BookQuoteSerializer.toJson(currentQuotes))
            repository.updateSavedBook(updated)
            toastMessage.value = "تم حذف الاقتباس"
        }
    }

    fun deleteBook(book: SavedBook) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSavedBook(book)
            toastMessage.value = "تم حذف كتاب «${book.title}» من المكتبة"
        }
    }

    fun deleteBookById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSavedBookById(id)
            toastMessage.value = "تم حذف الكتاب بنجاح"
        }
    }

    fun toggleFavorite(book: SavedBook) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = book.copy(isFavorite = !book.isFavorite)
            repository.updateSavedBook(updated)
            toastMessage.value = if (updated.isFavorite) "أضيف «${book.title}» للمفضلة" else "أزيل «${book.title}» من المفضلة"
        }
    }

    fun updateReadingStatus(book: SavedBook, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val nowCompleted = newStatus == "COMPLETED"
            val updated = book.copy(
                readingStatus = newStatus,
                currentPage = if (nowCompleted && book.pageCount > 0) book.pageCount else book.currentPage,
                completionDate = if (nowCompleted) System.currentTimeMillis() else null
            )
            repository.updateSavedBook(updated)
            val statusLabel = when (newStatus) {
                "READING" -> "أقرأه الآن"
                "COMPLETED" -> "مكتمل القراءة"
                else -> "أريد قراءته"
            }
            toastMessage.value = "تم تعديل حالة «${book.title}» إلى: $statusLabel"
        }
    }

    fun toggleViewMode() {
        viewMode.value = if (viewMode.value == "LIST") "GRID" else "LIST"
    }

    fun setYearlyGoal(goal: Int) {
        yearlyReadingGoal.value = goal.coerceIn(1, 200)
    }

    fun search(query: String) {
        searchQuery.value = query
    }

    fun filterByGenre(genre: String) {
        selectedGenre.value = genre
    }

    fun filterByStatus(status: String) {
        selectedReadingStatus.value = status
    }

    fun filterByFormat(format: String) {
        selectedFormat.value = format
    }

    fun setSort(sort: String) {
        sortBy.value = sort
    }

    fun clearToast() {
        toastMessage.value = null
    }

    fun generateLibrarySummaryText(): String {
        val books = allBooks.value
        val sb = StringBuilder()
        sb.append("📚 ملخص مكتبتي الشخصية من تطبيق كاتب (Khetta)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        val currentStats = stats.value
        sb.append("• إجمالي الكتب: ${currentStats.totalCount} كتاب\n")
        sb.append("• الكتب المكتملة: ${currentStats.completedCount}\n")
        sb.append("• أقرأها حالياً: ${currentStats.readingCount}\n")
        sb.append("• إجمالي الصفحات المقروءة: ${currentStats.totalPagesRead} صفحة\n")
        sb.append("• إجمالي الاقتباسات المحفوظة: ${currentStats.totalQuotesCount} اقتباس\n\n")

        sb.append("قائمة الكتب:\n")
        books.forEachIndexed { index, b ->
            val statusEmoji = when (b.readingStatus) {
                "COMPLETED" -> "✅"
                "READING" -> "📖"
                else -> "⏳"
            }
            sb.append("${index + 1}. $statusEmoji «${b.title}» - ${b.author} [${b.genre}] (${b.progressPercent}%)\n")
        }
        return sb.toString()
    }
}
