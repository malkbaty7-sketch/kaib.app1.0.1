package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppFile
import com.example.data.model.AppThemeData
import com.example.data.model.PageImageItem

@Composable
fun ImagePickerDialog(
    theme: AppThemeData,
    availableFiles: List<AppFile>,
    onDismiss: () -> Unit,
    onImageSelected: (PageImageItem) -> Unit
) {
    var selectedTab by remember { mutableStateOf("PRESETS") } // "PRESETS", "GALLERY", "FILES"

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val newImage = PageImageItem(
                sourceUri = uri.toString(),
                title = "صورة من الجوال",
                widthDp = 240f,
                heightDp = 180f,
                alignment = "CENTER"
            )
            onImageSelected(newImage)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = theme.primaryColor)
                        }
                        Column {
                            Text(
                                text = "إضافة صورة إلى الصفحة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                            Text(
                                text = "اختر من استوديو الجوال أو من مكتبة التطبيق",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Segmented Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabButton(
                        title = "مكتبة التطبيق",
                        icon = Icons.Default.Collections,
                        isSelected = selectedTab == "PRESETS",
                        theme = theme,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = "PRESETS" }

                    TabButton(
                        title = "استوديو الجوال",
                        icon = Icons.Default.PhotoLibrary,
                        isSelected = selectedTab == "GALLERY",
                        theme = theme,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = "GALLERY" }

                    TabButton(
                        title = "ملفات المشروع",
                        icon = Icons.Default.FolderOpen,
                        isSelected = selectedTab == "FILES",
                        theme = theme,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = "FILES" }
                }

                // Tab Content
                when (selectedTab) {
                    "PRESETS" -> {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "رسوم وأطر بيانية وتوثيقية جاهزة ومحسنة للكتابة:",
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(PresetIllustrationsCatalog.items) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                val newImage = PageImageItem(
                                                    sourceUri = item.id,
                                                    title = item.title,
                                                    caption = item.title,
                                                    widthDp = 220f,
                                                    heightDp = 150f,
                                                    alignment = "CENTER"
                                                )
                                                onImageSelected(newImage)
                                                onDismiss()
                                            }
                                            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            PresetIllustrationView(
                                                presetId = item.id,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                tintColor = theme.primaryColor
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = theme.textColor,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = item.category,
                                                fontSize = 9.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "GALLERY" -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(theme.primaryColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = theme.primaryColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Text(
                                    text = "اختيار صورة من استوديو الجوال",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textColor
                                )
                                Text(
                                    text = "يمكنك إضافة أي صورة أو رسم توضيحي أو وثيقة من هاتفك، والتحكم بمقاساتها وموقعها بحرية على الصفحة.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("فتح استوديو الصور الآن", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    "FILES" -> {
                        val imageFiles = availableFiles.filter {
                            it.fileType.contains("IMAGE", ignoreCase = true) ||
                            it.fileName.endsWith(".png", ignoreCase = true) ||
                            it.fileName.endsWith(".jpg", ignoreCase = true) ||
                            it.fileName.endsWith(".jpeg", ignoreCase = true)
                        }

                        if (imageFiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.FolderOff, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                    Text("لا توجد ملفات صور مرفوعة في المشروع حالياً", fontSize = 12.sp, color = Color.Gray)
                                    OutlinedButton(
                                        onClick = { selectedTab = "GALLERY" },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("اختر صورة من الجوال")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(imageFiles) { file ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val newImage = PageImageItem(
                                                    sourceUri = file.filePath,
                                                    title = file.fileName,
                                                    caption = file.fileName,
                                                    widthDp = 220f,
                                                    heightDp = 160f,
                                                    alignment = "CENTER"
                                                )
                                                onImageSelected(newImage)
                                                onDismiss()
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = theme.primaryColor)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("ملف مستورد محلياً", fontSize = 10.sp, color = Color.Gray)
                                            }
                                            Icon(Icons.Default.Add, contentDescription = null, tint = theme.primaryColor)
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
private fun TabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    theme: AppThemeData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.White else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isSelected) theme.primaryColor else Color.Gray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) theme.primaryColor else Color.Gray
            )
        }
    }
}
