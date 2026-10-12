package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeData
import com.example.data.model.DocumentProgressEntity
import com.example.data.model.ReaderSettingsState
import com.example.data.model.ReaderTheme
import com.example.ui.MainViewModel
import com.example.ui.components.ReaderSettingsBar
import com.example.ui.components.ReaderSettingsBottomSheet
import com.example.util.DocumentBookmarkItem
import com.example.util.EpubBookContent
import com.example.util.MultiFormatDocumentEngine
import com.example.util.PptxPresentationContent
import com.example.util.SpreadsheetContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiFormatDocumentViewerScreen(
    uriString: String,
    fileName: String,
    theme: AppThemeData,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uri = remember(uriString) { Uri.parse(uriString) }
    val extension = remember(fileName) { MultiFormatDocumentEngine.getFileExtension(fileName) }
    val category = remember(extension) { MultiFormatDocumentEngine.detectCategory(extension) }

    // Saved Progress state from Room
    val savedProgress by viewModel.getDocumentProgress(uriString).collectAsState(initial = null)
    var initialProgressLoaded by remember { mutableStateOf(false) }

    // Reading settings state (حالة إعدادات القراءة الشاملة)
    var readerSettings by remember {
        mutableStateOf(
            ReaderSettingsState(
                theme = ReaderTheme.LIGHT,
                fontSizeSp = 16f,
                lineHeightMultiplier = 1.6f
            )
        )
    }
    val readerTheme = readerSettings.theme
    val fontSizeSp = readerSettings.fontSizeSp
    val lineSpacing = readerSettings.lineHeightMultiplier
    val fontWeight = readerSettings.fontWeight

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showEpubChaptersSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showConverterStudioDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isHorizontalReadingMode by remember { mutableStateOf(false) }

    // Bookmarks list
    var bookmarks by remember { mutableStateOf<List<DocumentBookmarkItem>>(emptyList()) }

    // Format specific states
    // PDF
    var pdfTotalPages by remember { mutableStateOf(0) }
    var pdfCurrentPage by remember { mutableStateOf(0) }
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pdfScale by remember { mutableStateOf(1f) }
    var pdfOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isPdfContinuousSelectionView by remember { mutableStateOf(false) }

    // EPUB
    var epubBook by remember { mutableStateOf<EpubBookContent?>(null) }
    var epubCurrentChapterIndex by remember { mutableStateOf(0) }

    // PowerPoint (PPTX)
    var pptxPresentation by remember { mutableStateOf<PptxPresentationContent?>(null) }
    var pptxCurrentSlideIndex by remember { mutableStateOf(0) }

    // Text (DOCX / TXT / HTML)
    var textDocumentContent by remember { mutableStateOf("") }
    var docxParagraphs by remember { mutableStateOf<List<com.example.util.DocxParagraph>>(emptyList()) }
    val textScrollState = rememberScrollState()

    // Spreadsheet (XLSX / CSV)
    var spreadsheetData by remember { mutableStateOf<SpreadsheetContent?>(null) }

    var isLoading by remember { mutableStateOf(true) }

    // Determine current position index for bookmarking
    val currentPositionIndex = when (extension) {
        "pdf" -> pdfCurrentPage
        "epub" -> epubCurrentChapterIndex
        "pptx", "ppt" -> pptxCurrentSlideIndex
        else -> textScrollState.value
    }

    val isCurrentPositionBookmarked = remember(bookmarks, currentPositionIndex) {
        bookmarks.any { it.positionIndex == currentPositionIndex }
    }

    // Save progress helper
    fun saveCurrentProgress() {
        val totalPct = when {
            extension == "pdf" && pdfTotalPages > 0 -> ((pdfCurrentPage + 1).toFloat() / pdfTotalPages.toFloat()).coerceIn(0f, 1f)
            extension == "epub" && epubBook != null && epubBook!!.chapters.isNotEmpty() ->
                ((epubCurrentChapterIndex + 1).toFloat() / epubBook!!.chapters.size.toFloat()).coerceIn(0f, 1f)
            extension in listOf("pptx", "ppt") && pptxPresentation != null && pptxPresentation!!.slides.isNotEmpty() ->
                ((pptxCurrentSlideIndex + 1).toFloat() / pptxPresentation!!.slides.size.toFloat()).coerceIn(0f, 1f)
            textDocumentContent.isNotBlank() && textScrollState.maxValue > 0 ->
                (textScrollState.value.toFloat() / textScrollState.maxValue.toFloat()).coerceIn(0f, 1f)
            spreadsheetData != null -> 1f
            else -> 0f
        }

        val lastPageIdx = when (extension) {
            "epub" -> epubCurrentChapterIndex
            "pptx", "ppt" -> pptxCurrentSlideIndex
            else -> pdfCurrentPage
        }

        val entity = DocumentProgressEntity(
            fileUri = uriString,
            fileName = fileName,
            fileType = extension.uppercase(),
            lastPageIndex = lastPageIdx,
            scrollOffset = textScrollState.value,
            lastReadTimestamp = System.currentTimeMillis(),
            totalProgressPercentage = totalPct
        )
        viewModel.saveDocumentProgress(entity)

        if (extension == "epub") {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                com.example.data.preferences.ReaderPreferences(context).saveLastReadingPosition(
                    bookUri = uriString,
                    chapterIndex = epubCurrentChapterIndex,
                    scrollOffset = 0
                )
            }
        }
    }

    // Auto-save on back or disposal
    DisposableEffect(Unit) {
        onDispose {
            saveCurrentProgress()
        }
    }

    BackHandler {
        saveCurrentProgress()
        onBack()
    }

    // Load Document Content & Initial Progress & Bookmarks
    LaunchedEffect(uriString) {
        isLoading = true
        withContext(Dispatchers.IO) {
            bookmarks = MultiFormatDocumentEngine.getDocumentBookmarks(context, uriString)
            val initialSaved = viewModel.getDocumentProgressSync(uriString)
            when (extension) {
                "pdf" -> {
                    pdfTotalPages = MultiFormatDocumentEngine.getPdfPageCount(context, uri)
                    val startPage = initialSaved?.lastPageIndex?.coerceIn(0, (pdfTotalPages - 1).coerceAtLeast(0)) ?: 0
                    pdfCurrentPage = startPage
                    pdfBitmap = MultiFormatDocumentEngine.renderPdfPage(context, uri, startPage)
                }
                "epub" -> {
                    val book = MultiFormatDocumentEngine.parseEpub(context, uri)
                    epubBook = book
                    val dataStorePos = com.example.data.preferences.ReaderPreferences(context).getLastReadingPosition(uriString)
                    val startCh = if (dataStorePos.chapterIndex >= 0) {
                        dataStorePos.chapterIndex.coerceIn(0, (book.chapters.size - 1).coerceAtLeast(0))
                    } else {
                        initialSaved?.lastPageIndex?.coerceIn(0, (book.chapters.size - 1).coerceAtLeast(0)) ?: 0
                    }
                    epubCurrentChapterIndex = startCh
                }
                "pptx", "ppt" -> {
                    val pres = MultiFormatDocumentEngine.parsePptx(context, uri)
                    pptxPresentation = pres
                    val startSlide = initialSaved?.lastPageIndex?.coerceIn(0, (pres.slides.size - 1).coerceAtLeast(0)) ?: 0
                    pptxCurrentSlideIndex = startSlide
                }
                "xlsx", "xls", "csv", "tsv" -> {
                    spreadsheetData = MultiFormatDocumentEngine.parseSpreadsheet(context, uri, extension)
                }
                "docx" -> {
                    val paragraphs = com.example.util.DocxExtractor.extractParagraphs(context, uri)
                    docxParagraphs = paragraphs
                    textDocumentContent = if (paragraphs.isNotEmpty()) {
                        paragraphs.joinToString("\n\n") { it.text }
                    } else {
                        MultiFormatDocumentEngine.parseTextDocument(context, uri, extension)
                    }
                }
                else -> {
                    // TXT / HTML / RTF / MD
                    textDocumentContent = MultiFormatDocumentEngine.parseTextDocument(context, uri, extension)
                }
            }
        }
        isLoading = false

        savedProgress?.let { prog ->
            if (!initialProgressLoaded && prog.totalProgressPercentage > 0.05f) {
                val pct = (prog.totalProgressPercentage * 100).toInt()
                Toast.makeText(context, "تم استئناف القراءة تلقائياً: $pct%", Toast.LENGTH_SHORT).show()
                initialProgressLoaded = true
            }
        }
    }

    // Restore text scroll position after content loaded
    LaunchedEffect(isLoading, savedProgress) {
        if (!isLoading && savedProgress != null && savedProgress!!.scrollOffset > 0 && textScrollState.maxValue > 0) {
            textScrollState.scrollTo(savedProgress!!.scrollOffset.coerceAtMost(textScrollState.maxValue))
        }
    }

    // PDF Page Change renderer
    LaunchedEffect(pdfCurrentPage) {
        if (extension == "pdf" && pdfTotalPages > 0) {
            withContext(Dispatchers.IO) {
                pdfBitmap = MultiFormatDocumentEngine.renderPdfPage(context, uri, pdfCurrentPage)
            }
            pdfScale = 1f
            pdfOffset = androidx.compose.ui.geometry.Offset.Zero
            saveCurrentProgress()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = fileName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = readerTheme.text
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = theme.primaryColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = extension.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primaryColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            val progressDisplay = when (extension) {
                                "pdf" -> if (pdfTotalPages > 0) "صفحة ${pdfCurrentPage + 1} من $pdfTotalPages" else "PDF"
                                "epub" -> epubBook?.let { "فصل ${epubCurrentChapterIndex + 1} من ${it.chapters.size}" } ?: "EPUB"
                                "pptx", "ppt" -> pptxPresentation?.let { "شريحة ${pptxCurrentSlideIndex + 1} من ${it.slides.size}" } ?: "PowerPoint"
                                else -> {
                                    val pct = if (textScrollState.maxValue > 0) {
                                        ((textScrollState.value.toFloat() / textScrollState.maxValue.toFloat()) * 100).toInt()
                                    } else 0
                                    "تقدم: $pct%"
                                }
                            }
                            Text(progressDisplay, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        saveCurrentProgress()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = readerTheme.text)
                    }
                },
                actions = {
                    // 1. Converter studio launcher button
                    IconButton(onClick = { showConverterStudioDialog = true }) {
                        Icon(Icons.Default.Transform, contentDescription = "تحويل وأمان المستند", tint = theme.primaryColor)
                    }

                    // 2. Bookmark toggle button
                    IconButton(onClick = {
                        val currentIdx = currentPositionIndex
                        val label = when (extension) {
                            "pdf" -> "صفحة ${pdfCurrentPage + 1}"
                            "epub" -> epubBook?.chapters?.getOrNull(epubCurrentChapterIndex)?.title ?: "فصل ${epubCurrentChapterIndex + 1}"
                            "pptx", "ppt" -> "شريحة ${pptxCurrentSlideIndex + 1}"
                            else -> "موضع موثق"
                        }

                        val updated = if (isCurrentPositionBookmarked) {
                            bookmarks.filter { it.positionIndex != currentIdx }
                        } else {
                            bookmarks + DocumentBookmarkItem(
                                uriString = uriString,
                                label = label,
                                positionIndex = currentIdx
                            )
                        }
                        bookmarks = updated
                        MultiFormatDocumentEngine.saveDocumentBookmarks(context, uriString, updated)
                        Toast.makeText(
                            context,
                            if (isCurrentPositionBookmarked) "تمت إزالة العلامة المرجعية" else "تمت إضافة علامة مرجعية 🔖",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            imageVector = if (isCurrentPositionBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "علامة مرجعية",
                            tint = if (isCurrentPositionBookmarked) Color(0xFFF59E0B) else readerTheme.text
                        )
                    }

                    // Bookmarks list button
                    if (bookmarks.isNotEmpty()) {
                        IconButton(onClick = { showBookmarksSheet = true }) {
                            Icon(Icons.Default.Bookmarks, contentDescription = "العلامات المرجعية", tint = readerTheme.text)
                        }
                    }

                    // 3. Search in text / sheets
                    if (extension !in listOf("pdf")) {
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(Icons.Default.Search, contentDescription = "بحث في المستند", tint = readerTheme.text)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                isPdfContinuousSelectionView = !isPdfContinuousSelectionView
                            }
                        ) {
                            Icon(
                                imageVector = if (isPdfContinuousSelectionView) Icons.Default.ViewAgenda else Icons.Default.FormatPaint,
                                contentDescription = "تبديل وضع التحديد والتظليل",
                                tint = if (isPdfContinuousSelectionView) theme.primaryColor else readerTheme.text
                            )
                        }
                    }

                    // 4. EPUB Chapters Index button
                    if (extension == "epub" && epubBook != null) {
                        IconButton(onClick = { showEpubChaptersSheet = true }) {
                            Icon(Icons.Default.MenuBook, contentDescription = "فهرس الفصول", tint = readerTheme.text)
                        }
                    }

                    // 5. Format Settings (Font, theme, margins, eye-care)
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "إعدادات القراءة", tint = readerTheme.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = readerTheme.surface)
            )
        },
        bottomBar = {
            // PDF Bottom Page Controller Bar (only in single-page mode)
            if (extension == "pdf" && pdfTotalPages > 0 && !isPdfContinuousSelectionView) {
                Surface(
                    color = readerTheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (pdfCurrentPage > 0) pdfCurrentPage-- },
                            enabled = pdfCurrentPage > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الصفحة السابقة")
                        }

                        Slider(
                            value = pdfCurrentPage.toFloat(),
                            onValueChange = { pdfCurrentPage = it.toInt() },
                            valueRange = 0f..(pdfTotalPages - 1).coerceAtLeast(1).toFloat(),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(thumbColor = theme.primaryColor, activeTrackColor = theme.primaryColor)
                        )

                        IconButton(
                            onClick = { if (pdfCurrentPage < pdfTotalPages - 1) pdfCurrentPage++ },
                            enabled = pdfCurrentPage < pdfTotalPages - 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "الصفحة التالية")
                        }
                    }
                }
            } else if (extension == "epub" && epubBook != null && epubBook!!.chapters.isNotEmpty()) {
                // EPUB Bottom Chapter Navigation Bar
                Surface(
                    color = readerTheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (epubCurrentChapterIndex > 0) {
                                    epubCurrentChapterIndex--
                                    saveCurrentProgress()
                                }
                            },
                            enabled = epubCurrentChapterIndex > 0,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الفصل السابق", fontSize = 11.sp)
                        }

                        Text(
                            text = "${epubCurrentChapterIndex + 1} / ${epubBook!!.chapters.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = readerTheme.text
                        )

                        Button(
                            onClick = {
                                if (epubCurrentChapterIndex < epubBook!!.chapters.size - 1) {
                                    epubCurrentChapterIndex++
                                    saveCurrentProgress()
                                }
                            },
                            enabled = epubCurrentChapterIndex < epubBook!!.chapters.size - 1,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                        ) {
                            Text("الفصل التالي", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else if (extension in listOf("pptx", "ppt") && pptxPresentation != null && pptxPresentation!!.slides.isNotEmpty()) {
                // PowerPoint Slide Navigation Bar
                Surface(
                    color = readerTheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (pptxCurrentSlideIndex > 0) {
                                    pptxCurrentSlideIndex--
                                    saveCurrentProgress()
                                }
                            },
                            enabled = pptxCurrentSlideIndex > 0,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الشريحة السابقة", fontSize = 11.sp)
                        }

                        Text(
                            text = "شريحة ${pptxCurrentSlideIndex + 1} من ${pptxPresentation!!.slides.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = readerTheme.text
                        )

                        Button(
                            onClick = {
                                if (pptxCurrentSlideIndex < pptxPresentation!!.slides.size - 1) {
                                    pptxCurrentSlideIndex++
                                    saveCurrentProgress()
                                }
                            },
                            enabled = pptxCurrentSlideIndex < pptxPresentation!!.slides.size - 1,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Text("الشريحة التالية", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else if (extension !in listOf("pdf", "xlsx", "xls", "csv")) {
                ReaderSettingsBar(
                    settingsState = readerSettings,
                    onSettingsChanged = { readerSettings = it },
                    onOpenFullSettings = { showSettingsSheet = true }
                )
            }
        },
        containerColor = readerTheme.bg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = theme.primaryColor)
                        Text("جارٍ تحميل ومعالجة المستند...", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                when (extension) {
                    "pdf" -> {
                        if (isPdfContinuousSelectionView) {
                            com.example.ui.screens.PdfReaderWithTextSelectionScreen(
                                pdfUri = uri,
                                fileName = fileName,
                                initialPageIndex = pdfCurrentPage,
                                onBack = { isPdfContinuousSelectionView = false },
                                onCopyText = { selectedText ->
                                    Toast.makeText(context, "تم نسخ النص المحدد", Toast.LENGTH_SHORT).show()
                                },
                                onSaveProgress = { pageIndex, _ ->
                                    pdfCurrentPage = pageIndex
                                    saveCurrentProgress()
                                }
                            )
                        } else {
                            PdfPageView(
                                bitmap = pdfBitmap,
                                scale = pdfScale,
                                offset = pdfOffset,
                                onTransform = { s, o ->
                                    pdfScale = (pdfScale * s).coerceIn(1f, 4f)
                                    pdfOffset += o
                                }
                            )
                        }
                    }
                    "epub" -> {
                        val currentChapter = epubBook?.chapters?.getOrNull(epubCurrentChapterIndex)
                        if (currentChapter != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = currentChapter.title,
                                    fontSize = (fontSizeSp + 6).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = readerTheme.text,
                                    textAlign = TextAlign.Start
                                )
                                HorizontalDivider(color = readerTheme.text.copy(alpha = 0.1f))
                                Text(
                                    text = currentChapter.textContent,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp * lineSpacing).sp,
                                    fontWeight = fontWeight,
                                    color = readerTheme.text,
                                    textAlign = TextAlign.Start
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا توجد فصول متوفرة في الكتاب", color = Color.Gray)
                            }
                        }
                    }
                    "pptx", "ppt" -> {
                        // PowerPoint Slide Viewer
                        val pres = pptxPresentation
                        val slide = pres?.slides?.getOrNull(pptxCurrentSlideIndex)
                        if (slide != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Slide Card Frame
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = readerTheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = Color(0xFFD97706),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "شريحة ${slide.slideNumber}",
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = "${pptxCurrentSlideIndex + 1} / ${pres.slides.size}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Text(
                                            text = slide.title,
                                            fontSize = (fontSizeSp + 4).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = readerTheme.text
                                        )

                                        HorizontalDivider(color = readerTheme.text.copy(alpha = 0.1f))

                                        Text(
                                            text = slide.textContent,
                                            fontSize = fontSizeSp.sp,
                                            lineHeight = (fontSizeSp * lineSpacing).sp,
                                            fontWeight = fontWeight,
                                            color = readerTheme.text
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا توجد شرائح متوفرة في العرض التقديمي", color = Color.Gray)
                            }
                        }
                    }
                    "xlsx", "xls", "csv", "tsv" -> {
                        val data = spreadsheetData
                        if (data != null && data.headers.isNotEmpty()) {
                            SpreadsheetGridView(
                                data = data,
                                searchQuery = searchQuery,
                                theme = theme,
                                readerTheme = readerTheme
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("جدول البيانات فارغ أو تعذر تحليله.", color = Color.Gray)
                            }
                        }
                    }
                    else -> {
                        // DOCX / TXT / HTML / RTF Text View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(textScrollState)
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            if (searchQuery.isNotBlank()) {
                                val matchCount = countMatches(textDocumentContent, searchQuery)
                                Surface(
                                    color = theme.primaryColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "تم العثور على $matchCount نتيجة مطابقة لـ \"$searchQuery\"",
                                        fontSize = 11.sp,
                                        color = theme.primaryColor,
                                        modifier = Modifier.padding(8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (docxParagraphs.isNotEmpty()) {
                                docxParagraphs.forEach { para ->
                                    if (para.isHeading) {
                                        Text(
                                            text = para.text,
                                            fontSize = (fontSizeSp + 4).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.primaryColor,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        Text(
                                            text = para.text,
                                            fontSize = fontSizeSp.sp,
                                            lineHeight = (fontSizeSp * lineSpacing).sp,
                                            fontWeight = fontWeight,
                                            color = readerTheme.text,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = textDocumentContent,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp * lineSpacing).sp,
                                    fontWeight = fontWeight,
                                    color = readerTheme.text
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        ReaderSettingsBottomSheet(
            settingsState = readerSettings,
            onSettingsChanged = { readerSettings = it },
            onDismissRequest = { showSettingsSheet = false }
        )
    }

    // Bookmarks Drawer Bottom Sheet
    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false },
            containerColor = readerTheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("العلامات المرجعية المحفوظة 🔖", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = readerTheme.text)
                    Text("${bookmarks.size} إشارة", fontSize = 11.sp, color = Color.Gray)
                }
                HorizontalDivider(color = readerTheme.text.copy(alpha = 0.1f))

                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    items(bookmarks) { bm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    when (extension) {
                                        "pdf" -> pdfCurrentPage = bm.positionIndex
                                        "epub" -> epubCurrentChapterIndex = bm.positionIndex
                                        "pptx", "ppt" -> pptxCurrentSlideIndex = bm.positionIndex
                                        else -> {
                                            coroutineScope.launch {
                                                textScrollState.scrollTo(bm.positionIndex)
                                            }
                                        }
                                    }
                                    showBookmarksSheet = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                Text(bm.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = readerTheme.text)
                            }
                            IconButton(onClick = {
                                val updated = bookmarks.filter { it.id != bm.id }
                                bookmarks = updated
                                MultiFormatDocumentEngine.saveDocumentBookmarks(context, uriString, updated)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // EPUB Chapters Index Bottom Sheet
    if (showEpubChaptersSheet && epubBook != null) {
        ModalBottomSheet(
            onDismissRequest = { showEpubChaptersSheet = false },
            containerColor = readerTheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("فهرس فصول الكتاب 📚", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = readerTheme.text)
                HorizontalDivider(color = readerTheme.text.copy(alpha = 0.1f))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    itemsIndexed(epubBook!!.chapters) { idx, ch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    epubCurrentChapterIndex = idx
                                    showEpubChaptersSheet = false
                                    saveCurrentProgress()
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ch.title,
                                fontSize = 13.sp,
                                color = if (idx == epubCurrentChapterIndex) theme.primaryColor else readerTheme.text,
                                fontWeight = if (idx == epubCurrentChapterIndex) FontWeight.Bold else FontWeight.Normal
                            )
                            if (idx == epubCurrentChapterIndex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Converter & Security Studio Dialog (Direct from Viewer)
    if (showConverterStudioDialog) {
        DocumentToolsAndConverterDialog(
            theme = theme,
            initialDocUri = uri,
            initialDocName = fileName,
            onDismiss = { showConverterStudioDialog = false }
        )
    }
}

@Composable
private fun PdfPageView(
    bitmap: Bitmap?,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset,
    onTransform: (Float, androidx.compose.ui.geometry.Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onTransform(zoom, pan)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "صفحة PDF",
                modifier = Modifier
                    .fillMaxSize(0.96f)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun SpreadsheetGridView(
    data: SpreadsheetContent,
    searchQuery: String,
    theme: AppThemeData,
    readerTheme: ReaderTheme
) {
    val horizontalScrollState = rememberScrollState()
    val filteredRows = remember(searchQuery, data.rows) {
        if (searchQuery.isBlank()) {
            data.rows
        } else {
            val q = searchQuery.trim().lowercase()
            data.rows.filter { row -> row.any { it.lowercase().contains(q) } }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(readerTheme.surface)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(data.sheetName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = theme.primaryColor)
            Text("إجمالي: ${filteredRows.size} صف • ${data.headers.size} عمود", fontSize = 10.sp, color = Color.Gray)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                item {
                    Row(
                        modifier = Modifier
                            .background(theme.primaryColor.copy(alpha = 0.9f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "#",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .width(44.dp)
                                .padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                        data.headers.forEach { header ->
                            Text(
                                text = header,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .width(130.dp)
                                    .padding(8.dp)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.2f)),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                itemsIndexed(filteredRows) { index, row ->
                    val isEven = index % 2 == 0
                    Row(
                        modifier = Modifier
                            .background(if (isEven) readerTheme.bg else readerTheme.surface)
                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .width(44.dp)
                                .padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                        data.headers.indices.forEach { colIdx ->
                            val cellValue = row.getOrNull(colIdx) ?: ""
                            Text(
                                text = cellValue,
                                fontSize = 11.sp,
                                color = readerTheme.text,
                                modifier = Modifier
                                    .width(130.dp)
                                    .padding(8.dp)
                                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.2f)),
                                textAlign = TextAlign.Start,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun countMatches(text: String, query: String): Int {
    if (query.isBlank()) return 0
    val regex = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
    return regex.findAll(text).count()
}
