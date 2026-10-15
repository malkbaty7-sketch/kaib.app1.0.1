package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
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
    var showSortBottomSheet by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }
    var activeDocForBottomSheet by remember { mutableStateOf<ScannedDocumentItem?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    var showTopMoreMenu by remember { mutableStateOf(false) }

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
                folderName = if (selectedFolder != "الكل") selectedFolder else "الرئيسي"
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

    // Live counts for category chips
    val categoryCounts = remember(scannedDocuments, selectedFolder, searchQuery) {
        val inFolder = scannedDocuments.filter { doc ->
            (selectedFolder == "الكل" || doc.folderName == selectedFolder) &&
            (searchQuery.isBlank() || doc.name.contains(searchQuery, ignoreCase = true) || doc.extension.contains(searchQuery, ignoreCase = true))
        }
        DocumentCategory.values().associateWith { cat ->
            if (cat == DocumentCategory.ALL) inFolder.size
            else inFolder.count { it.category == cat }
        }
    }

    // Live counts for folders
    val folderCounts = remember(scannedDocuments) {
        customFolders.associateWith { folder ->
            if (folder == "الكل") scannedDocuments.size
            else scannedDocuments.count { it.folderName == folder }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "مدير المستندات",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = theme.textColor
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = theme.primaryColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${filteredList.size} ملف",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primaryColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "تصفح، قراءة وتحويل محلي بدون إنترنت",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = theme.primaryColor
                        )
                    }
                },
                actions = {
                    // View Toggle (List vs Grid) - 48dp touch target
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "تبديل طريقة العرض",
                            tint = theme.primaryColor
                        )
                    }

                    // Sort Trigger - 48dp touch target
                    IconButton(
                        onClick = { showSortBottomSheet = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = "ترتيب وفرز",
                            tint = theme.primaryColor
                        )
                    }

                    // Converter Studio Quick Access
                    IconButton(
                        onClick = {
                            targetDocForConverter = null
                            showConverterDialog = true
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = "محول وأمان المستندات",
                            tint = theme.primaryColor
                        )
                    }

                    // Overflow Menu for Mobile
                    Box {
                        IconButton(
                            onClick = { showTopMoreMenu = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "خيارات إضافية",
                                tint = theme.primaryColor
                            )
                        }
                        DropdownMenu(
                            expanded = showTopMoreMenu,
                            onDismissRequest = { showTopMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("إعادة فحص الذاكرة 🔄", fontSize = 13.sp) },
                                onClick = {
                                    showTopMoreMenu = false
                                    startAutoScan()
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = theme.primaryColor) }
                            )
                            DropdownMenuItem(
                                text = { Text("إنشاء مجلد جديد 📁", fontSize = 13.sp) },
                                onClick = {
                                    showTopMoreMenu = false
                                    showNewFolderDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = theme.primaryColor) }
                            )
                            DropdownMenuItem(
                                text = { Text("أدوات التحويل والتشكيل (Tashkeel) ⚡", fontSize = 13.sp) },
                                onClick = {
                                    showTopMoreMenu = false
                                    targetDocForConverter = null
                                    showConverterDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Transform, contentDescription = null, tint = theme.primaryColor) }
                            )
                        }
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
                modifier = Modifier
                    .navigationBarsPadding()
                    .shadow(8.dp, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تصفح واختيار ملف 📂", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        containerColor = theme.surfaceColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mobile Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                placeholder = { Text("بحث عن اسم ملف، كتاب، أو صيغة...", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح البحث", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primaryColor,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Folders Row with "+ New Folder" chip
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(customFolders) { folder ->
                    val isSelected = selectedFolder == folder
                    val count = folderCounts[folder] ?: 0
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFolder = folder },
                        label = {
                            Text(
                                text = if (folder == "الكل") "الكل ($count)" else "$folder ($count)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
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
                            selectedLeadingIconColor = Color(0xFFFBBF24),
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    )
                }

                item {
                    // New Folder Button directly in the flow
                    Surface(
                        onClick = { showNewFolderDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = theme.primaryColor.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.3f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(14.dp))
                            Text("مجلد جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                        }
                    }
                }
            }

            // Category Filter Chips with counts
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(DocumentCategory.values(), key = { it.name }) { cat ->
                    val isSelected = selectedCategory == cat
                    val count = categoryCounts[cat] ?: 0
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = "${cat.icon} ${cat.displayName} ($count)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.primaryColor,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    )
                }
            }

            // Storage scan indicator
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
                        Text("جارٍ فحص ومسح مجلدات الهاتف والمستندات...", fontSize = 11.sp, color = theme.primaryColor)
                    }
                }
            }

            // Content: Empty State, Grid, or List
            if (filteredList.isEmpty() && !isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(theme.primaryColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(36.dp), tint = theme.primaryColor)
                        }
                        Text("لم يتم العثور على مستندات مطابقة", fontWeight = FontWeight.Bold, color = theme.textColor, fontSize = 15.sp)
                        Text("انقر على زر 'تصفح واختيار ملف' أدناه لاختيار ملف من ذاكرة هاتفك", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        Button(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختيار ملف الآن", fontSize = 12.sp)
                        }
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.uriString }) { doc ->
                        val savedProg = progressMap[doc.uriString]
                        DocumentGridItemCard(
                            document = doc,
                            progress = savedProg,
                            theme = theme,
                            onClick = { onOpenDocument(doc.uriString, doc.name) },
                            onOpenOptions = { activeDocForBottomSheet = doc }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.uriString }) { doc ->
                        val savedProg = progressMap[doc.uriString]
                        DocumentItemCard(
                            document = doc,
                            progress = savedProg,
                            theme = theme,
                            onClick = { onOpenDocument(doc.uriString, doc.name) },
                            onOpenOptions = { activeDocForBottomSheet = doc },
                            onQuickConvert = {
                                targetDocForConverter = doc
                                showConverterDialog = true
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = theme.primaryColor)
                    Text("إنشاء مجلد تصنيف جديد", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اكتب اسم المجلد لتصنيف المستندات وتسهيل الوصول إليها:", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        label = { Text("اسم المجلد", fontSize = 12.sp) },
                        placeholder = { Text("مثال: بحوث تاريخية", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إنشاء وحفظ")
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

    // Mobile Ergonomic Document Actions Bottom Sheet
    activeDocForBottomSheet?.let { doc ->
        val savedProg = progressMap[doc.uriString]
        DocumentActionsBottomSheet(
            document = doc,
            progress = savedProg,
            theme = theme,
            availableFolders = customFolders.filter { it != "الكل" },
            onDismiss = { activeDocForBottomSheet = null },
            onRead = {
                activeDocForBottomSheet = null
                onOpenDocument(doc.uriString, doc.name)
            },
            onOpenConverter = {
                activeDocForBottomSheet = null
                targetDocForConverter = doc
                showConverterDialog = true
            },
            onMoveToFolder = { newFolder ->
                scannedDocuments = scannedDocuments.map {
                    if (it.uriString == doc.uriString) it.copy(folderName = newFolder) else it
                }
                activeDocForBottomSheet = null
                Toast.makeText(context, "تم نقل المستند إلى مجلد '$newFolder'", Toast.LENGTH_SHORT).show()
            },
            onShare = {
                activeDocForBottomSheet = null
                shareDoc(doc)
            },
            onDelete = {
                scannedDocuments = scannedDocuments.filter { it.uriString != doc.uriString }
                activeDocForBottomSheet = null
                Toast.makeText(context, "تمت إزالة المستند من القائمة", Toast.LENGTH_SHORT).show()
            },
            onCreateNewFolder = {
                activeDocForBottomSheet = null
                showNewFolderDialog = true
            }
        )
    }

    // Sort Bottom Sheet for Mobile
    if (showSortBottomSheet) {
        SortBottomSheet(
            currentSort = sortBy,
            theme = theme,
            onDismiss = { showSortBottomSheet = false },
            onSelectSort = { sortBy = it }
        )
    }
}

@Composable
private fun DocumentItemCard(
    document: ScannedDocumentItem,
    progress: DocumentProgressEntity?,
    theme: AppThemeData,
    onClick: () -> Unit,
    onOpenOptions: () -> Unit,
    onQuickConvert: () -> Unit
) {
    val ext = document.extension.uppercase()
    val badgeColor = when (ext) {
        "PDF" -> Color(0xFFEF4444)
        "EPUB" -> Color(0xFF8B5CF6)
        "DOCX", "DOC" -> Color(0xFF2563EB)
        "XLSX", "XLS", "CSV" -> Color(0xFF10B981)
        "PPTX", "PPT" -> Color(0xFFD97706)
        "HTML", "HTM" -> Color(0xFFF97316)
        "PNG", "JPG", "JPEG", "WEBP" -> Color(0xFF7C3AED)
        else -> Color(0xFF64748B)
    }

    val iconVector = when (ext) {
        "PDF", "EPUB" -> Icons.Default.MenuBook
        "DOCX", "DOC", "TXT", "RTF" -> Icons.Default.Description
        "XLSX", "XLS", "CSV" -> Icons.Default.TableChart
        "PPTX", "PPT" -> Icons.Default.Slideshow
        "PNG", "JPG", "JPEG", "WEBP" -> Icons.Default.Image
        else -> Icons.Default.Article
    }

    val sizeFormatted = formatFileSize(document.sizeBytes)
    val dateFormatted = remember(document.lastModified) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("ar"))
        sdf.format(Date(document.lastModified))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000)),
        shape = RoundedCornerShape(16.dp),
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(iconVector, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp))
                        Text(ext, fontSize = 8.sp, fontWeight = FontWeight.Black, color = badgeColor)
                    }
                }

                // File Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (document.folderName != "الرئيسي") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "📁 ${document.folderName}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text("•", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                        }
                        Text(sizeFormatted, fontSize = 11.sp, color = Color(0xFF64748B))
                        Text("•", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                        Text(dateFormatted, fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                // Menu button with 48dp touch target
                IconButton(
                    onClick = onOpenOptions,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "خيارات إضافية",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Progress Banner (if reading progress exists)
            if (progress != null && progress.totalProgressPercentage > 0f) {
                val pct = (progress.totalProgressPercentage * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.BookmarkAdded, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(14.dp))
                            val progressText = when (ext) {
                                "PDF" -> "الصفحة ${progress.lastPageIndex + 1} ($pct%)"
                                "EPUB" -> "الفصل ${progress.lastPageIndex + 1} ($pct%)"
                                "PPTX" -> "الشريحة ${progress.lastPageIndex + 1} ($pct%)"
                                else -> "تقدم القراءة: $pct%"
                            }
                            Text(progressText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                        }

                        LinearProgressIndicator(
                            progress = { progress.totalProgressPercentage },
                            modifier = Modifier
                                .width(70.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = theme.primaryColor,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            // Quick Actions Bar (Bottom of card) for comfortable mobile use
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direct "Read" pill button
                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    color = theme.primaryColor.copy(alpha = 0.08f),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(14.dp))
                        Text("قراءة وعرض", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Quick Convert action
                    Surface(
                        onClick = onQuickConvert,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEEF2FF),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Transform, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
                            Text("تحويل ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                        }
                    }

                    // Open full menu options
                    Surface(
                        onClick = onOpenOptions,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                            Text("المزيد", fontSize = 11.sp, color = Color(0xFF475569))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentGridItemCard(
    document: ScannedDocumentItem,
    progress: DocumentProgressEntity?,
    theme: AppThemeData,
    onClick: () -> Unit,
    onOpenOptions: () -> Unit
) {
    val ext = document.extension.uppercase()
    val badgeColor = when (ext) {
        "PDF" -> Color(0xFFEF4444)
        "EPUB" -> Color(0xFF8B5CF6)
        "DOCX", "DOC" -> Color(0xFF2563EB)
        "XLSX", "XLS", "CSV" -> Color(0xFF10B981)
        "PPTX", "PPT" -> Color(0xFFD97706)
        "HTML", "HTM" -> Color(0xFFF97316)
        "PNG", "JPG", "JPEG", "WEBP" -> Color(0xFF7C3AED)
        else -> Color(0xFF64748B)
    }

    val iconVector = when (ext) {
        "PDF", "EPUB" -> Icons.Default.MenuBook
        "DOCX", "DOC", "TXT", "RTF" -> Icons.Default.Description
        "XLSX", "XLS", "CSV" -> Icons.Default.TableChart
        "PPTX", "PPT" -> Icons.Default.Slideshow
        "PNG", "JPG", "JPEG", "WEBP" -> Icons.Default.Image
        else -> Icons.Default.Article
    }

    val sizeFormatted = formatFileSize(document.sizeBytes)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = ext,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onOpenOptions,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconVector, contentDescription = null, tint = badgeColor, modifier = Modifier.size(38.dp))
            }

            Text(
                text = document.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF1E293B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sizeFormatted, fontSize = 10.sp, color = Color.Gray)
                if (document.folderName != "الرئيسي") {
                    Text("📁 ${document.folderName}", fontSize = 9.sp, color = Color(0xFF475569))
                }
            }

            if (progress != null && progress.totalProgressPercentage > 0f) {
                LinearProgressIndicator(
                    progress = { progress.totalProgressPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = theme.primaryColor,
                    trackColor = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentActionsBottomSheet(
    document: ScannedDocumentItem,
    progress: DocumentProgressEntity?,
    theme: AppThemeData,
    availableFolders: List<String>,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onOpenConverter: () -> Unit,
    onMoveToFolder: (String) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCreateNewFolder: () -> Unit
) {
    val ext = document.extension.uppercase()
    val badgeColor = when (ext) {
        "PDF" -> Color(0xFFEF4444)
        "EPUB" -> Color(0xFF8B5CF6)
        "DOCX", "DOC" -> Color(0xFF2563EB)
        "XLSX", "XLS", "CSV" -> Color(0xFF10B981)
        "PPTX", "PPT" -> Color(0xFFD97706)
        "HTML", "HTM" -> Color(0xFFF97316)
        "PNG", "JPG", "JPEG", "WEBP" -> Color(0xFF7C3AED)
        else -> Color(0xFF64748B)
    }

    val iconVector = when (ext) {
        "PDF", "EPUB" -> Icons.Default.MenuBook
        "DOCX", "DOC", "TXT", "RTF" -> Icons.Default.Description
        "XLSX", "XLS", "CSV" -> Icons.Default.TableChart
        "PPTX", "PPT" -> Icons.Default.Slideshow
        "PNG", "JPG", "JPEG", "WEBP" -> Icons.Default.Image
        else -> Icons.Default.Article
    }

    val sizeFormatted = formatFileSize(document.sizeBytes)
    val dateFormatted = remember(document.lastModified) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("ar"))
        sdf.format(Date(document.lastModified))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(badgeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(iconVector, contentDescription = null, tint = badgeColor, modifier = Modifier.size(24.dp))
                        Text(ext, fontSize = 9.sp, fontWeight = FontWeight.Black, color = badgeColor)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF1F5F9)) {
                            Text(text = "📁 ${document.folderName}", fontSize = 10.sp, color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Text("•", fontSize = 10.sp, color = Color.LightGray)
                        Text(sizeFormatted, fontSize = 11.sp, color = Color.Gray)
                        Text("•", fontSize = 10.sp, color = Color.LightGray)
                        Text(dateFormatted, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // Progress Banner
            if (progress != null && progress.totalProgressPercentage > 0f) {
                val pct = (progress.totalProgressPercentage * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.BookmarkAdded, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(16.dp))
                            Text("الصفحة ${progress.lastPageIndex + 1} ($pct%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                        }
                        LinearProgressIndicator(
                            progress = { progress.totalProgressPercentage },
                            modifier = Modifier.width(80.dp).height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = theme.primaryColor,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 4 Large Thumb-friendly Action Cards (2x2 Grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action 1: Read (Primary)
                Surface(
                    onClick = onRead,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = theme.primaryColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("قراءة وعرض", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("فتح المستند", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                // Action 2: Convert & OCR Studio
                Surface(
                    onClick = onOpenConverter,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEEF2FF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4F46E5).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Transform, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("تحويل وأمان ⚡", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                            Text("Word/Excel/OCR", fontSize = 10.sp, color = Color(0xFF6366F1))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action 3: Share
                Surface(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(20.dp))
                        Text("مشاركة الملف", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }
                }

                // Action 4: Delete / Remove
                Surface(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Text("إزالة من القائمة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }
            }

            // Move to Folder Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نقل الملف إلى مجلد:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    TextButton(
                        onClick = onCreateNewFolder,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ مجلد جديد", fontSize = 11.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableFolders) { folder ->
                        val isCurrent = document.folderName == folder
                        FilterChip(
                            selected = isCurrent,
                            onClick = { onMoveToFolder(folder) },
                            label = { Text("📁 $folder", fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primaryColor.copy(alpha = 0.15f),
                                selectedLabelColor = theme.primaryColor,
                                containerColor = Color(0xFFF8FAFC)
                            ),
                            leadingIcon = if (isCurrent) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.heightIn(min = 38.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    currentSort: String,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onSelectSort: (String) -> Unit
) {
    val sortOptions = listOf(
        Triple("DATE_DESC", "الأحدث تاريخاً أولاً", Icons.Default.AccessTime),
        Triple("DATE_ASC", "الأقدم تاريخاً أولاً", Icons.Default.History),
        Triple("NAME_ASC", "أبجدياً بالاسم (أ - ي)", Icons.Default.SortByAlpha),
        Triple("SIZE_DESC", "الأكبر حجماً أولاً", Icons.Default.Storage),
        Triple("TYPE", "حسب نوع وصيغة الملف", Icons.Default.Category)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Sort, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(20.dp))
                }
                Text("ترتيب وفرز المستندات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            sortOptions.forEach { (key, label, icon) ->
                val isSelected = currentSort == key
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectSort(key)
                            onDismiss()
                        }
                        .background(if (isSelected) theme.primaryColor.copy(alpha = 0.1f) else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(icon, contentDescription = null, tint = if (isSelected) theme.primaryColor else Color.Gray, modifier = Modifier.size(22.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) theme.primaryColor else Color(0xFF1E293B)
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
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
