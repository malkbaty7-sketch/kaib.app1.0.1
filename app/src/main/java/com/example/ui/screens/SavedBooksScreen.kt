package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeData
import com.example.data.model.BookQuote
import com.example.data.model.SavedBook
import com.example.ui.SavedBooksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedBooksScreen(
    booksViewModel: SavedBooksViewModel,
    theme: AppThemeData,
    onBack: (() -> Unit)? = null,
    onStartProjectFromBook: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val books by booksViewModel.filteredBooks.collectAsState()
    val allBooksList by booksViewModel.allBooks.collectAsState()
    val stats by booksViewModel.stats.collectAsState()
    val searchQuery by booksViewModel.searchQuery.collectAsState()
    val selectedGenre by booksViewModel.selectedGenre.collectAsState()
    val selectedStatus by booksViewModel.selectedReadingStatus.collectAsState()
    val selectedFormat by booksViewModel.selectedFormat.collectAsState()
    val sortBy by booksViewModel.sortBy.collectAsState()
    val viewMode by booksViewModel.viewMode.collectAsState()
    val toastMessage by booksViewModel.toastMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var bookToEdit by remember { mutableStateOf<SavedBook?>(null) }
    var selectedBookForDetails by remember { mutableStateOf<SavedBook?>(null) }
    var bookToDelete by remember { mutableStateOf<SavedBook?>(null) }
    var showGoalEditDialog by remember { mutableStateOf(false) }

    // Random quote extraction for inspiration banner
    val randomQuoteWithBook = remember(allBooksList) {
        val quotes = allBooksList.flatMap { b -> b.quotesList.map { q -> Pair(b, q) } }
        quotes.shuffled().firstOrNull()
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            booksViewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    bookToEdit = null
                    showAddDialog = true
                },
                containerColor = theme.primaryColor,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.shadow(8.dp, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة كتاب جديد")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Stats Card & Reading Challenge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(theme.elevationDp, RoundedCornerShape(theme.cornerRadiusDp), spotColor = theme.shadowColor),
                shape = RoundedCornerShape(theme.cornerRadiusDp),
                colors = CardDefaults.cardColors(containerColor = theme.surfaceColor)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (onBack != null) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = theme.textColor
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(theme.primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "مكتبة الكتب وقوائم القراءة",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textColor
                                )
                                Text(
                                    text = "تتبع القراءة والاقتباسات والأهداف السنوية",
                                    fontSize = 11.sp,
                                    color = theme.textColor.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // View Mode Toggle (List / Shelf)
                            IconButton(
                                onClick = { booksViewModel.toggleViewMode() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (viewMode == "LIST") Icons.Default.GridView else Icons.Default.ViewAgenda,
                                    contentDescription = "تبديل المظهر",
                                    tint = theme.primaryColor
                                )
                            }

                            // Share / Export summary
                            IconButton(
                                onClick = {
                                    val summary = booksViewModel.generateLibrarySummaryText()
                                    clipboardManager.setText(AnnotatedString(summary))
                                    Toast.makeText(context, "تم نسخ تقرير المكتبة إلى الحافظة بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "تصدير التقرير", tint = theme.primaryColor)
                            }
                        }
                    }

                    // Reading Challenge Progress Bar (تحدي القراءة السنوي)
                    Surface(
                        color = theme.primaryColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                    Text("تحدي القراءة لعام ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "${stats.completedCount} من ${stats.yearlyGoal} كتب",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primaryColor
                                    )
                                    IconButton(
                                        onClick = { showGoalEditDialog = true },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل الهدف", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }

                            val challengePercent = if (stats.yearlyGoal > 0) {
                                ((stats.completedCount.toFloat() / stats.yearlyGoal) * 100).toInt().coerceIn(0, 100)
                            } else 0

                            LinearProgressIndicator(
                                progress = { challengePercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = theme.primaryColor,
                                trackColor = Color.Black.copy(alpha = 0.08f)
                            )
                        }
                    }

                    // Stat counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BookStatChip(title = "الإجمالي", count = stats.totalCount, color = theme.primaryColor)
                        BookStatChip(title = "أقرأه الآن", count = stats.readingCount, color = Color(0xFF0284C7))
                        BookStatChip(title = "مكتمل", count = stats.completedCount, color = Color(0xFF10B981))
                        BookStatChip(title = "الصفحات", count = stats.totalPagesRead, color = Color(0xFF8B5CF6))
                        BookStatChip(title = "اقتباسات", count = stats.totalQuotesCount, color = Color(0xFFF59E0B))
                    }
                }
            }

            // Inspiring Random Quote Box (If any exists)
            if (randomQuoteWithBook != null && searchQuery.isEmpty()) {
                val (quoteBook, quote) = randomQuoteWithBook
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0F766E).copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "«${quote.text}»",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B),
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "— ${quoteBook.title} (${quoteBook.author})",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString("«${quote.text}»\n— ${quoteBook.title}، ${quoteBook.author}"))
                                        Toast.makeText(context, "تم نسخ الاقتباس!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(20.dp)
                                ) {
                                    Text("نسخ", fontSize = 10.sp, color = Color(0xFF0F766E))
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar & Sort
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { booksViewModel.search(it) },
                    placeholder = { Text("بحث عن كتاب، مؤلف، وسم، أو موضوع...", fontSize = 11.sp, color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { booksViewModel.search("") }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = theme.primaryColor,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )

                // Sort Menu Box
                var sortMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "ترتيب", tint = theme.primaryColor)
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("أحدث الإضافات", fontSize = 12.sp, fontWeight = if (sortBy == "DATE_DESC") FontWeight.Bold else FontWeight.Normal) },
                            onClick = { booksViewModel.setSort("DATE_DESC"); sortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("نسبة التقدم %", fontSize = 12.sp, fontWeight = if (sortBy == "PROGRESS_DESC") FontWeight.Bold else FontWeight.Normal) },
                            onClick = { booksViewModel.setSort("PROGRESS_DESC"); sortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("عنوان الكتاب (أ - ي)", fontSize = 12.sp, fontWeight = if (sortBy == "TITLE_ASC") FontWeight.Bold else FontWeight.Normal) },
                            onClick = { booksViewModel.setSort("TITLE_ASC"); sortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("الأعلى تقييماً ★", fontSize = 12.sp, fontWeight = if (sortBy == "RATING_DESC") FontWeight.Bold else FontWeight.Normal) },
                            onClick = { booksViewModel.setSort("RATING_DESC"); sortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("الأكثر صفحات", fontSize = 12.sp, fontWeight = if (sortBy == "PAGES_DESC") FontWeight.Bold else FontWeight.Normal) },
                            onClick = { booksViewModel.setSort("PAGES_DESC"); sortMenuExpanded = false }
                        )
                    }
                }
            }

            // Status Filter Chips (Horizontally Scrollable)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val statuses = listOf(
                    "ALL" to "الكل",
                    "FAVORITE" to "المفضلة ♥",
                    "READING" to "أقرأه الآن",
                    "COMPLETED" to "مكتمل ✓",
                    "WANT_TO_READ" to "أريد قراءته"
                )
                items(statuses) { (key, label) ->
                    FilterChip(
                        selected = selectedStatus == key,
                        onClick = { booksViewModel.filterByStatus(key) },
                        label = { Text(label, fontSize = 11.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.primaryColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Genre & Format Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Formats
                val formats = listOf(
                    "ALL" to "كل الصيغ",
                    "BOOK" to "ورقي 📖",
                    "EBOOK" to "إلكتروني 📱",
                    "AUDIO" to "صوتي 🎧"
                )
                items(formats) { (fKey, fLabel) ->
                    AssistChip(
                        onClick = { booksViewModel.filterByFormat(fKey) },
                        label = { Text(fLabel, fontSize = 10.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedFormat == fKey) theme.primaryColor.copy(alpha = 0.2f) else Color.White,
                            labelColor = if (selectedFormat == fKey) theme.primaryColor else Color.DarkGray
                        )
                    )
                }

                val genres = listOf(
                    "ALL" to "جميع التصنيفات",
                    "أدب وروايات" to "أدب وروايات",
                    "علوم وتكنولوجيا" to "علوم وتكنولوجيا",
                    "فكر وفلسفة" to "فكر وفلسفة",
                    "تاريخ وحضارة" to "تاريخ وحضارة",
                    "شعر وبلاغة" to "شعر وبلاغة",
                    "عام" to "عام"
                )
                items(genres) { (key, label) ->
                    AssistChip(
                        onClick = { booksViewModel.filterByGenre(key) },
                        label = { Text(label, fontSize = 10.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedGenre == key) theme.primaryColor.copy(alpha = 0.15f) else Color.White,
                            labelColor = if (selectedGenre == key) theme.primaryColor else Color.DarkGray
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (selectedGenre == key) theme.primaryColor else Color.LightGray.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            HorizontalDivider(
                color = Color.Black.copy(alpha = 0.05f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Books Content (List or Shelf Grid)
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(theme.primaryColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedGenre != "ALL" || selectedStatus != "ALL" || selectedFormat != "ALL")
                                "لم يتم العثور على كتب مطابقة لمعايير البحث"
                            else
                                "مكتبتك فارغة حالياً",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "ابدأ بإضافة كتبك المفضلة وسجل تقدمك واقتباساتك لمتابعتها بدون إنترنت",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Button(
                            onClick = {
                                booksViewModel.search("")
                                booksViewModel.filterByGenre("ALL")
                                booksViewModel.filterByStatus("ALL")
                                booksViewModel.filterByFormat("ALL")
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة كتاب جديد", fontSize = 12.sp)
                        }
                    }
                }
            } else if (viewMode == "GRID") {
                // Shelf Grid View (مظهر الرفوف الشبكي)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = books, key = { it.id }) { book ->
                        BookShelfCard(
                            book = book,
                            theme = theme,
                            onToggleFavorite = { booksViewModel.toggleFavorite(book) },
                            onCardClick = { selectedBookForDetails = book },
                            onQuickAddPage = { booksViewModel.updateReadingProgress(book, book.currentPage + 10) }
                        )
                    }
                }
            } else {
                // Detailed List View (القائمة التفصيلية)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(
                        items = books,
                        key = { it.id }
                    ) { book ->
                        SavedBookItemCard(
                            book = book,
                            theme = theme,
                            onToggleFavorite = { booksViewModel.toggleFavorite(book) },
                            onEdit = {
                                bookToEdit = book
                                showAddDialog = true
                            },
                            onDelete = {
                                bookToDelete = book
                            },
                            onStatusChange = { newStatus ->
                                booksViewModel.updateReadingStatus(book, newStatus)
                            },
                            onCardClick = {
                                selectedBookForDetails = book
                            },
                            onQuickAddPage = { pagesToAdd ->
                                booksViewModel.updateReadingProgress(book, book.currentPage + pagesToAdd)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal: Add or Edit Book
    if (showAddDialog) {
        AddEditBookDialog(
            theme = theme,
            existingBook = bookToEdit,
            onDismiss = { showAddDialog = false },
            onSave = { title, author, desc, genre, pages, curPage, rating, status, colorHex, notes, tags, format ->
                if (bookToEdit == null) {
                    booksViewModel.addBook(
                        title = title,
                        author = author,
                        description = desc,
                        genre = genre,
                        pageCount = pages,
                        currentPage = curPage,
                        rating = rating,
                        readingStatus = status,
                        coverColorHex = colorHex,
                        notes = notes,
                        tags = tags,
                        format = format
                    )
                } else {
                    val updated = bookToEdit!!.copy(
                        title = title,
                        author = author,
                        description = desc,
                        genre = genre,
                        pageCount = pages,
                        currentPage = curPage,
                        rating = rating,
                        readingStatus = status,
                        coverColorHex = colorHex,
                        notes = notes,
                        tags = tags,
                        format = format
                    )
                    booksViewModel.updateBook(updated)
                }
                showAddDialog = false
            }
        )
    }

    // Modal: Book Details & Quotes Manager
    if (selectedBookForDetails != null) {
        BookDetailsDialog(
            book = selectedBookForDetails!!,
            theme = theme,
            onDismiss = { selectedBookForDetails = null },
            onEdit = {
                bookToEdit = selectedBookForDetails
                selectedBookForDetails = null
                showAddDialog = true
            },
            onDelete = {
                val toDel = selectedBookForDetails!!
                selectedBookForDetails = null
                bookToDelete = toDel
            },
            onToggleFavorite = {
                booksViewModel.toggleFavorite(selectedBookForDetails!!)
                selectedBookForDetails = selectedBookForDetails!!.copy(isFavorite = !selectedBookForDetails!!.isFavorite)
            },
            onUpdateProgress = { newPage ->
                booksViewModel.updateReadingProgress(selectedBookForDetails!!, newPage)
                selectedBookForDetails = selectedBookForDetails!!.copy(currentPage = newPage)
            },
            onAddQuote = { text, page, thought ->
                booksViewModel.addQuote(selectedBookForDetails!!, text, page, thought)
                val updatedList = selectedBookForDetails!!.quotesList.toMutableList()
                updatedList.add(BookQuote(text = text, pageNumber = page, thought = thought))
                selectedBookForDetails = selectedBookForDetails!!.copy(
                    quotesJson = com.example.data.model.BookQuoteSerializer.toJson(updatedList)
                )
            },
            onDeleteQuote = { qId ->
                booksViewModel.deleteQuote(selectedBookForDetails!!, qId)
                val updatedList = selectedBookForDetails!!.quotesList.filterNot { it.id == qId }
                selectedBookForDetails = selectedBookForDetails!!.copy(
                    quotesJson = com.example.data.model.BookQuoteSerializer.toJson(updatedList)
                )
            },
            onStartProject = { title, author ->
                onStartProjectFromBook?.invoke(title, author)
                selectedBookForDetails = null
            }
        )
    }

    // Modal: Edit Reading Goal
    if (showGoalEditDialog) {
        var goalInput by remember { mutableStateOf(stats.yearlyGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalEditDialog = false },
            title = { Text("تعديل هدف القراءة السنوي", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("كم كتاباً تطمح لقراءته هذا العام؟", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) goalInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = goalInput.toIntOrNull() ?: 12
                        booksViewModel.setYearlyGoal(num)
                        showGoalEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                ) {
                    Text("حفظ الهدف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGoalEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Modal: Delete Confirmation
    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("حذف الكتاب من المكتبة", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = { Text("هل أنت متأكد من رغبتك في حذف «${bookToDelete!!.title}»؟ سيتم حذف الملاحظات والاقتباسات المرتبطة به أيضاً.", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        bookToDelete?.let { booksViewModel.deleteBook(it) }
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("نعم، احذف", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { bookToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun BookStatChip(title: String, count: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontWeight = FontWeight.Black, fontSize = 13.sp, color = color)
            Text(title, fontSize = 9.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun SavedBookItemCard(
    book: SavedBook,
    theme: AppThemeData,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit,
    onCardClick: () -> Unit,
    onQuickAddPage: (Int) -> Unit
) {
    val coverColor = remember(book.coverColorHex) {
        try {
            Color(android.graphics.Color.parseColor(book.coverColorHex))
        } catch (e: Exception) {
            Color(0xFF1E293B)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Book Spine / Cover Mockup
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(76.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(coverColor, coverColor.copy(alpha = 0.8f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(3.dp)
                    ) {
                        val formatIcon = when (book.format) {
                            "AUDIO" -> Icons.Default.Headphones
                            "EBOOK" -> Icons.Default.TabletAndroid
                            else -> Icons.Default.Book
                        }
                        Icon(formatIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = book.title,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = book.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Favorite heart toggle
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(26.dp)
                        ) {
                            if (book.isFavorite) {
                                Icon(Icons.Default.Favorite, contentDescription = "المفضلة", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "إضافة للمفضلة", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Author & Format
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Text(
                            text = book.author,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Tags: Genre, Rating, Format badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = theme.primaryColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = book.genre,
                                fontSize = 9.sp,
                                color = theme.primaryColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        // Rating badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", book.rating),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }

                        // Quotes count if any
                        if (book.quotesList.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${book.quotesList.size} اقتباس",
                                    fontSize = 9.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        ReadingStatusBadge(status = book.readingStatus)
                    }
                }
            }

            // Reading Progress Bar (if pageCount > 0)
            if (book.pageCount > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "التقدم: ${book.currentPage} / ${book.pageCount} صفحة",
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "${book.progressPercent}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (book.progressPercent == 100) Color(0xFF16A34A) else theme.primaryColor
                        )
                    }
                    LinearProgressIndicator(
                        progress = { book.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (book.progressPercent == 100) Color(0xFF16A34A) else theme.primaryColor,
                        trackColor = Color.Black.copy(alpha = 0.06f)
                    )
                }
            }

            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))

            // Action Row: Quick add pages & Status switcher & Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick +5 or +10 pages button for active reading
                if (book.readingStatus != "COMPLETED" && book.pageCount > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = { onQuickAddPage(5) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("+5 ص", fontSize = 10.sp)
                        }
                        FilledTonalButton(
                            onClick = { onQuickAddPage(10) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("+10 ص", fontSize = 10.sp)
                        }
                    }
                } else {
                    // Status quick switcher
                    var statusMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { statusMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = when (book.readingStatus) {
                                    "READING" -> "أقرأه الآن"
                                    "COMPLETED" -> "مكتمل"
                                    else -> "أريد قراءته"
                                },
                                fontSize = 10.sp,
                                color = theme.primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(13.dp))
                        }

                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("أريد قراءته", fontSize = 11.sp) }, onClick = { onStatusChange("WANT_TO_READ"); statusMenuExpanded = false })
                            DropdownMenuItem(text = { Text("أقرأه الآن", fontSize = 11.sp) }, onClick = { onStatusChange("READING"); statusMenuExpanded = false })
                            DropdownMenuItem(text = { Text("مكتمل القراءة", fontSize = 11.sp) }, onClick = { onStatusChange("COMPLETED"); statusMenuExpanded = false })
                        }
                    }
                }

                // Edit and Delete buttons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

/**
 * Bookshelf Grid Card - visual 3D book spine representation on a virtual shelf
 */
@Composable
fun BookShelfCard(
    book: SavedBook,
    theme: AppThemeData,
    onToggleFavorite: () -> Unit,
    onCardClick: () -> Unit,
    onQuickAddPage: () -> Unit
) {
    val coverColor = remember(book.coverColorHex) {
        try {
            Color(android.graphics.Color.parseColor(book.coverColorHex))
        } catch (e: Exception) {
            Color(0xFF1E293B)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(12.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Book Cover 3D simulation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(coverColor, coverColor.copy(alpha = 0.82f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Favorite Icon overlay
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                ) {
                    if (book.isFavorite) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }

                // Format indicator
                Surface(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = when (book.format) {
                            "AUDIO" -> "🎧 صوتي"
                            "EBOOK" -> "📱 إلكتروني"
                            else -> "📖 ورقي"
                        },
                        fontSize = 8.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = book.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = book.author,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Info & Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReadingStatusBadge(status = book.readingStatus)
                    Text("${book.progressPercent}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                }

                LinearProgressIndicator(
                    progress = { book.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (book.progressPercent == 100) Color(0xFF16A34A) else theme.primaryColor,
                    trackColor = Color.Black.copy(alpha = 0.06f)
                )

                if (book.readingStatus == "READING") {
                    FilledTonalButton(
                        onClick = onQuickAddPage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+10 صفحات", fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingStatusBadge(status: String) {
    val (label, bg, fg) = when (status) {
        "READING" -> Triple("أقرأه الآن", Color(0xFFE0F2FE), Color(0xFF0369A1))
        "COMPLETED" -> Triple("مكتمل ✓", Color(0xFFDCFCE7), Color(0xFF15803D))
        else -> Triple("أريد قراءته", Color(0xFFFEF3C7), Color(0xFFB45309))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBookDialog(
    theme: AppThemeData,
    existingBook: SavedBook?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        author: String,
        description: String,
        genre: String,
        pages: Int,
        currentPage: Int,
        rating: Float,
        status: String,
        colorHex: String,
        notes: String,
        tags: String,
        format: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(existingBook?.title ?: "") }
    var author by remember { mutableStateOf(existingBook?.author ?: "") }
    var description by remember { mutableStateOf(existingBook?.description ?: "") }
    var genre by remember { mutableStateOf(existingBook?.genre ?: "أدب وروايات") }
    var pagesText by remember { mutableStateOf(if (existingBook != null && existingBook.pageCount > 0) existingBook.pageCount.toString() else "") }
    var currentPageText by remember { mutableStateOf(if (existingBook != null && existingBook.currentPage > 0) existingBook.currentPage.toString() else "0") }
    var rating by remember { mutableFloatStateOf(existingBook?.rating ?: 5.0f) }
    var readingStatus by remember { mutableStateOf(existingBook?.readingStatus ?: "WANT_TO_READ") }
    var format by remember { mutableStateOf(existingBook?.format ?: "BOOK") }
    var coverColorHex by remember { mutableStateOf(existingBook?.coverColorHex ?: "#0F766E") }
    var notes by remember { mutableStateOf(existingBook?.notes ?: "") }
    var tags by remember { mutableStateOf(existingBook?.tags ?: "") }

    val colorPalette = listOf(
        "#0F766E", // Deep Teal
        "#B45309", // Warm Amber
        "#1E3A8A", // Indigo Navy
        "#831843", // Burgundy Rose
        "#047857", // Emerald
        "#1E293B", // Dark Slate
        "#7C2D12", // Terracotta
        "#4338CA"  // Royal Violet
    )

    val genreList = listOf(
        "أدب وروايات",
        "علوم وتكنولوجيا",
        "فكر وفلسفة",
        "تاريخ وحضارة",
        "شعر وبلاغة",
        "عام"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (existingBook == null) "إضافة كتاب جديد إلى المكتبة" else "تعديل بيانات الكتاب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                }

                item {
                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان الكتاب *", fontSize = 11.sp) },
                        placeholder = { Text("مثال: مقدمة ابن خلدون", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    // Author
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("اسم المؤلف / الكاتب", fontSize = 11.sp) },
                        placeholder = { Text("مثال: عبد الرحمن بن خلدون", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    // Book Format (ورقي، إلكتروني، صوتي)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("نوع الكتاب وصيغته:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val formats = listOf("BOOK" to "ورقي 📖", "EBOOK" to "إلكتروني 📱", "AUDIO" to "صوتي 🎧")
                            formats.forEach { (fKey, fLabel) ->
                                FilterChip(
                                    selected = format == fKey,
                                    onClick = { format = fKey },
                                    label = { Text(fLabel, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = theme.primaryColor,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    // Genre selection chips
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("تصنيف الكتاب:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(genreList) { g ->
                                FilterChip(
                                    selected = genre == g,
                                    onClick = { genre = g },
                                    label = { Text(g, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = theme.primaryColor,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    // Row: Total Page count & Current Page
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pagesText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) pagesText = it },
                            label = { Text("إجمالي الصفحات", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = currentPageText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) currentPageText = it },
                            label = { Text("الصفحة الحالية", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    // Reading Status Dropdown
                    var statusExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { statusExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "حالة القراءة: " + when (readingStatus) {
                                    "READING" -> "أقرأه الآن"
                                    "COMPLETED" -> "مكتمل القراءة"
                                    else -> "أريد قراءته"
                                },
                                fontSize = 11.sp
                            )
                        }

                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("أريد قراءته", fontSize = 12.sp) }, onClick = { readingStatus = "WANT_TO_READ"; statusExpanded = false })
                            DropdownMenuItem(text = { Text("أقرأه الآن", fontSize = 12.sp) }, onClick = { readingStatus = "READING"; statusExpanded = false })
                            DropdownMenuItem(text = { Text("مكتمل القراءة", fontSize = 12.sp) }, onClick = { readingStatus = "COMPLETED"; statusExpanded = false })
                        }
                    }
                }

                item {
                    // Cover Color Picker
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("لون غلاف الكتاب:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            colorPalette.forEach { cHex ->
                                val parsed = try { Color(android.graphics.Color.parseColor(cHex)) } catch (e: Exception) { Color.Gray }
                                val isSelected = coverColorHex.equals(cHex, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(parsed)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.Black else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { coverColorHex = cHex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Tags & Keywords
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("الوسوم والكلمات الدلالية (مفصولة بفواصل)", fontSize = 10.sp) },
                        placeholder = { Text("مثال: تاريخ، فلسفة، أدب، سيرة", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("نبذة مختصرة عن الكتاب", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    // Dialog Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إلغاء")
                        }

                        Button(
                            onClick = {
                                val pages = pagesText.toIntOrNull() ?: 0
                                val curPage = currentPageText.toIntOrNull() ?: 0
                                onSave(title, author, description, genre, pages, curPage, rating, readingStatus, coverColorHex, notes, tags, format)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حفظ الكتاب", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rich Book Details Dialog with Reading Tracker, Quotes Manager, and Citation tools
 */
@Composable
fun BookDetailsDialog(
    book: SavedBook,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onUpdateProgress: (Int) -> Unit,
    onAddQuote: (String, Int?, String) -> Unit,
    onDeleteQuote: (String) -> Unit,
    onStartProject: (String, String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coverColor = remember(book.coverColorHex) {
        try { Color(android.graphics.Color.parseColor(book.coverColorHex)) } catch (e: Exception) { Color(0xFF1E293B) }
    }

    var selectedTab by remember { mutableStateOf("OVERVIEW") } // OVERVIEW, QUOTES, PROGRESS
    var showAddQuoteSheet by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp)
                .shadow(16.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفاصيل وسجل الكتاب", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                            if (book.isFavorite) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                }

                // Book Hero Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(coverColor, coverColor.copy(alpha = 0.8f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                        Text(book.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(book.author, fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            ReadingStatusBadge(status = book.readingStatus)
                            Text("• ${book.genre}", fontSize = 10.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                        }
                        if (book.pageCount > 0) {
                            Text("التقدم: ${book.currentPage} / ${book.pageCount} صفحة (${book.progressPercent}%)", fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                }

                // Tab Switcher (نظرة عامة / الاقتباسات والفوائد / التقدم)
                TabRow(
                    selectedTabIndex = when (selectedTab) {
                        "QUOTES" -> 1
                        "PROGRESS" -> 2
                        else -> 0
                    },
                    containerColor = Color.Transparent,
                    contentColor = theme.primaryColor
                ) {
                    Tab(
                        selected = selectedTab == "OVERVIEW",
                        onClick = { selectedTab = "OVERVIEW" },
                        text = { Text("نظرة عامة", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == "QUOTES",
                        onClick = { selectedTab = "QUOTES" },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("الاقتباسات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (book.quotesList.isNotEmpty()) {
                                    Surface(color = theme.primaryColor, shape = CircleShape) {
                                        Text("${book.quotesList.size}", fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == "PROGRESS",
                        onClick = { selectedTab = "PROGRESS" },
                        text = { Text("تتبع التقدم", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        "OVERVIEW" -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (book.description.isNotBlank()) {
                                    item {
                                        Text("نبذة عن الكتاب:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                        Text(book.description, fontSize = 12.sp, color = Color(0xFF334155), lineHeight = 18.sp)
                                    }
                                }

                                if (book.tags.isNotBlank()) {
                                    item {
                                        Text("الوسوم:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            book.tags.split(",", "،").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                                                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(4.dp)) {
                                                    Text("#$tag", fontSize = 10.sp, color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                if (book.notes.isNotBlank()) {
                                    item {
                                        Text("الملاحظات المستخلصة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                        Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Text(book.notes, fontSize = 11.sp, color = Color(0xFF475569), modifier = Modifier.padding(8.dp), lineHeight = 16.sp)
                                        }
                                    }
                                }

                                item {
                                    // Citation Copy Tools
                                    Surface(
                                        color = Color(0xFFF0FDF4),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("نسخ التوثيق المرجعي للكتاب:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                                Text("${book.author} (${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}). ${book.title}.", fontSize = 10.sp, color = Color.Gray)
                                            }
                                            TextButton(
                                                onClick = {
                                                    val citation = "المؤلف: ${book.author}، كتاب «${book.title}» (${book.genre})، ${book.pageCount} صفحة."
                                                    clipboardManager.setText(AnnotatedString(citation))
                                                    Toast.makeText(context, "تم نسخ التوثيق المرجعي!", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text("نسخ", fontSize = 11.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "QUOTES" -> {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showAddQuoteSheet = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة اقتباس جديد من الكتاب", fontSize = 11.sp)
                                }

                                if (book.quotesList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("لا توجد اقتباسات محفوظة لهذا الكتاب حتى الآن.\nاحفظ الجمل الملهمة والفوائد أثناء قراءتك!", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(book.quotesList) { quote ->
                                            Surface(
                                                color = Color(0xFFF8FAFC),
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("«${quote.text}»", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), lineHeight = 17.sp)
                                                    if (quote.thought.isNotBlank()) {
                                                        Text("فائدة/فكرة: ${quote.thought}", fontSize = 10.sp, color = Color(0xFF64748B), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(if (quote.pageNumber != null) "ص ${quote.pageNumber}" else "", fontSize = 9.sp, color = Color.Gray)
                                                        Row {
                                                            TextButton(
                                                                onClick = {
                                                                    clipboardManager.setText(AnnotatedString("«${quote.text}»\n— ${book.title}، ${book.author}"))
                                                                    Toast.makeText(context, "تم نسخ الاقتباس!", Toast.LENGTH_SHORT).show()
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                                modifier = Modifier.height(24.dp)
                                                            ) {
                                                                Text("نسخ", fontSize = 10.sp)
                                                            }
                                                            IconButton(
                                                                onClick = { onDeleteQuote(quote.id) },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "PROGRESS" -> {
                            var sliderPage by remember { mutableFloatStateOf(book.currentPage.toFloat()) }
                            val maxP = if (book.pageCount > 0) book.pageCount.toFloat() else 500f

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("تحديث موضع القراءة الحالي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                                Text("${sliderPage.toInt()} / ${book.pageCount} صفحة", fontSize = 18.sp, fontWeight = FontWeight.Black, color = theme.primaryColor)

                                Slider(
                                    value = sliderPage,
                                    onValueChange = { sliderPage = it },
                                    valueRange = 0f..maxP,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(thumbColor = theme.primaryColor, activeTrackColor = theme.primaryColor)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { sliderPage = (sliderPage - 10).coerceAtLeast(0f) }) { Text("-10") }
                                    OutlinedButton(onClick = { sliderPage = (sliderPage - 1).coerceAtLeast(0f) }) { Text("-1") }
                                    OutlinedButton(onClick = { sliderPage = (sliderPage + 1).coerceAtMost(maxP) }) { Text("+1") }
                                    OutlinedButton(onClick = { sliderPage = (sliderPage + 10).coerceAtMost(maxP) }) { Text("+10") }
                                    Button(onClick = { sliderPage = maxP }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))) { Text("أتممت الكتاب ✓") }
                                }

                                Button(
                                    onClick = { onUpdateProgress(sliderPage.toInt()) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("حفظ التقدم الجديد", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Add Quote Dialog overlay
                if (showAddQuoteSheet) {
                    var newQuoteText by remember { mutableStateOf("") }
                    var newQuotePage by remember { mutableStateOf("") }
                    var newQuoteThought by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showAddQuoteSheet = false },
                        title = { Text("إضافة اقتباس جديد", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = newQuoteText,
                                    onValueChange = { newQuoteText = it },
                                    label = { Text("نص الاقتباس *", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = newQuotePage,
                                    onValueChange = { if (it.all { ch -> ch.isDigit() }) newQuotePage = it },
                                    label = { Text("رقم الصفحة (اختياري)", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = newQuoteThought,
                                    onValueChange = { newQuoteThought = it },
                                    label = { Text("تعليقك / الفائدة المستخلصة", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth().height(60.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newQuoteText.isNotBlank()) {
                                        onAddQuote(newQuoteText, newQuotePage.toIntOrNull(), newQuoteThought)
                                        showAddQuoteSheet = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                            ) {
                                Text("إضافة")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showAddQuoteSheet = false }) {
                                Text("إلغاء")
                            }
                        }
                    )
                }

                HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))

                // Bottom Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onStartProject(book.title, book.author) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بدء مشروع كتابة مستوحى من هذا الكتاب", fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعديل", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
