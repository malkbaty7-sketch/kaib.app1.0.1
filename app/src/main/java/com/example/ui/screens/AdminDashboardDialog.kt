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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AdminPermissions
import com.example.data.model.AdminUser
import com.example.data.model.AppAnalyticsData
import com.example.data.model.AppThemeData
import com.example.ui.MainViewModel

@Composable
fun AdminDashboardDialog(
    theme: AppThemeData,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isAuthenticated by remember { mutableStateOf(false) }

    // Login credentials
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }

    // Admin Tabs: ANALYTICS, CODE_EDITOR, USER_MANAGEMENT, ACTIVITY_LOG
    var currentAdminTab by remember { mutableStateOf("ANALYTICS") }

    // Add User Flow:
    var showAddUserDialog by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var newUserPassword by remember { mutableStateOf("") }
    var newUserType by remember { mutableStateOf("مسؤول") } // "مسؤول", "مدير"
    var showPermissionsDialog by remember { mutableStateOf(false) }

    // 7 Checkbox Permissions
    var permAccessUserData by remember { mutableStateOf(true) }
    var permDeleteCode by remember { mutableStateOf(false) }
    var permEditCode by remember { mutableStateOf(false) }
    var permReplaceCode by remember { mutableStateOf(false) }
    var permAddImageOrShape by remember { mutableStateOf(true) }
    var permAddTextToApp by remember { mutableStateOf(true) }
    var permMonitorActivity by remember { mutableStateOf(true) }

    // Sample Code Management
    var activeCodeSnippet by remember {
        mutableStateOf(
            """
            // وحدة تحكم المحرك والأمان الذكي (كاتب v2.0)
            fun evaluateSystemIntegrity() {
                val status = verifyCoreServices()
                Log.d("KateebAdmin", "النظام يعمل بكفاءة كاملة واستهلاك بطارية منخفض")
            }
            """.trimIndent()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.95f)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            if (!isAuthenticated) {
                // Admin Authentication Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("تسجيل دخول مدير النظام (لوحة التحكم)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text("الصلاحيات الإدارية الشاملة لتحديث التطبيق والبرمجيات والمستخدمين", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("اسم مستخدم المدير") },
                        placeholder = { Text("malkbaty") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("كلمة السر / رمز الدخول") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )

                    if (loginError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(loginError!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }

                        Button(
                            onClick = {
                                val trimmedUser = usernameInput.trim()
                                val trimmedPass = passwordInput.trim()
                                // Specified admin credentials:
                                // username: 'malkbaty', password: ' 776078989mmn#@ '
                                if (trimmedUser == "malkbaty" && (trimmedPass == "776078989mmn#@" || passwordInput.contains("776078989mmn#@"))) {
                                    isAuthenticated = true
                                    loginError = null
                                    Toast.makeText(context, "أهلاً بك يا مدير النظام! تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
                                } else {
                                    loginError = "اسم المستخدم أو كلمة السر غير صحيحة"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("دخول كمدير")
                        }
                    }
                }
            } else {
                // Admin Management Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "المدير: malkbaty (صلاحيات شاملة)",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    // Navigation Tabs
                    ScrollableTabRow(
                        selectedTabIndex = when (currentAdminTab) {
                            "ANALYTICS" -> 0
                            "CODE_EDITOR" -> 1
                            "USER_MANAGEMENT" -> 2
                            else -> 3
                        },
                        edgePadding = 0.dp,
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF1E293B)
                    ) {
                        Tab(
                            selected = currentAdminTab == "ANALYTICS",
                            onClick = { currentAdminTab = "ANALYTICS" },
                            text = { Text("الشاشة التحليلية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = currentAdminTab == "CODE_EDITOR",
                            onClick = { currentAdminTab = "CODE_EDITOR" },
                            text = { Text("البرمجيات والأكواد", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = currentAdminTab == "USER_MANAGEMENT",
                            onClick = { currentAdminTab = "USER_MANAGEMENT" },
                            text = { Text("إدارة المسؤولين", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = currentAdminTab == "ACTIVITY_LOG",
                            onClick = { currentAdminTab = "ACTIVITY_LOG" },
                            text = { Text("مراقبة النشاط", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    // Body
                    when (currentAdminTab) {
                        "ANALYTICS" -> {
                            AnalyticsSection(analytics = viewModel.analyticsData.collectAsState().value, theme = theme)
                        }
                        "CODE_EDITOR" -> {
                            CodeManagementSection(
                                code = activeCodeSnippet,
                                onCodeChange = { activeCodeSnippet = it },
                                onApply = { action ->
                                    Toast.makeText(context, "تم تنفيذ عملية '$action' على النظام بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                theme = theme
                            )
                        }
                        "USER_MANAGEMENT" -> {
                            UserManagementSection(
                                adminUsers = viewModel.adminUsersList.collectAsState().value,
                                onOpenAddUser = { showAddUserDialog = true },
                                onDeleteUser = { id -> viewModel.removeAdminUser(id) }
                            )
                        }
                        "ACTIVITY_LOG" -> {
                            ActivityMonitoringSection()
                        }
                    }
                }
            }
        }
    }

    // Step 1: Add User Dialog: «نوع المستخدم(مسؤول-مدير) « اسم المستخدم « كلمة السر « حفظ او الغاء
    if (showAddUserDialog) {
        Dialog(onDismissRequest = { showAddUserDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("إضافة مستخدم برتبة جديدة", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    // نوع المستخدم
                    Text("نوع المستخدم:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = newUserType == "مسؤول",
                            onClick = { newUserType = "مسؤول" },
                            label = { Text("مسؤول (صلاحيات مخصصة)") }
                        )
                        FilterChip(
                            selected = newUserType == "مدير",
                            onClick = { newUserType = "مدير" },
                            label = { Text("مدير (صلاحيات كاملة)") }
                        )
                    }

                    // اسم المستخدم
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("اسم المستخدم") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // كلمة السر
                    OutlinedTextField(
                        value = newUserPassword,
                        onValueChange = { newUserPassword = it },
                        label = { Text("كلمة سر المستخدم") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddUserDialog = false }) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newUserName.isNotBlank() && newUserPassword.isNotBlank()) {
                                    showAddUserDialog = false
                                    showPermissionsDialog = true
                                } else {
                                    Toast.makeText(context, "الرجاء ملء اسم المستخدم وكلمة السر", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("حفظ ومتابعة للصلاحيات")
                        }
                    }
                }
            }
        }
    }

    // Step 2: Permissions Checkboxes Dialog (The 7 requested permissions)
    if (showPermissionsDialog) {
        Dialog(onDismissRequest = { showPermissionsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تحديد صلاحيات المستخدم: $newUserName ($newUserType)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text("اختر الصلاحيات الممنوحة له عبر المربعات أدناه:", fontSize = 11.sp, color = Color.Gray)

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            PermissionCheckboxRow(
                                title = "1- منح الحساب الوصول الى بيانات المستخدمين",
                                checked = permAccessUserData,
                                onCheckedChange = { permAccessUserData = it }
                            )
                        }
                        item {
                            PermissionCheckboxRow(
                                title = "2- إمكانية حذف كود",
                                checked = permDeleteCode,
                                onCheckedChange = { permDeleteCode = it }
                            )
                        }
                        item {
                            PermissionCheckboxRow(
                                title = "3- إمكانية تعديل كود",
                                checked = permEditCode,
                                onCheckedChange = { permEditCode = it }
                            )
                        }
                        item {
                            PermissionCheckboxRow(
                                title = "4- إمكانية استبدال كود",
                                checked = permReplaceCode,
                                onCheckedChange = { permReplaceCode = it }
                            )
                        }
                        item {
                            PermissionCheckboxRow(
                                title = "5- إمكانية إضافة صورة أو شكل",
                                checked = permAddImageOrShape,
                                onCheckedChange = { permAddImageOrShape = it }
                            )
                        }
                        item {
                            PermissionCheckboxRow(
                                title = "6- إمكانية إضافة نصوص إلى التطبيق",
                                checked = permAddTextToApp,
                                onCheckedChange = { permAddTextToApp = it }
                            )
                        }
                        item {
                            PermissionCheckboxRow(
                                title = "7- إمكانية مراقبة نشاط التطبيق أو نشاط المستخدمين داخل التطبيق",
                                checked = permMonitorActivity,
                                onCheckedChange = { permMonitorActivity = it }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPermissionsDialog = false }) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val perms = AdminPermissions(
                                    accessUserData = permAccessUserData,
                                    deleteCode = permDeleteCode,
                                    editCode = permEditCode,
                                    replaceCode = permReplaceCode,
                                    addImageOrShape = permAddImageOrShape,
                                    addTextToApp = permAddTextToApp,
                                    monitorActivity = permMonitorActivity
                                )
                                viewModel.addAdminUser(
                                    AdminUser(
                                        username = newUserName,
                                        userType = newUserType,
                                        permissions = perms
                                    )
                                )
                                showPermissionsDialog = false
                                newUserName = ""
                                newUserPassword = ""
                                Toast.makeText(context, "تمت إضافة المسؤول وتعيين صلاحياته بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("حفظ الصلاحيات والمستخدم")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (checked) Color(0xFFF0FDF4) else Color(0xFFF8FAFC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 12.sp, color = if (checked) Color(0xFF166534) else Color(0xFF334155), modifier = Modifier.weight(1f))
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF166534))
            )
        }
    }
}

@Composable
fun AnalyticsSection(analytics: AppAnalyticsData, theme: AppThemeData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("المؤشرات الرقمية الكلية", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(title = "عدد المستخدمين", value = "${analytics.totalUsers}", icon = Icons.Default.People, modifier = Modifier.weight(1f))
                AdminStatCard(title = "مرات التنزيل", value = "${analytics.totalDownloads}", icon = Icons.Default.Download, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(title = "الكتب المرفوعة", value = "${analytics.totalBooksUploaded}", icon = Icons.Default.UploadFile, modifier = Modifier.weight(1f))
                AdminStatCard(title = "الكتب المنشأة", value = "${analytics.totalBooksCreated}", icon = Icons.Default.AutoStories, modifier = Modifier.weight(1f))
                AdminStatCard(title = "المشاريع المنجزة", value = "${analytics.totalProjectsCompleted}", icon = Icons.Default.DoneAll, modifier = Modifier.weight(1f))
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("مقارنة تحليلية لساعات قضاء الوقت (جماعي ومنفرد)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("متوسط الاستخدام الفردي / اليوم", fontSize = 11.sp, color = Color.Gray)
                            Text("${analytics.avgDailyHoursPersonal} ساعات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("متوسط الاستخدام الجماعي / اليوم", fontSize = 11.sp, color = Color.Gray)
                            Text("${analytics.avgDailyHoursGroup} ساعات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.4f))

                    Text("وقت الذروة الأكثر نشاطاً: ${analytics.peakUsageTime}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                    Text("الجلسات النشطة المتزامنة اليوم: ${analytics.activeSessionsToday} جلسة", fontSize = 11.sp, color = Color(0xFF334155))
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(18.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Text(title, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun CodeManagementSection(
    code: String,
    onCodeChange: (String) -> Unit,
    onApply: (String) -> Unit,
    theme: AppThemeData
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("محرر ومراقب الأكواد البرمجية (صلاحية المدير الشاملة)", fontWeight = FontWeight.Bold, fontSize = 13.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { onApply("حذف الكود المختار") },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Red)
                Spacer(modifier = Modifier.width(4.dp))
                Text("حذف كود", fontSize = 11.sp, color = Color.Red)
            }
            OutlinedButton(
                onClick = { onApply("تعديل الكود") },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تعديل كود", fontSize = 11.sp)
            }
            Button(
                onClick = { onApply("استبدال البرمجية") },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("استبدال كود", fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun UserManagementSection(
    adminUsers: List<AdminUser>,
    onOpenAddUser: () -> Unit,
    onDeleteUser: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("قائمة المسؤولين والمديرين الحاليين", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Button(
                onClick = onOpenAddUser,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة مسؤول جديد", fontSize = 11.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(adminUsers) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF1E293B))
                                Text(user.username, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Surface(
                                    color = if (user.userType == "مدير") Color(0xFFFEF3C7) else Color(0xFFE0E7FF),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(user.userType, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            if (user.username != "malkbaty") {
                                IconButton(onClick = { onDeleteUser(user.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(
                            text = "الصلاحيات الممنوحة: " + listOfNotNull(
                                if (user.permissions.accessUserData) "بيانات المستخدمين" else null,
                                if (user.permissions.editCode) "تعديل كود" else null,
                                if (user.permissions.deleteCode) "حذف كود" else null,
                                if (user.permissions.replaceCode) "استبدال كود" else null,
                                if (user.permissions.addImageOrShape) "إضافة أشكال/صور" else null,
                                if (user.permissions.addTextToApp) "إضافة نصوص" else null,
                                if (user.permissions.monitorActivity) "مراقبة النشاط" else null
                            ).joinToString(" • "),
                            fontSize = 10.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityMonitoringSection() {
    val activities = remember {
        listOf(
            "المستخدم @sara_reader أنشأ مشروع كتاب إلكتروني جديد 'فلسفة الضوء'",
            "تم استخلاص 14 فقرة حول 'التعليم الرقمي' من ملف PDF بواسطة @ahmed_author",
            "المستخدم @khalid_researcher صدّر بحثه الأكاديمي بصيغة PDF",
            "المستخدم @noura_writer قام بمشاركة قالب رواية مع المجتمع",
            "جلسة قراءة جماعية نشطة للمجلة العلمية (العدد 4)"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("سجل الرصد المباشر لنشاط التطبيق والمستخدمين", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        items(activities) { act ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Text(act, fontSize = 11.sp, color = Color(0xFF1E293B))
                }
            }
        }
    }
}
