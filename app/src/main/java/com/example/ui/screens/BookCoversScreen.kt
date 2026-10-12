package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeData
import com.example.data.model.BookCoverTemplate
import com.example.data.repository.BookCoverRepository
import com.example.ui.MainViewModel
import com.example.ui.components.BookCoverView
import com.example.ui.components.CoverCustomizerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCoversScreen(
    viewModel: MainViewModel,
    theme: AppThemeData,
    onBack: () -> Unit,
    onApplyCoverToProject: ((BookCoverTemplate, isFrontCover: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val repository = remember { BookCoverRepository(context) }
    val customTemplates by repository.customTemplates.collectAsState()

    var selectedCategory by remember { mutableStateOf("الكل") }
    var editingTemplate by remember { mutableStateOf<BookCoverTemplate?>(null) }
    var pendingUploadedUri by remember { mutableStateOf<Uri?>(null) }
    var showApplyCoverDialog by remember { mutableStateOf<BookCoverTemplate?>(null) }

    // Photo picker for uploading from phone
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUploadedUri = uri
        }
    }

    val allTemplates = remember(customTemplates) {
        customTemplates + repository.builtInTemplates
    }

    val filteredTemplates = remember(allTemplates, selectedCategory) {
        if (selectedCategory == "الكل") {
            allTemplates
        } else if (selectedCategory == "نماذجي الخاصة") {
            customTemplates
        } else {
            allTemplates.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "استوديو نماذج أغلفة الكتب",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "نماذج احترافية وإمكانية رفع غلافك المخصص من الهاتف",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفع غلاف من الجوال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.surfaceColor)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(theme.backgroundBrush)
        ) {
            // Category Filter Bar
            val categories = listOf("الكل", "نماذجي الخاصة", "أدبي", "أكاديمي", "فكر وتاريخ", "تراثي", "عصري وتجريدي")

            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Tab(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (category == "نماذجي الخاصة") {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isSelected) theme.primaryColor else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) theme.primaryColor else Color.DarkGray
                                )
                                if (category == "نماذجي الخاصة" && customTemplates.isNotEmpty()) {
                                    Badge(containerColor = theme.primaryColor) {
                                        Text("${customTemplates.size}", color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Notice Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = theme.primaryColor.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تعديل النماذج واعتمادها",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = theme.textColor
                        )
                        Text(
                            text = "يمكنك النقر على أي نموذج لتعديل نصوصه بما يتناسب مع اسم كتابك ومؤلفه، أو رفعه مباشرة من استوديو هاتفك وحفظه في التطبيق.",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Covers Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 165.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredTemplates.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد نماذج مخصصة محفوظة بعد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "اضغط على 'رفع غلاف من الجوال' لاختيار صورة من جهازك واعتمادها كنموذج خاص بك.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    items(filteredTemplates, key = { it.id }) { template ->
                        BookCoverCard(
                            template = template,
                            theme = theme,
                            onEditClick = { editingTemplate = template },
                            onApplyClick = { showApplyCoverDialog = template },
                            onDeleteClick = if (template.isUserCustom) {
                                {
                                    repository.deleteCustomTemplate(template.id)
                                    Toast.makeText(context, "تم حذف النموذج المخصص", Toast.LENGTH_SHORT).show()
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Modal: Save Uploaded Photo as Custom Template
    if (pendingUploadedUri != null) {
        SaveUploadedCoverDialog(
            theme = theme,
            onDismiss = { pendingUploadedUri = null },
            onSave = { name, bookTitle, author ->
                val saved = repository.saveUserCoverFromUri(
                    sourceUri = pendingUploadedUri!!,
                    customTitle = name,
                    bookTitle = bookTitle,
                    authorName = author
                )
                if (saved != null) {
                    Toast.makeText(context, "تم حفظ الغلاف في نماذجك بنجاح!", Toast.LENGTH_SHORT).show()
                    editingTemplate = saved
                } else {
                    Toast.makeText(context, "فشل حفظ الغلاف", Toast.LENGTH_SHORT).show()
                }
                pendingUploadedUri = null
            }
        )
    }

    // Modal: Cover Customizer (Edit Texts & Colors)
    if (editingTemplate != null) {
        CoverCustomizerDialog(
            initialTemplate = editingTemplate!!,
            theme = theme,
            onDismiss = { editingTemplate = null },
            onSaveAsCustomTemplate = { updated ->
                repository.addCustomTemplate(updated)
            },
            onApplyToProject = { appliedTemplate, isFront ->
                onApplyCoverToProject?.invoke(appliedTemplate, isFront)
                val coverTypeName = if (isFront) "الغلاف الأمامي (أول صفحة)" else "الغلاف الخلفي (آخر صفحة)"
                Toast.makeText(context, "تم اعتماد الغلاف لـ $coverTypeName بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal: Quick Choose Front or Back Cover
    if (showApplyCoverDialog != null) {
        AlertDialog(
            onDismissRequest = { showApplyCoverDialog = null },
            title = { Text("اعتماد الغلاف للكتاب") },
            text = { Text("أين تود تطبيق هذا النموذج في كتابك؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyCoverToProject?.invoke(showApplyCoverDialog!!, true)
                        Toast.makeText(context, "تم اعتماد الغلاف كأول صفحة في الكتاب", Toast.LENGTH_SHORT).show()
                        showApplyCoverDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                ) {
                    Text("الغلاف الأمامي (أول صفحة)")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onApplyCoverToProject?.invoke(showApplyCoverDialog!!, false)
                        Toast.makeText(context, "تم اعتماد الغلاف كآخر صفحة في الكتاب", Toast.LENGTH_SHORT).show()
                        showApplyCoverDialog = null
                    }
                ) {
                    Text("الغلاف الخلفي (آخر صفحة)")
                }
            }
        )
    }
}

@Composable
fun BookCoverCard(
    template: BookCoverTemplate,
    theme: AppThemeData,
    onEditClick: () -> Unit,
    onApplyClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Book Cover Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditClick() },
                contentAlignment = Alignment.Center
            ) {
                BookCoverView(
                    template = template,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )

                if (template.isUserCustom) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("نموذجي", fontSize = 9.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & category info
            Text(
                text = template.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = theme.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "التصنيف: ${template.category}",
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل", fontSize = 10.sp)
                }

                Button(
                    onClick = onApplyClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("اعتماد", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SaveUploadedCoverDialog(
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onSave: (name: String, bookTitle: String, author: String) -> Unit
) {
    var templateName by remember { mutableStateOf("غلافي المخصص") }
    var bookTitle by remember { mutableStateOf("عنوان كتابي") }
    var authorName by remember { mutableStateOf("اسم الكاتب") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "حفظ الغلاف المرفوع في نماذجك",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor
                )

                Text(
                    text = "سيتم حفظ هذه الصورة ضمن نماذجك الخاصة لتمكنك من استخدامها في أي وقت.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("اسم النموذج") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    label = { Text("عنوان الكتاب على الغلاف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("اسم المؤلف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                            if (templateName.isNotBlank()) {
                                onSave(templateName, bookTitle, authorName)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ النموذج")
                    }
                }
            }
        }
    }
}
