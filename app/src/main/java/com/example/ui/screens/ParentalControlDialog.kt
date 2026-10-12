package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppThemeData
import com.example.data.model.ParentalControlConfig
import com.example.ui.MainViewModel

@Composable
fun ParentalControlDialog(
    theme: AppThemeData,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig by viewModel.parentalControlConfig.collectAsState()

    var isEnabled by remember { mutableStateOf(currentConfig.isEnabled) }
    var filterMature by remember { mutableStateOf(currentConfig.filterMatureContent) }
    var enteredPin by remember { mutableStateOf("") }
    var currentStoredPin by remember { mutableStateOf(currentConfig.pinCode) }
    var isPinVerified by remember { mutableStateOf(!currentConfig.isEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
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
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = Color.White)
                        }
                        Text("إعدادات الرقابة الأبوية", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textColor)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = Color.Black.copy(alpha = 0.06f))

                if (!isPinVerified && currentConfig.isEnabled) {
                    // Unlock with PIN
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("أدخل رمز الحماية (PIN) المكون من 4 أرقام لتعديل إعدادات الرقابة:", fontSize = 12.sp, color = Color.DarkGray)

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { if (it.length <= 4) enteredPin = it },
                            label = { Text("رمز PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(0.7f),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (enteredPin == currentStoredPin) {
                                    isPinVerified = true
                                } else {
                                    Toast.makeText(context, "رمز PIN غير صحيح", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("التحقق والمتابعة")
                        }
                    }
                } else {
                    // Settings Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تفعيل الرقابة الأبوية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("حماية التطبيق وقفل المحتوى بكلمة سر PIN", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0284C7))
                        )
                    }

                    if (isEnabled) {
                        OutlinedTextField(
                            value = currentStoredPin,
                            onValueChange = { if (it.length <= 4) currentStoredPin = it },
                            label = { Text("تعيين رمز PIN جديد (4 أرقام)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تصفية المحتوى المخصص للبالغين", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                Text("حجب الكتب غير المخصصة للأطفال تلقائياً", fontSize = 10.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = filterMature,
                                onCheckedChange = { filterMature = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0284C7))
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateParentalControl(
                                    ParentalControlConfig(
                                        isEnabled = isEnabled,
                                        pinCode = currentStoredPin,
                                        filterMatureContent = filterMature
                                    )
                                )
                                Toast.makeText(context, "تم حفظ إعدادات الرقابة الأبوية", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حفظ")
                        }
                    }
                }
            }
        }
    }
}
