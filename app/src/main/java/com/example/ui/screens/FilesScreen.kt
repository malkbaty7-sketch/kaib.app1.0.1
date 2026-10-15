package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppFile
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FilesScreen(
    viewModel: MainViewModel,
    onNavigateToExtraction: () -> Unit,
    onOpenDeviceDocuments: () -> Unit = {}
) {
    val context = LocalContext.current
    val files by viewModel.allFiles.collectAsState()
    val theme by viewModel.currentTheme.collectAsState()
    val selectedIds by viewModel.selectedFileIdsForExtraction.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFileForPreview by remember { mutableStateOf<AppFile?>(null) }
    var showManualTextDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFileFromUri(uri)
            Toast.makeText(context, "جاري استيراد واستخلاص الملف...", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header Card with 3D Elevation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(theme.elevationDp, RoundedCornerShape(theme.cornerRadiusDp), spotColor = theme.shadowColor),
            shape = RoundedCornerShape(theme.cornerRadiusDp),
            colors = CardDefaults.cardColors(containerColor = theme.surfaceColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "إدارة الملفات والمصادر",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                            Text(
                                text = "استيراد PDF و DOCX ونصوص للتحليل والاستخلاص",
                                fontSize = 11.sp,
                                color = theme.textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Quick access to Device Documents Manager
                Surface(
                    onClick = onOpenDeviceDocuments,
                    shape = RoundedCornerShape(12.dp),
                    color = theme.primaryColor.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(20.dp))
                            Text("تصفح مدير مستندات الهاتف (PDF, Word, Excel) 📂", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                        }
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(20.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain",
                                    "application/msword"
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفع ملف", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showManualTextDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نص مباشر", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Selection Notification Bar (If files are selected for extraction)
        if (selectedIds.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = theme.primaryColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Text(
                            text = "تم تحديد ${selectedIds.size} ملف للاستخلاص",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onNavigateToExtraction,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("استخلاص الآن", color = theme.primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Search and Select All row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في أسماء الملفات...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primaryColor,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            if (files.isNotEmpty()) {
                TextButton(onClick = { viewModel.selectAllFilesForExtraction(files.map { it.id }) }) {
                    Text(
                        if (selectedIds.size == files.size) "إلغاء التحديد" else "تحديد الكل",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor
                    )
                }
            }
        }

        // Files List
        if (filteredFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Text(
                        text = if (searchQuery.isBlank()) "لا توجد ملفات مرفوعة حالياً" else "لا توجد نتائج مطابقة",
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "اضغط على 'رفع ملف' لاختيار ملف من ذاكرة جهازك",
                        fontSize = 12.sp,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFiles, key = { it.id }) { file ->
                    val isSelected = selectedIds.contains(file.id)
                    FileItemCard(
                        file = file,
                        isSelected = isSelected,
                        theme = theme,
                        onToggleSelect = { viewModel.toggleFileSelectionForExtraction(file.id) },
                        onViewContent = { selectedFileForPreview = file },
                        onExtractTopic = {
                            viewModel.toggleFileSelectionForExtraction(file.id)
                            onNavigateToExtraction()
                        },
                        onDelete = { viewModel.deleteFile(file.id) }
                    )
                }
            }
        }
    }

    // Modal: Preview File Text
    selectedFileForPreview?.let { file ->
        FilePreviewDialog(
            file = file,
            theme = theme,
            onDismiss = { selectedFileForPreview = null }
        )
    }

    // Modal: Add Manual Text
    if (showManualTextDialog) {
        ManualTextDialog(
            theme = theme,
            onDismiss = { showManualTextDialog = false },
            onSave = { title, content ->
                viewModel.addManualTextFile(title, content)
                showManualTextDialog = false
            }
        )
    }
}

@Composable
fun FileItemCard(
    file: AppFile,
    isSelected: Boolean,
    theme: com.example.data.model.AppThemeData,
    onToggleSelect: () -> Unit,
    onViewContent: () -> Unit,
    onExtractTopic: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewContent() }
            .shadow(
                if (isSelected) 4.dp else 1.dp,
                RoundedCornerShape(14.dp),
                spotColor = theme.shadowColor
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) theme.primaryColor else Color.Black.copy(alpha = 0.06f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(checkedColor = theme.primaryColor)
                    )

                    val badgeColor = when (file.fileType.uppercase()) {
                        "PDF" -> Color(0xFFD32F2F)
                        "DOCX", "DOC" -> Color(0xFF1976D2)
                        else -> Color(0xFF388E3C)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.fileType.uppercase(),
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = file.fileName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Delete button with 48dp touch target
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${file.wordCount} كلمة • ${(file.fileSize / 1024).coerceAtLeast(1)} KB",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Preview button (comfortable 36dp pill)
                    Surface(
                        onClick = onViewContent,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF475569))
                            Text("معاينة", fontSize = 11.sp, color = Color(0xFF475569))
                        }
                    }

                    // Extract button (comfortable 36dp pill)
                    Surface(
                        onClick = onExtractTopic,
                        shape = RoundedCornerShape(8.dp),
                        color = theme.primaryColor.copy(alpha = 0.12f),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor)
                            Text("استخلاص", fontSize = 11.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilePreviewDialog(
    file: AppFile,
    theme: com.example.data.model.AppThemeData,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "عدد الكلمات: ${file.wordCount} | النوع: ${file.fileType}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = Color.Black.copy(alpha = 0.08f))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFFDF8))
                        .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = if (file.extractedText.isNotBlank()) file.extractedText else "(لا يوجد نص مستخلص)",
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFF2B2B2B),
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(file.extractedText))
                            Toast.makeText(context, "تم نسخ النص إلى الحافظة!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ النص الكامل", fontSize = 12.sp)
                    }

                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("إغلاق", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ManualTextDialog(
    theme: com.example.data.model.AppThemeData,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "إضافة نص أو مرجع مباشر",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان المرجع أو المستند") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("محتوى النص أو المقال...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (content.isNotBlank()) {
                                onSave(title, content)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        enabled = content.isNotBlank()
                    ) {
                        Text("حفظ المرجع", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
