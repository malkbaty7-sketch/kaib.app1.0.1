package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppThemeData
import com.example.data.model.CustomFontEntity
import com.example.util.FontStorageManager

@Composable
fun CustomFontPickerDialog(
    currentSelectedFont: String,
    customFonts: List<CustomFontEntity>,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onSelectFont: (String) -> Unit,
    onImportFont: (Uri) -> Unit,
    onDeleteCustomFont: (Long) -> Unit
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportFont(uri)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                        Icon(Icons.Default.FontDownload, contentDescription = null, tint = theme.primaryColor)
                        Column {
                            Text(
                                text = "إدارة واختيار خطوط الكتابة والنشر",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "يدعم الخطوط العربية القياسية واستيراد خطوط خارجية (.ttf / .otf)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Import button
                Button(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("*/*", "font/*", "application/x-font-ttf", "application/x-font-opentype"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+ استيراد خط جديد من الجهاز (.ttf / .otf)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Section 1: Standard Built-in Fonts
                    item {
                        Text(
                            text = "الخطوط العربية الأساسية المتوفرة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = theme.primaryColor,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(FontStorageManager.defaultFonts) { (fontKey, fontLabel) ->
                        val isSelected = currentSelectedFont.equals(fontKey, ignoreCase = true)
                        val fontFamily = remember(fontKey) { FontStorageManager.getFontFamily(fontKey, emptyList()) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) theme.primaryColor else Color.LightGray.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectFont(fontKey) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) theme.primaryColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fontLabel,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "بسم الله الرحمن الرحيم - ن والقلم وما يسطرون",
                                        fontFamily = fontFamily,
                                        fontSize = 15.sp,
                                        color = if (isSelected) theme.primaryColor else Color.DarkGray
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "محدد", tint = theme.primaryColor, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }

                    // Section 2: Imported Custom Fonts
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "الخطوط المخصصة المستوردة (${customFonts.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = theme.primaryColor,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (customFonts.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.03f)
                            ) {
                                Text(
                                    text = "لم يتم استيراد أي خط خارجي حتى الآن. يمكنك استيراد خطوطك المفضلة بصيغة .ttf أو .otf بنقرة واحدة.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    } else {
                        items(customFonts, key = { it.id }) { customFont ->
                            val isSelected = currentSelectedFont.equals(customFont.fontName, ignoreCase = true)
                            val customFamily = remember(customFont) {
                                FontStorageManager.getFontFamily(customFont.fontName, customFonts)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) theme.primaryColor else Color.LightGray.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectFont(customFont.fontName) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) theme.primaryColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = customFont.fontName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF16A34A).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "مستورد",
                                                    color = Color(0xFF16A34A),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "معاينة: كاتب للروايات والقصص المصورة والكتب الرقمية",
                                            fontFamily = customFamily,
                                            fontSize = 14.sp,
                                            color = if (isSelected) theme.primaryColor else Color.DarkGray
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "محدد", tint = theme.primaryColor, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        IconButton(
                                            onClick = { onDeleteCustomFont(customFont.id) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف الخط", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
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
