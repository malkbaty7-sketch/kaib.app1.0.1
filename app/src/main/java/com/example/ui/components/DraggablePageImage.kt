package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppThemeData
import com.example.data.model.PageImageItem
import kotlin.math.roundToInt

@Composable
fun DraggablePageImage(
    image: PageImageItem,
    theme: AppThemeData,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdateOffset: (Float, Float) -> Unit,
    onOpenSettings: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentOffsetX by remember(image.offsetX) { mutableFloatStateOf(image.offsetX) }
    var currentOffsetY by remember(image.offsetY) { mutableFloatStateOf(image.offsetY) }

    val alignModifier = when (image.alignment) {
        "START" -> Alignment.CenterStart
        "END" -> Alignment.CenterEnd
        "FREE" -> Alignment.TopStart
        else -> Alignment.Center
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = image.marginHorizontalDp.dp,
                vertical = image.marginVerticalDp.dp
            ),
        contentAlignment = alignModifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = currentOffsetX.roundToInt(),
                        y = currentOffsetY.roundToInt()
                    )
                }
                .graphicsLayer {
                    rotationZ = image.rotationDeg
                    alpha = image.opacity
                }
                .pointerInput(image.id) {
                    detectDragGestures(
                        onDragStart = {
                            onSelect()
                        },
                        onDragEnd = {
                            onUpdateOffset(currentOffsetX, currentOffsetY)
                        },
                        onDragCancel = {
                            onUpdateOffset(currentOffsetX, currentOffsetY)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOffsetX += dragAmount.x
                            currentOffsetY += dragAmount.y
                        }
                    )
                }
        ) {
            // Quick Floating Toolbar on Top when Selected
            if (isSelected) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Drag Indicator icon
                        Icon(
                            Icons.Default.OpenWith,
                            contentDescription = "اسحب للتحريك في أي اتجاه",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "اسحب للتحريك",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Settings / Controls Button
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "تعديل الأبعاد والمسافات",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Delete Button
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف الصورة",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Image Container Card
            Box(
                modifier = Modifier
                    .size(width = image.widthDp.dp, height = image.heightDp.dp)
                    .shadow(
                        elevation = image.elevationDp.dp,
                        shape = RoundedCornerShape(image.cornerRadiusDp.dp)
                    )
                    .clip(RoundedCornerShape(image.cornerRadiusDp.dp))
                    .then(
                        if (image.borderThicknessDp > 0f) {
                            val borderColor = try {
                                Color(android.graphics.Color.parseColor(image.borderColorHex))
                            } catch (e: Exception) {
                                Color.DarkGray
                            }
                            Modifier.border(
                                width = image.borderThicknessDp.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(image.cornerRadiusDp.dp)
                            )
                        } else Modifier
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 2.dp,
                                color = theme.primaryColor,
                                shape = RoundedCornerShape(image.cornerRadiusDp.dp)
                            )
                        } else Modifier
                    )
                    .clickable { onSelect() }
            ) {
                if (image.sourceUri.startsWith("preset:")) {
                    PresetIllustrationView(
                        presetId = image.sourceUri,
                        modifier = Modifier.fillMaxSize(),
                        tintColor = theme.primaryColor
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(image.sourceUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = image.title.ifBlank { "صورة مدرجة" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Caption Text
            if (image.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = image.caption,
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.widthIn(max = image.widthDp.dp)
                )
            }
        }
    }
}
