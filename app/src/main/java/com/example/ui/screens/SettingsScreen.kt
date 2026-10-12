package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.MainViewModel
import com.example.ui.SavedBooksViewModel
import com.example.util.AppLanguage
import com.example.util.AppLocalization

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    booksViewModel: SavedBooksViewModel? = null,
    onStartProjectFromBook: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val theme by viewModel.currentTheme.collectAsState()
    val emotionGlobalActive by viewModel.emotionSuggestionsGlobalActive.collectAsState()
    val files by viewModel.allFiles.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val extractions by viewModel.allExtractedTexts.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    val isVip by viewModel.isVipUser.collectAsState()
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val parentalConfig by viewModel.parentalControlConfig.collectAsState()

    var showSavedBooks by remember { mutableStateOf(false) }
    var showCoverStudio by remember { mutableStateOf(false) }
    val savedBooksList by booksViewModel?.allBooks?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    BackHandler(enabled = showSavedBooks || showCoverStudio) {
        if (showCoverStudio) showCoverStudio = false
        else if (showSavedBooks) showSavedBooks = false
    }

    if (showCoverStudio) {
        BookCoversScreen(
            viewModel = viewModel,
            theme = theme,
            onBack = { showCoverStudio = false }
        )
    } else if (showSavedBooks && booksViewModel != null) {
        SavedBooksScreen(
            booksViewModel = booksViewModel,
            theme = theme,
            onBack = { showSavedBooks = false },
            onStartProjectFromBook = { title, author ->
                showSavedBooks = false
                onStartProjectFromBook?.invoke(title, author)
            }
        )
    } else {
        // Dialog trigger states
    var showHelpDialog by remember { mutableStateOf(false) }
    var helpInitialTab by remember { mutableStateOf("FAQ") }
    var showBrowserDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showParentalDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var showPreflightDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
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
                    .padding(18.dp),
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
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                }
                Column {
                    Text(
                        text = AppLocalization.string(currentLang, "app_name") + " - " + AppLocalization.string(currentLang, "settings_title"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor
                    )
                    Text(
                        text = "التحكم في الميزات والحساب والرقابة والأدوات المتقدمة",
                        fontSize = 11.sp,
                        color = theme.textColor.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // 0. Saved Books Library Card (مكتبة الكتب المحفوظة)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clickable { showSavedBooks = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F766E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "مكتبة الكتب المحفوظة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = theme.textColor
                            )
                            Surface(
                                color = Color(0xFF0F766E).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${savedBooksList.size} كتاب",
                                    fontSize = 10.sp,
                                    color = Color(0xFF0F766E),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "إدارة وحفظ المراجع وقوائم القراءة والتقييمات محلياً بدون إنترنت",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
            }
        }

        // 0.1 Book Cover Templates Studio Card (استوديو نماذج أغلفة الكتب)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clickable { showCoverStudio = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF854D0E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "استوديو نماذج أغلفة الكتب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = theme.textColor
                        )
                        Text(
                            text = "تصفح النماذج الجاهزة، رفع صور من الجوال، وتخصيص عناوين الأغلفة",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
            }
        }

        // 0.5. Google Drive Cloud Sync Card (Task 2.1)
        val syncMeta by viewModel.syncMetadata.collectAsState()
        val syncStatus by viewModel.syncStatus.collectAsState()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "المزامنة السحابية (Google Drive API)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = theme.textColor
                            )
                            Text(
                                text = "الحساب: ${syncMeta.connectedAccountEmail ?: "غير متصل"}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (syncStatus == com.example.data.sync.SyncStatus.SYNCING) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = theme.primaryColor)
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                // Auto sync switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مزامنة تلقائية للمشاريع", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("رفع التعديلات فورياً إلى مجلد التطبيق بالسحاب", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = syncMeta.autoSyncEnabled,
                        onCheckedChange = { viewModel.updateAutoSyncConfig(it, syncMeta.syncOnlyOnWifi) }
                    )
                }

                // Wi-Fi only switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("المزامنة عبر Wi-Fi فقط", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("الحفاظ على بيانات الباقة وعدم المزامنة إلا عبر شبكة لاسلكية", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = syncMeta.syncOnlyOnWifi,
                        onCheckedChange = { viewModel.updateAutoSyncConfig(syncMeta.autoSyncEnabled, it) }
                    )
                }

                // Status banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.04f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = syncMeta.lastSyncMessage,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        if (syncMeta.lastBackupSizeKb > 0) {
                            Text(
                                text = "${syncMeta.lastBackupSizeKb} KB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.performDriveSync(forceManual = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مزامنة الآن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.restoreFromDriveBackup() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.primaryColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعادة النسخة", fontSize = 12.sp)
                    }
                }
            }
        }

        // 1. Help & Instructions (?) + Support Contact
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المساعدة والدعم الفني", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.textColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Help (?)
                    OutlinedButton(
                        onClick = {
                            helpInitialTab = "FAQ"
                            showHelpDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("التعليمات (?)", fontSize = 11.sp)
                    }

                    // Contact Support
                    Button(
                        onClick = {
                            helpInitialTab = "SUPPORT"
                            showHelpDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فريق الدعم", fontSize = 11.sp)
                    }
                }
            }
        }

        // 2. User Profile, Account & Published Works
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clickable { showProfileDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLoggedIn) Color(0xFF10B981) else Color(0xFF64748B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Text("الملف الشخصي وتسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                        Text(
                            if (isLoggedIn) "مسجل الدخول • إدارة أعمالك المنشورة وإتاحتها للعامة" else "تسجيل الدخول عبر Google أو الهاتف أو البريد مع OTP",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
            }
        }

        // 3. Research & PDF Book Browser
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clickable { showBrowserDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Text("متصفح البحوث والكتب الإلكترونية PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                        Text("بحث متقدم عن كتب PDF فقط وتنزيلها وجلبها للتطبيق", fontSize = 10.sp, color = Color.Gray)
                    }
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
            }
        }

        // 4. Monetization & VIP Subscription (خطة الأرباح وإزالة الإعلانات)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clickable { showSubscriptionDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (isVip) Color(0xFFFEF3C7) else Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFD97706)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("الاشتراكات المميزة (Katib VIP) 💳", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                            if (isVip) {
                                Surface(color = Color(0xFFD97706), shape = RoundedCornerShape(4.dp)) {
                                    Text("مُفعل", fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                        Text("اشتراكات Google Play، إزالة الإعلانات، وتوليد صور AI بدون حدود", fontSize = 10.sp, color = Color.Gray)
                    }
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
            }
        }

        // 4.1 Writing Analytics & Productivity Dashboard (Task 4.3)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clickable { showAnalyticsDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0D9488)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Insights, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("إحصائيات وتحليلات الإنتاجية 📊", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                            Surface(color = Color(0xFF0D9488).copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                                Text("جديد", fontSize = 9.sp, color = Color(0xFF0D9488), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text("سرعة التأليف (WPM)، أيام الالتزام المتتالية 🔥، ورسوم بيانية أسبوعية", fontSize = 10.sp, color = Color.Gray)
                    }
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
            }
        }

        // 4.2 Global Publishing Pre-flight Check (Task 4.4)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clickable { showPreflightDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("فحص الجودة والامتثال للنشر (Pre-flight) ✈️", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                        }
                        Text("مطابقة معايير Amazon KDP، فحص دقة 300 DPI، والهوامش الحية", fontSize = 10.sp, color = Color.Gray)
                    }
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
            }
        }

        // 5. Parental Control (الرقابة الأبوية)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clickable { showParentalDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0284C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Text("الرقابة الأبوية وحماية المحتوى", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.textColor)
                        Text(
                            if (parentalConfig.isEnabled) "مُفعلة • حظر المحتوى غير المناسب وقفل PIN" else "تعطيل أو تفعيل الرقابة الأبوية برمز PIN",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
            }
        }

        // 6. Language Selection (تبديل اللغة)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppLocalization.string(currentLang, "language_selection"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.textColor)
                    Text(
                        text = if (currentLang.layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) "اتجاه الواجهة: RTL (يمين لليسار)" else "Layout: LTR (Left-to-Right)",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "تبديل لغة التطبيق بالكامل بشكل فوري ومستقل عن لغة نظام الجهاز (مع تغيير تلقائي لاتجاه الواجهات LTR/RTL):",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                // 2x2 Grid of Languages
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentLang == AppLanguage.ARABIC,
                        onClick = { viewModel.setAppLanguage(AppLanguage.ARABIC) },
                        label = { Text("🇸🇦 العربية (RTL)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = currentLang == AppLanguage.ENGLISH,
                        onClick = { viewModel.setAppLanguage(AppLanguage.ENGLISH) },
                        label = { Text("🇺🇸 English (LTR)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentLang == AppLanguage.SPANISH,
                        onClick = { viewModel.setAppLanguage(AppLanguage.SPANISH) },
                        label = { Text("🇪🇸 Español (LTR)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = currentLang == AppLanguage.FRENCH,
                        onClick = { viewModel.setAppLanguage(AppLanguage.FRENCH) },
                        label = { Text("🇫🇷 Français (LTR)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 7. Global Emotion Engine Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "محرك المشاعر الذكي (FR-ES-04)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = theme.textColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تفعيل الاقتراحات الذكية للمشاعر عالمياً",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "عند التعطيل، لن تظهر بالونات أو لوحة الاقتراحات في المحرر",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked = emotionGlobalActive,
                        onCheckedChange = { viewModel.emotionSuggestionsGlobalActive.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = theme.primaryColor)
                    )
                }
            }
        }

        // 8. Admin Dashboard & Code Management (لوحة تحكم المدير)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clickable { showAdminDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF38BDF8))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("لوحة تحكم المدير ومحرر الأكواد", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                        }
                        Text("تسجيل دخول المشرفين، إدارة المستخدمين والصلاحيات ومراقبة النشاط", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.LightGray)
            }
        }

        // Database & Storage Stats
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "بيانات التطبيق المحلية (Room Database)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = theme.textColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(title = "المصادر", value = "${files.size}", theme = theme)
                    StatItem(title = "الكتب", value = "${savedBooksList.size}", theme = theme)
                    StatItem(title = "المشاريع", value = "${projects.size}", theme = theme)
                    StatItem(title = "الاستخلاصات", value = "${extractions.size}", theme = theme)
                }
            }
        }

        // About Application Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "عن تطبيق كاتب (Kateeb)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = theme.textColor
                )
                Text(
                    text = "تطبيق إنتاجية متكامل لقراءة واستخلاص الكتب والـ PDF وتأليف الكتب والأبحاث والروايات والمجلات، مع تحكم بالصفحات والترقيم وثيمات 3D، ويعمل محلياً دون اتصال بالإنترنت وموفر للبطارية.",
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = Color.DarkGray
                )
                Divider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الإصدار", fontSize = 11.sp, color = Color.Gray)
                    Text("2.5.0 (النسخة الشاملة)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                }
            }
        }
    }

    // Dialogs
    if (showHelpDialog) {
        HelpAndSupportDialog(
            theme = theme,
            viewModel = viewModel,
            initialTab = helpInitialTab,
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showBrowserDialog) {
        ResearchBrowserDialog(
            theme = theme,
            viewModel = viewModel,
            onDismiss = { showBrowserDialog = false }
        )
    }

    if (showProfileDialog) {
        UserProfileAndAuthDialog(
            theme = theme,
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showSubscriptionDialog) {
        PaywallScreen(
            theme = theme,
            viewModel = viewModel,
            onDismiss = { showSubscriptionDialog = false }
        )
    }

    if (showAnalyticsDialog) {
        AnalyticsDashboardDialog(
            theme = theme,
            viewModel = viewModel,
            onDismiss = { showAnalyticsDialog = false }
        )
    }

    if (showPreflightDialog) {
        val report = remember { viewModel.runPreflightValidation(context) }
        com.example.ui.components.PreflightValidationDialog(
            report = report,
            theme = theme,
            onDismiss = { showPreflightDialog = false },
            onProceedExportEpub = {
                val f = viewModel.exportToEpub(context)
                if (f != null) {
                    android.widget.Toast.makeText(context, "تم تصدير حزمة EPUB3 بنجاح: ${f.name}", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onProceedExportPdf = {
                val f = viewModel.exportToPdf(context)
                if (f != null) {
                    android.widget.Toast.makeText(context, "تم تصدير ملف PDF بنجاح: ${f.name}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showParentalDialog) {
        ParentalControlDialog(
            theme = theme,
            viewModel = viewModel,
            onDismiss = { showParentalDialog = false }
        )
    }

    if (showAdminDialog) {
        AdminDashboardDialog(
            theme = theme,
            viewModel = viewModel,
            onDismiss = { showAdminDialog = false }
        )
    }
    }
}

@Composable
fun StatItem(title: String, value: String, theme: com.example.data.model.AppThemeData) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
        Text(title, fontSize = 11.sp, color = Color.Gray)
    }
}
