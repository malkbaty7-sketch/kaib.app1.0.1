package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.data.model.AppThemeData
import com.example.util.ArabicDocumentProcessor
import com.example.util.MultiFormatDocumentEngine
import com.example.util.OfflineDocumentConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentToolsAndConverterDialog(
    theme: AppThemeData,
    initialDocUri: Uri? = null,
    initialDocName: String? = null,
    onDismiss: () -> Unit,
    onOpenConvertedDocument: ((uriString: String, fileName: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("ARABIC_STUDIO") } // ARABIC_STUDIO, CONVERT, SECURITY, UTILS
    var selectedUri by remember { mutableStateOf<Uri?>(initialDocUri) }
    var selectedDocName by remember { mutableStateOf(initialDocName ?: "مستند") }

    var isProcessing by remember { mutableStateOf(false) }
    var processingStatusMessage by remember { mutableStateOf("") }
    var resultFile by remember { mutableStateOf<File?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    // Arabic Studio specific states (Tashkeel, Gemini OCR, Multiple Formats)
    var arabicTargetFormat by remember { mutableStateOf(ArabicDocumentProcessor.TargetFormat.DOCX) }
    var arabicReadingMode by remember { mutableStateOf(ArabicDocumentProcessor.ReadingMode.AUTO) }
    var arabicKeepLines by remember { mutableStateOf(true) }
    var arabicExtractedPreview by remember { mutableStateOf("") }
    var arabicProcessStatus by remember { mutableStateOf("") }

    // Multi-File selection for Merge
    var mergeUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Split parameters
    var splitStartPage by remember { mutableStateOf("1") }
    var splitEndPage by remember { mutableStateOf("5") }

    // Watermark parameter
    var watermarkText by remember { mutableStateOf("نسخة معتمدة ومحمية - كاتب") }

    // Compression level
    var compressionLevel by remember { mutableStateOf(OfflineDocumentConverter.CompressionLevel.BALANCED) }

    // SAF Picker for single document
    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val (name, _) = MultiFormatDocumentEngine.getFileNameAndSize(context, uri)
            selectedDocName = name
            resultFile = null
            resultMessage = null
        }
    }

    // SAF Picker for multiple PDFs (Merge)
    val multiPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            mergeUris = uris
        }
    }

    // SAF Picker for multiple Images to PDF
    val imagesToPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isProcessing = true
                processingStatusMessage = "جارٍ تجميع وتصيير ${uris.size} صور إلى مستند PDF..."
                val file = withContext(Dispatchers.IO) {
                    OfflineDocumentConverter.convertImagesToPdf(context, uris, "ألبوم_صور")
                }
                resultFile = file
                resultMessage = "تم إنشاء مستند PDF بنجاح من ${uris.size} صورة!"
                isProcessing = false
            }
        }
    }

    fun shareFile(file: File) {
        try {
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة الملف المحول"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر المشاركة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(22.dp)),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Transform, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "محرك وأدوات المستندات بدون إنترنت",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                            Text(
                                text = "تحويل الصيغ، الأمان، الدمج والتقسيم محلياً 100%",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Selected File Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = theme.primaryColor)
                            Column {
                                Text(
                                    text = if (selectedUri != null) selectedDocName else "لم يتم اختيار مستند بعد",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (selectedUri != null) "جاهز للتحويل والمعالجة" else "اختر ملفاً لبدء العمليات",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Button(
                            onClick = {
                                singlePickerLauncher.launch(arrayOf("*/*"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor.copy(alpha = 0.12f), contentColor = theme.primaryColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (selectedUri == null) "اختيار ملف" else "تغيير الملف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        Pair("ARABIC_STUDIO", "✨ استخراج وتشكيل عربي"),
                        Pair("CONVERT", "⚡ تحويل الصيغ"),
                        Pair("SECURITY", "🔒 أمان وتشفير PDF"),
                        Pair("UTILS", "✂️ دمج وتقسيم وضغط")
                    )
                    for ((id, title) in tabs) {
                        val isSelected = activeTab == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { activeTab = id }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) theme.primaryColor else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Processing Status Indicator
                if (isProcessing) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = theme.primaryColor.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = theme.primaryColor, strokeWidth = 2.dp)
                            Text(processingStatusMessage, fontSize = 12.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Result Box (Success / Output)
                if (resultMessage != null && resultFile != null && !isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                Text(resultMessage!!, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF065F46))
                            }
                            Text("اسم الملف: ${resultFile!!.name} (${resultFile!!.length() / 1024} KB)", fontSize = 11.sp, color = Color(0xFF047857))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onOpenConvertedDocument?.invoke(Uri.fromFile(resultFile!!).toString(), resultFile!!.name)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("عرض الملف الآن", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { shareFile(resultFile!!) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مشاركة وتصدير", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Tab Content Body
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeTab) {
                        "ARABIC_STUDIO" -> {
                            // 1. Header description
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = theme.primaryColor.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(20.dp))
                                            Text("محوّل ومستخرج المستندات العربي المتقدم", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.primaryColor)
                                        }
                                        Text("PDF · DOCX · TXT · صور (ممسوحة ضوئياً) مع الحفاظ التام على النص والتشكيل العربي (Harakat)", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }

                            // 2. File picker dropzone
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { singlePickerLauncher.launch(arrayOf("*/*")) }
                                        .border(2.dp, if (selectedUri != null) theme.primaryColor else Color(0xFFCBD5E1), RoundedCornerShape(14.dp)),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (selectedUri != null) Icons.Default.InsertDriveFile else Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            modifier = Modifier.size(34.dp),
                                            tint = if (selectedUri != null) theme.primaryColor else Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = if (selectedUri != null) selectedDocName else "اضغط هنا لاختيار مستند أو صورة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (selectedUri != null) theme.textColor else Color(0xFF475569)
                                        )
                                        Text(
                                            text = "المدعوم: PDF، DOCX، TXT، صور ممسوحة (JPG, PNG, WebP)",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            // 3. Conversion Target Selector
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("التحويل إلى صيغة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.textColor)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            ArabicDocumentProcessor.TargetFormat.values().forEach { target ->
                                                val isSel = arabicTargetFormat == target
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { arabicTargetFormat = target }
                                                        .background(if (isSel) theme.primaryColor else Color(0xFFF1F5F9))
                                                        .padding(vertical = 8.dp),
                                                    color = Color.Transparent
                                                ) {
                                                    Text(
                                                        text = target.displayName,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) Color.White else Color(0xFF334155),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Reading Mode Selector (طريقة القراءة)
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("طريقة القراءة والمعالجة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.textColor)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val modes = listOf(
                                                Pair(ArabicDocumentProcessor.ReadingMode.AUTO, "تلقائي (مباشر وOCR)"),
                                                Pair(ArabicDocumentProcessor.ReadingMode.AI, "ذكاء اصطناعي دائماً (OCR)")
                                            )
                                            modes.forEach { (m, label) ->
                                                val isSel = arabicReadingMode == m
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { arabicReadingMode = m }
                                                        .background(if (isSel) theme.primaryColor else Color(0xFFF1F5F9))
                                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                                    color = Color.Transparent
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) Color.White else Color(0xFF334155),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. Checkbox (الحفاظ على الأسطر كما في الأصل)
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { arabicKeepLines = !arabicKeepLines }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = arabicKeepLines,
                                            onCheckedChange = { arabicKeepLines = it }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("الحفاظ على الأسطر كما في الأصل", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.textColor)
                                            Text("للحفاظ على فواصل الأسطر والترتيب الدقيق للشعر والقوائم والعناوين", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }

                            // 6. Action Button
                            item {
                                Button(
                                    onClick = {
                                        if (selectedUri == null) {
                                            Toast.makeText(context, "الرجاء اختيار ملف أولاً", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        coroutineScope.launch {
                                            isProcessing = true
                                            arabicProcessStatus = "جاري قراءة الملف..."
                                            processingStatusMessage = "جاري فحص وقراءة الملف..."
                                            val res = ArabicDocumentProcessor.processAndConvert(
                                                context = context,
                                                fileUri = selectedUri!!,
                                                fileName = selectedDocName,
                                                targetFormat = arabicTargetFormat,
                                                readingMode = arabicReadingMode,
                                                keepLines = arabicKeepLines,
                                                onProgress = { st ->
                                                    arabicProcessStatus = st
                                                    processingStatusMessage = st
                                                }
                                            )
                                            if (res.isSuccess) {
                                                val proc = res.getOrThrow()
                                                arabicExtractedPreview = proc.fullText
                                                resultFile = proc.outputFile
                                                resultMessage = "تم التحويل وحفظ النص والتشكيل بنجاح!" + if (proc.wasAiUsed) " (عبر الذكاء الاصطناعي OCR)" else ""
                                                arabicProcessStatus = "تم التحويل بنجاح ✔"
                                            } else {
                                                val err = res.exceptionOrNull()?.localizedMessage ?: "حدث خطأ"
                                                arabicProcessStatus = "خطأ: $err"
                                                Toast.makeText(context, "خطأ: $err", Toast.LENGTH_LONG).show()
                                            }
                                            isProcessing = false
                                        }
                                    },
                                    enabled = selectedUri != null && !isProcessing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جاري العمل...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تحويل واستخراج النص والتشكيل", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // 7. Status Message
                            if (arabicProcessStatus.isNotBlank()) {
                                item {
                                    Text(
                                        text = arabicProcessStatus,
                                        fontSize = 12.sp,
                                        color = if (arabicProcessStatus.startsWith("خطأ")) Color.Red else Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // 8. Extracted Textarea Preview & Action Buttons
                            if (arabicExtractedPreview.isNotBlank()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(Icons.Default.FormatQuote, contentDescription = null, tint = theme.primaryColor)
                                                    Text("معاينة النص والتشكيل المستخرج:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                                                }
                                                Text("${arabicExtractedPreview.length} حرف", fontSize = 10.sp, color = Color.Gray)
                                            }

                                            OutlinedTextField(
                                                value = arabicExtractedPreview,
                                                onValueChange = { arabicExtractedPreview = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp),
                                                textStyle = androidx.compose.ui.text.TextStyle(
                                                    fontSize = 14.sp,
                                                    lineHeight = 22.sp
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            )

                                            // Action Buttons: Copy, Share, Open Converted Document
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(arabicExtractedPreview))
                                                        Toast.makeText(context, "تم نسخ النص مع التشكيل إلى الحافظة ✔", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("نسخ النص", fontSize = 11.sp)
                                                }

                                                if (resultFile != null) {
                                                    Button(
                                                        onClick = { shareFile(resultFile!!) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                        contentPadding = PaddingValues(vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("مشاركة وتصدير", fontSize = 11.sp)
                                                    }
                                                }

                                                if (onOpenConvertedDocument != null && resultFile != null) {
                                                    Button(
                                                        onClick = {
                                                            onOpenConvertedDocument(Uri.fromFile(resultFile!!).toString(), resultFile!!.name)
                                                            onDismiss()
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                                        contentPadding = PaddingValues(vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("عرض الملف", fontSize = 11.sp)
                                                    }
                                                }
                                            }

                                            if (arabicTargetFormat == ArabicDocumentProcessor.TargetFormat.PDF) {
                                                Text(
                                                    text = "💡 عند ظهور نافذة المعاينة اختر «حفظ كـ PDF». كما يمكنك فتح ملف HTML وتصديره بخط Amiri.",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B),
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "CONVERT" -> {
                            item {
                                Text("تحويلات المستند المختار:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                            }

                            // 1. PDF to DOCX
                            item {
                                ConversionActionCard(
                                    title = "تحويل PDF إلى مستند Word (DOCX)",
                                    description = "استخراج النصوص وهيكلة ملف Word مفتوح التنسيق بدون إنترنت.",
                                    icon = Icons.Default.Article,
                                    iconTint = Color(0xFF2563EB),
                                    enabled = selectedUri != null,
                                    onClick = {
                                        coroutineScope.launch {
                                            isProcessing = true
                                            processingStatusMessage = "جارٍ استخراج وتوليد ملف Word (DOCX)..."
                                            val file = withContext(Dispatchers.IO) {
                                                OfflineDocumentConverter.convertPdfToDocx(context, selectedUri!!, selectedDocName)
                                            }
                                            resultFile = file
                                            resultMessage = "تم تحويل PDF إلى مستند Word بنجاح!"
                                            isProcessing = false
                                        }
                                    }
                                )
                            }

                            // 2. PDF to High-Res Images
                            item {
                                ConversionActionCard(
                                    title = "تحويل PDF إلى صور عالية الدقة (PNG)",
                                    description = "تصيير وتصدير كل صفحة كصورة منفصلة بجودة فائقة.",
                                    icon = Icons.Default.Image,
                                    iconTint = Color(0xFF8B5CF6),
                                    enabled = selectedUri != null,
                                    onClick = {
                                        coroutineScope.launch {
                                            isProcessing = true
                                            processingStatusMessage = "جارٍ تصيير وحفظ صفحات PDF كصور PNG..."
                                            val list = withContext(Dispatchers.IO) {
                                                OfflineDocumentConverter.convertPdfToImages(context, selectedUri!!, selectedDocName)
                                            }
                                            if (list.isNotEmpty()) {
                                                resultFile = list.first()
                                                resultMessage = "تم تصيير وتصدير ${list.size} صورة في مجلد التحويلات!"
                                            } else {
                                                Toast.makeText(context, "فشل تصيير الصور أو المستند فارغ", Toast.LENGTH_SHORT).show()
                                            }
                                            isProcessing = false
                                        }
                                    }
                                )
                            }

                            // 3. PDF to TXT
                            item {
                                ConversionActionCard(
                                    title = "استخراج النصوص الصافية (PDF إلى TXT)",
                                    description = "استخلاص النص العربي بدقة UTF-8 للتعديل والنسخ السريع.",
                                    icon = Icons.Default.TextFields,
                                    iconTint = Color(0xFF10B981),
                                    enabled = selectedUri != null,
                                    onClick = {
                                        coroutineScope.launch {
                                            isProcessing = true
                                            processingStatusMessage = "جارٍ استخراج النصوص من PDF..."
                                            val file = withContext(Dispatchers.IO) {
                                                OfflineDocumentConverter.convertPdfToTxt(context, selectedUri!!, selectedDocName)
                                            }
                                            resultFile = file
                                            resultMessage = "تم استخراج النص الصافي وحفظه في ملف TXT!"
                                            isProcessing = false
                                        }
                                    }
                                )
                            }

                            // 4. EPUB to PDF / DOCX
                            item {
                                ConversionActionCard(
                                    title = "تحويل كتاب EPUB إلى PDF أو DOCX",
                                    description = "فك الكتاب الرقمي وتجميعه في ملف PDF منسق أو Word.",
                                    icon = Icons.Default.MenuBook,
                                    iconTint = Color(0xFFF59E0B),
                                    enabled = selectedUri != null && selectedDocName.endsWith(".epub", ignoreCase = true),
                                    onClick = {
                                        coroutineScope.launch {
                                            isProcessing = true
                                            processingStatusMessage = "جارٍ تفكيك فصول EPUB وبناء كتاب PDF منسق..."
                                            val file = withContext(Dispatchers.IO) {
                                                OfflineDocumentConverter.convertEpubToPdf(context, selectedUri!!, selectedDocName)
                                            }
                                            resultFile = file
                                            resultMessage = "تم تحويل كتاب EPUB إلى PDF بنجاح!"
                                            isProcessing = false
                                        }
                                    }
                                )
                            }

                            // 5. Images to PDF
                            item {
                                ConversionActionCard(
                                    title = "تحويل صور متعددة إلى مستند PDF واحد",
                                    description = "اختر صورة واحدة أو مجموعة صور من المعرض لتجميعها في PDF.",
                                    icon = Icons.Default.PhotoLibrary,
                                    iconTint = Color(0xFFEC4899),
                                    enabled = true,
                                    onClick = {
                                        imagesToPdfPickerLauncher.launch(arrayOf("image/*"))
                                    }
                                )
                            }

                            // 6. Extract Tables to CSV
                            item {
                                ConversionActionCard(
                                    title = "استخراج الجداول والبيانات إلى Excel (CSV)",
                                    description = "استخراج الجداول من المستندات وقواعد البيانات إلى ملف CSV.",
                                    icon = Icons.Default.TableChart,
                                    iconTint = Color(0xFF059669),
                                    enabled = selectedUri != null,
                                    onClick = {
                                        coroutineScope.launch {
                                            isProcessing = true
                                            processingStatusMessage = "جارٍ فحص وهيكلة الجداول..."
                                            val file = withContext(Dispatchers.IO) {
                                                OfflineDocumentConverter.extractTablesToCsv(context, selectedUri!!, selectedDocName)
                                            }
                                            resultFile = file
                                            resultMessage = "تم استخراج الجدول بنجاح بصيغة CSV المتوافقة مع Excel!"
                                            isProcessing = false
                                        }
                                    }
                                )
                            }
                        }

                        "SECURITY" -> {
                            item {
                                Text("أمان وتشفير المستندات:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                            }

                            // 1. Check and Unlock PDF
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF0284C7))
                                            Text("فحص وإلغاء حماية مستند PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text("فحص حالة تشفير الملف، إزالة القيود، واستخراج نسخة غير مشفرة.", fontSize = 11.sp, color = Color.Gray)

                                        Button(
                                            onClick = {
                                                if (selectedUri != null) {
                                                    coroutineScope.launch {
                                                        isProcessing = true
                                                        processingStatusMessage = "جارٍ فحص الحماية وفك التشفير..."
                                                        val report = withContext(Dispatchers.IO) {
                                                            OfflineDocumentConverter.checkPdfSecurity(context, selectedUri!!)
                                                        }
                                                        if (report.canOpen) {
                                                            val unlocked = withContext(Dispatchers.IO) {
                                                                OfflineDocumentConverter.unlockAndSavePdf(context, selectedUri!!, selectedDocName)
                                                            }
                                                            resultFile = unlocked
                                                            resultMessage = "المستند غير مقيد وتم حفظ نسخة غير مشفرة بنجاح!"
                                                        } else {
                                                            Toast.makeText(context, report.statusMessage, Toast.LENGTH_LONG).show()
                                                        }
                                                        isProcessing = false
                                                    }
                                                } else {
                                                    Toast.makeText(context, "الرجاء اختيار ملف PDF أولاً", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                        ) {
                                            Text("فحص وإلغاء القيود", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // 2. Add Security Watermark Lock
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFFDC2626))
                                            Text("تأمين المستند بعلامة مائية رقمية (Watermark)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text("إضافة نص حماية رسمي ومائل عبر كافة صفحات المستند لمنع الانتحال.", fontSize = 11.sp, color = Color.Gray)

                                        OutlinedTextField(
                                            value = watermarkText,
                                            onValueChange = { watermarkText = it },
                                            label = { Text("نص العلامة المائية", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true
                                        )

                                        Button(
                                            onClick = {
                                                if (selectedUri != null) {
                                                    coroutineScope.launch {
                                                        isProcessing = true
                                                        processingStatusMessage = "جارٍ دمج العلامة المائية الأمنية..."
                                                        val file = withContext(Dispatchers.IO) {
                                                            OfflineDocumentConverter.addSecurityWatermarkToPdf(context, selectedUri!!, watermarkText, selectedDocName)
                                                        }
                                                        resultFile = file
                                                        resultMessage = "تم تأمين وحماية المستند بعلامة مائية معتمدة!"
                                                        isProcessing = false
                                                    }
                                                } else {
                                                    Toast.makeText(context, "الرجاء اختيار ملف PDF أولاً", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                        ) {
                                            Text("تطبيق وحفظ المستند المحمي", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        "UTILS" -> {
                            item {
                                Text("أدوات المعالجة المتقدمة:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                            }

                            // 1. Merge PDFs
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.CallMerge, contentDescription = null, tint = theme.primaryColor)
                                            Text("دمج ملفات PDF متعددة (Merge PDFs)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text("اختر ملفين أو أكثر لدمج صفحاتها في ملف PDF واحد متصل.", fontSize = 11.sp, color = Color.Gray)

                                        if (mergeUris.isNotEmpty()) {
                                            Text("تم اختيار: ${mergeUris.size} ملفات للدمج", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { multiPdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("اختيار ملفات PDF للدمج", fontSize = 11.sp)
                                            }

                                            if (mergeUris.size >= 2) {
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            isProcessing = true
                                                            processingStatusMessage = "جارٍ دمج ${mergeUris.size} مستندات في ملف واحد..."
                                                            val file = withContext(Dispatchers.IO) {
                                                                OfflineDocumentConverter.mergePdfFiles(context, mergeUris, "مستند_مدمج")
                                                            }
                                                            resultFile = file
                                                            resultMessage = "تم دمج كافة الملفات في مستند PDF واحد بنجاح!"
                                                            isProcessing = false
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                                                ) {
                                                    Text("دمج الآن", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Split PDF
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.CallSplit, contentDescription = null, tint = Color(0xFFD97706))
                                            Text("تقسيم واستخراج صفحات PDF (Split PDF)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text("حدد نطاق الصفحات المراد استخراجها في مستند مستقل.", fontSize = 11.sp, color = Color.Gray)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = splitStartPage,
                                                onValueChange = { splitStartPage = it },
                                                label = { Text("من صفحة", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = splitEndPage,
                                                onValueChange = { splitEndPage = it },
                                                label = { Text("إلى صفحة", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                singleLine = true
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (selectedUri != null) {
                                                    coroutineScope.launch {
                                                        isProcessing = true
                                                        val start = splitStartPage.toIntOrNull() ?: 1
                                                        val end = splitEndPage.toIntOrNull() ?: 5
                                                        processingStatusMessage = "جارٍ استخراج الصفحات من $start إلى $end..."
                                                        val file = withContext(Dispatchers.IO) {
                                                            OfflineDocumentConverter.splitPdfFile(context, selectedUri!!, start, end, selectedDocName)
                                                        }
                                                        resultFile = file
                                                        resultMessage = "تم اقتطاع وحفظ الصفحات في مستند PDF جديد!"
                                                        isProcessing = false
                                                    }
                                                } else {
                                                    Toast.makeText(context, "الرجاء اختيار ملف PDF أولاً", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                        ) {
                                            Text("اقتطاع الصفحات الآن", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // 3. Compress PDF
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Compress, contentDescription = null, tint = Color(0xFF4F46E5))
                                            Text("ضغط مستند PDF لتقليل الحجم (Compress PDF)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text("إعادة تشكيل الصفحات ومعدل الضغط لتصغير حجم الملف للمشاركة السريعة.", fontSize = 11.sp, color = Color.Gray)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (lvl in OfflineDocumentConverter.CompressionLevel.values()) {
                                                val isSelected = compressionLevel == lvl
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { compressionLevel = lvl },
                                                    label = { Text(lvl.displayName, fontSize = 10.sp) },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (selectedUri != null) {
                                                    coroutineScope.launch {
                                                        isProcessing = true
                                                        processingStatusMessage = "جارٍ ضغط وتحسين حجم المستند..."
                                                        val file = withContext(Dispatchers.IO) {
                                                            OfflineDocumentConverter.compressPdfFile(context, selectedUri!!, compressionLevel, selectedDocName)
                                                        }
                                                        resultFile = file
                                                        resultMessage = "تم ضغط المستند بنجاح وتقليص حجمه!"
                                                        isProcessing = false
                                                    }
                                                } else {
                                                    Toast.makeText(context, "الرجاء اختيار ملف PDF أولاً", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                                        ) {
                                            Text("بدء الضغط الآن", fontSize = 11.sp)
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
}

@Composable
private fun ConversionActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .border(1.dp, if (enabled) Color.LightGray.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (enabled) Color.White else Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = if (enabled) 0.12f else 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (enabled) iconTint else Color.Gray, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (enabled) Color(0xFF1E293B) else Color.Gray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 15.sp
                )
            }

            Icon(
                Icons.Default.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (enabled) iconTint else Color.LightGray
            )
        }
    }
}
