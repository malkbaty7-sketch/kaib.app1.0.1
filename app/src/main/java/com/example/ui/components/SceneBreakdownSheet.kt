package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.model.ChapterItem
import com.example.data.model.Scene
import com.example.util.DocumentParser

@Composable
fun SceneBreakdownSheet(
    chapter: ChapterItem?,
    scenes: List<Scene>,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onAddScene: (String) -> Unit,
    onUpdateScene: (Scene) -> Unit,
    onDeleteScene: (String) -> Unit,
    onReorderScenes: (Int, Int) -> Unit,
    onAutoSplit: () -> Unit,
    onMergeScenes: () -> Unit
) {
    var expandedSceneId by remember { mutableStateOf<String?>(scenes.firstOrNull()?.id) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newSceneTitle by remember { mutableStateOf("") }

    val totalSceneWords = remember(scenes) {
        scenes.sumOf { DocumentParser.countWords(it.content) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
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
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = theme.primaryColor)
                        Column {
                            Text(
                                text = "محرك تفكيك المشاهد (Scene Breakdown)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "الفصل: ${chapter?.title ?: ""} • إجمالي كلمات المشاهد: $totalSceneWords كلمة",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ إضافة مشهد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onAutoSplit,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.primaryColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تقسيم الفصل تلقائياً", fontSize = 12.sp)
                    }

                    if (scenes.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onMergeScenes,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF16A34A))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("دمج المشاهد بالفصل", fontSize = 12.sp, color = Color(0xFF16A34A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (scenes.isEmpty()) {
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
                            Icon(Icons.Default.MovieFilter, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                            Text("لا توجد مشاهد منفصلة في هذا الفصل بعد", color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                "يمكنك تقسيم الفصل الحالي إلى مشاهد أو النقر على '+ إضافة مشهد' لبناء هيكل مشهدي دقيق.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(scenes, key = { _, s -> s.id }) { index, scene ->
                            val isExpanded = expandedSceneId == scene.id
                            val wordCount = remember(scene.content) { DocumentParser.countWords(scene.content) }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isExpanded) 1.5.dp else 1.dp,
                                        color = if (isExpanded) theme.primaryColor else Color.LightGray.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) theme.primaryColor.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    // Row with index, title, tension, reorder arrows, delete
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
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = theme.primaryColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.primaryColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = scene.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "$wordCount كلمة",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                    Text("•", fontSize = 10.sp, color = Color.Gray)
                                                    Text(
                                                        text = "مستوى التوتر: ${(scene.tensionLevel * 100).toInt()}%",
                                                        fontSize = 10.sp,
                                                        color = if (scene.tensionLevel > 0.7f) Color(0xFFDC2626) else theme.primaryColor
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Move Up
                                            IconButton(
                                                onClick = { if (index > 0) onReorderScenes(index, index - 1) },
                                                enabled = index > 0,
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "تحريك لأعلى", modifier = Modifier.size(16.dp))
                                            }

                                            // Move Down
                                            IconButton(
                                                onClick = { if (index < scenes.lastIndex) onReorderScenes(index, index + 1) },
                                                enabled = index < scenes.lastIndex,
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "تحريك لأسفل", modifier = Modifier.size(16.dp))
                                            }

                                            // Delete Scene
                                            IconButton(
                                                onClick = { onDeleteScene(scene.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف المشهد", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                            }

                                            // Toggle Expand
                                            IconButton(
                                                onClick = { expandedSceneId = if (isExpanded) null else scene.id },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Title field
                                            OutlinedTextField(
                                                value = scene.title,
                                                onValueChange = { onUpdateScene(scene.copy(title = it)) },
                                                label = { Text("عنوان المشهد", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )

                                            // Summary field
                                            OutlinedTextField(
                                                value = scene.summary,
                                                onValueChange = { onUpdateScene(scene.copy(summary = it)) },
                                                label = { Text("ملخص/ملاحظات المشهد", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )

                                            // Tension Slider
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("منحنى التوتر الدرامي للمشهد:", fontSize = 11.sp, color = Color.Gray)
                                                    Text("${(scene.tensionLevel * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                                                }
                                                Slider(
                                                    value = scene.tensionLevel,
                                                    onValueChange = { onUpdateScene(scene.copy(tensionLevel = it)) },
                                                    valueRange = 0f..1f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = theme.primaryColor,
                                                        activeTrackColor = theme.primaryColor
                                                    )
                                                )
                                            }

                                            // Content Editor
                                            OutlinedTextField(
                                                value = scene.content,
                                                onValueChange = { onUpdateScene(scene.copy(content = it)) },
                                                label = { Text("نص المشهد", fontSize = 11.sp) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 120.dp, max = 260.dp),
                                                placeholder = { Text("اكتب أحداث وحوارات هذا المشهد هنا...") }
                                            )
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة مشهد جديد", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSceneTitle,
                    onValueChange = { newSceneTitle = it },
                    label = { Text("عنوان المشهد") },
                    placeholder = { Text("مثال: المواجهة في الحديقة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddScene(newSceneTitle)
                        newSceneTitle = ""
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
