package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeData

@Composable
fun RichWritingToolbar(
    theme: AppThemeData,
    fontSizeSp: Int,
    onFontSizeChange: (Int) -> Unit,
    textAlign: androidx.compose.ui.text.style.TextAlign,
    onTextAlignChange: (androidx.compose.ui.text.style.TextAlign) -> Unit,
    lineSpacingFactor: Float,
    onLineSpacingChange: (Float) -> Unit,
    textColor: Color,
    onTextColorChange: (Color) -> Unit,
    highlightColor: Color?,
    onHighlightColorChange: (Color?) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onUndoToSaveCheckpoint: (() -> Unit)? = null,
    undoCount: Int = 0,
    redoCount: Int = 0,
    onInsertText: (prefix: String, suffix: String, defaultPlaceholder: String) -> Unit,
    onAddImageClick: () -> Unit,
    onSearchReplaceClick: () -> Unit,
    onVoiceTypingClick: () -> Unit,
    content: String,
    imagesCount: Int = 0
) {
    var showColorPalette by remember { mutableStateOf(false) }
    var showHighlightPalette by remember { mutableStateOf(false) }
    var showLineSpacingMenu by remember { mutableStateOf(false) }

    val wordsCount = remember(content) {
        if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    }
    val charsCount = remember(content) { content.length }
    val paragraphsCount = remember(content) {
        if (content.isBlank()) 0 else content.split("\n\n").count { it.isNotBlank() }
    }
    val readingTimeMinutes = remember(wordsCount) {
        maxOf(1, (wordsCount / 180))
    }

    val presetColors = listOf(
        Color(0xFF222222) to "أسود داكن",
        Color(0xFF4A3728) to "بني كلاسيكي",
        Color(0xFF1E3A8A) to "أزرق نيلي",
        Color(0xFF065F46) to "أخضر زمردي",
        Color(0xFF881337) to "أحمر قاني",
        Color(0xFF581C87) to "بنفسجي ملكي"
    )

    val presetHighlights = listOf(
        null to "بدون",
        Color(0xFFFEF08A) to "أصفر",
        Color(0xFFA7F3D0) to "أخضر",
        Color(0xFFBAE6FD) to "سماوي",
        Color(0xFFFECDD3) to "وردي",
        Color(0xFFFED7AA) to "برتقالي"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Undo/Redo + Font Size + Alignment + Search + Voice + Add Image
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Add Image Action Button (بارز باللون الأساسي)
                Button(
                    onClick = onAddImageClick,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة صورة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (imagesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "$imagesCount",
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                VerticalDivider()

                // 1. زر وسهم التراجع الذكي (Undo) مع دعم الاستمرار بالضغط حتى نقطة الحفظ
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (canUndo) theme.primaryColor.copy(alpha = 0.08f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (canUndo) theme.primaryColor.copy(alpha = 0.35f) else Color.LightGray.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .pointerInput(canUndo, onUndoToSaveCheckpoint) {
                            if (canUndo) {
                                detectTapGestures(
                                    onTap = { onUndo() },
                                    onLongPress = {
                                        if (onUndoToSaveCheckpoint != null) {
                                            onUndoToSaveCheckpoint()
                                        } else {
                                            onUndo()
                                        }
                                    }
                                )
                            }
                        }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "تراجع (اضغط مطولاً للتراجع حتى آخر حفظ)",
                            modifier = Modifier.size(15.dp),
                            tint = if (canUndo) theme.primaryColor else Color.LightGray
                        )
                        if (undoCount > 0) {
                            Text(
                                text = "$undoCount",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canUndo) theme.primaryColor else Color.LightGray
                            )
                        }
                    }
                }

                // 2. زر الإعادة بجانبه (Redo) يعكس ما قام به زر التراجع
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (canRedo) theme.primaryColor.copy(alpha = 0.08f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (canRedo) theme.primaryColor.copy(alpha = 0.35f) else Color.LightGray.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .pointerInput(canRedo) {
                            if (canRedo) {
                                detectTapGestures(
                                    onTap = { onRedo() },
                                    onLongPress = { onRedo() }
                                )
                            }
                        }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "إعادة التراجع",
                            modifier = Modifier.size(15.dp),
                            tint = if (canRedo) theme.primaryColor else Color.LightGray
                        )
                        if (redoCount > 0) {
                            Text(
                                text = "$redoCount",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canRedo) theme.primaryColor else Color.LightGray
                            )
                        }
                    }
                }

                VerticalDivider()

                // Font Size Decrement
                IconButton(
                    onClick = { onFontSizeChange((fontSizeSp - 2).coerceAtLeast(12)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text("A-", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Current Font Size Badge
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${fontSizeSp}sp",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Font Size Increment
                IconButton(
                    onClick = { onFontSizeChange((fontSizeSp + 2).coerceAtMost(32)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text("A+", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                VerticalDivider()

                // Text Alignments
                IconButton(
                    onClick = { onTextAlignChange(androidx.compose.ui.text.style.TextAlign.Right) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatAlignRight,
                        contentDescription = "محاذاة يمين",
                        modifier = Modifier.size(16.dp),
                        tint = if (textAlign == androidx.compose.ui.text.style.TextAlign.Right) theme.primaryColor else Color.Gray
                    )
                }

                IconButton(
                    onClick = { onTextAlignChange(androidx.compose.ui.text.style.TextAlign.Center) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.FormatAlignCenter,
                        contentDescription = "توسيط",
                        modifier = Modifier.size(16.dp),
                        tint = if (textAlign == androidx.compose.ui.text.style.TextAlign.Center) theme.primaryColor else Color.Gray
                    )
                }

                IconButton(
                    onClick = { onTextAlignChange(androidx.compose.ui.text.style.TextAlign.Left) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatAlignLeft,
                        contentDescription = "محاذاة يسار",
                        modifier = Modifier.size(16.dp),
                        tint = if (textAlign == androidx.compose.ui.text.style.TextAlign.Left) theme.primaryColor else Color.Gray
                    )
                }

                IconButton(
                    onClick = { onTextAlignChange(androidx.compose.ui.text.style.TextAlign.Justify) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.FormatAlignJustify,
                        contentDescription = "ضبط كامل",
                        modifier = Modifier.size(16.dp),
                        tint = if (textAlign == androidx.compose.ui.text.style.TextAlign.Justify) theme.primaryColor else Color.Gray
                    )
                }

                VerticalDivider()

                // Line Spacing Toggle
                TextButton(
                    onClick = {
                        val next = when (lineSpacingFactor) {
                            1.2f -> 1.5f
                            1.5f -> 1.8f
                            1.8f -> 2.2f
                            else -> 1.2f
                        }
                        onLineSpacingChange(next)
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.FormatLineSpacing, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${lineSpacingFactor}x", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                VerticalDivider()

                // Color Palette Trigger
                IconButton(
                    onClick = { showColorPalette = !showColorPalette },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.FormatColorText, contentDescription = "لون الخط", modifier = Modifier.size(16.dp), tint = textColor)
                }

                // Highlight Color Trigger
                IconButton(
                    onClick = { showHighlightPalette = !showHighlightPalette },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.BorderColor,
                        contentDescription = "تمييز النص",
                        modifier = Modifier.size(16.dp),
                        tint = highlightColor ?: Color.Gray
                    )
                }

                VerticalDivider()

                // Search & Replace
                IconButton(
                    onClick = onSearchReplaceClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.FindReplace, contentDescription = "بحث واستبدال", modifier = Modifier.size(16.dp), tint = Color.DarkGray)
                }

                // Voice Typing
                IconButton(
                    onClick = onVoiceTypingClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "كتابة صوتية", modifier = Modifier.size(16.dp), tint = theme.primaryColor)
                }
            }

            // Row 2: Formatting Styles (H1, H2, H3, Bold, Italic, Underline, Strikethrough, Lists, Quote, Callout, Divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // H1 Heading
                TextButton(
                    onClick = { onInsertText("# ", "\n", "عنوان رئيسي") },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("H1", fontWeight = FontWeight.Black, fontSize = 13.sp, color = theme.primaryColor)
                }

                // H2 Heading
                TextButton(
                    onClick = { onInsertText("## ", "\n", "عنوان فرعي") },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("H2", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.primaryColor)
                }

                // H3 Heading
                TextButton(
                    onClick = { onInsertText("### ", "\n", "عنوان جزئي") },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("H3", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = theme.primaryColor)
                }

                VerticalDivider()

                // Bold
                IconButton(
                    onClick = { onInsertText("**", "**", "نص عريض") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.FormatBold, contentDescription = "عريض", modifier = Modifier.size(16.dp))
                }

                // Italic
                IconButton(
                    onClick = { onInsertText("*", "*", "نص مائل") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "مائل", modifier = Modifier.size(16.dp))
                }

                // Underline
                IconButton(
                    onClick = { onInsertText("<u>", "</u>", "نص مسطر") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.FormatUnderlined, contentDescription = "تسطير", modifier = Modifier.size(16.dp))
                }

                // Strikethrough
                IconButton(
                    onClick = { onInsertText("~~", "~~", "نص مشطوب") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.FormatStrikethrough, contentDescription = "شطب", modifier = Modifier.size(16.dp))
                }

                VerticalDivider()

                // Bullet List
                IconButton(
                    onClick = { onInsertText("\n• ", "", "عنصر قائمة") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "قائمة نقطية", modifier = Modifier.size(16.dp))
                }

                // Numbered List
                IconButton(
                    onClick = { onInsertText("\n1. ", "", "عنصر مرقم") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = "قائمة مرقمة", modifier = Modifier.size(16.dp))
                }

                // Checklist / Task Box
                IconButton(
                    onClick = { onInsertText("\n[ ] ", "", "مهمة مطلوب إنجازها") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = "قائمة مهام", modifier = Modifier.size(16.dp))
                }

                VerticalDivider()

                // Quote
                IconButton(
                    onClick = { onInsertText("«", "»", "اقتباس أدبي مؤثر") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.FormatQuote, contentDescription = "اقتباس", modifier = Modifier.size(16.dp))
                }

                // Callout Box
                IconButton(
                    onClick = { onInsertText("\n╔══ 💡 تنويه وإضاءة فكرية ══╗\n", "\n╚═════════════════════════╝\n", "اكتب الملاحظة هنا...") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "صندوق ملاحظة", modifier = Modifier.size(16.dp), tint = Color(0xFFD97706))
                }

                // Divider Line
                IconButton(
                    onClick = { onInsertText("\n\n❖ ❖ ❖\n\n", "", "") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = "فاصل زخرفي", modifier = Modifier.size(16.dp))
                }
            }

            // Optional Sub-palette: Color Swatches
            if (showColorPalette) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("لون النص:", fontSize = 10.sp, color = Color.Gray)
                    presetColors.forEach { (color, name) ->
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (textColor == color) 2.dp else 1.dp,
                                    color = if (textColor == color) theme.primaryColor else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onTextColorChange(color)
                                    showColorPalette = false
                                }
                        )
                    }
                }
            }

            // Optional Sub-palette: Highlight Swatches
            if (showHighlightPalette) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("التظليل:", fontSize = 10.sp, color = Color.Gray)
                    presetHighlights.forEach { (color, name) ->
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(color ?: Color.White)
                                .border(
                                    width = if (highlightColor == color) 2.dp else 1.dp,
                                    color = if (highlightColor == color) theme.primaryColor else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onHighlightColorChange(color)
                                    showHighlightPalette = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == null) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Stats row (Words, Characters, Paragraphs, Reading Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordsCount كلمة • $charsCount حرف • $paragraphsCount فقرات",
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "وقت القراءة المقدر: ~$readingTimeMinutes دقيقة",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(16.dp)
            .background(Color.Black.copy(alpha = 0.12f))
    )
}
