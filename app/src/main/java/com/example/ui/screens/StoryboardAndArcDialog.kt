package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeData
import com.example.data.model.ChapterItem
import com.example.data.model.StoryElement

@Composable
fun StoryboardAndArcDialog(
    chapters: List<ChapterItem>,
    storyElements: List<StoryElement>,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onUpdateChapterTension: (index: Int, tension: Float) -> Unit,
    onAddStoryElement: (name: String, role: String, desc: String, motivation: String, colorHex: String) -> Unit,
    onDeleteStoryElement: (StoryElement) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Narrative Arc, 1: Characters & Lore
    var showAddElementDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = theme.primaryColor)
                        Text(
                            text = "مخطط القصة وتصاعد التوتر الدرامي",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF7F7F7),
                    contentColor = theme.primaryColor,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("منحنى التوتر الدرامي (Arc)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("بطاقات الشخصيات والعالم (${storyElements.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                if (selectedTab == 0) {
                    // TAB 0: Visual Dramatic Arc
                    DramaticArcView(
                        chapters = chapters,
                        theme = theme,
                        onUpdateChapterTension = onUpdateChapterTension
                    )
                } else {
                    // TAB 1: Characters and Lore Cards
                    CharactersAndLoreView(
                        storyElements = storyElements,
                        theme = theme,
                        onAddClick = { showAddElementDialog = true },
                        onDeleteElement = onDeleteStoryElement
                    )
                }
            }
        }
    }

    if (showAddElementDialog) {
        AddStoryElementDialog(
            theme = theme,
            onDismiss = { showAddElementDialog = false },
            onSave = { name, role, desc, motiv, colorHex ->
                onAddStoryElement(name, role, desc, motiv, colorHex)
                showAddElementDialog = false
            }
        )
    }
}

@Composable
fun DramaticArcView(
    chapters: List<ChapterItem>,
    theme: AppThemeData,
    onUpdateChapterTension: (index: Int, tension: Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "رسم بياني تفاعلي يوضح تصاعد التوتر والإثارة من المقدمة حتى ذروة الأحداث والخاتمة:",
            fontSize = 11.sp,
            color = Color.Gray
        )

        // Tension Graph Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .shadow(2.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCFC))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    if (chapters.size >= 2) {
                        val path = Path()
                        val stepX = w / (chapters.size - 1)

                        val points = chapters.mapIndexed { idx, ch ->
                            val x = idx * stepX
                            val tensionClamped = ch.tensionLevel.coerceIn(0.05f, 0.95f)
                            val y = h - (tensionClamped * h)
                            Offset(x, y)
                        }

                        path.moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val pPrev = points[i - 1]
                            val pCur = points[i]
                            val controlX = (pPrev.x + pCur.x) / 2
                            path.cubicTo(controlX, pPrev.y, controlX, pCur.y, pCur.x, pCur.y)
                        }

                        // Draw Curve
                        drawPath(
                            path = path,
                            color = theme.primaryColor,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw Point Circles
                        points.forEachIndexed { idx, pt ->
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = theme.primaryColor,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "تحكم في مستوى التوتر لكل فصل/قسم (اسحب الشريط):",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textColor
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chapters.size) { idx ->
                val ch = chapters[idx]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ch.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.textColor)
                            val tensionPercent = (ch.tensionLevel * 100).toInt()
                            Text("التوتر: $tensionPercent%", fontSize = 11.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = ch.tensionLevel,
                            onValueChange = { newVal -> onUpdateChapterTension(idx, newVal) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = theme.primaryColor,
                                activeTrackColor = theme.primaryColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CharactersAndLoreView(
    storyElements: List<StoryElement>,
    theme: AppThemeData,
    onAddClick: () -> Unit,
    onDeleteElement: (StoryElement) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سجل الشخصيات، المواقع، والأدوات الدرامية:",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة بطاقة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (storyElements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF9F9F9)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.RecentActors, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(36.dp))
                    Text("لا توجد بطاقات شخصيات أو مواقع مضافة بعد", fontSize = 12.sp, color = Color.Gray)
                    Text("أضف أبطال روايتك وأهدافهم النفسية لتتبعها أثناء الكتابة", fontSize = 10.sp, color = Color.LightGray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(storyElements, key = { it.id }) { item ->
                    StoryElementCard(
                        element = item,
                        theme = theme,
                        onDelete = { onDeleteElement(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun StoryElementCard(
    element: StoryElement,
    theme: AppThemeData,
    onDelete: () -> Unit
) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(element.colorTag))
    } catch (e: Exception) {
        theme.primaryColor
    }

    val typeLabel = when (element.roleOrType) {
        "CHARACTER" -> "شخصية"
        "LOCATION" -> "موقع / بيئة"
        "LORE" -> "خلفية تاريخية / أسطورة"
        else -> "عنصر حبكة"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(tagColor))
                    Text(element.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.textColor)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tagColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(typeLabel, fontSize = 10.sp, color = tagColor, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            if (element.description.isNotBlank()) {
                Text(element.description, fontSize = 12.sp, color = Color.DarkGray, lineHeight = 18.sp)
            }

            if (element.psychologicalMotivation.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF7F7F7))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "الدافع والمحرك النفسي: ${element.psychologicalMotivation}",
                        fontSize = 11.sp,
                        color = Color(0xFF555555),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AddStoryElementDialog(
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onSave: (name: String, role: String, desc: String, motiv: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CHARACTER") }
    var desc by remember { mutableStateOf("") }
    var motivation by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#D2691E") }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("إضافة عنصر قصة / شخصية", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textColor)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم أو العنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "CHARACTER" to "شخصية",
                        "LOCATION" to "موقع",
                        "LORE" to "عالم/أسطورة",
                        "PLOT_DEVICE" to "عنصر حبكة"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = role == key,
                            onClick = { role = key },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primaryColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("الوصف والسمات البارزة") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
                )

                OutlinedTextField(
                    value = motivation,
                    onValueChange = { motivation = it },
                    label = { Text("الدافع الداخلي أو الأهمية السردية") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, role, desc, motivation, colorHex)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ البطاقة")
                    }
                }
            }
        }
    }
}
