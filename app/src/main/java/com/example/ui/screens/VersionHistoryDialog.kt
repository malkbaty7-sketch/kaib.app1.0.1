package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeData
import com.example.data.model.Project
import com.example.data.model.ProjectSnapshot
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VersionHistoryDialog(
    project: Project,
    snapshots: List<ProjectSnapshot>,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onCreateSnapshot: (label: String) -> Unit,
    onRestoreSnapshot: (ProjectSnapshot) -> Unit,
    onDeleteSnapshot: (Int) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Icon(Icons.Default.History, contentDescription = null, tint = theme.primaryColor)
                        Text(
                            text = "سجل النسخ الاحتياطية والتاريخ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "احفظ لقطات زمنية (Snapshots) للرجوع إليها في أي وقت أو استعادة المحتوى السابق.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ نسخة احتياطية للوضع الحالي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Snapshots List
                if (snapshots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF9F9F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.RestorePage, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                            Text("لا توجد نسخ محفوظة بعد", fontSize = 13.sp, color = Color.Gray)
                            Text("أنشئ أول نقطة استرجاع لتأمين عملك الأدبي", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(snapshots, key = { it.id }) { snapshot ->
                            SnapshotItemCard(
                                snapshot = snapshot,
                                theme = theme,
                                onRestore = {
                                    onRestoreSnapshot(snapshot)
                                    Toast.makeText(context, "تمت استعادة النسخة بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = {
                                    onDeleteSnapshot(snapshot.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSnapshotDialog(
            theme = theme,
            onDismiss = { showCreateDialog = false },
            onConfirm = { label ->
                onCreateSnapshot(label)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun SnapshotItemCard(
    snapshot: ProjectSnapshot,
    theme: AppThemeData,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date(snapshot.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCFC))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = snapshot.snapshotLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = theme.textColor
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dateStr • ${snapshot.wordCount} كلمة",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                OutlinedButton(
                    onClick = onRestore,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = theme.primaryColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استعادة هذه النسخة", fontSize = 11.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CreateSnapshotDialog(
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var label by remember {
        val nowStr = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())
        mutableStateOf("مسودة $nowStr")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("تسمية النسخة الاحتياطية", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textColor)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (label.isNotBlank()) onConfirm(label) },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ النسخة")
                    }
                }
            }
        }
    }
}
