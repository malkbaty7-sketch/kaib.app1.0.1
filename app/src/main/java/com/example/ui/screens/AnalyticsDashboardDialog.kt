package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppThemeData
import com.example.data.model.DailyWritingAnalyticsEntity
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardDialog(
    theme: AppThemeData,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentDayData by viewModel.todayAnalytics.collectAsState()
    val allHistory by viewModel.analyticsHistory.collectAsState()
    var isEditingGoal by remember { mutableStateOf(false) }
    var tempGoalInput by remember { mutableStateOf(currentDayData.targetWordGoal.toString()) }

    val recentDays = remember(allHistory) {
        if (allHistory.isEmpty()) {
            listOf(currentDayData)
        } else {
            allHistory.take(7).reversed()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D9488)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "إحصائيات وتحليلات الإنتاجية 📊",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                            Text(
                                text = "تتبع سرعة التأليف، الأهداف اليومية، وأيام الالتزام",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Streak Banner (أيام الالتزام المتتالية)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔥", fontSize = 22.sp)
                            }
                            Column {
                                Text(
                                    text = "سلسلة الالتزام: ${currentDayData.streakDays} أيام متواصلة!",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    text = "استمر في الكتابة يومياً للحفاظ على الزخم الإبداعي",
                                    fontSize = 10.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFFF59E0B),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ملتزم ⭐",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Daily Goal Progress Card
                val progressFraction = (currentDayData.netWords.toFloat() / currentDayData.targetWordGoal.toFloat()).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = progressFraction,
                    animationSpec = tween(durationMillis = 800),
                    label = "goalProgress"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("هدف الكلمات اليومي 🎯", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                                Text("${currentDayData.netWords} من ${currentDayData.targetWordGoal} كلمة", fontSize = 11.sp, color = Color.Gray)
                            }
                            TextButton(onClick = { isEditingGoal = !isEditingGoal }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEditingGoal) "إلغاء" else "تعديل الهدف", fontSize = 11.sp)
                            }
                        }

                        if (isEditingGoal) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = tempGoalInput,
                                    onValueChange = { tempGoalInput = it },
                                    label = { Text("الهدف اليومي (كلمة)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Button(
                                    onClick = {
                                        val newG = tempGoalInput.toIntOrNull() ?: 2000
                                        viewModel.updateDailyWordGoal(newG)
                                        isEditingGoal = false
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("حفظ")
                                }
                            }
                        }

                        // Linear Progress
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Color(0xFF0D9488),
                            trackColor = Color(0xFFE2E8F0)
                        )

                        Text(
                            text = if (progressFraction >= 1f) "تهانينا! لقد حققت هدف اليوم بالكامل! 🏆" else "متبقي ${currentDayData.targetWordGoal - currentDayData.netWords} كلمة للوصول للهدف",
                            fontSize = 11.sp,
                            fontWeight = if (progressFraction >= 1f) FontWeight.Bold else FontWeight.Normal,
                            color = if (progressFraction >= 1f) Color(0xFF0D9488) else Color.Gray
                        )
                    }
                }

                // Interactive 7-Day Word Output Bar Chart (Task 4.3)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الإنتاجية الأسبوعية (آخر 7 أيام) 📈", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                            Text("الكلمات الصافية / يوم", fontSize = 10.sp, color = Color.Gray)
                        }

                        // Custom Canvas Bar Chart
                        WeeklyOutputBarChart(days = recentDays, targetGoal = currentDayData.targetWordGoal)
                    }
                }

                // Velocity & Productivity Metrics Grid
                Text("مؤشرات الأداء والكتابة:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        icon = Icons.Default.Speed,
                        iconTint = Color(0xFF2563EB),
                        title = "سرعة التأليف",
                        value = "${currentDayData.wordsPerMinute} ك/د",
                        subtitle = "كلمة في الدقيقة (WPM)",
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        icon = Icons.Default.Timer,
                        iconTint = Color(0xFF7C3AED),
                        title = "وقت الجلسات",
                        value = "${currentDayData.sessionMinutes} دقيقة",
                        subtitle = "إجمالي التركيز اليوم",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        icon = Icons.Default.AddCircle,
                        iconTint = Color(0xFF10B981),
                        title = "كلمات مضافة",
                        value = "+${currentDayData.wordsAdded}",
                        subtitle = "أفكار ومسودات جديدة",
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        icon = Icons.Default.WbTwilight,
                        iconTint = Color(0xFFF59E0B),
                        title = "ساعة الذروة",
                        value = "${currentDayData.peakHour}:00",
                        subtitle = "أعلى أوقات نشاطك الذهني",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyOutputBarChart(
    days: List<DailyWritingAnalyticsEntity>,
    targetGoal: Int
) {
    val maxWords = maxOf(targetGoal, days.maxOfOrNull { it.netWords } ?: 2000)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barCount = days.size
                if (barCount == 0) return@Canvas

                val barWidth = (canvasWidth / (barCount * 1.8f)).coerceAtMost(36.dp.toPx())
                val spaceBetween = (canvasWidth - (barWidth * barCount)) / (barCount + 1)

                // Draw target goal dashed line
                val targetY = canvasHeight - ((targetGoal.toFloat() / maxWords.toFloat()) * canvasHeight)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.7f),
                    start = Offset(0f, targetY),
                    end = Offset(canvasWidth, targetY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Draw bars
                days.forEachIndexed { index, day ->
                    val barHeight = ((day.netWords.toFloat() / maxWords.toFloat()) * canvasHeight).coerceAtLeast(10f)
                    val xPos = spaceBetween + index * (barWidth + spaceBetween)
                    val yPos = canvasHeight - barHeight

                    val isGoalMet = day.netWords >= day.targetWordGoal
                    val barBrush = if (isGoalMet) {
                        Brush.verticalGradient(listOf(Color(0xFF0D9488), Color(0xFF14B8A6)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)))
                    }

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(xPos, yPos),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }

        // Days labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEach { day ->
                val daySub = day.dateKey.takeLast(5) // MM-DD
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(daySub, fontSize = 9.sp, color = Color.Gray)
                    Text("${day.netWords}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Text(title, fontSize = 11.sp, color = Color.Gray)
            }
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
            Text(subtitle, fontSize = 9.sp, color = Color.Gray)
        }
    }
}
