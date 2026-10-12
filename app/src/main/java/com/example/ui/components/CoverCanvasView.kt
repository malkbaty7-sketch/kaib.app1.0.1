package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeData
import com.example.data.model.BookCoverTemplate

@Composable
fun CoverCanvasView(
    template: BookCoverTemplate,
    isBackCover: Boolean,
    theme: AppThemeData,
    onEditClick: () -> Unit,
    onOpenStudioClick: () -> Unit,
    onDeleteCover: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isBackCover) Icons.Default.Bookmark else Icons.Default.Book,
                        contentDescription = null,
                        tint = theme.primaryColor
                    )
                    Column {
                        Text(
                            text = if (isBackCover) "صفحة الغلاف الخلفي (نهاية الكتاب)" else "صفحة الغلاف الأمامي (أول صفحة في الكتاب)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                        Text(
                            text = if (isBackCover) "يتضمن نبذة الكتاب والباركود وسيرة المؤلف" else "يتضمن عنوان الكتاب واسم المؤلف ودار النشر",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(onClick = onDeleteCover) {
                    Icon(Icons.Default.Delete, contentDescription = "إزالة الغلاف", tint = Color(0xFFDC2626))
                }
            }

            // Center: Book Cover Realistic Display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                BookCoverView(
                    template = template,
                    isBackCover = isBackCover,
                    modifier = Modifier
                        .fillMaxHeight(0.92f)
                        .wrapContentWidth()
                )
            }

            // Bottom Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تعديل الكتابة والألوان", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenStudioClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استوديو النماذج", fontSize = 12.sp)
                }
            }
        }
    }
}
