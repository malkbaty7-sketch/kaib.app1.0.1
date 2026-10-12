package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LineSpacingPreset
import com.example.data.model.ReaderFontWeightOption
import com.example.data.model.ReaderSettingsState
import com.example.data.model.ReaderTheme
import java.util.Locale

/**
 * شريط سريع وخفيف لإعدادات القراءة (Reader Settings Bar)
 * يمكن تثبيته في أسفل واجهة القراءة للتحكم المباشر في الخط والثيم
 */
@Composable
fun ReaderSettingsBar(
    settingsState: ReaderSettingsState,
    onSettingsChanged: (ReaderSettingsState) -> Unit,
    onOpenFullSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .testTag("reader_quick_settings_bar"),
        shape = RoundedCornerShape(24.dp),
        color = settingsState.theme.surfaceColor,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, settingsState.theme.borderColor.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // أزرار التحكم السريع في حجم الخط (A- و A+)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        val newSize = (settingsState.fontSizeSp - 1.5f).coerceAtLeast(ReaderSettingsState.MIN_FONT_SIZE)
                        onSettingsChanged(settingsState.copy(fontSizeSp = newSize))
                    },
                    modifier = Modifier.size(36.dp).testTag("quick_decrease_font_button"),
                    enabled = settingsState.fontSizeSp > ReaderSettingsState.MIN_FONT_SIZE
                ) {
                    Icon(
                        imageVector = Icons.Default.TextDecrease,
                        contentDescription = "تصغير الخط",
                        tint = settingsState.theme.textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = settingsState.theme.textColor.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = "${settingsState.fontSizeSp.toInt()} sp",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = settingsState.theme.textColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val newSize = (settingsState.fontSizeSp + 1.5f).coerceAtMost(ReaderSettingsState.MAX_FONT_SIZE)
                        onSettingsChanged(settingsState.copy(fontSizeSp = newSize))
                    },
                    modifier = Modifier.size(36.dp).testTag("quick_increase_font_button"),
                    enabled = settingsState.fontSizeSp < ReaderSettingsState.MAX_FONT_SIZE
                ) {
                    Icon(
                        imageVector = Icons.Default.TextIncrease,
                        contentDescription = "تكبير الخط",
                        tint = settingsState.theme.textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // دوائر سريعة لاختيار الثيم اللوني
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReaderTheme.values().forEach { themeOption ->
                    val isSelected = settingsState.theme == themeOption
                    val animatedBorderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f),
                        animationSpec = tween(durationMillis = 200),
                        label = "theme_border"
                    )

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(themeOption.backgroundColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = animatedBorderColor,
                                shape = CircleShape
                            )
                            .clickable {
                                onSettingsChanged(settingsState.copy(theme = themeOption))
                            }
                            .testTag("quick_theme_${themeOption.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(themeOption.textColor)
                            )
                        }
                    }
                }
            }

            // زر فتح الإعدادات الشاملة (Bottom Sheet)
            IconButton(
                onClick = onOpenFullSettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .testTag("open_full_reader_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "فتح كافة إعدادات القراءة",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * نافذة سفلية متكاملة (Reader Settings Bottom Sheet)
 * تتيح التحكم الدقيق والشامل في حجم الخط، سمكه، تباعد الأسطر، الثيمات، والمعاينة الحية
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsBottomSheet(
    settingsState: ReaderSettingsState,
    onSettingsChanged: (ReaderSettingsState) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = settingsState.theme.surfaceColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp,
        modifier = modifier.testTag("reader_settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // شريط العنوان وأزرار الإجراءات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "إعدادات وتخصيص القراءة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = settingsState.theme.textColor
                    )
                }

                // زر استعادة الوضع الافتراضي
                TextButton(
                    onClick = {
                        onSettingsChanged(ReaderSettingsState())
                    },
                    modifier = Modifier.testTag("reset_reader_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "إعادة ضبط",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استعادة الافتراضي", fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = settingsState.theme.textColor.copy(alpha = 0.1f))

            // 1. قسم ثيم وألوان القراءة (Reader Theme)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "نمط الإضاءة والخلفية:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = settingsState.theme.textColor.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReaderTheme.values().forEach { themeOption ->
                        val isSelected = settingsState.theme == themeOption
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSettingsChanged(settingsState.copy(theme = themeOption))
                                }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("theme_card_${themeOption.name}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = themeOption.backgroundColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(themeOption.textColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = themeOption.backgroundColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = themeOption.title,
                                    color = themeOption.textColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 2. قسم حجم الخط (Font Size)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حجم الخط:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = settingsState.theme.textColor.copy(alpha = 0.8f)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${settingsState.fontSizeSp.toInt()} sp",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newSize = (settingsState.fontSizeSp - 1f).coerceAtLeast(ReaderSettingsState.MIN_FONT_SIZE)
                            onSettingsChanged(settingsState.copy(fontSizeSp = newSize))
                        },
                        enabled = settingsState.fontSizeSp > ReaderSettingsState.MIN_FONT_SIZE
                    ) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "تصغير", tint = settingsState.theme.textColor)
                    }

                    Slider(
                        value = settingsState.fontSizeSp,
                        onValueChange = { newSize ->
                            onSettingsChanged(settingsState.copy(fontSizeSp = newSize))
                        },
                        valueRange = ReaderSettingsState.MIN_FONT_SIZE..ReaderSettingsState.MAX_FONT_SIZE,
                        steps = 22,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("font_size_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    IconButton(
                        onClick = {
                            val newSize = (settingsState.fontSizeSp + 1f).coerceAtMost(ReaderSettingsState.MAX_FONT_SIZE)
                            onSettingsChanged(settingsState.copy(fontSizeSp = newSize))
                        },
                        enabled = settingsState.fontSizeSp < ReaderSettingsState.MAX_FONT_SIZE
                    ) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "تكبير", tint = settingsState.theme.textColor)
                    }
                }
            }

            // 3. قسم سمك الخط (Font Weight)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "سمك الخط:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = settingsState.theme.textColor.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderFontWeightOption.values().forEach { option ->
                        val isSelected = settingsState.fontWeight == option.weight
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSettingsChanged(settingsState.copy(fontWeight = option.weight))
                                }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("weight_option_${option.name}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else settingsState.theme.backgroundColor
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.title,
                                    fontWeight = option.weight,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else settingsState.theme.textColor
                                )
                            }
                        }
                    }
                }
            }

            // 4. قسم تباعد الأسطر (Line Height / Spacing)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تباعد الأسطر:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = settingsState.theme.textColor.copy(alpha = 0.8f)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1fx", settingsState.lineHeightMultiplier),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // بطاقات التباعد الجاهزة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LineSpacingPreset.values().forEach { preset ->
                        val isSelected = Math.abs(settingsState.lineHeightMultiplier - preset.multiplier) < 0.08f
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSettingsChanged(settingsState.copy(lineHeightMultiplier = preset.multiplier))
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else settingsState.theme.backgroundColor
                        ) {
                            Text(
                                text = preset.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else settingsState.theme.textColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // سلايدر للتحكم الدقيق في التباعد
                Slider(
                    value = settingsState.lineHeightMultiplier,
                    onValueChange = { newSpacing ->
                        onSettingsChanged(settingsState.copy(lineHeightMultiplier = newSpacing))
                    },
                    valueRange = ReaderSettingsState.MIN_LINE_HEIGHT..ReaderSettingsState.MAX_LINE_HEIGHT,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("line_spacing_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // 5. قسم المعاينة الحية التفاعلية (Live Reading Preview)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "معاينة حية للقراءة 👁️:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = settingsState.theme.textColor.copy(alpha = 0.8f)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, settingsState.theme.borderColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .testTag("live_reading_preview_box"),
                    shape = RoundedCornerShape(16.dp),
                    color = settingsState.theme.backgroundColor
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "«أَعَزُّ مَكَانٍ فِي الدُّنَى سَرْجُ سَابِحٍ • وَخَيْرُ جَلِيسٍ فِي الزَّمَانِ كِتَابُ»",
                            fontSize = (settingsState.fontSizeSp).sp,
                            lineHeight = (settingsState.fontSizeSp * settingsState.lineHeightMultiplier).sp,
                            fontWeight = settingsState.fontWeight,
                            color = settingsState.theme.textColor,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "القراءة الممتعة تبدأ من التنسيق المريح للعين — تتغير النصوص والألوان وتتناسق الأسطر لحظياً لتلائم تفضيلاتك الشخصية.",
                            fontSize = (settingsState.fontSizeSp * 0.88f).sp,
                            lineHeight = (settingsState.fontSizeSp * 0.88f * settingsState.lineHeightMultiplier).sp,
                            fontWeight = FontWeight.Normal,
                            color = settingsState.theme.textColor.copy(alpha = 0.85f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
