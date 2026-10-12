package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeData
import com.example.data.model.SharedProjectPermissions
import com.example.ui.MainViewModel

@Composable
fun ProjectShareDialog(
    theme: AppThemeData,
    projectName: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var allowReuseTemplate by remember { mutableStateOf(true) }
    var allowEditCopy by remember { mutableStateOf(true) }
    var allowDownload by remember { mutableStateOf(true) }
    var allowRepublish by remember { mutableStateOf(false) }

    val shareLink = "https://kateeb.app/p/${projectName.hashCode().toString().takeLast(6)}"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text("مشاركة المشروع وصلاحيات النشر", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.textColor)
                            Text(projectName, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = Color.Black.copy(alpha = 0.06f))

                Text(
                    text = "حدد صلاحيات القبول أو الرفض للآخرين عند مشاركة هذا العمل:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )

                // 1- السماح للمستخدمين بإعادة استخدام القالب
                SharePermissionToggleRow(
                    title = "1- السماح للمستخدمين بإعادة استخدام القالب",
                    subtitle = "يمكن للآخرين إنشاء مشاريع جديدة بناءً على تصميمك",
                    checked = allowReuseTemplate,
                    onCheckedChange = { allowReuseTemplate = it },
                    theme = theme
                )

                // 2- السماح للمستخدمين بالتعديل على النسخة التي قمت بمشاركتها
                SharePermissionToggleRow(
                    title = "2- السماح بالتعديل على النسخة المشاركة",
                    subtitle = "تمكين وضع التحرير التشاركي للمستند",
                    checked = allowEditCopy,
                    onCheckedChange = { allowEditCopy = it },
                    theme = theme
                )

                // 3- السماح للمستخدمين بالتحميل
                SharePermissionToggleRow(
                    title = "3- السماح للمستخدمين بالتحميل",
                    subtitle = "تنزيل نسخة PDF أو Word أو EPUB مباشرة",
                    checked = allowDownload,
                    onCheckedChange = { allowDownload = it },
                    theme = theme
                )

                // 4- إمكانية إعادة نشر هذا الملف من قِبل الآخرين
                SharePermissionToggleRow(
                    title = "4- إمكانية إعادة نشر هذا الملف من قِبل الآخرين",
                    subtitle = "إتاحة تداول الملف ونشره في صفحات المجتمع العامة",
                    checked = allowRepublish,
                    onCheckedChange = { allowRepublish = it },
                    theme = theme
                )

                Divider(color = Color.Black.copy(alpha = 0.06f))

                // Share Link Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(shareLink, fontSize = 11.sp, color = theme.primaryColor, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(shareLink))
                            Toast.makeText(context, "تم نسخ رابط المشاركة إلى الحافظة!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الرابط", tint = theme.primaryColor)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val perms = SharedProjectPermissions(
                                allowReuseTemplate = allowReuseTemplate,
                                allowEditCopy = allowEditCopy,
                                allowDownload = allowDownload,
                                allowRepublish = allowRepublish
                            )
                            viewModel.shareProjectWithPermissions(projectName, perms)
                            Toast.makeText(context, "تم تطبيق إعدادات المشاركة بنجاح!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نشر ومشاركة")
                    }
                }
            }
        }
    }
}

@Composable
fun SharePermissionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    theme: AppThemeData
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (checked) Color(0xFFF0FDF4) else Color(0xFFF9FAFB)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (checked) Color(0xFFBBF7D0) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (checked) Color(0xFF166534) else Color(0xFF334155)
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = theme.primaryColor,
                    checkedTrackColor = theme.primaryColor.copy(alpha = 0.5f)
                )
            )
        }
    }
}
