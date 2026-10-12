package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeData
import com.example.data.model.PageImageItem

@Composable
fun PageImageControlDialog(
    image: PageImageItem,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onSave: (PageImageItem) -> Unit,
    onDelete: () -> Unit,
    onResetPosition: () -> Unit
) {
    var widthDp by remember { mutableFloatStateOf(image.widthDp) }
    var heightDp by remember { mutableFloatStateOf(image.heightDp) }
    var rotationDeg by remember { mutableFloatStateOf(image.rotationDeg) }
    var marginHorizontalDp by remember { mutableFloatStateOf(image.marginHorizontalDp) }
    var marginVerticalDp by remember { mutableFloatStateOf(image.marginVerticalDp) }
    var cornerRadiusDp by remember { mutableFloatStateOf(image.cornerRadiusDp) }
    var borderThicknessDp by remember { mutableFloatStateOf(image.borderThicknessDp) }
    var borderColorHex by remember { mutableStateOf(image.borderColorHex) }
    var elevationDp by remember { mutableFloatStateOf(image.elevationDp) }
    var opacity by remember { mutableFloatStateOf(image.opacity) }
    var caption by remember { mutableStateOf(image.caption) }
    var alignment by remember { mutableStateOf(image.alignment) }
    var lockAspectRatio by remember { mutableStateOf(true) }

    val initialRatio = remember {
        if (image.heightDp > 0f) image.widthDp / image.heightDp else 1.33f
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = theme.primaryColor)
                        }
                        Column {
                            Text(
                                text = "التحكم في أبعاد ومسافات الصورة",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                            Text(
                                text = "ضبط المقاسات، المسافات، الدوران والتموضع",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = Color.LightGray.copy(alpha = 0.4f))

                // Scrollable Controls Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Quick Size Presets
                    Text("المقاسات السريعة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickSizeButton("صغير (140dp)", modifier = Modifier.weight(1f)) {
                            widthDp = 140f
                            heightDp = 105f
                        }
                        QuickSizeButton("متوسط (220dp)", modifier = Modifier.weight(1f)) {
                            widthDp = 220f
                            heightDp = 165f
                        }
                        QuickSizeButton("كبير (300dp)", modifier = Modifier.weight(1f)) {
                            widthDp = 300f
                            heightDp = 225f
                        }
                    }

                    // Width & Height Sliders
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("عرض الصورة (Width): ${widthDp.toInt()} dp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("قفل النسبة", fontSize = 10.sp, color = Color.Gray)
                                    Checkbox(
                                        checked = lockAspectRatio,
                                        onCheckedChange = { lockAspectRatio = it },
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Slider(
                                value = widthDp,
                                onValueChange = {
                                    widthDp = it
                                    if (lockAspectRatio) {
                                        heightDp = (it / initialRatio).coerceIn(60f, 400f)
                                    }
                                },
                                valueRange = 80f..360f,
                                colors = SliderDefaults.colors(thumbColor = theme.primaryColor, activeTrackColor = theme.primaryColor)
                            )

                            Text("ارتفاع الصورة (Height): ${heightDp.toInt()} dp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = heightDp,
                                onValueChange = {
                                    heightDp = it
                                    if (lockAspectRatio) {
                                        widthDp = (it * initialRatio).coerceIn(80f, 360f)
                                    }
                                },
                                valueRange = 60f..380f,
                                colors = SliderDefaults.colors(thumbColor = theme.primaryColor, activeTrackColor = theme.primaryColor)
                            )
                        }
                    }

                    // Margins & Spacing (مسافات وهوامش الصورة)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("مسافات وهوامش الصورة (Margins):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المسافة الأفقية (يمين/يسار):", fontSize = 11.sp, color = Color.DarkGray)
                                Text("${marginHorizontalDp.toInt()} dp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                            }
                            Slider(
                                value = marginHorizontalDp,
                                onValueChange = { marginHorizontalDp = it },
                                valueRange = 0f..40f,
                                colors = SliderDefaults.colors(thumbColor = theme.primaryColor, activeTrackColor = theme.primaryColor)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المسافة الرأسية (أعلى/أسفل):", fontSize = 11.sp, color = Color.DarkGray)
                                Text("${marginVerticalDp.toInt()} dp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                            }
                            Slider(
                                value = marginVerticalDp,
                                onValueChange = { marginVerticalDp = it },
                                valueRange = 0f..40f,
                                colors = SliderDefaults.colors(thumbColor = theme.primaryColor, activeTrackColor = theme.primaryColor)
                            )
                        }
                    }

                    // Alignment & Free Drag Presets
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("محاذاة وتموضع الصورة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AlignmentOptionButton(
                                    title = "وسط",
                                    isSelected = alignment == "CENTER",
                                    theme = theme,
                                    modifier = Modifier.weight(1f)
                                ) { alignment = "CENTER" }

                                AlignmentOptionButton(
                                    title = "يمين",
                                    isSelected = alignment == "START",
                                    theme = theme,
                                    modifier = Modifier.weight(1f)
                                ) { alignment = "START" }

                                AlignmentOptionButton(
                                    title = "يسار",
                                    isSelected = alignment == "END",
                                    theme = theme,
                                    modifier = Modifier.weight(1f)
                                ) { alignment = "END" }

                                AlignmentOptionButton(
                                    title = "سحب حر",
                                    isSelected = alignment == "FREE",
                                    theme = theme,
                                    modifier = Modifier.weight(1f)
                                ) { alignment = "FREE" }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "إزاحة السحب الحالية: X=${image.offsetX.toInt()}px, Y=${image.offsetY.toInt()}px",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                TextButton(
                                    onClick = onResetPosition,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("إعادة للمنتصف", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // Rotation & Angle
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("دوران الصورة (Rotation): ${rotationDeg.toInt()}°", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row {
                                    IconButton(
                                        onClick = { rotationDeg = (rotationDeg - 90f + 360f) % 360f },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.RotateLeft, contentDescription = "دوران يسار")
                                    }
                                    IconButton(
                                        onClick = { rotationDeg = (rotationDeg + 90f) % 360f },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.RotateRight, contentDescription = "دوران يمين")
                                    }
                                    IconButton(
                                        onClick = { rotationDeg = 0f },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = "تصفير")
                                    }
                                }
                            }
                            Slider(
                                value = rotationDeg,
                                onValueChange = { rotationDeg = it },
                                valueRange = -180f..180f,
                                colors = SliderDefaults.colors(thumbColor = theme.primaryColor, activeTrackColor = theme.primaryColor)
                            )
                        }
                    }

                    // Aesthetics: Corner Radius, Shadow, Opacity
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("تنسيق ومظهر الحواف والظلال:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            // Corner Radius
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("انحناء الزوايا (Radius):", fontSize = 11.sp)
                                Text("${cornerRadiusDp.toInt()} dp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                            }
                            Slider(
                                value = cornerRadiusDp,
                                onValueChange = { cornerRadiusDp = it },
                                valueRange = 0f..40f
                            )

                            // Opacity
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الشفافية (Opacity):", fontSize = 11.sp)
                                Text("${(opacity * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                            }
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                valueRange = 0.2f..1.0f
                            )

                            // Border Thickness
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("سماكة الإطار (Border):", fontSize = 11.sp)
                                Text("${borderThicknessDp.toInt()} dp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                            }
                            Slider(
                                value = borderThicknessDp,
                                onValueChange = { borderThicknessDp = it },
                                valueRange = 0f..8f
                            )
                        }
                    }

                    // Caption / Description
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("التعليق أو الشرح أسفل الصورة (Caption)") },
                        placeholder = { Text("مثال: الشكل 1: خريطة المعارك التاريخية...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف الصورة", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val updated = image.copy(
                                widthDp = widthDp,
                                heightDp = heightDp,
                                rotationDeg = rotationDeg,
                                marginHorizontalDp = marginHorizontalDp,
                                marginVerticalDp = marginVerticalDp,
                                cornerRadiusDp = cornerRadiusDp,
                                borderThicknessDp = borderThicknessDp,
                                borderColorHex = borderColorHex,
                                elevationDp = elevationDp,
                                opacity = opacity,
                                caption = caption,
                                alignment = alignment
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تطبيق التعديلات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSizeButton(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(title, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun AlignmentOptionButton(
    title: String,
    isSelected: Boolean,
    theme: AppThemeData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) theme.primaryColor else Color.White,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) theme.primaryColor else Color.LightGray
        )
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color.DarkGray
            )
        }
    }
}
