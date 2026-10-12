package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeData
import com.example.data.model.ChapterItem
import com.example.data.model.CorkboardCard
import com.example.data.model.Scene
import com.example.data.model.SceneSerializer
import com.example.data.model.StoryElement
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalCorkboardScreen(
    chapters: List<ChapterItem>,
    storyElements: List<StoryElement>,
    theme: AppThemeData,
    onBack: () -> Unit,
    onUpdateChapters: (List<ChapterItem>) -> Unit,
    onNavigateToChapter: (Int) -> Unit
) {
    // Pan and Zoom Transformation state
    var scale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Cards on the Corkboard
    var cards by remember(chapters, storyElements) {
        val list = mutableListOf<CorkboardCard>()
        var xOffset = 60f
        var yOffset = 80f

        // 1. Chapters cards
        chapters.forEachIndexed { chIdx, ch ->
            list.add(
                CorkboardCard(
                    id = "ch_${ch.id}",
                    targetId = ch.id,
                    cardType = "CHAPTER",
                    title = "فصل ${chIdx + 1}: ${ch.title}",
                    content = if (ch.content.length > 80) ch.content.take(80) + "..." else ch.content.ifBlank { "فصل بدون محتوى بعد" },
                    posX = xOffset,
                    posY = yOffset,
                    colorHex = "#FFF9C4", // Warm Yellow Card
                    tag = "فصل كامل"
                )
            )

            // Sub-scenes for this chapter
            val scenes = SceneSerializer.fromJson(ch.scenesJson)
            scenes.forEachIndexed { scIdx, sc ->
                list.add(
                    CorkboardCard(
                        id = "sc_${sc.id}",
                        targetId = sc.id,
                        cardType = "SCENE",
                        title = "مشهد ${scIdx + 1}: ${sc.title}",
                        content = sc.content.ifBlank { sc.summary.ifBlank { "مشهد فرعي..." } },
                        posX = xOffset,
                        posY = yOffset + 240f + (scIdx * 190f),
                        colorHex = "#E0F2FE", // Light Blue Card
                        connectedCardIds = listOf("ch_${ch.id}"),
                        tag = "مشهد فرعي"
                    )
                )
            }

            xOffset += 260f
        }

        // 2. Character & Plot Story Elements
        storyElements.forEachIndexed { elemIdx, elem ->
            val color = when (elem.roleOrType) {
                "CHARACTER" -> "#FCE7F3" // Pink
                "LOCATION" -> "#DCFCE7" // Green
                else -> "#FEF3C7" // Amber
            }
            val label = when (elem.roleOrType) {
                "CHARACTER" -> "شخصية"
                "LOCATION" -> "موقع"
                else -> "عنصر حبكة"
            }
            list.add(
                CorkboardCard(
                    id = "elem_${elem.id}",
                    targetId = elem.id.toString(),
                    cardType = "CHARACTER",
                    title = elem.name,
                    content = elem.description.ifBlank { elem.psychologicalMotivation.ifBlank { "عنصر روائي" } },
                    posX = 60f + (elemIdx * 240f),
                    posY = 650f,
                    colorHex = color,
                    tag = label
                )
            )
        }

        mutableStateOf<List<CorkboardCard>>(list)
    }

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "CHAPTERS", "SCENES", "ELEMENTS"
    var linkingFromCardId by remember { mutableStateOf<String?>(null) }
    var showHelpBanner by remember { mutableStateOf(true) }

    val filteredCards = remember(cards, selectedFilter) {
        when (selectedFilter) {
            "CHAPTERS" -> cards.filter { it.cardType == "CHAPTER" }
            "SCENES" -> cards.filter { it.cardType == "SCENE" }
            "ELEMENTS" -> cards.filter { it.cardType == "CHARACTER" }
            else -> cards
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("لوحة التخطيط والتنظيم المرئي (Digital Corkboard) 📌", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("خرائط القصة، ربط المشاهد بالخيوط الدرامية وإعادة الترتيب التفاعلي", fontSize = 10.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Reset View Button
                    IconButton(onClick = {
                        scale = 1.0f
                        panOffset = Offset(0f, 0f)
                    }) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "إعادة ضبط المنظور", tint = theme.primaryColor)
                    }

                    // Zoom In
                    IconButton(onClick = { scale = (scale + 0.2f).coerceAtMost(2.5f) }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "تكبير")
                    }

                    // Zoom Out
                    IconButton(onClick = { scale = (scale - 0.2f).coerceAtLeast(0.5f) }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "تصغير")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFBF4E8))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFD7A768)) // Warm Cork texture base
        ) {
            // Filter and Status Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFC08040),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "ALL" to "الكل 📋",
                            "CHAPTERS" to "الفصول 📖",
                            "SCENES" to "المشاهد 🎬",
                            "ELEMENTS" to "الشخصيات 👥"
                        ).forEach { (fKey, label) ->
                            FilterChip(
                                selected = selectedFilter == fKey,
                                onClick = { selectedFilter = fKey },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8B4513),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (linkingFromCardId != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDC2626)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("اختر بطاقة أخرى لربط خيط درامي 🧵", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { linkingFromCardId = null }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    } else {
                        Text("التقريب: ${(scale * 100).toInt()}% • اسحب اللوحة للتنقل", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Main Interactive Corkboard Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(0.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 2.5f)
                            panOffset += pan
                        }
                    }
            ) {
                // Background Connection Lines Layer (Curved threads connecting cards)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = panOffset.x,
                            translationY = panOffset.y
                        )
                ) {
                    cards.forEach { card ->
                        val fromCenter = Offset(card.posX + 105f, card.posY + 70f)
                        card.connectedCardIds.forEach { targetId ->
                            val targetCard = cards.firstOrNull { it.id == targetId }
                            if (targetCard != null) {
                                val toCenter = Offset(targetCard.posX + 105f, targetCard.posY + 70f)

                                val path = Path().apply {
                                    moveTo(fromCenter.x, fromCenter.y)
                                    val controlX = (fromCenter.x + toCenter.x) / 2f
                                    val controlY = ((fromCenter.y + toCenter.y) / 2f) - 40f
                                    quadraticTo(controlX, controlY, toCenter.x, toCenter.y)
                                }

                                // Draw connection thread with shadow
                                drawPath(
                                    path = path,
                                    color = Color(0xFFB91C1C),
                                    style = Stroke(
                                        width = 3.5f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f),
                                        cap = StrokeCap.Round
                                    )
                                )

                                // Draw pin heads at both ends
                                drawCircle(color = Color(0xFFDC2626), radius = 6f, center = fromCenter)
                                drawCircle(color = Color(0xFFDC2626), radius = 6f, center = toCenter)
                            }
                        }
                    }
                }

                // Foreground Movable Index Cards
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = panOffset.x,
                            translationY = panOffset.y
                        )
                ) {
                    filteredCards.forEach { card ->
                        DraggableIndexCard(
                            card = card,
                            isLinkingSource = linkingFromCardId == card.id,
                            theme = theme,
                            onPositionChange = { dx, dy ->
                                cards = cards.map {
                                    if (it.id == card.id) it.copy(posX = it.posX + dx, posY = it.posY + dy) else it
                                }
                            },
                            onStartLinking = {
                                linkingFromCardId = card.id
                            },
                            onCardClick = {
                                if (linkingFromCardId != null && linkingFromCardId != card.id) {
                                    // Connect them
                                    cards = cards.map {
                                        if (it.id == linkingFromCardId) {
                                            val currentConnections = it.connectedCardIds
                                            val updated = if (currentConnections.contains(card.id)) currentConnections - card.id else currentConnections + card.id
                                            it.copy(connectedCardIds = updated)
                                        } else it
                                    }
                                    linkingFromCardId = null
                                } else if (card.cardType == "CHAPTER") {
                                    val chIdx = chapters.indexOfFirst { it.id == card.targetId }
                                    if (chIdx != -1) onNavigateToChapter(chIdx)
                                }
                            },
                            onMoveChapterOrder = { step ->
                                val chIdx = chapters.indexOfFirst { it.id == card.targetId }
                                if (chIdx != -1) {
                                    val targetIdx = (chIdx + step).coerceIn(0, chapters.lastIndex)
                                    if (targetIdx != chIdx) {
                                        val mutableChapters = chapters.toMutableList()
                                        val item = mutableChapters.removeAt(chIdx)
                                        mutableChapters.add(targetIdx, item)
                                        onUpdateChapters(mutableChapters)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableIndexCard(
    card: CorkboardCard,
    isLinkingSource: Boolean,
    theme: AppThemeData,
    onPositionChange: (Float, Float) -> Unit,
    onStartLinking: () -> Unit,
    onCardClick: () -> Unit,
    onMoveChapterOrder: (Int) -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(card.colorHex))
    } catch (_: Exception) {
        Color(0xFFFFF9C4)
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(card.posX.roundToInt(), card.posY.roundToInt()) }
            .width(220.dp)
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(cardColor)
            .border(
                width = if (isLinkingSource) 2.5.dp else 1.dp,
                color = if (isLinkingSource) Color(0xFFDC2626) else Color.Black.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(card.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onPositionChange(dragAmount.x, dragAmount.y)
                }
            }
            .clickable { onCardClick() }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Pushpin & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = card.tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Link thread handle
                IconButton(
                    onClick = onStartLinking,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.DeviceHub,
                        contentDescription = "ربط خيط درامي",
                        tint = if (isLinkingSource) Color(0xFFDC2626) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Card Title
            Text(
                text = card.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Card Excerpt / Synopsis
            Text(
                text = card.content,
                fontSize = 10.sp,
                color = Color.DarkGray,
                lineHeight = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Reorder Actions if Chapter
            if (card.cardType == "CHAPTER") {
                HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ترتيب الفصل:", fontSize = 9.sp, color = Color.Gray)
                    Row {
                        IconButton(onClick = { onMoveChapterOrder(-1) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "تقديم", modifier = Modifier.size(13.dp))
                        }
                        IconButton(onClick = { onMoveChapterOrder(1) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "تأخير", modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
    }
}
