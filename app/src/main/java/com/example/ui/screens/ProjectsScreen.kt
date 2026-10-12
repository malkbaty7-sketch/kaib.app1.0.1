package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.data.model.defaultWritingTemplates
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectsScreen(
    viewModel: MainViewModel,
    onOpenEditor: (Project) -> Unit,
    onOpenDeviceDocuments: () -> Unit = {}
) {
    val projects by viewModel.allProjects.collectAsState()
    val theme by viewModel.currentTheme.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(theme.elevationDp, RoundedCornerShape(theme.cornerRadiusDp), spotColor = theme.shadowColor),
                shape = RoundedCornerShape(theme.cornerRadiusDp),
                colors = CardDefaults.cardColors(containerColor = theme.surfaceColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Text(
                            text = "مكتبة المشاريع والكتب (FR-08, FR-09)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                        Text(
                            text = "إدارة وتأليف الكتب الإلكترونية والأبحاث والمستندات",
                            fontSize = 11.sp,
                            color = theme.textColor.copy(alpha = 0.75f)
                        )
                    }
                }

                Button(
                    onClick = { showNewProjectDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشروع جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Device Documents & External Library Section (Task 1)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDeviceDocuments() }
                .border(1.dp, theme.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("مستندات الجهاز والمكتبة الخارجية 📂", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF065F46))
                        Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(4.dp)) {
                            Text("جديد", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                    Text("تصفح وقراءة الكتب الرقمية (PDF, EPUB) ومستندات Word وExcel مع حفظ موضع القراءة تلقائياً", fontSize = 11.sp, color = Color(0xFF047857), lineHeight = 15.sp)
                }
                Icon(Icons.Default.ArrowForwardIos, contentDescription = "فتح", tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
            }
        }

        // Projects Grid/List
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.6f))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = theme.primaryColor.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "لا توجد مشاريع كتابة حالياً",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "اضغط على 'مشروع جديد' أو استخلص نصوصاً من مصادرك لبدء كتابك الأول",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { showNewProjectDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إنشاء مشروع الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCardItem(
                        project = project,
                        theme = theme,
                        onOpen = {
                            viewModel.loadProjectIntoEditor(project)
                            onOpenEditor(project)
                        },
                        onDelete = { viewModel.deleteProject(project.id) }
                    )
                }
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            theme = theme,
            onDismiss = { showNewProjectDialog = false },
            onCreate = { title, type, templateId, emotions ->
                showNewProjectDialog = false
                viewModel.createProject(
                    title = title,
                    type = type,
                    templateId = templateId,
                    emotions = emotions,
                    onCreated = { created ->
                        onOpenEditor(created)
                    }
                )
            }
        )
    }
}

@Composable
fun ProjectCardItem(
    project: Project,
    theme: com.example.data.model.AppThemeData,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val template = defaultWritingTemplates.firstOrNull { it.id == project.writingTemplateId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(theme.elevationDp / 2, RoundedCornerShape(16.dp), spotColor = theme.shadowColor)
            .border(1.dp, theme.primaryColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val typeLabel = when (project.projectType) {
                        "E_BOOK" -> "كتاب إلكتروني"
                        "RESEARCH_PAPER" -> "بحث علمي"
                        else -> "مستند عام"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(theme.primaryColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }

                    if (template != null) {
                        Text(
                            text = "• ${template.name}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }

            Text(
                text = project.projectName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                Text(
                    text = "آخر تعديل: ${dateFormat.format(Date(project.lastModified))} | المقاس: ${project.paperSize}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح في المحرر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
