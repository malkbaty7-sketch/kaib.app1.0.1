package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeData
import com.example.data.model.defaultAppThemes
import com.example.ui.MainViewModel

@Composable
fun ThemesScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val currentThemeId by viewModel.currentThemeId.collectAsState()
    val activeTheme by viewModel.currentTheme.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(activeTheme.elevationDp, RoundedCornerShape(activeTheme.cornerRadiusDp), spotColor = activeTheme.shadowColor),
            shape = RoundedCornerShape(activeTheme.cornerRadiusDp),
            colors = CardDefaults.cardColors(containerColor = activeTheme.surfaceColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(activeTheme.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White)
                }
                Column {
                    Text(
                        text = "التخصيص البصري والثيمات ثلاثية الأبعاد (FR-UI-01)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTheme.textColor
                    )
                    Text(
                        text = "3 ثيمات دافئة مدروسة تقلل إجهاد العين مع تأثيرات العمق 3D",
                        fontSize = 11.sp,
                        color = activeTheme.textColor.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // Theme Cards
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(defaultAppThemes, key = { it.id }) { themeItem ->
                val isSelected = currentThemeId == themeItem.id
                ThemeSelectionCard(
                    themeItem = themeItem,
                    isSelected = isSelected,
                    onSelect = {
                        viewModel.currentThemeId.value = themeItem.id
                        Toast.makeText(context, "تم تطبيق ثيم: ${themeItem.name}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun ThemeSelectionCard(
    themeItem: AppThemeData,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(themeItem.elevationDp, RoundedCornerShape(themeItem.cornerRadiusDp), spotColor = themeItem.shadowColor)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) themeItem.primaryColor else Color.Black.copy(alpha = 0.08f),
                shape = RoundedCornerShape(themeItem.cornerRadiusDp)
            )
            .clickable { onSelect() },
        shape = RoundedCornerShape(themeItem.cornerRadiusDp),
        colors = CardDefaults.cardColors(containerColor = themeItem.surfaceColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = themeItem.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeItem.textColor
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(themeItem.primaryColor)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("المفعل حالياً", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = themeItem.primaryColor)
                }
            }

            // Live 3D Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeItem.backgroundBrush)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .shadow(themeItem.elevationDp, RoundedCornerShape(8.dp), spotColor = themeItem.shadowColor),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معاينة الورقة وعمق الظلال 3D",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = themeItem.textColor
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(themeItem.primaryColor)
                        )
                    }
                }
            }

            // Color Palette Swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الألوان:", fontSize = 11.sp, color = Color.Gray)
                listOf(
                    themeItem.primaryColor,
                    themeItem.secondaryColor,
                    themeItem.surfaceColor,
                    themeItem.textColor
                ).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                    )
                }
            }

            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) themeItem.primaryColor else themeItem.primaryColor.copy(alpha = 0.15f),
                    contentColor = if (isSelected) Color.White else themeItem.primaryColor
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isSelected) "الثيم نشط" else "تطبيق هذا الثيم", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
