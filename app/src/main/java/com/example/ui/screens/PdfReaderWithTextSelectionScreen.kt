package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.PdfAnnotator
import com.example.util.PdfHighlightArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// 1. نموذج بيانات يمثل منطقة النص المحددة أو المظللة
data class HighlightArea(
    val pageIndex: Int,
    val bounds: Rect,
    val note: String = "",
    val color: Color = Color(0x66FFEB3B) // لون أصفر شفاف افتراضي للتظليل
)

// 2. الواجهة الرئيسية لقارئ ومحرر PDF مع أدوات التحديد، التظليل، التوقيع الرقمي وحفظ المستند المعدل
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderWithTextSelectionScreen(
    pdfUri: Uri,
    fileName: String = "مستند PDF",
    initialPageIndex: Int = 0,
    onBack: () -> Unit = {},
    onCopyText: (String) -> Unit = {},
    onSaveProgress: (pageIndex: Int, totalPages: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var selectedText by remember { mutableStateOf("") }
    val highlights = remember { mutableStateListOf<HighlightArea>() }
    var activeHighlightColor by remember { mutableStateOf(Color(0x66FFEB3B)) }
    var pfdRef by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    // مسارات التوقيع الرقمي حسب كل صفحة
    val signaturesByPage = remember { mutableStateMapOf<Int, ComposePath>() }

    // نمط التفاعل: تصفح عادي، تظليل نصوص، أو توقيع رقمي
    var readerInteractionMode by remember { mutableStateOf(PdfInteractionMode.HIGHLIGHT) }

    // حوار توقيع رقمي كامل (Signature Pad Dialog)
    var showSignatureDialogForPage by remember { mutableStateOf<Int?>(null) }
    var isSavingPdf by remember { mutableStateOf(false) }

    // حالة التحكم بـ Scale (التكبير/التصغير)
    var scale by remember { mutableFloatStateOf(1f) }
    val listState = rememberLazyListState()

    // تحميل ملف الـ PDF بأمان
    LaunchedEffect(pdfUri) {
        withContext(Dispatchers.IO) {
            try {
                var pfd = try {
                    context.contentResolver.openFileDescriptor(pdfUri, "r")
                } catch (e: Exception) {
                    null
                }

                // إذا لم يتم فتح الـ Descriptor مباشرة، ننسخ مؤقتاً للكاش
                if (pfd == null) {
                    val tempFile = File(context.cacheDir, "pdf_selection_view_${System.currentTimeMillis()}.pdf")
                    context.contentResolver.openInputStream(pdfUri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists()) {
                        pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                }

                pfd?.let { fd ->
                    pfdRef = fd
                    val renderer = PdfRenderer(fd)
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // الانتقال للصفحة المحددة مبدئياً
    LaunchedEffect(pageCount) {
        if (pageCount > 0 && initialPageIndex in 0 until pageCount) {
            listState.scrollToItem(initialPageIndex)
        }
    }

    // تنظيف الموارد عند مغادرة الشاشة
    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                pfdRef?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // حفظ التقدم عند تمرير الصفحات
    LaunchedEffect(listState.firstVisibleItemIndex, pageCount) {
        if (pageCount > 0) {
            onSaveProgress(listState.firstVisibleItemIndex, pageCount)
        }
    }

    // دالة تصدير وحفظ الـ PDF إلى Uri محدد عبر SAF
    val exportToUri: (Uri) -> Unit = { targetUri ->
        isSavingPdf = true
        coroutineScope.launch {
            // تحويل التظليلات إلى PdfHighlightArea
            val highlightsMap = mutableMapOf<Int, MutableList<PdfHighlightArea>>()
            highlights.forEach { h ->
                val list = highlightsMap.getOrPut(h.pageIndex) { mutableListOf() }
                list.add(
                    PdfHighlightArea(
                        rect = RectF(h.bounds.left, h.bounds.top, h.bounds.right, h.bounds.bottom),
                        color = h.color.toArgb()
                    )
                )
            }

            // تحويل مسارات التوقيع إلى android.graphics.Path
            val androidSignatures = mutableMapOf<Int, android.graphics.Path>()
            signaturesByPage.forEach { (page, compPath) ->
                androidSignatures[page] = compPath.asAndroidPath()
            }

            val success = withContext(Dispatchers.IO) {
                PdfAnnotator.annotateAndSaveFullPdfToUri(
                    context = context,
                    inputPdfUri = pdfUri,
                    targetUri = targetUri,
                    highlightsByPage = highlightsMap,
                    signatureByPage = androidSignatures
                )
            }

            isSavingPdf = false
            if (success) {
                Toast.makeText(context, "تم حفظ المستند المعدل في المسار المختار بنجاح 💾✨", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "تعذر تصدير المستند المعدل إلى المسار المختار", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // مشغل Storage Access Framework (SAF) لحفظ المستند في أي مجلد يختاره المستخدم (CreateDocument)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri: Uri? ->
        if (targetUri != null) {
            exportToUri(targetUri)
        }
    }

    // دالة تصدير وحفظ الـ PDF مع التظليلات والتوقيع
    val handleSaveAnnotatedPdf: () -> Unit = {
        val cleanName = fileName.replace(".pdf", "").replace(" ", "_")
        val suggestedFileName = "معدل_${cleanName}.pdf"
        try {
            createDocumentLauncher.launch(suggestedFileName)
        } catch (e: Exception) {
            // إذا تعذر فتح SAF نرجع للحفظ المحلي التلقائي
            isSavingPdf = true
            coroutineScope.launch {
                val exportDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "annotated_pdfs")
                if (!exportDir.exists()) exportDir.mkdirs()
                val outputFile = File(exportDir, "معدل_${cleanName}_${System.currentTimeMillis()}.pdf")

                val highlightsMap = mutableMapOf<Int, MutableList<PdfHighlightArea>>()
                highlights.forEach { h ->
                    val list = highlightsMap.getOrPut(h.pageIndex) { mutableListOf() }
                    list.add(
                        PdfHighlightArea(
                            rect = RectF(h.bounds.left, h.bounds.top, h.bounds.right, h.bounds.bottom),
                            color = h.color.toArgb()
                        )
                    )
                }

                val androidSignatures = mutableMapOf<Int, android.graphics.Path>()
                signaturesByPage.forEach { (page, compPath) ->
                    androidSignatures[page] = compPath.asAndroidPath()
                }

                val success = withContext(Dispatchers.IO) {
                    PdfAnnotator.annotateAndSaveFullPdf(
                        context = context,
                        inputPdfUri = pdfUri,
                        outputFile = outputFile,
                        highlightsByPage = highlightsMap,
                        signatureByPage = androidSignatures
                    )
                }

                isSavingPdf = false
                if (success) {
                    Toast.makeText(context, "تم حفظ المستند بالتعديلات والتوقيع بنجاح 💾: ${outputFile.name}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "تعذر حفظ التعديلات على المستند", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = fileName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (pageCount > 0) {
                            Text(
                                text = "صفحة ${listState.firstVisibleItemIndex + 1} من $pageCount • تكبير ${(scale * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("pdf_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    // تبديل نمط التفاعل (تظليل، توقيع، قراءة)
                    IconButton(
                        onClick = {
                            readerInteractionMode = when (readerInteractionMode) {
                                PdfInteractionMode.HIGHLIGHT -> PdfInteractionMode.SIGNATURE
                                PdfInteractionMode.SIGNATURE -> PdfInteractionMode.VIEW_ONLY
                                PdfInteractionMode.VIEW_ONLY -> PdfInteractionMode.HIGHLIGHT
                            }
                            val msg = when (readerInteractionMode) {
                                PdfInteractionMode.HIGHLIGHT -> "تم تفعيل نمط التظليل وتحديد النصوص 🖌️"
                                PdfInteractionMode.SIGNATURE -> "تم تفعيل نمط إضافة التوقيع الرقمي ✍️"
                                PdfInteractionMode.VIEW_ONLY -> "تم تفعيل نمط التصفح الحر 📖"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("toggle_interaction_mode_button")
                    ) {
                        val (icon, tint) = when (readerInteractionMode) {
                            PdfInteractionMode.HIGHLIGHT -> Icons.Default.FormatPaint to MaterialTheme.colorScheme.primary
                            PdfInteractionMode.SIGNATURE -> Icons.Default.Draw to MaterialTheme.colorScheme.secondary
                            PdfInteractionMode.VIEW_ONLY -> Icons.Default.PanTool to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(imageVector = icon, contentDescription = "نمط التفاعل", tint = tint)
                    }

                    // حفظ المستند المعدل مع التوقيع والتظليلات
                    IconButton(
                        onClick = handleSaveAnnotatedPdf,
                        enabled = !isSavingPdf,
                        modifier = Modifier.testTag("save_annotated_pdf_button")
                    ) {
                        if (isSavingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "حفظ المستند المعدل",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // زر التوقيع المخصص للصفحة الظاهرة الحالية
                    IconButton(
                        onClick = {
                            showSignatureDialogForPage = listState.firstVisibleItemIndex
                        },
                        modifier = Modifier.testTag("add_signature_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gesture,
                            contentDescription = "لوحة التوقيع الرقمي",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    // إعادة ضبط التكبير
                    if (scale != 1f) {
                        IconButton(onClick = { scale = 1f }) {
                            Icon(Icons.Default.ZoomOutMap, contentDescription = "إعادة ضبط التكبير")
                        }
                    }

                    // زر نسخ النص المحدد
                    if (selectedText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                copyToClipboard(context, selectedText)
                                onCopyText(selectedText)
                            },
                            modifier = Modifier.testTag("copy_selected_text_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ النص المحدد",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // شريط الأدوات بحسب النمط المختار
                when (readerInteractionMode) {
                    PdfInteractionMode.HIGHLIGHT -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "لون التظليل:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val palette = listOf(
                                        Color(0x66FFEB3B) to "أصفر",
                                        Color(0x664CAF50) to "أخضر",
                                        Color(0x662196F3) to "أزرق",
                                        Color(0x66E91E63) to "وردي",
                                        Color(0x66FF9800) to "برتقالي"
                                    )
                                    palette.forEach { (color, _) ->
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(color.copy(alpha = 0.9f))
                                                .border(
                                                    width = if (activeHighlightColor == color) 2.5.dp else 1.dp,
                                                    color = if (activeHighlightColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable { activeHighlightColor = color }
                                        )
                                    }
                                }
                                if (highlights.isNotEmpty()) {
                                    TextButton(
                                        onClick = { highlights.clear() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("مسح التظليلات", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    PdfInteractionMode.SIGNATURE -> {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المس الصفحة للرسم أو افتح لوحة التوقيع ✍️",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                if (signaturesByPage.isNotEmpty()) {
                                    TextButton(
                                        onClick = { signaturesByPage.clear() },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("مسح التواقيع", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    PdfInteractionMode.VIEW_ONLY -> {
                        // لا شريط أدوات سفلي في وضع التصفح الحر
                    }
                }

                // شريط الأدوات المساعد السفلي عند تحديد نص (Floating Reader Toolbar)
                if (selectedText.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "تم تحديد النص:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = selectedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        copyToClipboard(context, selectedText)
                                        onCopyText(selectedText)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("نسخ نقي")
                                }
                                IconButton(onClick = { selectedText = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFE2E8F0))
        ) {
            val renderer = pdfRenderer
            if (renderer != null && pageCount > 0) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(readerInteractionMode) {
                            if (readerInteractionMode == PdfInteractionMode.VIEW_ONLY) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 3.5f)
                                }
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(pageCount) { pageIndex ->
                        PdfPageViewItem(
                            renderer = renderer,
                            pageIndex = pageIndex,
                            scale = scale,
                            interactionMode = readerInteractionMode,
                            activeColor = activeHighlightColor,
                            highlights = highlights.filter { it.pageIndex == pageIndex },
                            pageSignature = signaturesByPage[pageIndex],
                            onSignatureUpdated = { newPath ->
                                signaturesByPage[pageIndex] = newPath
                            },
                            onTextSelected = { text, bounds ->
                                selectedText = text
                                highlights.add(HighlightArea(pageIndex, bounds, color = activeHighlightColor))
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator()
                    Text("جارٍ تصيير صفحات الـ PDF بدقة عالية...", fontSize = 13.sp, color = Color.DarkGray)
                }
            }
        }
    }

    // لوحة توقيع رقمي نافذة منبثقة (Dedicated Signature Pad Dialog)
    showSignatureDialogForPage?.let { targetPage ->
        DigitalSignatureDialog(
            pageIndex = targetPage,
            onDismiss = { showSignatureDialogForPage = null },
            onSignatureConfirmed = { signedPath ->
                signaturesByPage[targetPage] = signedPath
                showSignatureDialogForPage = null
                Toast.makeText(context, "تم حفظ التوقيع الرقمي على الصفحة ${targetPage + 1} ✍️", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

enum class PdfInteractionMode {
    HIGHLIGHT,
    SIGNATURE,
    VIEW_ONLY
}

// 3. مكون عرض صفحة PDF واحدة مع طبقة التحديد والتوقيع التفاعلي
@Composable
private fun PdfPageViewItem(
    renderer: PdfRenderer,
    pageIndex: Int,
    scale: Float,
    interactionMode: PdfInteractionMode,
    activeColor: Color,
    highlights: List<HighlightArea>,
    pageSignature: ComposePath?,
    onSignatureUpdated: (ComposePath) -> Unit,
    onTextSelected: (String, Rect) -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectionStart by remember { mutableStateOf<Offset?>(null) }
    var selectionEnd by remember { mutableStateOf<Offset?>(null) }

    // تصيير الصفحة إلى Bitmap في الخلفية
    LaunchedEffect(pageIndex, scale) {
        withContext(Dispatchers.IO) {
            try {
                val page = synchronized(renderer) {
                    renderer.openPage(pageIndex)
                }
                val width = (page.width * scale.coerceAtLeast(1f)).toInt().coerceAtLeast(1)
                val height = (page.height * scale.coerceAtLeast(1f)).toInt().coerceAtLeast(1)

                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                synchronized(renderer) {
                    page.close()
                }
                bitmap = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // شريط رقم الصفحة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "صفحة ${pageIndex + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (highlights.isNotEmpty()) {
                        Text(
                            text = "${highlights.size} تظليل",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (pageSignature != null) {
                        Text(
                            text = "✍️ موقعة",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                val currentBitmap = bitmap
                if (currentBitmap != null) {
                    // 1. عرض صورة صفحة الـ PDF
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "صفحة ${pageIndex + 1}",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )

                    // 2. طبقة الرسم والتحديد والتوقيع التفاعلي
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(interactionMode) {
                                when (interactionMode) {
                                    PdfInteractionMode.HIGHLIGHT -> {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val changes = event.changes.firstOrNull()
                                                if (changes != null) {
                                                    if (changes.pressed) {
                                                        if (selectionStart == null) {
                                                            selectionStart = changes.position
                                                        }
                                                        selectionEnd = changes.position
                                                    } else if (selectionStart != null && selectionEnd != null) {
                                                        val start = selectionStart!!
                                                        val end = selectionEnd!!
                                                        val dist = (end - start).getDistance()
                                                        if (dist > 10f) {
                                                            val rect = Rect(
                                                                topLeft = Offset(
                                                                    x = minOf(start.x, end.x),
                                                                    y = minOf(start.y, end.y)
                                                                ),
                                                                bottomRight = Offset(
                                                                    x = maxOf(start.x, end.x),
                                                                    y = maxOf(start.y, end.y)
                                                                )
                                                            )
                                                            onTextSelected("النص المظلل من الصفحة ${pageIndex + 1}", rect)
                                                        }
                                                        selectionStart = null
                                                        selectionEnd = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    PdfInteractionMode.SIGNATURE -> {
                                        var currentStroke = ComposePath()
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentStroke = ComposePath().apply {
                                                    pageSignature?.let { addPath(it) }
                                                    moveTo(offset.x, offset.y)
                                                }
                                                onSignatureUpdated(currentStroke)
                                            },
                                            onDrag = { change, _ ->
                                                currentStroke.lineTo(change.position.x, change.position.y)
                                                onSignatureUpdated(ComposePath().apply { addPath(currentStroke) })
                                            }
                                        )
                                    }
                                    PdfInteractionMode.VIEW_ONLY -> {
                                        // بدون استماع للمس للتركيز على التصفح
                                    }
                                }
                            }
                    ) {
                        // رسم التظليلات المحفوظة سابقاً
                        highlights.forEach { highlight ->
                            drawRect(
                                color = highlight.color,
                                topLeft = highlight.bounds.topLeft,
                                size = highlight.bounds.size
                            )
                        }

                        // رسم التوقيع الرقمي إن وجد
                        pageSignature?.let { signPath ->
                            drawPath(
                                path = signPath,
                                color = Color.Black,
                                style = Stroke(width = 4.5f)
                            )
                        }

                        // رسم مربع التحديد النشط أثناء سحب الأصبع
                        val start = selectionStart
                        val end = selectionEnd
                        if (start != null && end != null && interactionMode == PdfInteractionMode.HIGHLIGHT) {
                            val topLeft = Offset(
                                x = minOf(start.x, end.x),
                                y = minOf(start.y, end.y)
                            )
                            val size = Size(
                                width = kotlin.math.abs(end.x - start.x),
                                height = kotlin.math.abs(end.y - start.y)
                            )
                            drawRect(
                                color = Color(0x442196F3),
                                topLeft = topLeft,
                                size = size
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

// 4. حوار لوحة التوقيع الرقمي المخصص (Digital Signature Pad Dialog)
@Composable
private fun DigitalSignatureDialog(
    pageIndex: Int,
    onDismiss: () -> Unit,
    onSignatureConfirmed: (ComposePath) -> Unit
) {
    var signaturePath by remember { mutableStateOf(ComposePath()) }
    var hasDrawn by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التوقيع الرقمي (صفحة ${pageIndex + 1})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "ارسم توقيعك بإصبعك داخل المربع الأبيض بالأسفل:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // لوحة الرسم
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        hasDrawn = true
                                        val newP = ComposePath().apply {
                                            addPath(signaturePath)
                                            moveTo(offset.x, offset.y)
                                        }
                                        signaturePath = newP
                                    },
                                    onDrag = { change, _ ->
                                        val newP = ComposePath().apply {
                                            addPath(signaturePath)
                                            lineTo(change.position.x, change.position.y)
                                        }
                                        signaturePath = newP
                                    }
                                )
                            }
                    ) {
                        drawPath(
                            path = signaturePath,
                            color = Color.Black,
                            style = Stroke(width = 5f)
                        )
                    }

                    if (!hasDrawn) {
                        Text(
                            text = "✍️ مكان التوقيع هنا...",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // أزرار التحكم
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            signaturePath = ComposePath()
                            hasDrawn = false
                        },
                        enabled = hasDrawn
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسح التوقيع", color = MaterialTheme.colorScheme.error)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                if (hasDrawn) {
                                    onSignatureConfirmed(signaturePath)
                                }
                            },
                            enabled = hasDrawn
                        ) {
                            Text("تثبيت التوقيع")
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("PDF Selected Text", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "تم نسخ النص المحدد إلى الحافظة بنجاح 📋", Toast.LENGTH_SHORT).show()
}
