package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BookCoverTemplate
import com.example.data.model.CoverStickerLayer
import com.example.data.model.CoverTextLayer
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BookCoverView(
    template: BookCoverTemplate,
    modifier: Modifier = Modifier,
    isBackCover: Boolean = false,
    elevation: Dp = 8.dp,
    showBookCrease: Boolean = true,
    is3DMode: Boolean = template.is3DView,
    selectedElementId: String? = null,
    onSelectElement: ((String?) -> Unit)? = null,
    onMoveTextLayer: ((String, Float, Float) -> Unit)? = null,
    onMoveStickerLayer: ((String, Float, Float) -> Unit)? = null
) {
    val bgColors = template.backgroundColors.mapNotNull {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    }.ifEmpty { listOf(Color(0xFF1E293B), Color(0xFF0F172A)) }

    val titleColor = try { Color(android.graphics.Color.parseColor(template.titleColor)) } catch (e: Exception) { Color.White }
    val subtitleColor = try { Color(android.graphics.Color.parseColor(template.subtitleColor)) } catch (e: Exception) { Color(0xFFE2E8F0) }
    val authorColor = try { Color(android.graphics.Color.parseColor(template.authorColor)) } catch (e: Exception) { Color(0xFFF8FAFC) }
    val accentColor = try { Color(android.graphics.Color.parseColor(template.accentColor)) } catch (e: Exception) { Color(0xFFF59E0B) }

    // Angled Gradient Brush
    val bgBrush = remember(bgColors, template.gradientAngle, template.backgroundType) {
        if (template.backgroundType == "SOLID_COLOR" || bgColors.size == 1) {
            Brush.verticalGradient(listOf(bgColors.first(), bgColors.first()))
        } else {
            val rad = Math.toRadians(template.gradientAngle.toDouble())
            val x = cos(rad).toFloat()
            val y = sin(rad).toFloat()
            val start = Offset(if (x < 0) -x * 500f else 0f, if (y < 0) -y * 800f else 0f)
            val end = Offset(if (x >= 0) x * 500f else 0f, if (y >= 0) y * 800f else 0f)
            Brush.linearGradient(
                colors = bgColors,
                start = start,
                end = end
            )
        }
    }

    // Outer 3D Transformation Container
    Box(
        modifier = modifier
            .then(
                if (is3DMode) {
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .graphicsLayer {
                            rotationY = -18f
                            rotationX = 3f
                            cameraDistance = 14f * density
                            shadowElevation = 20.dp.toPx()
                            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp)
                            clip = false
                        }
                } else {
                    Modifier
                }
            )
            .aspectRatio(0.68f),
        contentAlignment = Alignment.Center
    ) {
        // Underneath 3D Book Spine Shadow / Pages Stack when in 3D Mode
        if (is3DMode) {
            // Pages edge layer on the right
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.97f)
                    .width(16.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF94A3B8))
                        ),
                        RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
                    )
            )
        }

        CardBookContainer(
            modifier = Modifier.fillMaxSize(),
            elevation = if (is3DMode) 16.dp else elevation
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBrush)
            ) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                // Image background if provided
                if (!template.imageUri.isNullOrBlank()) {
                    val imageModel = if (template.imageUri.startsWith("/") || template.imageUri.startsWith("file://")) {
                        File(template.imageUri.removePrefix("file://"))
                    } else {
                        template.imageUri
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = template.bookTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = template.imageOpacity.coerceIn(0.05f, 1.0f)
                            }
                    )

                    // Optional dark vignette overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                }

                // Decorative inner border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                // Standard Base Layout (Front or Back Cover)
                if (!isBackCover) {
                    FrontCoverContent(
                        template = template,
                        titleColor = titleColor,
                        subtitleColor = subtitleColor,
                        authorColor = authorColor,
                        accentColor = accentColor
                    )
                } else {
                    BackCoverContent(
                        template = template,
                        titleColor = titleColor,
                        subtitleColor = subtitleColor,
                        authorColor = authorColor,
                        accentColor = accentColor
                    )
                }

                // Render Free-form Text Layers
                template.textLayers.forEach { textLayer ->
                    val isSelected = selectedElementId == textLayer.id
                    val textClr = try { Color(android.graphics.Color.parseColor(textLayer.color)) } catch (e: Exception) { Color.White }
                    val shadowClr = try { Color(android.graphics.Color.parseColor(textLayer.shadowColor)) } catch (e: Exception) { Color.Black }
                    val strokeClr = try { Color(android.graphics.Color.parseColor(textLayer.strokeColor)) } catch (e: Exception) { Color.Black }

                    val xOffset = boxWidth * textLayer.xRatio
                    val yOffset = boxHeight * textLayer.yRatio

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset - 60.dp, y = yOffset - 20.dp)
                            .graphicsLayer {
                                rotationZ = textLayer.rotation
                                scaleX = textLayer.scale
                                scaleY = textLayer.scale
                            }
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                                } else Modifier
                            )
                            .clickable { onSelectElement?.invoke(textLayer.id) }
                            .then(
                                if (onMoveTextLayer != null) {
                                    Modifier.pointerInput(textLayer.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val pWidth = this@pointerInput.size.width.toFloat().coerceAtLeast(1f)
                                            val pHeight = this@pointerInput.size.height.toFloat().coerceAtLeast(1f)
                                            val newX = (textLayer.xRatio + dragAmount.x / pWidth).coerceIn(0.05f, 0.95f)
                                            val newY = (textLayer.yRatio + dragAmount.y / pHeight).coerceIn(0.05f, 0.95f)
                                            onMoveTextLayer(textLayer.id, newX, newY)
                                        }
                                    }
                                } else Modifier
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = textLayer.text,
                            color = textClr,
                            fontSize = (textLayer.fontSize * 0.75f).sp,
                            fontWeight = if (textLayer.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (textLayer.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = if (textLayer.hasShadow) Shadow(color = shadowClr, blurRadius = 8f, offset = Offset(2f, 2f)) else null
                            )
                        )
                    }
                }

                // Render Sticker & Badge Layers
                template.stickerLayers.forEach { sticker ->
                    val isSelected = selectedElementId == sticker.id
                    val stClr = try { Color(android.graphics.Color.parseColor(sticker.color)) } catch (e: Exception) { Color(0xFFF59E0B) }

                    val xOffset = boxWidth * sticker.xRatio
                    val yOffset = boxHeight * sticker.yRatio

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset - 35.dp, y = yOffset - 35.dp)
                            .graphicsLayer {
                                rotationZ = sticker.rotation
                                scaleX = sticker.scale
                                scaleY = sticker.scale
                                alpha = sticker.opacity
                            }
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.5.dp, Color(0xFFF43F5E), RoundedCornerShape(12.dp))
                                } else Modifier
                            )
                            .clickable { onSelectElement?.invoke(sticker.id) }
                            .then(
                                if (onMoveStickerLayer != null) {
                                    Modifier.pointerInput(sticker.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val pWidth = this@pointerInput.size.width.toFloat().coerceAtLeast(1f)
                                            val pHeight = this@pointerInput.size.height.toFloat().coerceAtLeast(1f)
                                            val newX = (sticker.xRatio + dragAmount.x / pWidth).coerceIn(0.05f, 0.95f)
                                            val newY = (sticker.yRatio + dragAmount.y / pHeight).coerceIn(0.05f, 0.95f)
                                            onMoveStickerLayer(sticker.id, newX, newY)
                                        }
                                    }
                                } else Modifier
                            )
                    ) {
                        StickerBadgeView(sticker = sticker, color = stClr)
                    }
                }

                // Realistic book spine crease / shadow on left edge
                if (showBookCrease) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(if (is3DMode) 24.dp else 18.dp)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = if (is3DMode) 0.55f else 0.40f),
                                        Color.White.copy(alpha = if (is3DMode) 0.25f else 0.15f),
                                        Color.Black.copy(alpha = 0.20f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // 3D Lighting Sheen Overlay
                if (is3DMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.25f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(400f, 600f)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun StickerBadgeView(sticker: CoverStickerLayer, color: Color) {
    when (sticker.type) {
        "BEST_SELLER" -> {
            Surface(
                color = color,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text(sticker.label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        "AWARD" -> {
            Surface(
                color = color,
                shape = CircleShape,
                shadowElevation = 6.dp,
                modifier = Modifier.size(46.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(sticker.label, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        "SPECIAL_EDITION" -> {
            Surface(
                color = color.copy(alpha = 0.9f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                shadowElevation = 3.dp
            ) {
                Text(
                    text = sticker.label,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        "BARCODE" -> {
            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Text("ISBN 978-0-1234", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        else -> {
            Surface(
                color = color,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text(sticker.label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CardBookContainer(
    modifier: Modifier = Modifier,
    elevation: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp))
    ) {
        content()
    }
}

@Composable
private fun FrontCoverContent(
    template: BookCoverTemplate,
    titleColor: Color,
    subtitleColor: Color,
    authorColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header / Ornament
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (template.publisherName.isNotBlank()) {
                Text(
                    text = template.publisherName,
                    color = subtitleColor.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Center: Main Title & Subtitle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = template.bookTitle,
                color = titleColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (template.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                // Decorative divider
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = template.subtitle,
                    color = subtitleColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom: Author Name
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = template.authorName,
                color = authorColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "تأليف وإعداد",
                color = subtitleColor.copy(alpha = 0.7f),
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BackCoverContent(
    template: BookCoverTemplate,
    titleColor: Color,
    subtitleColor: Color,
    authorColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Book title reminder
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = template.bookTitle,
                color = titleColor.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(1.5.dp)
                    .background(accentColor)
            )
        }

        // Center: Synopsis / Blurb
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = template.backSynopsis.ifBlank { "نبذة موجزة عن الكتاب وأهم محاوره الفكرية والأدبية..." },
                color = subtitleColor,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            if (template.backAuthorBio.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = template.backAuthorBio,
                    color = authorColor.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom: Barcode / ISBN placeholder & Publisher
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "ISBN Barcode",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "ISBN 978-9960-01-234-5",
                        fontSize = 8.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "إصدار رسمي رقمي ومطبوع",
                        fontSize = 7.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}
