package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppThemeData
import com.example.util.IssueSeverity
import com.example.util.PreflightIssue
import com.example.util.PreflightReport

@Composable
fun PreflightValidationDialog(
    report: PreflightReport,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onProceedExportEpub: () -> Unit,
    onProceedExportPdf: () -> Unit
) {
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
                // Top bar
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
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "فحص الجودة والتوافق للنشر (Pre-flight) ✈️",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                            Text(
                                text = "التحقق من الامتثال لمعايير Amazon KDP و Apple Books",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Overall Score Badge
                val isReady = report.isReadyForKdp
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isReady) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isReady) Color(0xFF16A34A) else Color(0xFFF59E0B)),
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
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isReady) Color(0xFF16A34A) else Color(0xFFF59E0B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${report.scorePercentage}%",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = if (isReady) "الكتاب جاهز تماماً للنشر الدولي! 🚀" else "يحتاج الكتاب لبعض التحسينات البسيطة ⚠️",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isReady) Color(0xFF166534) else Color(0xFFB45309)
                                )
                                Text(
                                    text = if (isReady) "مطابق لمعايير Amazon KDP و EPUB 3.0" else "راجع التنبيهات الموضحة أدناه قبل التصدير النهائي",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Checklist Section
                Text("تفاصيل تقرير الامتثال والجودة:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.issues.forEach { issue ->
                        IssueItemCard(issue)
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onProceedExportEpub()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير EPUB3 جاهز لـ KDP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onProceedExportPdf()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير PDF جاهز للمطابع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueItemCard(issue: PreflightIssue) {
    val (bgColor, borderColor, icon, iconColor) = when (issue.severity) {
        IssueSeverity.PASS -> Quad(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Icons.Default.CheckCircle, Color(0xFF16A34A))
        IssueSeverity.WARNING -> Quad(Color(0xFFFFFBEB), Color(0xFFFDE68A), Icons.Default.Warning, Color(0xFFD97706))
        IssueSeverity.ERROR -> Quad(Color(0xFFFEF2F2), Color(0xFFFECACA), Icons.Default.Cancel, Color(0xFFDC2626))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(issue.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
                Text(issue.message, fontSize = 10.sp, color = Color(0xFF475569), lineHeight = 15.sp)
                if (issue.fixHint != null) {
                    Text("💡 نصيحة: ${issue.fixHint}", fontSize = 10.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
