package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeData

@Composable
fun SearchAndReplaceDialog(
    theme: AppThemeData,
    currentText: String,
    onDismiss: () -> Unit,
    onApplyReplace: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    val matchesCount = remember(searchQuery, currentText) {
        if (searchQuery.isBlank()) 0
        else {
            try {
                Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE).findAll(currentText).count()
            } catch (e: Exception) {
                0
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FindReplace, contentDescription = null, tint = theme.primaryColor)
                        Text(
                            text = "البحث والاستبدال في النص",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("الكلمة أو العبارة المراد البحث عنها") },
                    placeholder = { Text("اكتب كلمة للبحث...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = if (searchQuery.isNotBlank()) "تم العثور على $matchesCount مطابقة" else "",
                            color = if (matchesCount > 0) theme.primaryColor else Color.Gray
                        )
                    }
                )

                // Replace field
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = { replaceQuery = it },
                    label = { Text("استبدال بـ") },
                    placeholder = { Text("النص البديل الجديد...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                val replaced = currentText.replace(searchQuery, replaceQuery, ignoreCase = true)
                                onApplyReplace(replaced)
                                onDismiss()
                            }
                        },
                        enabled = searchQuery.isNotBlank() && matchesCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("استبدال الكل ($matchesCount)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
