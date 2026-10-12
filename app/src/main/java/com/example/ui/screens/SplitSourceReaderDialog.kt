package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppFile
import com.example.data.model.AppThemeData

@Composable
fun SplitSourceReaderDialog(
    files: List<AppFile>,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onInsertTextIntoEditor: (String) -> Unit
) {
    var selectedFileId by remember { mutableStateOf(files.firstOrNull()?.id) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val currentFile = remember(selectedFileId, files) {
        files.firstOrNull { it.id == selectedFileId } ?: files.firstOrNull()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.VerticalSplit, contentDescription = null, tint = theme.primaryColor)
                        Text(
                            text = "قارئ ومحلل المصادر المتزامن",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                if (files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد ملفات مصادر مرفوعة في المشروع", color = Color.Gray)
                    }
                } else {
                    // File selection chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        files.forEach { file ->
                            val isChosen = file.id == currentFile?.id
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedFileId = file.id },
                                label = { Text(file.fileName, fontSize = 11.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Search within file
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث وتصفية داخل المصدر...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Source Reader Content Box
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            val contentText = currentFile?.extractedText ?: ""
                            val filteredParagraphs = remember(contentText, searchQuery) {
                                val paras = contentText.split("\n\n").filter { it.isNotBlank() }
                                if (searchQuery.isBlank()) {
                                    paras
                                } else {
                                    paras.filter { it.contains(searchQuery, ignoreCase = true) }
                                }
                            }

                            if (filteredParagraphs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("لا توجد نصوص مطابقة", color = Color.Gray, fontSize = 12.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredParagraphs) { para ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                SelectionContainer {
                                                    Text(
                                                        text = para,
                                                        fontSize = 12.sp,
                                                        lineHeight = 20.sp,
                                                        color = Color(0xFF2B2B2B)
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(
                                                        onClick = {
                                                            onInsertTextIntoEditor(para)
                                                            Toast.makeText(context, "تم إدراج الاقتباس في المحرر!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(14.dp), tint = theme.primaryColor)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("اقتباس إلى الفصل النشط 📥", fontSize = 11.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
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
        }
    }
}
