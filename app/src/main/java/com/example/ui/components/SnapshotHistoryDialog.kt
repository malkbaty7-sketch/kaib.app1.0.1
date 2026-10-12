package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppThemeData
import com.example.data.model.ChapterItem
import com.example.data.model.SnapshotEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SnapshotHistoryDialog(
    currentChapter: ChapterItem?,
    chapterSnapshots: List<SnapshotEntity>,
    allSnapshots: List<SnapshotEntity>,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onCaptureSnapshotNow: () -> Unit,
    onRestoreSnapshot: (SnapshotEntity) -> Unit,
    onDeleteSnapshot: (Long) -> Unit
) {
    var viewAllChapters by remember { mutableStateOf(false) }
    var selectedSnapshotIdForDiff by remember { mutableStateOf<Long?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val displayedList = if (viewAllChapters) allSnapshots else chapterSnapshots
    val timeFormatter = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
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
                        Icon(Icons.Default.History, contentDescription = null, tint = theme.primaryColor)
                        Column {
                            Text(
                                text = "نظام استرجاع اللقطات والنسخ الزمنية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "حفظ دوري تلقائي كل 15 دقيقة مع إمكانية استعادة أي مسودة سابقة",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions & Filter Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !viewAllChapters,
                        onClick = { viewAllChapters = false },
                        label = { Text("لقطات الفصل الحالي (${chapterSnapshots.size})", fontSize = 12.sp) },
                        leadingIcon = {
                            if (!viewAllChapters) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )

                    FilterChip(
                        selected = viewAllChapters,
                        onClick = { viewAllChapters = true },
                        label = { Text("جميع لقطات الكتاب (${allSnapshots.size})", fontSize = 12.sp) },
                        leadingIcon = {
                            if (viewAllChapters) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )

                    Button(
                        onClick = onCaptureSnapshotNow,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("أخذ لقطة فورية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Snapshots List
                if (displayedList.isEmpty()) {
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
                            Icon(Icons.Default.Backup, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                            Text("لا توجد لقطات محفوظة بعد", color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                "سيتم التقاط نسخ تلقائية دورياً أثناء الكتابة، أو يمكنك الضغط على 'أخذ لقطة فورية' الآن.",
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
                        items(displayedList, key = { it.id }) { snapshot ->
                            val isExpanded = selectedSnapshotIdForDiff == snapshot.id
                            val dateStr = remember(snapshot.timestamp) { timeFormatter.format(Date(snapshot.timestamp)) }
                            val isCurrentChapterSnapshot = snapshot.chapterId == currentChapter?.id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isCurrentChapterSnapshot) 1.5.dp else 1.dp,
                                        color = if (isCurrentChapterSnapshot) theme.primaryColor.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentChapterSnapshot) theme.primaryColor.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = snapshot.chapterTitle.ifBlank { "الفصل" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = theme.primaryColor
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color.Black.copy(alpha = 0.06f)
                                                ) {
                                                    Text(
                                                        text = "${snapshot.wordCount} كلمة",
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color.Black.copy(alpha = 0.06f)
                                                ) {
                                                    Text(
                                                        text = "#${snapshot.contentHash.take(6)}",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color.Gray,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = dateStr,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(snapshot.deltaContent))
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                            }

                                            IconButton(
                                                onClick = { onDeleteSnapshot(snapshot.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp), tint = Color.Red.copy(alpha = 0.6f))
                                            }

                                            Button(
                                                onClick = { onRestoreSnapshot(snapshot) },
                                                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("استعادة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Content Preview toggle
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                selectedSnapshotIdForDiff = if (isExpanded) null else snapshot.id
                                            }
                                            .background(Color.Black.copy(alpha = 0.03f))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isExpanded) "إخفاء المعاينة التفصيلية" else "معاينة محتوى اللقطة...",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Gray
                                        )
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = snapshot.deltaContent,
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp,
                                                color = Color(0xFF1E293B)
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
}
