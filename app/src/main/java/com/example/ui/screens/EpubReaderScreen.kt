package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ReaderSettingsState
import com.example.data.model.ReaderTheme
import com.example.ui.EpubReaderViewModel
import com.example.ui.components.ReaderSettingsBar
import com.example.ui.components.ReaderSettingsBottomSheet
import com.example.util.EpubBookContent
import com.example.util.EpubChapter

/**
 * شاشة قراءة كتب EPUB مخصصة بالكامل تدعم:
 * 1. استرجاع وحفظ آخر فصل تم الوصول إليه تلقائياً عبر DataStore Preferences.
 * 2. دعم حفظ تقدم القراءة وموضع التمرير.
 * 3. شريط إعدادات قراءة متكامل (حجم وسمك الخط، الثيمات، تباعد الأسطر).
 * 4. فهرس الفصول السريع (TOC) مع تمييز الفصل الحالي.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    bookId: String = "default_book_1",
    bookTitle: String = "كتاب إلكتروني",
    epubBook: EpubBookContent? = null,
    viewModel: EpubReaderViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    // 1. مراقبة رقم الفصل وموضع القراءة المحفوظ في Jetpack DataStore
    val savedChapterIndex by viewModel.getBookLastChapter(bookId).collectAsState()
    val savedReadingPosition by viewModel.getReadingPosition(bookId).collectAsState()

    // حالة الفصل الحالي في واجهة المستخدم (تأخذ القيمة المحفوظة في البداية)
    var currentChapterIndex by remember { mutableIntStateOf(0) }
    var isInitializedFromDataStore by remember { mutableStateOf(false) }

    // إعدادات القراءة (الثيم وحجم وسمك الخط وتباعد الأسطر)
    var readerSettings by remember { mutableStateOf(ReaderSettingsState()) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }

    val chapters = epubBook?.chapters ?: emptyList()
    val totalChapters = chapters.size

    // ضبط الفصل الحالي فور تحميل القيمة من DataStore لأول مرة
    LaunchedEffect(savedChapterIndex, totalChapters) {
        if (!isInitializedFromDataStore && totalChapters > 0) {
            val validIndex = savedChapterIndex.coerceIn(0, totalChapters - 1)
            currentChapterIndex = validIndex
            isInitializedFromDataStore = true
        }
    }

    // دالة لتغيير الفصل مع الحفظ التلقائي في DataStore
    val onSelectChapter: (Int) -> Unit = { newChapterIndex ->
        val safeIndex = if (totalChapters > 0) newChapterIndex.coerceIn(0, totalChapters - 1) else 0
        currentChapterIndex = safeIndex
        val progressPct = if (totalChapters > 0) (safeIndex + 1).toFloat() / totalChapters.toFloat() else 0f
        // حفظ رقم الفصل وموضع القراءة في Jetpack DataStore
        viewModel.saveBookChapter(bookId, safeIndex)
        viewModel.saveReadingPosition(
            bookId = bookId,
            chapterIndex = safeIndex,
            scrollOffset = 0,
            totalProgressPercentage = progressPct
        )
    }

    val currentChapter: EpubChapter? = if (chapters.isNotEmpty() && currentChapterIndex in chapters.indices) {
        chapters[currentChapterIndex]
    } else null

    val scrollState = rememberScrollState()

    // إعادة ضبط موضع التمرير لأعلى الصفحة عند تبديل الفصل
    LaunchedEffect(currentChapterIndex) {
        scrollState.scrollTo(0)
    }

    val currentTheme = readerSettings.theme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = bookTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = currentTheme.textColor
                        )
                        if (totalChapters > 0) {
                            Text(
                                text = "فصل ${currentChapterIndex + 1} من $totalChapters: ${currentChapter?.title ?: ""}",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = currentTheme.textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = currentTheme.textColor
                        )
                    }
                },
                actions = {
                    // زر فهرس فصول الكتاب
                    IconButton(onClick = { showTocSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "فهرس الفصول",
                            tint = currentTheme.textColor
                        )
                    }
                    // زر فتح صفيحة إعدادات الخط والثيم
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "إعدادات القراءة",
                            tint = currentTheme.textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.backgroundColor
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(currentTheme.backgroundColor)
            ) {
                // شريط التنقل بين الفصول السابق والتالي
                if (totalChapters > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onSelectChapter(currentChapterIndex - 1) },
                            enabled = currentChapterIndex > 0,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = currentTheme.textColor
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الفصل السابق", fontSize = 12.sp)
                        }

                        // مؤشر رقم الفصل المحفوظ
                        Surface(
                            shape = CircleShape,
                            color = currentTheme.surfaceColor,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "${currentChapterIndex + 1} / $totalChapters",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.textColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = { onSelectChapter(currentChapterIndex + 1) },
                            enabled = currentChapterIndex < totalChapters - 1,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = currentTheme.textColor
                            )
                        ) {
                            Text("الفصل التالي", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // شريط إعدادات القراءة السريع في الأسفل
                ReaderSettingsBar(
                    settingsState = readerSettings,
                    onSettingsChanged = { readerSettings = it },
                    onOpenFullSettings = { showSettingsSheet = true }
                )
            }
        },
        containerColor = currentTheme.backgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(currentTheme.backgroundColor)
        ) {
            if (chapters.isEmpty()) {
                // لا توجد فصول
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = currentTheme.textColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "جاري قراءة أو تجهيز فصول الكتاب...",
                        color = currentTheme.textColor,
                        fontSize = 15.sp
                    )
                }
            } else if (currentChapter != null) {
                // عرض محتوى الفصل الحالي
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // عنوان الفصل
                    if (currentChapter.title.isNotBlank()) {
                        Text(
                            text = currentChapter.title,
                            fontSize = (readerSettings.fontSizeSp + 6f).sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.textColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            textAlign = TextAlign.Start
                        )
                        HorizontalDivider(
                            color = currentTheme.textColor.copy(alpha = 0.15f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // نص الفصل مع التخصيص
                    Text(
                        text = currentChapter.textContent,
                        fontSize = readerSettings.fontSizeSp.sp,
                        fontWeight = readerSettings.fontWeight,
                        lineHeight = (readerSettings.fontSizeSp * readerSettings.lineHeightMultiplier).sp,
                        color = currentTheme.textColor,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    // ورقة فهرس الفصول
    if (showTocSheet && totalChapters > 0) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            containerColor = currentTheme.backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "فهرس فصول الكتاب 📖",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.textColor
                    )
                    Text(
                        text = "$totalChapters فصول",
                        fontSize = 12.sp,
                        color = currentTheme.textColor.copy(alpha = 0.6f)
                    )
                }

                HorizontalDivider(color = currentTheme.textColor.copy(alpha = 0.12f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        val isSelected = index == currentChapterIndex
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = chapter.title.ifBlank { "الفصل ${index + 1}" },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else currentTheme.textColor
                                )
                            },
                            leadingContent = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else currentTheme.surfaceColor,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else currentTheme.textColor
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = "موضعك الحالي",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSelected) currentTheme.surfaceColor.copy(alpha = 0.6f) else currentTheme.backgroundColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectChapter(index)
                                    showTocSheet = false
                                }
                        )
                    }
                }
            }
        }
    }

    // ورقة إعدادات القراءة الكاملة
    if (showSettingsSheet) {
        ReaderSettingsBottomSheet(
            settingsState = readerSettings,
            onSettingsChanged = { readerSettings = it },
            onDismissRequest = { showSettingsSheet = false }
        )
    }
}
