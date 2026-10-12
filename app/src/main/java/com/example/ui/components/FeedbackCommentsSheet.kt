package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppThemeData
import com.example.data.model.CommentEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackCommentsSheet(
    chapterTitle: String,
    comments: List<CommentEntity>,
    theme: AppThemeData,
    onDismiss: () -> Unit,
    onAddComment: (authorName: String, snippet: String, commentText: String) -> Unit,
    onToggleResolved: (id: Long, currentResolved: Boolean) -> Unit,
    onReplyToComment: (id: Long, replyText: String) -> Unit,
    onDeleteComment: (id: Long) -> Unit
) {
    var filterStatus by remember { mutableStateOf("ALL") } // "ALL", "PENDING", "RESOLVED"
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var replyingCommentId by remember { mutableStateOf<Long?>(null) }
    var replyInput by remember { mutableStateOf("") }

    val filteredComments = remember(comments, filterStatus) {
        when (filterStatus) {
            "PENDING" -> comments.filter { !it.isResolved }
            "RESOLVED" -> comments.filter { it.isResolved }
            else -> comments
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
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
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
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RateReview, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "ملاحظات القراءة التجريبية والمحررين 📝",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                            Text(
                                text = "الفصل: $chapterTitle (${comments.size} ملاحظة)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.3f))

                // Action Bar: Filter & Add Comment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filters
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = filterStatus == "ALL",
                            onClick = { filterStatus = "ALL" },
                            label = { Text("الكل (${comments.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterStatus == "PENDING",
                            onClick = { filterStatus = "PENDING" },
                            label = { Text("قيد المراجعة (${comments.count { !it.isResolved }})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterStatus == "RESOLVED",
                            onClick = { filterStatus = "RESOLVED" },
                            label = { Text("معالجة (${comments.count { it.isResolved }})", fontSize = 11.sp) }
                        )
                    }

                    Button(
                        onClick = { showAddCommentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ إضافة تعليق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Comments List
                if (filteredComments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.MarkChatRead, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                            Text("لا توجد ملاحظات أو تعليقات هنا حالياً", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                            Text("يمكن للمحررين والقراء إضافة ملاحظات جانبية لمراجعة الصياغة والحبكة", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredComments, key = { it.id }) { comment ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (comment.isResolved) 1.dp else 1.5.dp,
                                        color = if (comment.isResolved) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF2563EB).copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (comment.isResolved) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Top Row: Author, Status Badge, Delete
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
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (comment.isResolved) Color(0xFF10B981) else Color(0xFF2563EB)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    if (comment.isResolved) Icons.Default.Check else Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(comment.timestamp))
                                                Text(dateStr, fontSize = 9.sp, color = Color.Gray)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Toggle Resolved
                                            TextButton(
                                                onClick = { onToggleResolved(comment.id, comment.isResolved) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    if (comment.isResolved) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (comment.isResolved) Color(0xFF10B981) else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    if (comment.isResolved) "معالج ✓" else "قيد العمل",
                                                    fontSize = 10.sp,
                                                    color = if (comment.isResolved) Color(0xFF10B981) else Color.Gray
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteComment(comment.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    // Quoted Snippet if any
                                    if (comment.selectedTextRange.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.FormatQuote, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                                                Text(
                                                    text = "«${comment.selectedTextRange}»",
                                                    fontSize = 11.sp,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    color = Color(0xFF92400E)
                                                )
                                            }
                                        }
                                    }

                                    // Main Comment Body
                                    Text(
                                        text = comment.commentText,
                                        fontSize = 12.sp,
                                        color = if (comment.isResolved) Color.Gray else Color.Black,
                                        style = if (comment.isResolved) androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.None) else androidx.compose.ui.text.TextStyle()
                                    )

                                    // Author's Reply if exists
                                    if (comment.replyText.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = theme.primaryColor.copy(alpha = 0.08f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(Icons.Default.Reply, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(14.dp))
                                                    Text("رد المؤلف:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = theme.primaryColor)
                                                }
                                                Text(comment.replyText, fontSize = 11.sp, color = theme.textColor)
                                            }
                                        }
                                    }

                                    // Reply button / inline box
                                    if (replyingCommentId == comment.id) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = replyInput,
                                                onValueChange = { replyInput = it },
                                                placeholder = { Text("اكتب ردك على الملاحظة...", fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            Button(
                                                onClick = {
                                                    if (replyInput.isNotBlank()) {
                                                        onReplyToComment(comment.id, replyInput.trim())
                                                        replyInput = ""
                                                        replyingCommentId = null
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                                            ) {
                                                Text("إرسال", fontSize = 11.sp)
                                            }
                                        }
                                    } else if (comment.replyText.isBlank()) {
                                        TextButton(
                                            onClick = {
                                                replyingCommentId = comment.id
                                                replyInput = ""
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("رد الكاتب على الملاحظة", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add New Comment
    if (showAddCommentDialog) {
        var author by remember { mutableStateOf("قارئ تجريبي") }
        var quoteText by remember { mutableStateOf("") }
        var commentBody by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddCommentDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("إضافة ملاحظة قراءة ومراجعة ✍️", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textColor)
                    Text("سجل اقتراح تعديل أو تصحيح أو نقد أدبي حول فقرة معينة في هذا الفصل.", fontSize = 11.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("اسم المراجع / القارئ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = quoteText,
                        onValueChange = { quoteText = it },
                        label = { Text("النص المقتبس أو الفقرة المستهدفة (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = commentBody,
                        onValueChange = { commentBody = it },
                        label = { Text("الملاحظة أو الاقتراح") },
                        placeholder = { Text("مثال: الجملة تحتاج تقوية في الوصف، أو يوجد تناقض مع بداية المشهد...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddCommentDialog = false }) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (commentBody.isNotBlank()) {
                                    onAddComment(author.ifBlank { "محرر" }, quoteText.trim(), commentBody.trim())
                                    showAddCommentDialog = false
                                }
                            },
                            enabled = commentBody.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                        ) {
                            Text("حفظ الملاحظة")
                        }
                    }
                }
            }
        }
    }
}
