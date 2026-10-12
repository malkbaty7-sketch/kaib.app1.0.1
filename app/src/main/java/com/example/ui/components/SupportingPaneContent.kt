package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.util.DocumentParser
import java.text.SimpleDateFormat
import java.util.*

enum class SupportingPaneTab {
    OUTLINE_AND_SCENES,
    CHARACTER_CARDS,
    SOURCE_READER,
    SNAPSHOTS
}

@Composable
fun SupportingPaneContent(
    activeChapter: ChapterItem?,
    allChapters: List<ChapterItem>,
    activeChapterIndex: Int,
    scenes: List<Scene>,
    storyElements: List<StoryElement>,
    extractedTexts: List<ExtractedText>,
    snapshots: List<SnapshotEntity>,
    theme: AppThemeData,
    onSelectChapter: (Int) -> Unit,
    onAddChapter: () -> Unit,
    onAddScene: () -> Unit,
    onUpdateScene: (Scene) -> Unit,
    onDeleteScene: (String) -> Unit,
    onReorderScenes: (Int, Int) -> Unit,
    onAddStoryElement: () -> Unit,
    onDeleteStoryElement: (StoryElement) -> Unit,
    onInsertTextIntoActiveChapter: (String) -> Unit,
    onRestoreSnapshot: (SnapshotEntity) -> Unit,
    onCaptureSnapshotNow: () -> Unit,
    onCloseSupportingPane: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(SupportingPaneTab.OUTLINE_AND_SCENES) }
    val clipboardManager = LocalClipboardManager.current
    val timeFormatter = remember { SimpleDateFormat("MM/dd hh:mm a", Locale("ar")) }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header with tab selector and collapse button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اللوحة المساعدة (Supporting Pane)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryColor
                )
                IconButton(
                    onClick = onCloseSupportingPane,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إخفاء اللوحة", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = currentTab.ordinal,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = currentTab == SupportingPaneTab.OUTLINE_AND_SCENES,
                    onClick = { currentTab = SupportingPaneTab.OUTLINE_AND_SCENES },
                    text = { Text("المخطط والمشاهد", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = currentTab == SupportingPaneTab.CHARACTER_CARDS,
                    onClick = { currentTab = SupportingPaneTab.CHARACTER_CARDS },
                    text = { Text("الشخصيات (${storyElements.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = currentTab == SupportingPaneTab.SOURCE_READER,
                    onClick = { currentTab = SupportingPaneTab.SOURCE_READER },
                    text = { Text("المصادر (${extractedTexts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = currentTab == SupportingPaneTab.SNAPSHOTS,
                    onClick = { currentTab = SupportingPaneTab.SNAPSHOTS },
                    text = { Text("النسخ (${snapshots.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))

            // Tab Content
            when (currentTab) {
                SupportingPaneTab.OUTLINE_AND_SCENES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("فصول ومشاهد العمل:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = onAddChapter,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("+ فصل", fontSize = 11.sp, color = theme.primaryColor)
                                }
                                TextButton(
                                    onClick = onAddScene,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("+ مشهد", fontSize = 11.sp, color = theme.primaryColor)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Chapters Overview
                            item {
                                Text("الفصول:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            itemsIndexed(allChapters) { idx, ch ->
                                val isSelected = idx == activeChapterIndex
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSelectChapter(idx) },
                                    color = if (isSelected) theme.primaryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) theme.primaryColor else Color.LightGray.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ch.tabName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) theme.primaryColor else Color.DarkGray
                                        )
                                        Text(
                                            text = "${DocumentParser.countWords(ch.content)} ك",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            // Scenes of Active Chapter
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مشاهد الفصل النشط (${scenes.size}):",
                                        fontSize = 11.sp,
                                        color = theme.primaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${scenes.sumOf { DocumentParser.countWords(it.content) }} كلمة",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            if (scenes.isEmpty()) {
                                item {
                                    Text(
                                        text = "لا توجد مشاهد مخصصة لهذا الفصل بعد. اضغط '+ مشهد' للبدء.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            } else {
                                itemsIndexed(scenes, key = { _, s -> s.id }) { sIdx, scene ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp)),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${sIdx + 1}. ${scene.title}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = { if (sIdx > 0) onReorderScenes(sIdx, sIdx - 1) },
                                                        enabled = sIdx > 0,
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { if (sIdx < scenes.lastIndex) onReorderScenes(sIdx, sIdx + 1) },
                                                        enabled = sIdx < scenes.lastIndex,
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { onDeleteScene(scene.id) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Red.copy(alpha = 0.5f))
                                                    }
                                                }
                                            }
                                            if (scene.summary.isNotBlank()) {
                                                Text(
                                                    text = scene.summary,
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SupportingPaneTab.CHARACTER_CARDS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("بطاقات الشخصيات والعناصر:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = onAddStoryElement,
                                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+ بطاقة جديدة", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (storyElements.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("لا توجد شخصيات أو عناصر مسجلة بعد", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(storyElements, key = { it.id }) { element ->
                                    val colorTag = try {
                                        Color(android.graphics.Color.parseColor(element.colorTag))
                                    } catch (_: Exception) {
                                        theme.primaryColor
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colorTag.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                                            .background(colorTag)
                                                    )
                                                    Text(text = element.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                                IconButton(
                                                    onClick = { onDeleteStoryElement(element) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Red.copy(alpha = 0.5f))
                                                }
                                            }
                                            if (element.roleOrType.isNotBlank()) {
                                                Text(text = "الدور: ${element.roleOrType}", fontSize = 11.sp, color = theme.primaryColor)
                                            }
                                            if (element.psychologicalMotivation.isNotBlank()) {
                                                Text(text = "الدافع: ${element.psychologicalMotivation}", fontSize = 10.sp, color = Color.DarkGray)
                                            }
                                            if (element.description.isNotBlank()) {
                                                Text(text = element.description, fontSize = 10.sp, color = Color.Gray, maxLines = 3)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SupportingPaneTab.SOURCE_READER -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("المصادر والنصوص المستخلصة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        if (extractedTexts.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("لا توجد مقتطفات مستخلصة من ملفاتك بعد", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(extractedTexts, key = { it.id }) { textRecord ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = textRecord.topicQuery,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = theme.primaryColor
                                                )
                                                Row {
                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setText(AnnotatedString(textRecord.extractedContent))
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(14.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            onInsertTextIntoActiveChapter("\n\n${textRecord.extractedContent}")
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "إدراج بالنص", modifier = Modifier.size(14.dp), tint = theme.primaryColor)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = textRecord.extractedContent,
                                                fontSize = 11.sp,
                                                color = Color.DarkGray,
                                                maxLines = 6
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SupportingPaneTab.SNAPSHOTS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("النسخ واللقطات الزمنية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = onCaptureSnapshotNow,
                                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+ لقطة الآن", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (snapshots.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("لا توجد لقطات مسجلة بعد", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(snapshots, key = { it.id }) { snap ->
                                    val dateStr = remember(snap.timestamp) { timeFormatter.format(Date(snap.timestamp)) }
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(text = snap.chapterTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text(text = "$dateStr • ${snap.wordCount} كلمة", fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Button(
                                                    onClick = { onRestoreSnapshot(snap) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("استعادة", fontSize = 10.sp)
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
