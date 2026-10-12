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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.AppThemeData
import com.example.data.model.DocumentProgressEntity
import com.example.ui.MainViewModel
import com.example.util.DocumentCategory
import com.example.util.MultiFormatDocumentEngine
import com.example.util.ScannedDocumentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDocumentsScreen(
    theme: AppThemeData,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenDocument: (uriString: String, fileName: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var scannedDocuments by remember { mutableStateOf<List<ScannedDocumentItem>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(DocumentCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    // Folder and sorting states
    var customFolders by remember { mutableStateOf(listOf("الكل", "الرئيسي", "المحولة", "المفضلة", "مسودات ومراجع")) }
    var selectedFolder by remember { mutableStateOf("الكل") }
    var sortBy by remember { mutableStateOf("DATE_DESC") } // DATE_DESC, DATE_ASC, NAME_ASC, SIZE_DESC, TYPE
    var showSortMenu by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }

    // Converter Studio Dialog
    var showConverterDialog by remember { mutableStateOf(false) }
    var targetDocForConverter by remember { mutableStateOf<ScannedDocumentItem?>(null) }

    val allProgressList by viewModel.allDocumentProgress.collectAsState(initial = emptyList())
    val progressMap = remember(allProgressList) {
        allProgressList.associateBy { it.fileUri }
    }

    // Storage Access Framework (SAF) File Picker Launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val (name, size) = MultiFormatDocumentEngine.getFileNameAndSize(context, uri)
            val ext = MultiFormatDocumentEngine.getFileExtension(name)
            val cat = MultiFormatDocumentEngine.detectCategory(ext)
            val newItem = ScannedDocumentItem(
                uriString = uri.toString(),
                name = name,
                extension = ext,
                sizeBytes = size,
                lastModified = System.currentTimeMillis(),
                category = cat,
                folderName = "الرئيسي"
            )
            scannedDocuments = (listOf(newItem) + scannedDocuments).distinctBy { it.uriString }
            onOpenDocument(uri.toString(), name)
        }
    }

    fun startAutoScan() {
        coroutineScope.launch {
            isScanning = true
            val list = withContext(Dispatchers.IO) {
                MultiFormatDocumentEngine.scanStorageDocuments(context)
            }
            scannedDocuments = list
            isScanning = false
            Toast.makeText(context, "تم العثور على ${list.size} مستند في ذاكرة الجهاز", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        startAutoScan()
    }

    fun shareDoc(doc: ScannedDocumentItem) {
        try {
            val uri = Uri.parse(doc.uriString)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة المستند"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر المشاركة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Filter and Sort Pipeline
    val filteredList = remember(scannedDocuments, selectedCategory, selectedFolder, searchQuery, sortBy) {
        val filtered = scannedDocuments.filter { doc ->
            val matchCat = selectedCategory == DocumentCategory.ALL || doc.category == selectedCategory
            val matchFolder = selectedFolder == "الكل" || doc.folderName == selectedFolder
            val matchQuery = searchQuery.isBlank() || doc.name.contains(searchQuery, ignoreCase = true) || doc.extension.contains(searchQuery, ignoreCase = true)
            matchCat && matchFolder && matchQuery
        }

        when (sortBy) {
            "DATE_ASC" -> filtered.sortedBy { it.lastModified }
            "NAME_ASC" -> filtered.sortedBy { it.name.lowercase() }
            "SIZE_DESC" -> filtered.sortedByDescending { it.sizeBytes }
            "TYPE" -> filtered.sortedBy { it.extension }
            else -> filtered.sortedByDescending { it.lastModified } // DATE_DESC
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("مستندات الجهاز والمكتبة الذكية", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                            Text("عارض ومحول المستندات محلياً بدون إنترنت", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Studio Converter Launcher Button
                    IconButton(onClick = {
                        targetDocForConverter = null
                        showConverterDialog = true
                    }) {
                        Icon(Icons.Default.Transform, contentDescription = "محول وأمان المستندات", tint = theme.primaryColor)
                    }

                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "ترتيب وفرز", tint = theme.primaryColor)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("الأحدث تاريخاً أولاً", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = { sortBy = "DATE_DESC"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("الأقدم تاريخاً", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = { sortBy = "DATE_ASC"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("أبجدياً بالاسم (أ-ي)", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = { sortBy = "NAME_ASC"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("الأكبر حجماً أولاً", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = { sortBy = "SIZE_DESC"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("حسب نوع وصيغة الملف", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = { sortBy = "TYPE"; showSortMenu = false }
                            )
                        }
                    }

                    // Rescan button
                    IconButton(onClick = { startAutoScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "إعادة المسح", tint = theme.primaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val mimeTypes = arrayOf(
                        "application/pdf",
                        "application/epub+zip",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/msword",
                        "text/plain",
                        "text/html",
                        "text/rtf",
                        "application/rtf",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "text/csv",
                        "*/*"
                    )
                    openDocumentLauncher.launch(mimeTypes)
                },
                containerColor = theme.primaryColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تصفح واختيار ملف من الهاتف 📂", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        containerColor = theme.surfaceColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث عن اسم كتاب، ملف Word، شريحة، أو جدول...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primaryColor,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )

            // Folders Row with "New Folder" action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(customFolders) { folder ->
                        val isSelected = selectedFolder == folder
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFolder = folder },
                            label = { Text(folder, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Icon(
                                    if (folder == "المفضلة") Icons.Default.Star else Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1E293B),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color(0xFFFBBF24)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { showNewFolderDialog = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "إنشاء مجلد", tint = theme.primaryColor)
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DocumentCategory.values(), key = { it.name }) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text("${cat.icon} ${cat.displayName}", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.primaryColor,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Storage scan progress indicator
            if (isScanning) {
                Surface(
                    color = theme.primaryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = theme.primaryColor)
                        Text("جارٍ فحص ومسح مجلدات الجهاز والمستندات...", fontSize = 11.sp, color = theme.primaryColor)
                    }
                }
            }

            // Results summary header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المستندات (${filteredList.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Text("جاهز للقراءة والتحويل بدون إنترنت", fontSize = 10.sp, color = Color.Gray)
                }
            }

            if (filteredList.isEmpty() && !isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color.LightGray)
                        Text("لم يتم العثور على مستندات مطابقة", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                        Text("انقر على زر 'تصفح واختيار ملف' أدناه أو زر 'محول المستندات' في الأعلى", fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.uriString }) { doc ->
                        val savedProg = progressMap[doc.uriString]
                        DocumentItemCard(
                            document = doc,
                            progress = savedProg,
                            theme = theme,
                            availableFolders = customFolders.filter { it != "الكل" },
                            onClick = {
                                onOpenDocument(doc.uriString, doc.name)
                            },
                            onOpenConverter = {
                                targetDocForConverter = doc
                                showConverterDialog = true
                            },
                            onMoveToFolder = { newFolder ->
                                scannedDocuments = scannedDocuments.map {
                                    if (it.uriString == doc.uriString) it.copy(folderName = newFolder) else it
                                }
                                Toast.makeText(context, "تم نقل المستند إلى مجلد '$newFolder'", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                shareDoc(doc)
                            },
                            onDelete = {
                                scannedDocuments = scannedDocuments.filter { it.uriString != doc.uriString }
                                Toast.makeText(context, "تمت إزالة المستند من القائمة", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("إنشاء مجلد تصنيف جديد", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderNameInput,
                    onValueChange = { newFolderNameInput = it },
                    label = { Text("اسم المجلد", fontSize = 11.sp) },
                    placeholder = { Text("مثال: بحوث تاريخية", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newFolderNameInput.trim()
                        if (name.isNotBlank() && !customFolders.contains(name)) {
                            customFolders = customFolders + name
                            selectedFolder = name
                        }
                        newFolderNameInput = ""
                        showNewFolderDialog = false
                    }
                ) {
                    Text("إنشاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Converter & Utilities Studio Dialog
    if (showConverterDialog) {
        DocumentToolsAndConverterDialog(
            theme = theme,
            initialDocUri = targetDocForConverter?.let { Uri.parse(it.uriString) },
            initialDocName = targetDocForConverter?.name,
            onDismiss = { showConverterDialog = false },
            onOpenConvertedDocument = { uriStr, docName ->
                showConverterDialog = false
                onOpenDocument(uriStr, docName)
            }
        )
    }
}

@Composable
private fun DocumentItemCard(
    document: ScannedDocumentItem,
    progress: DocumentProgressEntity?,
    theme: AppThemeData,
    availableFolders: List<String>,
    onClick: () -> Unit,
    onOpenConverter: () -> Unit,
    onMoveToFolder: (String) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = document.extension.uppercase()
    val badgeColor = when (ext) {
        "PDF" -> Color(0xFFEF4444)
        "EPUB" -> Color(0xFF8B5CF6)
        "DOCX", "DOC" -> Color(0xFF2563EB)
        "XLSX", "XLS", "CSV" -> Color(0xFF10B981)
        "PPTX", "PPT" -> Color(0xFFD97706)
        "HTML", "HTM" -> Color(0xFFF97316)
        else -> Color(0xFF64748B)
    }

    val iconVector = when (ext) {
        "PDF", "EPUB" -> Icons.Default.MenuBook
        "DOCX", "DOC", "TXT", "RTF" -> Icons.Default.Description
        "XLSX", "XLS", "CSV" -> Icons.Default.TableChart
        "PPTX", "PPT" -> Icons.Default.Slideshow
        else -> Icons.Default.Article
    }

    val sizeFormatted = formatFileSize(document.sizeBytes)
    val dateFormatted = remember(document.lastModified) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("ar"))
        sdf.format(Date(document.lastModified))
    }

    var showMenu by remember { mutableStateOf(false) }
    var showFolderSubMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, Color.LightGray.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Format Icon Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(iconVector, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                        Text(ext, fontSize = 8.sp, fontWeight = FontWeight.Black, color = badgeColor)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = document.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (document.folderName != "الرئيسي") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = document.folderName,
                                    fontSize = 9.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(sizeFormatted, fontSize = 10.sp, color = Color.Gray)
                        Text("•", fontSize = 10.sp, color = Color.LightGray)
                        Text(dateFormatted, fontSize = 10.sp, color = Color.Gray)
                    }
                }

                // Quick Action & More Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات إضافية", tint = Color.Gray)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                            showFolderSubMenu = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = { Text("قراءة واستعراض المستند", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تحويل وأمان المستند ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor) },
                            leadingIcon = { Icon(Icons.Default.Transform, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showMenu = false
                                onOpenConverter()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("نقل إلى مجلد...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showFolderSubMenu = !showFolderSubMenu
                            }
                        )

                        if (showFolderSubMenu) {
                            for (folder in availableFolders) {
                                DropdownMenuItem(
                                    text = { Text("📁 $folder", fontSize = 11.sp) },
                                    onClick = {
                                        onMoveToFolder(folder)
                                        showMenu = false
                                        showFolderSubMenu = false
                                    }
                                )
                            }
                        }

                        DropdownMenuItem(
                            text = { Text("مشاركة المستند", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showMenu = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("إزالة من القائمة", fontSize = 12.sp, color = Color(0xFFEF4444)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Progress Banner
            if (progress != null && progress.totalProgressPercentage > 0f) {
                val pct = (progress.totalProgressPercentage * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.BookmarkAdded, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(14.dp))
                            val progressText = when (ext) {
                                "PDF" -> "تم التوقف عند الصفحة ${progress.lastPageIndex + 1} ($pct%)"
                                "EPUB" -> "تم التوقف عند الفصل ${progress.lastPageIndex + 1} ($pct%)"
                                "PPTX" -> "تم التوقف عند الشريحة ${progress.lastPageIndex + 1} ($pct%)"
                                else -> "تقدم القراءة: $pct%"
                            }
                            Text(progressText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                        }

                        LinearProgressIndicator(
                            progress = { progress.totalProgressPercentage },
                            modifier = Modifier
                                .width(60.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = theme.primaryColor,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}
