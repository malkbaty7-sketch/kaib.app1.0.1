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
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppThemeData
import com.example.data.model.AppUserProfile
import com.example.data.model.PublishedWork
import com.example.ui.MainViewModel

@Composable
fun UserProfileAndAuthDialog(
    theme: AppThemeData,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val profile by viewModel.currentUserProfile.collectAsState()

    // Auth States
    var authMethod by remember { mutableStateOf("EMAIL") } // EMAIL, PHONE, GOOGLE
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    val correctOtp = "123456"

    // Profile Edit States
    var isEditingProfile by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(profile.displayName) }
    var editUsername by remember { mutableStateOf(profile.username) }

    // Publish New Work Modal
    var showPublishWorkDialog by remember { mutableStateOf(false) }
    var newWorkTitle by remember { mutableStateOf("") }
    var newWorkExcerpt by remember { mutableStateOf("") }
    var newWorkIsPublic by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!isLoggedIn) "تسجيل الدخول وإنشاء حساب" else "الملف الشخصي للكاتب",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = theme.textColor
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = Color.Black.copy(alpha = 0.08f))

                if (!isLoggedIn) {
                    // Login / Registration View with Email, Phone, Google + OTP
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("اختر وسيلة تسجيل الدخول للوصول إلى كافة الميزات:", fontSize = 12.sp, color = Color.Gray)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = authMethod == "EMAIL",
                                onClick = { authMethod = "EMAIL"; otpSent = false },
                                label = { Text("البريد الإلكتروني", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = authMethod == "PHONE",
                                onClick = { authMethod = "PHONE"; otpSent = false },
                                label = { Text("رقم الهاتف", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = authMethod == "GOOGLE",
                                onClick = { authMethod = "GOOGLE"; otpSent = false },
                                label = { Text("حساب Google", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        when (authMethod) {
                            "EMAIL" -> {
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("البريد الإلكتروني") },
                                    placeholder = { Text("user@example.com") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            "PHONE" -> {
                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = { phoneInput = it },
                                    label = { Text("رقم الهاتف (مع مفتاح الدولة)") },
                                    placeholder = { Text("+966 5x xxx xxxx") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            "GOOGLE" -> {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setUserLoggedIn(true)
                                            Toast.makeText(context, "تم تسجيل الدخول بحساب Google بنجاح!", Toast.LENGTH_SHORT).show()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF4285F4))
                                        Text("المتابعة الفورية باستخدام حساب Google", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        if (authMethod != "GOOGLE") {
                            if (!otpSent) {
                                Button(
                                    onClick = {
                                        otpSent = true
                                        Toast.makeText(context, "تم إرسال رمز التحقق OTP (الرمز التجريبي: 123456)", Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                                ) {
                                    Text("إرسال رمز التحقق (OTP)")
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "تم إرسال رمز التحقق المكون من 6 أرقام. للتحقق التجريبي استخدم: 123456",
                                            fontSize = 11.sp,
                                            color = Color(0xFF92400E),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = otpInput,
                                        onValueChange = { if (it.length <= 6) otpInput = it },
                                        label = { Text("أدخل رمز التحقق (OTP)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            if (otpInput == correctOtp || otpInput.length == 6) {
                                                viewModel.setUserLoggedIn(true)
                                                Toast.makeText(context, "تم التحقق وتسجيل الدخول بنجاح!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "رمز التحقق غير صحيح، أدخل 123456", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                                    ) {
                                        Text("تأكيد الرمز والدخول")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Profile Dashboard: Info, Avatar, Works Portfolio with Visibility Controls
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // User Info Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(theme.primaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(profile.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                                        Text("@${profile.username}", fontSize = 11.sp, color = theme.primaryColor, fontWeight = FontWeight.Medium)
                                        Text(profile.email, fontSize = 10.sp, color = Color.Gray)
                                    }

                                    IconButton(onClick = { isEditingProfile = !isEditingProfile }) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل الملف", tint = theme.primaryColor)
                                    }
                                }
                            }
                        }

                        // Edit Form
                        if (isEditingProfile) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = editName,
                                            onValueChange = { editName = it },
                                            label = { Text("الاسم الظاهر") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = editUsername,
                                            onValueChange = { editUsername = it },
                                            label = { Text("اسم المعرف (@username)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.updateUserProfile(editName, editUsername)
                                                isEditingProfile = false
                                                Toast.makeText(context, "تم تحديث الملف الشخصي", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                                        ) {
                                            Text("حفظ التعديلات")
                                        }
                                    }
                                }
                            }
                        }

                        // Published Works Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الأعمال النصية المنشورة في الصفحة الشخصية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Button(
                                    onClick = { showPublishWorkDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نشر عمل جديد", fontSize = 11.sp)
                                }
                            }
                        }

                        // Published Works List
                        items(profile.publishedWorks) { work ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCFD)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(work.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                                        Surface(
                                            color = if (work.isPublic) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (work.isPublic) "عام (مرئي للجميع)" else "خاص (مرئي لي فقط)",
                                                fontSize = 10.sp,
                                                color = if (work.isPublic) Color(0xFF15803D) else Color(0xFF475569),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(work.excerpt, fontSize = 11.sp, color = Color.Gray, lineHeight = 16.sp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = {
                                            viewModel.toggleWorkVisibility(work.id)
                                            Toast.makeText(context, "تم تغيير حالة الرؤية", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(if (work.isPublic) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (work.isPublic) "جعله خاصاً" else "جعله عاماً", fontSize = 11.sp)
                                        }

                                        IconButton(onClick = { viewModel.deletePublishedWork(work.id) }, modifier = Modifier.size(26.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.setUserLoggedIn(false)
                                    Toast.makeText(context, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تسجيل الخروج من الحساب", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to publish new work to profile
    if (showPublishWorkDialog) {
        Dialog(onDismissRequest = { showPublishWorkDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("نشر عمل نصي في صفحتك الشخصية", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = newWorkTitle,
                        onValueChange = { newWorkTitle = it },
                        label = { Text("عنوان العمل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newWorkExcerpt,
                        onValueChange = { newWorkExcerpt = it },
                        label = { Text("مقتطف أو محتوى العمل") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("السماح للجميع برؤية المنشور", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(if (newWorkIsPublic) "المنشور عام ويمكن للجميع تصفحه" else "المنشور خاص بك فقط", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = newWorkIsPublic,
                            onCheckedChange = { newWorkIsPublic = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = theme.primaryColor)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPublishWorkDialog = false }) { Text("إلغاء") }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (newWorkTitle.isNotBlank()) {
                                    viewModel.addPublishedWork(
                                        PublishedWork(
                                            title = newWorkTitle,
                                            excerpt = newWorkExcerpt,
                                            isPublic = newWorkIsPublic
                                        )
                                    )
                                    showPublishWorkDialog = false
                                    newWorkTitle = ""
                                    newWorkExcerpt = ""
                                    Toast.makeText(context, "تم نشر العمل بنجاح في ملفك الشخصي!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                        ) {
                            Text("نشر")
                        }
                    }
                }
            }
        }
    }
}
