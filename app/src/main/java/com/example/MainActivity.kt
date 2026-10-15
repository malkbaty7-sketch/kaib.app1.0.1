package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.SavedBooksViewModel
import com.example.ui.screens.*
import com.example.util.AppLanguage
import com.example.util.AppLocalization

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val savedBooksViewModel: SavedBooksViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentLang by viewModel.appLanguage.collectAsState()
            val layoutDirection = currentLang.layoutDirection

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                val theme by viewModel.currentTheme.collectAsState()
                val activeProject by viewModel.activeProject.collectAsState()
                val toastMessage by viewModel.saveToastMessage.collectAsState()
                val isVip by viewModel.isVipUser.collectAsState()

                var currentTab by remember { mutableStateOf("PROJECTS") } // FILES, EXTRACTION, PROJECTS, THEMES, SETTINGS
                var isEditingProject by remember { mutableStateOf(false) }
                var showDeviceDocsScreen by remember { mutableStateOf(false) }
                var viewingDocUri by remember { mutableStateOf<String?>(null) }
                var viewingDocName by remember { mutableStateOf("") }

                // Quick Global Dialogs
                var showHelpDialog by remember { mutableStateOf(false) }
                var showBrowserDialog by remember { mutableStateOf(false) }
                var showSubscriptionDialog by remember { mutableStateOf(false) }
                var showAnalyticsDialog by remember { mutableStateOf(false) }
                var showArabicConverterStudio by remember { mutableStateOf(false) }

                BackHandler(enabled = viewingDocUri != null || showDeviceDocsScreen || isEditingProject || currentTab != "PROJECTS") {
                    if (viewingDocUri != null) {
                        viewingDocUri = null
                    } else if (showDeviceDocsScreen) {
                        showDeviceDocsScreen = false
                    } else if (isEditingProject) {
                        viewModel.saveCurrentProject()
                        isEditingProject = false
                    } else if (currentTab != "PROJECTS") {
                        currentTab = "PROJECTS"
                    }
                }

                LaunchedEffect(toastMessage) {
                    toastMessage?.let { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.saveToastMessage.value = null
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = theme.surfaceColor
                ) {
                    if (viewingDocUri != null) {
                        com.example.ui.screens.MultiFormatDocumentViewerScreen(
                            uriString = viewingDocUri!!,
                            fileName = viewingDocName,
                            theme = theme,
                            viewModel = viewModel,
                            onBack = { viewingDocUri = null }
                        )
                    } else if (showDeviceDocsScreen) {
                        com.example.ui.screens.DeviceDocumentsScreen(
                            theme = theme,
                            viewModel = viewModel,
                            onBack = { showDeviceDocsScreen = false },
                            onOpenDocument = { uriStr, docName ->
                                viewingDocUri = uriStr
                                viewingDocName = docName
                            }
                        )
                    } else if (isEditingProject && activeProject != null) {
                        EditorScreen(
                            viewModel = viewModel,
                            onBack = {
                                viewModel.saveCurrentProject()
                                isEditingProject = false
                            }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(theme.backgroundBrush),
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = AppLocalization.string(currentLang, "app_name"),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp,
                                                color = theme.textColor
                                            )
                                            if (isVip) {
                                                Surface(
                                                    color = Color(0xFFD97706),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "VIP",
                                                        fontSize = 10.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    actions = {
                                        // Device Documents & External Library (Task 1)
                                        IconButton(onClick = { showDeviceDocsScreen = true }) {
                                            Icon(Icons.Default.FolderSpecial, contentDescription = "مستندات الجهاز والمكتبة الخارجية", tint = theme.primaryColor)
                                        }

                                        // Writing & Productivity Analytics Quick Access (Task 4.3)
                                        IconButton(onClick = { showAnalyticsDialog = true }) {
                                            Icon(Icons.Default.Insights, contentDescription = "إحصائيات وتحليلات الإنتاجية", tint = theme.primaryColor)
                                        }

                                        // Arabic Document Converter & Tashkeel Studio
                                        IconButton(onClick = { showArabicConverterStudio = true }) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = "محوّل المستندات والتشكيل العربي", tint = theme.primaryColor)
                                        }

                                        // Research & PDF Browser Quick Access
                                        IconButton(onClick = { showBrowserDialog = true }) {
                                            Icon(Icons.Default.Public, contentDescription = "متصفح البحوث", tint = theme.primaryColor)
                                        }

                                        // Help & FAQ (?)
                                        IconButton(onClick = { showHelpDialog = true }) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(theme.primaryColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("?", fontWeight = FontWeight.Black, fontSize = 16.sp, color = theme.primaryColor)
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                                )
                            },
                            bottomBar = {
                                Column {
                                    // Ad Banner (Displayed only when !isVip)
                                    if (!isVip) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showSubscriptionDialog = true },
                                            color = Color(0xFF1E293B)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Surface(color = Color(0xFFF59E0B), shape = RoundedCornerShape(4.dp)) {
                                                        Text("إعلان", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                    Text("استمتع بتجربة خالية من الإعلانات مع كاتب VIP", fontSize = 11.sp, color = Color.White)
                                                }
                                                Text("إزالة الإعلانات", fontSize = 11.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    NavigationBar(
                                        modifier = Modifier
                                            .shadow(8.dp, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                                        containerColor = Color.White,
                                        tonalElevation = 8.dp
                                    ) {
                                        NavigationBarItem(
                                            selected = currentTab == "FILES",
                                            onClick = { currentTab = "FILES" },
                                            icon = { Icon(Icons.Default.Folder, contentDescription = "الملفات") },
                                            label = { Text(AppLocalization.string(currentLang, "tab_files"), fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = theme.primaryColor,
                                                indicatorColor = theme.primaryColor.copy(alpha = 0.15f)
                                            )
                                        )

                                        NavigationBarItem(
                                            selected = currentTab == "EXTRACTION",
                                            onClick = { currentTab = "EXTRACTION" },
                                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "الاستخلاص") },
                                            label = { Text(AppLocalization.string(currentLang, "tab_extraction"), fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = theme.primaryColor,
                                                indicatorColor = theme.primaryColor.copy(alpha = 0.15f)
                                            )
                                        )

                                        NavigationBarItem(
                                            selected = currentTab == "PROJECTS",
                                            onClick = { currentTab = "PROJECTS" },
                                            icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "المشاريع") },
                                            label = { Text(AppLocalization.string(currentLang, "tab_projects"), fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = theme.primaryColor,
                                                indicatorColor = theme.primaryColor.copy(alpha = 0.15f)
                                            )
                                        )

                                        NavigationBarItem(
                                            selected = currentTab == "THEMES",
                                            onClick = { currentTab = "THEMES" },
                                            icon = { Icon(Icons.Default.Palette, contentDescription = "الثيمات") },
                                            label = { Text(AppLocalization.string(currentLang, "tab_themes"), fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = theme.primaryColor,
                                                indicatorColor = theme.primaryColor.copy(alpha = 0.15f)
                                            )
                                        )

                                        NavigationBarItem(
                                            selected = currentTab == "SETTINGS",
                                            onClick = { currentTab = "SETTINGS" },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                                            label = { Text(AppLocalization.string(currentLang, "tab_settings"), fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = theme.primaryColor,
                                                indicatorColor = theme.primaryColor.copy(alpha = 0.15f)
                                            )
                                        )
                                    }
                                }
                            }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                                    .background(theme.backgroundBrush)
                            ) {
                                when (currentTab) {
                                    "FILES" -> FilesScreen(
                                        viewModel = viewModel,
                                        onNavigateToExtraction = { currentTab = "EXTRACTION" },
                                        onOpenDeviceDocuments = { showDeviceDocsScreen = true }
                                    )
                                    "EXTRACTION" -> ExtractionScreen(
                                        viewModel = viewModel,
                                        onNavigateToEditor = { isEditingProject = true }
                                    )
                                    "PROJECTS" -> ProjectsScreen(
                                        viewModel = viewModel,
                                        onOpenEditor = {
                                            isEditingProject = true
                                        },
                                        onOpenDeviceDocuments = {
                                            showDeviceDocsScreen = true
                                        }
                                    )
                                    "THEMES" -> ThemesScreen(
                                        viewModel = viewModel
                                    )
                                    else -> SettingsScreen(
                                        viewModel = viewModel,
                                        booksViewModel = savedBooksViewModel,
                                        onStartProjectFromBook = { bookTitle, bookAuthor ->
                                            viewModel.createProject(
                                                title = "كتاب: $bookTitle",
                                                type = "E_BOOK",
                                                templateId = "novel_action",
                                                emotions = listOf("warmth", "motivation"),
                                                initialContent = "مستوحى من كتاب «$bookTitle» للمؤلف $bookAuthor.",
                                                onCreated = {
                                                    isEditingProject = true
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Global Dialogs
                if (showHelpDialog) {
                    HelpAndSupportDialog(
                        theme = theme,
                        viewModel = viewModel,
                        initialTab = "FAQ",
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

                if (showArabicConverterStudio) {
                    com.example.ui.screens.DocumentToolsAndConverterDialog(
                        theme = theme,
                        onDismiss = { showArabicConverterStudio = false },
                        onOpenConvertedDocument = { uriStr, docName ->
                            showArabicConverterStudio = false
                            viewingDocUri = uriStr
                            viewingDocName = docName
                        }
                    )
                }
            }
        }
    }
}
