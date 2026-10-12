package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PresetIllustrationInfo(
    val id: String,
    val title: String,
    val category: String,
    val description: String
)

object PresetIllustrationsCatalog {
    val items = listOf(
        PresetIllustrationInfo(
            id = "preset:islamic_border",
            title = "زخرفة وإطار أندلسي",
            category = "زخارف وأطر",
            description = "إطار هندسي إسلامي بنمط التوريق والتناظر الكلاسيكي"
        ),
        PresetIllustrationInfo(
            id = "preset:quill_parchment",
            title = "ريشة حبر ومخطوطة",
            category = "أدب ورواية",
            description = "رمز الريشة الأدبية ومخطوطة الكتابة التراثية"
        ),
        PresetIllustrationInfo(
            id = "preset:academic_chart",
            title = "مخطط بحثي تحليلي",
            category = "أكاديمي وبحثي",
            description = "رسم بياني علمي مناسب للرسائل والأبحاث التوثيقية"
        ),
        PresetIllustrationInfo(
            id = "preset:historic_seal",
            title = "ختم ملكي وتوثيق",
            category = "تاريخ وتوثيق",
            description = "شارة شمعية مذهبة مع إكليل غار للتتويج والتوثيق"
        ),
        PresetIllustrationInfo(
            id = "preset:knowledge_lamp",
            title = "قنديل الحكمة والمعرفة",
            category = "فلسفة وفكر",
            description = "مصباح إنارة فكرية يرمز للبصيرة والتأمل"
        ),
        PresetIllustrationInfo(
            id = "preset:geometric_mandala",
            title = "نجمة هندسية مثمنة",
            category = "زخارف وأطر",
            description = "تصميم هندسي متناظر مستوحى من العمارة الإسلامية"
        ),
        PresetIllustrationInfo(
            id = "preset:quote_callout",
            title = "شريط تنويه واقتباس",
            category = "أدب ورواية",
            description = "إطار فني جذاب لإبراز المقاطع المؤثرة والأقوال"
        ),
        PresetIllustrationInfo(
            id = "preset:tree_of_wisdom",
            title = "شجرة الفكر والأصل",
            category = "فلسفة وفكر",
            description = "شجرة متفرعة ترمز لأنساب الأفكار والنمو المعرفي"
        )
    )
}

@Composable
fun PresetIllustrationView(
    presetId: String,
    modifier: Modifier = Modifier,
    tintColor: Color = Color(0xFF8B5E3C)
) {
    when (presetId) {
        "preset:islamic_border" -> IslamicBorderGraphic(modifier, tintColor)
        "preset:quill_parchment" -> QuillParchmentGraphic(modifier, tintColor)
        "preset:academic_chart" -> AcademicChartGraphic(modifier, tintColor)
        "preset:historic_seal" -> HistoricSealGraphic(modifier, tintColor)
        "preset:knowledge_lamp" -> KnowledgeLampGraphic(modifier, tintColor)
        "preset:geometric_mandala" -> GeometricMandalaGraphic(modifier, tintColor)
        "preset:quote_callout" -> QuoteCalloutGraphic(modifier, tintColor)
        "preset:tree_of_wisdom" -> TreeOfWisdomGraphic(modifier, tintColor)
        else -> DefaultPlaceholderGraphic(modifier, tintColor)
    }
}

@Composable
fun IslamicBorderGraphic(modifier: Modifier, tint: Color) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFBF8F2), Color(0xFFF3E9DA))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = 2.dp.toPx()

            // Outer decorative frame
            drawRect(
                color = tint,
                size = Size(w, h),
                style = Stroke(width = strokeWidth)
            )

            // Inner margin rect
            val inset = 8.dp.toPx()
            drawRect(
                color = tint.copy(alpha = 0.5f),
                topLeft = Offset(inset, inset),
                size = Size(w - (inset * 2), h - (inset * 2)),
                style = Stroke(width = 1.dp.toPx())
            )

            // Corner ornamental diamonds
            val cornerSize = 12.dp.toPx()
            fun drawDiamond(cx: Float, cy: Float) {
                val path = Path().apply {
                    moveTo(cx, cy - cornerSize)
                    lineTo(cx + cornerSize, cy)
                    lineTo(cx, cy + cornerSize)
                    lineTo(cx - cornerSize, cy)
                    close()
                }
                drawPath(path, color = tint)
            }

            drawDiamond(inset, inset)
            drawDiamond(w - inset, inset)
            drawDiamond(inset, h - inset)
            drawDiamond(w - inset, h - inset)

            // Center rosette
            drawCircle(
                color = tint.copy(alpha = 0.25f),
                radius = minOf(w, h) * 0.22f,
                center = Offset(w / 2, h / 2),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = tint,
                radius = 4.dp.toPx(),
                center = Offset(w / 2, h / 2)
            )
        }
        Text(
            text = "❖ زخرفة وتوريق أندلسي ❖",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
fun QuillParchmentGraphic(modifier: Modifier, tint: Color) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF8E7), Color(0xFFF5E6CA))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.HistoryEdu,
                contentDescription = "ريشة ومخطوطة",
                tint = tint,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "مخطوطة أدبية",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                text = "«نُونْ وَالقَلَمِ وَمَا يَسْطُرُون»",
                fontSize = 10.sp,
                color = tint.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun AcademicChartGraphic(modifier: Modifier, tint: Color) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pad = 16.dp.toPx()

            // Axes
            drawLine(
                color = Color.Gray,
                start = Offset(pad, h - pad),
                end = Offset(w - pad, h - pad),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color.Gray,
                start = Offset(pad, pad),
                end = Offset(pad, h - pad),
                strokeWidth = 2.dp.toPx()
            )

            // Bars
            val barCount = 4
            val availableW = w - (pad * 2)
            val barW = availableW / (barCount * 1.8f)
            val step = availableW / barCount
            val heights = listOf(0.4f, 0.75f, 0.55f, 0.9f)

            heights.forEachIndexed { i, factor ->
                val x = pad + (i * step) + (barW * 0.4f)
                val barH = (h - (pad * 2)) * factor
                drawRoundRect(
                    color = tint.copy(alpha = 0.75f + (i * 0.05f)),
                    topLeft = Offset(x, (h - pad) - barH),
                    size = Size(barW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
        }
        Text(
            text = "رسم توضيحي بياني",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF166534),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
        )
    }
}

@Composable
fun HistoricSealGraphic(modifier: Modifier, tint: Color) {
    Box(
        modifier = modifier
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFEF2F2), Color(0xFFFEE2E2))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "ختم موثق",
                tint = Color(0xFFB91C1C),
                modifier = Modifier.size(46.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ختم التوثيق والاعتماد",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF991B1B)
            )
            Text(
                text = "مرجع موثق • نسخة معتمدة",
                fontSize = 9.sp,
                color = Color(0xFF7F1D1D)
            )
        }
    }
}

@Composable
fun KnowledgeLampGraphic(modifier: Modifier, tint: Color) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "قنديل المعرفة",
                tint = Color(0xFFD97706),
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "إشراقة فكرية",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF92400E)
            )
            Text(
                text = "«العلم نور يضيء المسالك»",
                fontSize = 10.sp,
                color = Color(0xFFB45309)
            )
        }
    }
}

@Composable
fun GeometricMandalaGraphic(modifier: Modifier, tint: Color) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFF3E8FF), Color(0xFFE9D5FF))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "نجمة هندسية",
                tint = Color(0xFF7E22CE),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "نجمة هندسية متناظرة",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B21A8)
            )
        }
    }
}

@Composable
fun QuoteCalloutGraphic(modifier: Modifier, tint: Color) {
    Box(
        modifier = modifier
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = "اقتباس",
                tint = Color(0xFF1D4ED8),
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = "شريط تنويه واقتباس بارز",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E40AF)
            )
            Text(
                text = "«خير جليس في الزمان كتاب»",
                fontSize = 10.sp,
                color = Color(0xFF1E3A8A)
            )
        }
    }
}

@Composable
fun TreeOfWisdomGraphic(modifier: Modifier, tint: Color) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Park,
                contentDescription = "شجرة الفكر",
                tint = Color(0xFF047857),
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "شجرة المعرفة والأصول",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF065F46)
            )
        }
    }
}

@Composable
fun DefaultPlaceholderGraphic(modifier: Modifier, tint: Color) {
    Box(
        modifier = modifier
            .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = "صورة",
            tint = tint,
            modifier = Modifier.size(40.dp)
        )
    }
}
