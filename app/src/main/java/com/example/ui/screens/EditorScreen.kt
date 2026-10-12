package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.CoverCanvasView
import com.example.ui.components.CoverCustomizerDialog
import com.example.ui.components.CustomFontPickerDialog
import com.example.ui.components.DraggablePageImage
import com.example.ui.components.ImagePickerDialog
import com.example.ui.components.PageImageControlDialog
import com.example.ui.components.PlotBranchingDialog
import com.example.ui.components.ProofreadingSheetDialog
import com.example.ui.components.FeedbackCommentsSheet
import com.example.ui.components.ImageGeneratorDialog
import com.example.ui.components.RichWritingToolbar
import com.example.ui.components.SceneBreakdownSheet
import com.example.ui.components.SearchAndReplaceDialog
import com.example.ui.components.SnapshotHistoryDialog
import com.example.ui.components.StockImagePickerBottomSheet
import com.example.ui.components.SupportingPaneContent
import com.example.ui.components.TtsPlayerBar
import com.example.ui.screens.DigitalCorkboardScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.activeProject.collectAsState()
    val chapters by viewModel.activeChapters.collectAsState()
    val activeIdx by viewModel.activeChapterIndex.collectAsState()
    val theme by viewModel.currentTheme.collectAsState()

    val paperColorHex by viewModel.editorPaperColor.collectAsState()
    val paperSize by viewModel.editorPaperSize.collectAsState()
    val pageViewMode by viewModel.editorPageViewMode.collectAsState()
    val pageNumberStyle by viewModel.editorPageNumberStyle.collectAsState()
    val pageNumberPosition by viewModel.editorPageNumberPosition.collectAsState()
    val suggestionsActive by viewModel.editorEmotionSuggestionsActive.collectAsState()
    val suggestions by viewModel.emotionSuggestions.collectAsState()
    val selectedEmotions by viewModel.editorSelectedEmotions.collectAsState()

    var showPaperSettingsDialog by remember { mutableStateOf(false) }
    var showSuggestionsDialog by remember { mutableStateOf(false) }
    var showAiAssistantDialog by remember { mutableStateOf(false) }
    var showEmotionsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showRenameTabDialog by remember { mutableStateOf(false) }
    var showStoryboardDialog by remember { mutableStateOf(false) }
    var showSourceReaderDialog by remember { mutableStateOf(false) }
    var showVersionHistoryDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showGrammarDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showEBookPreview by remember { mutableStateOf(false) }
    var showCoversStudio by remember { mutableStateOf(false) }
    var editingCoverTemplate by remember { mutableStateOf<Pair<BookCoverTemplate, Boolean>?>(null) }
    var showServicesMenu by remember { mutableStateOf(false) }

    // Phase 1 Features States
    var showSnapshotHistoryDialog by remember { mutableStateOf(false) }
    var showSceneBreakdownSheet by remember { mutableStateOf(false) }
    var showFontPickerDialog by remember { mutableStateOf(false) }
    var isSupportingPaneVisible by remember { mutableStateOf(true) }

    // Phase 2 Features States (Stock Images, TTS, Proofreading, Plot Branching)
    var showStockImagePicker by remember { mutableStateOf(false) }
    var showTtsPlayer by remember { mutableStateOf(false) }
    var showProofreadingDialog by remember { mutableStateOf(false) }
    var showPlotBranchingDialog by remember { mutableStateOf(false) }

    val ttsPlaybackState by viewModel.ttsManager.playbackState.collectAsState()
    val ttsCurrentSentence by viewModel.ttsManager.currentSentenceText.collectAsState()
    val ttsSentenceIndex by viewModel.ttsManager.currentSentenceIndex.collectAsState()
    val ttsSpeechRate by viewModel.ttsManager.speechRate.collectAsState()

    val proofreadingIssues by viewModel.proofreadingIssues.collectAsState()
    val isProofreading by viewModel.isProofreading.collectAsState()

    val plotBranchOptions by viewModel.plotBranchOptions.collectAsState()
    val isGeneratingBranches by viewModel.isGeneratingBranches.collectAsState()

    var isFocusMode by remember { mutableStateOf(false) }

    // Phase 3 States (Review Mode, AI Image Generation, Digital Corkboard)
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showAiImageGeneratorDialog by remember { mutableStateOf(false) }
    var showCorkboardScreen by remember { mutableStateOf(false) }

    // Phase 4 States (Analytics, Pre-flight validation, Paywall)
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var showPreflightDialog by remember { mutableStateOf(false) }
    var showPaywallDialog by remember { mutableStateOf(false) }

    // Katib View Modes: defaultView, word, text, eBook
    var katibViewMode by remember { mutableStateOf("defaultView") }

    val isReviewMode by viewModel.isReviewModeActive.collectAsState()
    val activeChapterComments by viewModel.activeChapterComments.collectAsState()
    val exportMetadata by viewModel.exportMetadata.collectAsState()

    val currentChapter = chapters.getOrNull(activeIdx)

    val storyElements by viewModel.activeStoryElements.collectAsState()
    val snapshots by viewModel.activeSnapshots.collectAsState()
    val detailedSnapshots by viewModel.detailedSnapshots.collectAsState()
    val allDetailedSnapshots by viewModel.allDetailedSnapshots.collectAsState()
    val customFonts by viewModel.customFonts.collectAsState()
    val selectedFont by viewModel.selectedEditorFont.collectAsState()
    val activeFontFamily = remember(selectedFont, customFonts) {
        com.example.util.FontStorageManager.getFontFamily(selectedFont, customFonts)
    }
    val currentChapterScenes = remember(currentChapter?.scenesJson) {
        viewModel.getActiveChapterScenes()
    }
    val allExtractedTexts by viewModel.allExtractedTexts.collectAsState()

    val isAiGenerating by viewModel.isAiGenerating.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()
    val targetWordCount by viewModel.editorTargetWordCount.collectAsState()

    val speechRecognizerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                val current = currentChapter?.content ?: ""
                val updated = if (current.isBlank()) spokenText else "$current\n$spokenText"
                viewModel.updateCurrentChapterContent(updated)
                Toast.makeText(context, "تمت إضافة النص المُملى", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onVoiceTypingClick() {
        try {
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "تحدث الآن لتسجيل النص...")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "الكتابة الصوتية غير مدعومة على هذا الجهاز", Toast.LENGTH_SHORT).show()
        }
    }

    val currentPaperColor = remember(paperColorHex) {
        try {
            Color(android.graphics.Color.parseColor(paperColorHex))
        } catch (e: Exception) {
            Color(0xFFFFFDD0)
        }
    }

    if (showEBookPreview) {
        com.example.ui.screens.EBookPreviewScreen(
            project = project,
            chapters = chapters,
            frontCover = viewModel.getProjectFrontCover(),
            backCover = viewModel.getProjectBackCover(),
            theme = theme,
            onBackToEditor = { showEBookPreview = false }
        )
        return
    }

    if (showCoversStudio) {
        com.example.ui.screens.BookCoversScreen(
            viewModel = viewModel,
            theme = theme,
            onBack = { showCoversStudio = false },
            onApplyCoverToProject = { template, isFront ->
                if (isFront) {
                    viewModel.setProjectFrontCover(template)
                } else {
                    viewModel.setProjectBackCover(template)
                }
                showCoversStudio = false
            }
        )
        return
    }

    if (showCorkboardScreen) {
        DigitalCorkboardScreen(
            chapters = chapters,
            storyElements = storyElements,
            theme = theme,
            onBack = { showCorkboardScreen = false },
            onUpdateChapters = { updatedList ->
                viewModel.updateChaptersOrder(updatedList)
            },
            onNavigateToChapter = { targetIdx ->
                viewModel.selectChapter(targetIdx)
                showCorkboardScreen = false
            }
        )
        return
    }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            if (!isFocusMode) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = project?.projectName ?: "محرر خطة",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${chapters.size} صفحات • $paperSize • نمط العرض: ${if (pageViewMode == "SINGLE_PAGE") "صفحة مفردة" else "صفحتين"}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "خروج")
                        }
                    },
                    actions = {
                        // 1. Completed E-Book Preview Button (زر عرض شكل الكتاب المكتمل)
                        Button(
                            onClick = { showEBookPreview = true },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("معاينة الكتاب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. AI Writing Assistant (المساعد الذكي)
                        IconButton(onClick = { showAiAssistantDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "المساعد الذكي", tint = theme.primaryColor)
                        }

                        // 3. Export (تصدير)
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "تصدير", tint = theme.primaryColor)
                        }

                        // 4. Save (حفظ)
                        IconButton(onClick = {
                            viewModel.saveCurrentProject()
                            Toast.makeText(context, "تم حفظ العمل بنجاح!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "حفظ", tint = theme.primaryColor)
                        }

                        // 5. Overflow Dropdown Menu with all Tools and Services
                        Box {
                            IconButton(onClick = { showServicesMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "المزيد من الخدمات والأدوات")
                            }
                            DropdownMenu(
                                expanded = showServicesMenu,
                                onDismissRequest = { showServicesMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("إحصائيات وتحليلات الإنتاجية 📊") },
                                    leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null, tint = Color(0xFF0D9488)) },
                                    onClick = {
                                        showServicesMenu = false
                                        showAnalyticsDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("فحص الجودة والامتثال للنشر (Pre-flight) ✈️") },
                                    leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2563EB)) },
                                    onClick = {
                                        showServicesMenu = false
                                        showPreflightDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ترقية الحساب (Katib VIP) 💳") },
                                    leadingIcon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFD97706)) },
                                    onClick = {
                                        showServicesMenu = false
                                        showPaywallDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("لوحة التخطيط والتنظيم المرئي 📌") },
                                    leadingIcon = { Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = Color(0xFFD97706)) },
                                    onClick = {
                                        showServicesMenu = false
                                        showCorkboardScreen = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("توليد الصور بالذكاء الاصطناعي 🎨") },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF9333EA)) },
                                    onClick = {
                                        showServicesMenu = false
                                        showAiImageGeneratorDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ملاحظات القراءة التجريبية (${activeChapterComments.size}) 📝") },
                                    leadingIcon = { Icon(Icons.Default.RateReview, contentDescription = null, tint = Color(0xFF2563EB)) },
                                    onClick = {
                                        showServicesMenu = false
                                        showFeedbackSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isReviewMode) "إنهاء وضع المراجعة 👁️" else "تفعيل وضع المراجعة التجريبية 👁️") },
                                    leadingIcon = { Icon(if (isReviewMode) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        viewModel.toggleReviewMode()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("القارئ الصوتي والتدقيق السمعي 🎧") },
                                    leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showTtsPlayer = true
                                        val content = currentChapter?.content ?: ""
                                        if (content.isNotBlank()) {
                                            viewModel.startTtsReading(content)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مكتبة الصور السحابية (Unsplash) 🖼️") },
                                    leadingIcon = { Icon(Icons.Default.ImageSearch, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showStockImagePicker = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("التدقيق اللغوي الذكي (Proofreading) 🔍") },
                                    leadingIcon = { Icon(Icons.Default.Spellcheck, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showProofreadingDialog = true
                                        viewModel.runProofreadingForCurrentChapter()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مسارات وتفرع الحبكة (Plot Branching) 🔀") },
                                    leadingIcon = { Icon(Icons.Default.AltRoute, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showPlotBranchingDialog = true
                                        viewModel.generatePlotBranchesForCurrentScene()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تفكيك المشاهد (Scene Breakdown) 🎬") },
                                    leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showSceneBreakdownSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("استرجاع لقطات العمل (Snapshots) 🕒") },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showSnapshotHistoryDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("إدارة وتثبيت الخطوط 🔤") },
                                    leadingIcon = { Icon(Icons.Default.FontDownload, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showFontPickerDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("استوديو أغلفة الكتب 📖") },
                                    leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showCoversStudio = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تخصيص الورقة والصفحة 🎨") },
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showPaperSettingsDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مخطط القصة والتوتر الدرامي 📊") },
                                    leadingIcon = { Icon(Icons.Default.ShowChart, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showStoryboardDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("قارئ المصادر والاقتباسات 📚") },
                                    leadingIcon = { Icon(Icons.Default.VerticalSplit, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showSourceReaderDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("سجل النسخ واللقطات الاحتياطية 🕒") },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showVersionHistoryDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تنقيح وتصحيح النصوص ✍️") },
                                    leadingIcon = { Icon(Icons.Default.Spellcheck, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showGrammarDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("أهداف عدد الكلمات 🎯") },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showGoalDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (suggestions.isNotEmpty()) "الاقتراحات الذكية 💡 (${suggestions.size})" else "الاقتراحات الذكية 💡") },
                                    leadingIcon = { Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = Color(0xFFFFA000)) },
                                    onClick = {
                                        showServicesMenu = false
                                        showSuggestionsDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مشاركة المشروع 🔗") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        showShareDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("وضع التركيز الكامل 🔲") },
                                    leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null, tint = theme.primaryColor) },
                                    onClick = {
                                        showServicesMenu = false
                                        isFocusMode = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.surfaceColor)
                )
            }
        },
        floatingActionButton = {
            if (isFocusMode) {
                FloatingActionButton(
                    onClick = { isFocusMode = false },
                    containerColor = theme.primaryColor,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.FullscreenExit, contentDescription = "إنهاء وضع التركيز")
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(theme.backgroundBrush)
        ) {
            val isDualPane = maxWidth >= 768.dp && !isFocusMode

            Row(modifier = Modifier.fillMaxSize()) {
                // Primary Pane (Main Editor Studio Canvas & Controls)
                Column(
                    modifier = Modifier
                        .weight(if (isDualPane && isSupportingPaneVisible) 0.62f else 1f)
                        .fillMaxHeight()
                ) {
                    // Reorderable Chapters & Tabs Bar (FR-SE-04, FR-SE-05)
            if (!isFocusMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Scrollable Tab List
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasFrontCover = chapters.any { it.sectionType == "FRONT_COVER" }
                        val hasBackCover = chapters.any { it.sectionType == "BACK_COVER" }

                        // Quick Add Front Cover if not added yet
                        if (!hasFrontCover) {
                            AssistChip(
                                onClick = {
                                    val defaultTemplate = com.example.data.model.BookCoverTemplate(
                                        title = "الغلاف الأمامي",
                                        bookTitle = project?.projectName ?: "عنوان الكتاب",
                                        subtitle = "كتاب إلكتروني متكامل",
                                        authorName = "اسم المؤلف",
                                        publisherName = "دار النشر",
                                        category = "عام"
                                    )
                                    viewModel.setProjectFrontCover(defaultTemplate)
                                    viewModel.activeChapterIndex.value = 0
                                },
                                label = { Text("+ غلاف البداية 📖", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = theme.primaryColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.5f))
                            )
                        }

                        chapters.forEachIndexed { index, chapter ->
                            val isSelected = index == activeIdx
                            val isCover = chapter.sectionType == "FRONT_COVER" || chapter.sectionType == "BACK_COVER"
                            Card(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.activeChapterIndex.value = index }
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) theme.primaryColor else if (isCover) Color(0xFFD97706).copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) theme.primaryColor.copy(alpha = 0.12f) else if (isCover) Color(0xFFFEF3C7).copy(alpha = 0.5f) else Color(0xFFFAFAFA)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = chapter.tabName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) theme.primaryColor else if (isCover) Color(0xFF92400E) else Color.DarkGray
                                    )
                                    if (isSelected && chapters.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteChapter(index) },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(12.dp), tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }

                        // Add Tab Button (+)
                        IconButton(
                            onClick = { viewModel.addNewChapter("فصل جديد") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "إضافة تبويب", tint = theme.primaryColor)
                        }

                        // Quick Add Back Cover if not added yet
                        if (!hasBackCover) {
                            AssistChip(
                                onClick = {
                                    val defaultTemplate = com.example.data.model.BookCoverTemplate(
                                        title = "الغلاف الخلفي",
                                        bookTitle = project?.projectName ?: "عنوان الكتاب",
                                        subtitle = "",
                                        authorName = "اسم المؤلف",
                                        backSynopsis = "نبذة موجزة عن الكتاب ومحتواه...",
                                        backAuthorBio = "عن المؤلف...",
                                        category = "عام"
                                    )
                                    viewModel.setProjectBackCover(defaultTemplate)
                                    viewModel.activeChapterIndex.value = viewModel.activeChapters.value.lastIndex
                                },
                                label = { Text("+ غلاف النهاية 🔖", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF334155)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                            )
                        }
                    }

                    // Tab Order Controls (Left / Right) (FR-SE-05)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.moveChapter(activeIdx, -1)
                            },
                            enabled = activeIdx > 0,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "تقديم التبويب", modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.moveChapter(activeIdx, 1)
                            },
                            enabled = activeIdx < chapters.lastIndex,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "تأخير التبويب", modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = { showRenameTabDialog = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = "تسمية التبويب", modifier = Modifier.size(18.dp), tint = theme.primaryColor)
                        }
                    }
                }
            }
            }

            // Quick Services Action Bar (شريط أدوات الكتابة والخدمات السريعة)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { showAnalyticsDialog = true },
                        leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF0D9488)) },
                        label = { Text("تحليلات الإنتاجية 📊", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = { showPreflightDialog = true },
                        leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF2563EB)) },
                        label = { Text("فحص KDP ✈️", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = { showCorkboardScreen = true },
                        leadingIcon = { Icon(Icons.Default.DashboardCustomize, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFD97706)) },
                        label = { Text("لوحة التخطيط 📌", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = { showAiImageGeneratorDialog = true },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF9333EA)) },
                        label = { Text("توليد صور AI 🎨", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = { showFeedbackSheet = true },
                        leadingIcon = { Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF2563EB)) },
                        label = { Text("ملاحظات (${activeChapterComments.size}) 📝", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = { viewModel.toggleReviewMode() },
                        leadingIcon = { Icon(if (isReviewMode) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (isReviewMode) Color(0xFF2563EB) else Color.Gray) },
                        label = { Text(if (isReviewMode) "وضع المراجعة (نشط) 👁️" else "وضع المراجعة 👁️", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            showTtsPlayer = !showTtsPlayer
                            if (showTtsPlayer && ttsPlaybackState == com.example.util.TtsPlaybackState.STOPPED) {
                                val content = currentChapter?.content ?: ""
                                if (content.isNotBlank()) {
                                    viewModel.startTtsReading(content)
                                }
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (showTtsPlayer) Color(0xFF10B981) else theme.primaryColor) },
                        label = { Text(if (showTtsPlayer) "قارئ الصوت 🎧 (نشط)" else "قراءة صوتية 🎧", fontSize = 11.sp, fontWeight = if (showTtsPlayer) FontWeight.Bold else FontWeight.Normal) }
                    )
                    AssistChip(
                        onClick = { showStockImagePicker = true },
                        leadingIcon = { Icon(Icons.Default.ImageSearch, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("صور سحابية 🖼️", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            showProofreadingDialog = true
                            viewModel.runProofreadingForCurrentChapter()
                        },
                        leadingIcon = { Icon(Icons.Default.Spellcheck, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF0284C7)) },
                        label = { Text("تدقيق لغوي 🔍", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            showPlotBranchingDialog = true
                            if (plotBranchOptions.isEmpty()) {
                                viewModel.generatePlotBranchesForCurrentScene()
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF7C3AED)) },
                        label = { Text("تفرع الأحداث 🔀", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { showFontPickerDialog = true },
                        leadingIcon = { Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("الخطوط: $selectedFont", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = { showSceneBreakdownSheet = true },
                        leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("تفكيك المشاهد 🎬", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { showSnapshotHistoryDialog = true },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("استرجاع اللقطات 🕒", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { showCoversStudio = true },
                        leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("الأغلفة", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { showPaperSettingsDialog = true },
                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("الورقة", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { showAiAssistantDialog = true },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF7C3AED)) },
                        label = { Text("المساعد الذكي", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    AssistChip(
                        onClick = { showStoryboardDialog = true },
                        leadingIcon = { Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("مخطط القصة", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { showSourceReaderDialog = true },
                        leadingIcon = { Icon(Icons.Default.VerticalSplit, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("المصادر", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { showGrammarDialog = true },
                        leadingIcon = { Icon(Icons.Default.Spellcheck, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("تنقيح النص", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { showVersionHistoryDialog = true },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("سجل اللقطات", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { showGoalDialog = true },
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("هدف الكلمات", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { showSuggestionsDialog = true },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (suggestions.isNotEmpty()) Color(0xFFFFA000) else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                if (suggestions.isNotEmpty()) "اقتراحات (${suggestions.size})" else "اقتراحات",
                                fontSize = 11.sp
                            )
                        }
                    )
                    AssistChip(
                        onClick = { showExportDialog = true },
                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("تصدير", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            viewModel.saveCurrentProject()
                            Toast.makeText(context, "تم حفظ العمل بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("حفظ", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { isFocusMode = true },
                        leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.primaryColor) },
                        label = { Text("تركيز", fontSize = 11.sp) }
                    )
                }
            }

            // Word Count Goal Progress Bar
            if (targetWordCount > 0) {
                val totalWords = remember(chapters) {
                    chapters.sumOf { ch ->
                        if (ch.content.isBlank()) 0 else ch.content.trim().split("\\s+".toRegex()).size
                    }
                }
                val progress = (totalWords.toFloat() / targetWordCount.toFloat()).coerceIn(0f, 1f)
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التقدم نحو الهدف:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("$totalWords / $targetWordCount كلمة", fontSize = 10.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (progress >= 1f) Color(0xFF4CAF50) else theme.primaryColor,
                        trackColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                }
            }

            // Phase 2 TTS Audio Review Player Bar (Task 2.3)
            if (showTtsPlayer) {
                TtsPlayerBar(
                    playbackState = ttsPlaybackState,
                    currentSentence = ttsCurrentSentence,
                    currentSentenceIndex = ttsSentenceIndex,
                    currentSpeed = ttsSpeechRate,
                    theme = theme,
                    onPlay = {
                        val content = currentChapter?.content ?: ""
                        if (content.isNotBlank()) {
                            if (ttsPlaybackState == com.example.util.TtsPlaybackState.PAUSED) {
                                viewModel.ttsManager.play()
                            } else {
                                viewModel.startTtsReading(content)
                            }
                        } else {
                            Toast.makeText(context, "الفصل فارغ من النصوص للبدء في القراءة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPause = { viewModel.ttsManager.pause() },
                    onSeekForward = { viewModel.ttsManager.seekForward() },
                    onSeekBackward = { viewModel.ttsManager.seekBackward() },
                    onSpeedChange = { speed -> viewModel.ttsManager.setSpeed(speed) },
                    onClose = {
                        viewModel.stopTtsReading()
                        showTtsPlayer = false
                    }
                )
            }

            // Phase 3 Review Mode Active Banner (Task 3.1)
            if (isReviewMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.RateReview, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            Column {
                                Text("وضع القراءة والمراجعة التجريبية نشط 📝", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E40AF))
                                Text("تصفح النصوص مع ملاحظات القراء والمحررين دون التعديل على النص الأصلي", fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { showFeedbackSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("الملاحظات (${activeChapterComments.size})", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.toggleReviewMode() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("إنهاء", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Katib View Mode Selector (شريط اختيار نمط العرض: افتراضي، صفحة Word، نص Text، كتاب إلكتروني)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                color = Color(0xFFE2DDD3),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "defaultView" to "افتراضي",
                        "word" to "صفحة Word",
                        "text" to "نص Text",
                        "eBook" to "كتاب إلكتروني"
                    ).forEach { (modeKey, modeTitle) ->
                        val isSelected = katibViewMode == modeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { katibViewMode = modeKey },
                            label = {
                                Text(
                                    text = modeTitle,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD0631B),
                                containerColor = Color.White.copy(alpha = 0.8f)
                            ),
                            border = null
                        )
                    }
                }
            }

            // Paper Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentChapterImages = remember(currentChapter?.imagesJson) {
                    PageImageSerializer.fromJson(currentChapter?.imagesJson)
                }

                if (currentChapter?.sectionType == "FRONT_COVER" || currentChapter?.sectionType == "BACK_COVER") {
                    val isBack = currentChapter.sectionType == "BACK_COVER"
                    val coverTemplate = com.example.data.model.BookCoverTemplate.fromJson(currentChapter.content) ?: com.example.data.model.BookCoverTemplate(
                        title = if (isBack) "الغلاف الخلفي" else "الغلاف الأمامي",
                        bookTitle = project?.projectName ?: "عنوان الكتاب",
                        subtitle = if (isBack) "" else "كتاب إلكتروني متكامل",
                        authorName = "اسم الكاتب",
                        publisherName = if (isBack) "" else "دار النشر",
                        category = "عام"
                    )
                    CoverCanvasView(
                        template = coverTemplate,
                        isBackCover = isBack,
                        theme = theme,
                        onEditClick = { editingCoverTemplate = Pair(coverTemplate, !isBack) },
                        onOpenStudioClick = { showCoversStudio = true },
                        onDeleteCover = {
                            if (isBack) viewModel.removeProjectBackCover() else viewModel.removeProjectFrontCover()
                        }
                    )
                } else if (pageViewMode == "SINGLE_PAGE") {
                    // Single Page
                    SinglePaperSheet(
                        paperColor = currentPaperColor,
                        pageIndex = activeIdx,
                        pageNumberStyle = pageNumberStyle,
                        pageNumberPosition = pageNumberPosition,
                        chapter = currentChapter,
                        theme = theme,
                        onTitleChange = { viewModel.updateCurrentChapterTitle(it) },
                        onContentChange = { viewModel.updateCurrentChapterContent(it) },
                        onVoiceTypingClick = { onVoiceTypingClick() },
                        images = currentChapterImages,
                        onAddImage = { viewModel.addImageToActiveChapter(it) },
                        onUpdateImage = { viewModel.updateImageInActiveChapter(it) },
                        onDeleteImage = { viewModel.deleteImageFromActiveChapter(it) },
                        availableFiles = allFiles,
                        isReadOnly = isReviewMode,
                        fontFamily = activeFontFamily,
                        katibViewMode = katibViewMode
                    )
                } else {
                    // Double Page (Side by Side preview) (FR-UI-05)
                    val nextChapter = chapters.getOrNull(activeIdx + 1)
                    val nextChapterImages = remember(nextChapter?.imagesJson) {
                        PageImageSerializer.fromJson(nextChapter?.imagesJson)
                    }
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SinglePaperSheet(
                                paperColor = currentPaperColor,
                                pageIndex = activeIdx * 2,
                                pageNumberStyle = pageNumberStyle,
                                pageNumberPosition = pageNumberPosition,
                                chapter = currentChapter,
                                theme = theme,
                                onTitleChange = { viewModel.updateCurrentChapterTitle(it) },
                                onContentChange = { viewModel.updateCurrentChapterContent(it) },
                                onVoiceTypingClick = { onVoiceTypingClick() },
                                images = currentChapterImages,
                                onAddImage = { viewModel.addImageToActiveChapter(it) },
                                onUpdateImage = { viewModel.updateImageInActiveChapter(it) },
                                onDeleteImage = { viewModel.deleteImageFromActiveChapter(it) },
                                availableFiles = allFiles,
                                isReadOnly = isReviewMode,
                                fontFamily = activeFontFamily,
                                katibViewMode = katibViewMode
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SinglePaperSheet(
                                paperColor = currentPaperColor,
                                pageIndex = (activeIdx * 2) + 1,
                                pageNumberStyle = pageNumberStyle,
                                pageNumberPosition = pageNumberPosition,
                                chapter = nextChapter,
                                theme = theme,
                                onTitleChange = { /* secondary preview */ },
                                onContentChange = { /* secondary preview */ },
                                images = nextChapterImages,
                                availableFiles = allFiles,
                                isReadOnly = true,
                                fontFamily = activeFontFamily,
                                katibViewMode = katibViewMode
                            )
                        }
                    }
                }
            }

            // Katib Bottom Navigation Bar with Insets / SafeArea protection (الشريط السفلي المتكامل للتنقل بين الصفحات والأغلفة)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xFFEFEBE4),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Slider for pages / chapters
                    if (chapters.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "صفحة ${activeIdx + 1} من ${chapters.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0631B)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = activeIdx.toFloat().coerceIn(0f, (chapters.size - 1).coerceAtLeast(1).toFloat()),
                                onValueChange = { newVal ->
                                    viewModel.activeChapterIndex.value = newVal.toInt().coerceIn(0, chapters.size - 1)
                                },
                                valueRange = 0f..(chapters.size - 1).coerceAtLeast(1).toFloat(),
                                steps = if (chapters.size > 2) chapters.size - 2 else 0,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD0631B),
                                    activeTrackColor = Color(0xFFD0631B),
                                    inactiveTrackColor = Color.LightGray.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    // Navigation Buttons: السابقة، الغلاف الأمامي، الفهرس، الغلاف الخلفي، التالية
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (activeIdx > 0) {
                                    viewModel.activeChapterIndex.value = activeIdx - 1
                                }
                            },
                            enabled = activeIdx > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0631B)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("السابقة", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val frontCoverIdx = chapters.indexOfFirst { it.sectionType == "FRONT_COVER" }
                                if (frontCoverIdx >= 0) {
                                    viewModel.activeChapterIndex.value = frontCoverIdx
                                } else {
                                    val defaultTemplate = com.example.data.model.BookCoverTemplate(
                                        title = "الغلاف الأمامي",
                                        bookTitle = project?.projectName ?: "عنوان الكتاب",
                                        subtitle = "كتاب إلكتروني متكامل",
                                        authorName = "اسم المؤلف",
                                        publisherName = "دار النشر",
                                        category = "عام"
                                    )
                                    viewModel.setProjectFrontCover(defaultTemplate)
                                    viewModel.activeChapterIndex.value = 0
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0631B)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("الغلاف الأمامي", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                showCorkboardScreen = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0631B)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("الفهرس", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val backCoverIdx = chapters.indexOfFirst { it.sectionType == "BACK_COVER" }
                                if (backCoverIdx >= 0) {
                                    viewModel.activeChapterIndex.value = backCoverIdx
                                } else {
                                    val defaultTemplate = com.example.data.model.BookCoverTemplate(
                                        title = "الغلاف الخلفي",
                                        bookTitle = project?.projectName ?: "عنوان الكتاب",
                                        subtitle = "الخاتمة والنبذة",
                                        authorName = "اسم المؤلف",
                                        publisherName = "دار النشر",
                                        category = "عام"
                                    )
                                    viewModel.setProjectBackCover(defaultTemplate)
                                    viewModel.activeChapterIndex.value = chapters.size - 1
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0631B)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("الغلاف الخلفي", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (activeIdx < chapters.size - 1) {
                                    viewModel.activeChapterIndex.value = activeIdx + 1
                                }
                            },
                            enabled = activeIdx < chapters.size - 1,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0631B)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("التالية", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Supporting Pane (Tablets, Foldables & Multi-Window - Task 1.4)
        if (isDualPane && isSupportingPaneVisible) {
            SupportingPaneContent(
                activeChapter = currentChapter,
                allChapters = chapters,
                activeChapterIndex = activeIdx,
                scenes = currentChapterScenes,
                storyElements = storyElements,
                extractedTexts = allExtractedTexts,
                snapshots = detailedSnapshots,
                theme = theme,
                onSelectChapter = { viewModel.selectChapter(it) },
                onAddChapter = { viewModel.addNewChapter("فصل جديد") },
                onAddScene = { viewModel.addSceneToActiveChapter("مشهد جديد") },
                onUpdateScene = { viewModel.updateSceneInActiveChapter(it) },
                onDeleteScene = { viewModel.deleteSceneFromActiveChapter(it) },
                onReorderScenes = { f, t -> viewModel.reorderScenesInActiveChapter(f, t) },
                onAddStoryElement = { showStoryboardDialog = true },
                onDeleteStoryElement = { viewModel.deleteStoryElement(it) },
                onInsertTextIntoActiveChapter = {
                    val cur = currentChapter?.content ?: ""
                    viewModel.updateCurrentChapterContent(if (cur.isBlank()) it.trim() else "$cur$it")
                },
                onRestoreSnapshot = { viewModel.restoreDetailedSnapshot(it) },
                onCaptureSnapshotNow = {
                    if (currentChapter != null) viewModel.captureChapterSnapshot(currentChapter, isManual = true)
                },
                onCloseSupportingPane = { isSupportingPaneVisible = false },
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
            )
        }
    }
}
    }

    // Modal: Paper & Layout Customization (FR-UI-03..07)
    if (showPaperSettingsDialog) {
        PaperSettingsDialog(
            currentPaperColor = paperColorHex,
            currentPaperSize = paperSize,
            currentViewMode = pageViewMode,
            currentNumberStyle = pageNumberStyle,
            currentNumberPosition = pageNumberPosition,
            theme = theme,
            onDismiss = { showPaperSettingsDialog = false },
            onApply = { color, size, mode, numStyle, numPos ->
                viewModel.editorPaperColor.value = color
                viewModel.editorPaperSize.value = size
                viewModel.editorPageViewMode.value = mode
                viewModel.editorPageNumberStyle.value = numStyle
                viewModel.editorPageNumberPosition.value = numPos
                showPaperSettingsDialog = false
            }
        )
    }

    // Modal: Smart Emotion Suggestions Dashboard (FR-ES-03)
    if (showSuggestionsDialog) {
        EmotionSuggestionsDialog(
            suggestions = suggestions,
            theme = theme,
            onDismiss = { showSuggestionsDialog = false },
            onAccept = { suggestion -> viewModel.acceptEmotionSuggestion(suggestion) },
            onReject = { id -> viewModel.rejectEmotionSuggestion(id) }
        )
    }

    // Modal: AI Writing Assistant (FR-SE-02, Gemini AI Copilot)
    if (showAiAssistantDialog) {
        AiAssistantDialog(
            theme = theme,
            isGenerating = isAiGenerating,
            onDismiss = { showAiAssistantDialog = false },
            onInsertIntro = {
                viewModel.applyAiIntroduction()
                showAiAssistantDialog = false
            },
            onInsertConclusion = {
                viewModel.applyAiConclusion()
                showAiAssistantDialog = false
            },
            onRefineStyle = {
                viewModel.generateAiContentForActiveChapter("REFINE")
                showAiAssistantDialog = false
            },
            onExpandWithAi = {
                viewModel.generateAiContentForActiveChapter("EXPAND")
                showAiAssistantDialog = false
            },
            onCustomAiPrompt = { query ->
                viewModel.generateAiContentForActiveChapter("CUSTOM", query)
                showAiAssistantDialog = false
            }
        )
    }

    // Modal: Storyboard & Narrative Arc (FR-Pro-03)
    if (showStoryboardDialog) {
        StoryboardAndArcDialog(
            chapters = chapters,
            storyElements = storyElements,
            theme = theme,
            onDismiss = { showStoryboardDialog = false },
            onUpdateChapterTension = { idx, tension ->
                viewModel.updateChapterTension(idx, tension)
            },
            onAddStoryElement = { name, role, desc, motiv, colorHex ->
                viewModel.addStoryElement(name, role, desc, motiv, colorHex)
            },
            onDeleteStoryElement = { element ->
                viewModel.deleteStoryElement(element)
            }
        )
    }

    // Modal: Split Source Reader (FR-Pro-04)
    if (showSourceReaderDialog) {
        SplitSourceReaderDialog(
            files = allFiles,
            theme = theme,
            onDismiss = { showSourceReaderDialog = false },
            onInsertTextIntoEditor = { quotedText ->
                val curr = currentChapter?.content ?: ""
                val updated = if (curr.isBlank()) "『اقتباس』:\n$quotedText" else "$curr\n\n『اقتباس』:\n$quotedText"
                viewModel.updateCurrentChapterContent(updated)
            }
        )
    }

    // Modal: Version History & Snapshots (FR-Pro-06)
    if (showVersionHistoryDialog && project != null) {
        VersionHistoryDialog(
            project = project!!,
            snapshots = snapshots,
            theme = theme,
            onDismiss = { showVersionHistoryDialog = false },
            onCreateSnapshot = { label ->
                viewModel.createProjectSnapshot(label)
            },
            onRestoreSnapshot = { snapshot ->
                viewModel.restoreProjectSnapshot(snapshot)
            },
            onDeleteSnapshot = { snapshotId ->
                viewModel.deleteProjectSnapshot(snapshotId)
            }
        )
    }

    // Modal: Rename Tab
    if (showRenameTabDialog && currentChapter != null) {
        RenameTabDialog(
            currentTitle = currentChapter.title,
            theme = theme,
            onDismiss = { showRenameTabDialog = false },
            onSave = { newTitle ->
                viewModel.updateCurrentChapterTitle(newTitle)
                showRenameTabDialog = false
            }
        )
    }

    if (showGoalDialog) {
        TargetWordCountDialog(
            currentGoal = targetWordCount,
            theme = theme,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                viewModel.updateTargetWordCount(newGoal)
                showGoalDialog = false
            }
        )
    }

    // Modal: Export Dialog (FR-13, FR-14)
    if (showExportDialog) {
        ExportDialog(
            theme = theme,
            pageCount = chapters.size + 2,
            initialMetadata = exportMetadata,
            onUpdateMetadata = { viewModel.updateExportMetadata(it) },
            onDismiss = { showExportDialog = false },
            onPreflightCheck = { showPreflightDialog = true },
            onExportPdf = {
                val file = viewModel.exportToPdf(context)
                if (file != null) {
                    shareFile(context, file, "application/pdf")
                } else {
                    Toast.makeText(context, "فشل تصدير ملف PDF", Toast.LENGTH_SHORT).show()
                }
                showExportDialog = false
            },
            onExportDocx = {
                val file = viewModel.exportToDocx(context)
                if (file != null) {
                    shareFile(context, file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                } else {
                    Toast.makeText(context, "فشل تصدير ملف DOCX", Toast.LENGTH_SHORT).show()
                }
                showExportDialog = false
            },
            onExportEpub = {
                val file = viewModel.exportToEpub(context)
                if (file != null) {
                    shareFile(context, file, "application/epub+zip")
                } else {
                    Toast.makeText(context, "فشل تصدير كتاب EPUB", Toast.LENGTH_SHORT).show()
                }
                showExportDialog = false
            },
            onExportTxt = {
                val file = viewModel.exportToTxt(context)
                if (file != null) {
                    shareFile(context, file, "text/plain")
                    Toast.makeText(context, "تم تصدير وحفظ ملف TXT بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "فشل تصدير ملف TXT", Toast.LENGTH_SHORT).show()
                }
                showExportDialog = false
            },
            onExportHtml = {
                val file = viewModel.exportToHtml(context)
                if (file != null) {
                    shareFile(context, file, "text/html")
                    Toast.makeText(context, "تم تصدير وحفظ ملف HTML بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "فشل تصدير ملف HTML", Toast.LENGTH_SHORT).show()
                }
                showExportDialog = false
            }
        )
    }

    // Modal: Edit Cover Texts & Colors Dialog
    if (editingCoverTemplate != null) {
        val (templateToEdit, isFront) = editingCoverTemplate!!
        CoverCustomizerDialog(
            initialTemplate = templateToEdit,
            theme = theme,
            onDismiss = { editingCoverTemplate = null },
            onSaveAsCustomTemplate = { customTmpl ->
                val repo = com.example.data.repository.BookCoverRepository(context)
                repo.addCustomTemplate(customTmpl)
            },
            onApplyToProject = { updatedTmpl, appliedIsFront ->
                if (appliedIsFront) {
                    viewModel.setProjectFrontCover(updatedTmpl)
                } else {
                    viewModel.setProjectBackCover(updatedTmpl)
                }
                editingCoverTemplate = null
            }
        )
    }

    // Modal: Grammar and Text Refinement (تنقيح وتصحيح النصوص والأخطاء)
    if (showGrammarDialog) {
        GrammarRefinementDialog(
            theme = theme,
            currentContent = currentChapter?.content ?: "",
            onDismiss = { showGrammarDialog = false },
            onApplyReplacements = { correctedText ->
                viewModel.updateCurrentChapterContent(correctedText)
                Toast.makeText(context, "تم تطبيق التعديلات اللغوية بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal: Project Sharing with Accept/Reject Permissions (مشاركة المشاريع وصلاحيات النشر)
    if (showShareDialog) {
        ProjectShareDialog(
            theme = theme,
            projectName = project?.projectName ?: "مشروعي",
            viewModel = viewModel,
            onDismiss = { showShareDialog = false }
        )
    }

    // Phase 1: Snapshot History Dialog (Task 1.1)
    if (showSnapshotHistoryDialog) {
        SnapshotHistoryDialog(
            currentChapter = currentChapter,
            chapterSnapshots = detailedSnapshots,
            allSnapshots = allDetailedSnapshots,
            theme = theme,
            onDismiss = { showSnapshotHistoryDialog = false },
            onCaptureSnapshotNow = {
                if (currentChapter != null) {
                    viewModel.captureChapterSnapshot(currentChapter, isManual = true)
                }
            },
            onRestoreSnapshot = { snapshot ->
                viewModel.restoreDetailedSnapshot(snapshot)
                showSnapshotHistoryDialog = false
            },
            onDeleteSnapshot = { id ->
                viewModel.deleteDetailedSnapshot(id)
            }
        )
    }

    // Phase 1: Scene Breakdown Sheet (Task 1.2)
    if (showSceneBreakdownSheet) {
        SceneBreakdownSheet(
            chapter = currentChapter,
            scenes = currentChapterScenes,
            theme = theme,
            onDismiss = { showSceneBreakdownSheet = false },
            onAddScene = { title -> viewModel.addSceneToActiveChapter(title) },
            onUpdateScene = { scene -> viewModel.updateSceneInActiveChapter(scene) },
            onDeleteScene = { sceneId -> viewModel.deleteSceneFromActiveChapter(sceneId) },
            onReorderScenes = { f, t -> viewModel.reorderScenesInActiveChapter(f, t) },
            onAutoSplit = { viewModel.autoSplitActiveChapterIntoScenes() },
            onMergeScenes = { viewModel.mergeScenesIntoActiveChapter() }
        )
    }

    // Phase 1: Custom Font Picker Dialog (Task 1.3)
    if (showFontPickerDialog) {
        CustomFontPickerDialog(
            currentSelectedFont = selectedFont,
            customFonts = customFonts,
            theme = theme,
            onDismiss = { showFontPickerDialog = false },
            onSelectFont = { fontName ->
                viewModel.selectEditorFont(fontName)
            },
            onImportFont = { uri ->
                viewModel.importCustomFont(uri)
            },
            onDeleteCustomFont = { id ->
                viewModel.deleteCustomFont(id)
            }
        )
    }

    // Phase 2: Stock Image Picker BottomSheet (Task 2.2)
    if (showStockImagePicker) {
        StockImagePickerBottomSheet(
            theme = theme,
            onDismiss = { showStockImagePicker = false },
            onImageDownloadedAndSelected = { localPath ->
                val newImg = PageImageItem(
                    sourceUri = localPath,
                    title = "صورة سحابية",
                    caption = "صورة مجانية عالية الدقة",
                    widthDp = 220f,
                    heightDp = 160f,
                    alignment = "CENTER"
                )
                viewModel.addImageToActiveChapter(newImg)
                showStockImagePicker = false
                Toast.makeText(context, "تم تحميل الصورة بنجاح وإدراجها في الصفحة!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Phase 2: AI Proofreading Dialog
    if (showProofreadingDialog) {
        ProofreadingSheetDialog(
            issues = proofreadingIssues,
            isLoading = isProofreading,
            theme = theme,
            onDismiss = { showProofreadingDialog = false },
            onApplySingleFix = { issue ->
                viewModel.applyProofreadingFix(issue)
            },
            onApplyAllFixes = { issues ->
                viewModel.applyAllProofreadingFixes(issues)
            }
        )
    }

    // Phase 2: AI Plot Branching Dialog
    if (showPlotBranchingDialog) {
        PlotBranchingDialog(
            options = plotBranchOptions,
            isLoading = isGeneratingBranches,
            theme = theme,
            onDismiss = { showPlotBranchingDialog = false },
            onSelectBranch = { branch ->
                viewModel.selectPlotBranch(branch)
                showPlotBranchingDialog = false
            }
        )
    }

    // Phase 3: Beta Reader Feedback Sheet (Task 3.1)
    if (showFeedbackSheet) {
        FeedbackCommentsSheet(
            chapterTitle = currentChapter?.title ?: "الفصل الحالي",
            comments = activeChapterComments,
            theme = theme,
            onDismiss = { showFeedbackSheet = false },
            onAddComment = { author, quote, body ->
                viewModel.addChapterComment(author, quote, body)
            },
            onToggleResolved = { id, current ->
                viewModel.toggleCommentResolved(id, current)
            },
            onReplyToComment = { id, reply ->
                viewModel.replyToComment(id, reply)
            },
            onDeleteComment = { id ->
                viewModel.deleteComment(id)
            }
        )
    }

    // Phase 3: AI Text-to-Image Generator Dialog (Task 3.2)
    if (showAiImageGeneratorDialog) {
        ImageGeneratorDialog(
            theme = theme,
            onDismiss = { showAiImageGeneratorDialog = false },
            onApplyToCover = { imagePath ->
                val currentCover = viewModel.getProjectFrontCover() ?: com.example.data.model.BookCoverTemplate(
                    title = "الغلاف الذكي",
                    bookTitle = project?.projectName ?: "عنوان الكتاب",
                    authorName = "اسم المؤلف",
                    backgroundType = "IMAGE_URI",
                    imageUri = imagePath
                )
                viewModel.setProjectFrontCover(currentCover.copy(backgroundType = "IMAGE_URI", imageUri = imagePath))
                Toast.makeText(context, "تم تعيين اللوحة المولدة بالذكاء الاصطناعي كغلاف للكتاب!", Toast.LENGTH_SHORT).show()
            },
            onInsertIntoPage = { imagePath ->
                val newImg = PageImageItem(
                    sourceUri = imagePath,
                    title = "لوحة ذكاء اصطناعي",
                    caption = "تم التوليد عبر Imagen",
                    widthDp = 240f,
                    heightDp = 180f,
                    alignment = "CENTER"
                )
                viewModel.addImageToActiveChapter(newImg)
                Toast.makeText(context, "تم إدراج اللوحة في صفحة الفصل!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Phase 4: Writing Analytics Dialog (Task 4.3)
    if (showAnalyticsDialog) {
        AnalyticsDashboardDialog(
            theme = theme,
            viewModel = viewModel,
            onDismiss = { showAnalyticsDialog = false }
        )
    }

    // Phase 4: Pre-flight Global Publishing Check (Task 4.4)
    if (showPreflightDialog) {
        val report = remember { viewModel.runPreflightValidation(context) }
        com.example.ui.components.PreflightValidationDialog(
            report = report,
            theme = theme,
            onDismiss = { showPreflightDialog = false },
            onProceedExportEpub = {
                val f = viewModel.exportToEpub(context)
                if (f != null) shareFile(context, f, "application/epub+zip")
            },
            onProceedExportPdf = {
                val f = viewModel.exportToPdf(context)
                if (f != null) shareFile(context, f, "application/pdf")
            }
        )
    }

    // Phase 4: Google Play Billing Paywall (Task 4.2)
    if (showPaywallDialog) {
        PaywallScreen(
            theme = theme,
            viewModel = viewModel,
            onDismiss = { showPaywallDialog = false }
        )
    }
}

private fun shareFile(context: android.content.Context, file: java.io.File, mimeType: String) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة المستند عبر:"))
    } catch (e: Exception) {
        Toast.makeText(context, "تم حفظ الملف بنجاح في: ${file.name}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun SinglePaperSheet(
    paperColor: Color,
    pageIndex: Int,
    pageNumberStyle: Int,
    pageNumberPosition: String,
    chapter: ChapterItem?,
    theme: com.example.data.model.AppThemeData,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onVoiceTypingClick: () -> Unit = {},
    images: List<PageImageItem> = emptyList(),
    onAddImage: (PageImageItem) -> Unit = {},
    onUpdateImage: (PageImageItem) -> Unit = {},
    onDeleteImage: (String) -> Unit = {},
    availableFiles: List<AppFile> = emptyList(),
    isReadOnly: Boolean = false,
    fontFamily: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    katibViewMode: String = "defaultView"
) {
    // Undo / Redo history & Save Checkpoints
    val coroutineScope = rememberCoroutineScope()
    val localCtx = LocalContext.current
    var undoStack by remember(chapter?.id) { mutableStateOf(listOf<String>()) }
    var redoStack by remember(chapter?.id) { mutableStateOf(listOf<String>()) }
    var currentContent by remember(chapter?.id, chapter?.content) { mutableStateOf(chapter?.content ?: "") }
    var lastSavedCheckpoint by remember(chapter?.id) { mutableStateOf(chapter?.content ?: "") }

    // Text formatting state
    var fontSizeSp by remember { mutableIntStateOf(16) }
    var lineSpacingFactor by remember { mutableFloatStateOf(1.5f) }
    var textAlign by remember { mutableStateOf(TextAlign.Right) }
    var textColor by remember { mutableStateOf(Color(0xFF2C2C2C)) }
    var highlightColor by remember { mutableStateOf<Color?>(null) }

    // Dialog & selection state
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var editingImageItem by remember { mutableStateOf<PageImageItem?>(null) }
    var selectedImageId by remember { mutableStateOf<String?>(null) }
    var showSearchReplaceDialog by remember { mutableStateOf(false) }

    // View mode styling: Word (32dp margin, sharp A4), Text (12dp compact), eBook (24dp cream)
    val pageMargins = when (katibViewMode) {
        "word" -> 32.dp
        "text" -> 12.dp
        else -> 24.dp
    }
    val cardCornerRadius = when (katibViewMode) {
        "word" -> 0.dp
        "text" -> 8.dp
        else -> 12.dp
    }
    val effectivePaperColor = when (katibViewMode) {
        "word" -> Color(0xFFFFFDF9)
        "eBook" -> Color(0xFFFFFDF9)
        else -> paperColor
    }

    fun handleTextEdit(newText: String) {
        if (newText != currentContent) {
            undoStack = (undoStack + currentContent).takeLast(50)
            redoStack = emptyList()
            currentContent = newText
            onContentChange(newText)
        }
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.last()
            undoStack = undoStack.dropLast(1)
            redoStack = redoStack + currentContent
            currentContent = previous
            onContentChange(previous)
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.last()
            redoStack = redoStack.dropLast(1)
            undoStack = undoStack + currentContent
            currentContent = next
            onContentChange(next)
        }
    }

    fun handleUndoToSaveCheckpoint() {
        if (undoStack.isEmpty()) {
            Toast.makeText(localCtx, "لا توجد تعديلات سابقة للتراجع عنها", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            var steps = 0
            while (undoStack.isNotEmpty()) {
                val previous = undoStack.last()
                undoStack = undoStack.dropLast(1)
                redoStack = redoStack + currentContent
                currentContent = previous
                onContentChange(previous)
                steps++

                // التوقف عند الوصول إلى آخر موضع قام فيه المستخدم بالحفظ أو عند نهاية المكدس
                if (previous == lastSavedCheckpoint || undoStack.isEmpty()) {
                    break
                }
                kotlinx.coroutines.delay(80)
            }
            Toast.makeText(localCtx, "تم التراجع ($steps خطوات) إلى آخر موضع محفوظ 💾", Toast.LENGTH_SHORT).show()
        }
    }

    fun insertFormatting(prefix: String, suffix: String, placeholder: String) {
        val snippet = "$prefix$placeholder$suffix"
        val newText = if (currentContent.isBlank()) snippet else "$currentContent $snippet"
        handleTextEdit(newText)
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(if (katibViewMode == "text") 2.dp else 10.dp, RoundedCornerShape(cardCornerRadius), spotColor = Color(0x44000000))
            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(cardCornerRadius)),
        shape = RoundedCornerShape(cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = effectivePaperColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pageMargins),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Katib Chapter Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (katibViewMode) {
                        "word" -> "مستند Word A4"
                        "text" -> "نص حر"
                        "eBook" -> "كتاب إلكتروني"
                        else -> "مشروع"
                    },
                    style = androidx.compose.ui.text.TextStyle(color = Color.Gray, fontSize = 11.sp)
                )
                Text(
                    text = "الفصل ${pageIndex + 1}: ${chapter?.title?.ifBlank { "الحدث المحفز" } ?: "الفصل 1"}",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFFD0631B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp)
            // Top Page Number (if position is TOP_*)
            if (pageNumberPosition.startsWith("TOP")) {
                PageNumberView(
                    pageIndex = pageIndex,
                    styleId = pageNumberStyle,
                    position = pageNumberPosition
                )
            }

            // Editable Chapter Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (chapter != null) {
                    if (!isReadOnly) {
                        OutlinedTextField(
                            value = chapter.title,
                            onValueChange = onTitleChange,
                            placeholder = { Text("عنوان الفصل أو القسم") },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = fontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C)
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = theme.primaryColor,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )

                        // Rich Writing & Text Editing Toolbar
                        RichWritingToolbar(
                            theme = theme,
                            fontSizeSp = fontSizeSp,
                            onFontSizeChange = { fontSizeSp = it },
                            textAlign = textAlign,
                            onTextAlignChange = { textAlign = it },
                            lineSpacingFactor = lineSpacingFactor,
                            onLineSpacingChange = { lineSpacingFactor = it },
                            textColor = textColor,
                            onTextColorChange = { textColor = it },
                            highlightColor = highlightColor,
                            onHighlightColorChange = { highlightColor = it },
                            canUndo = undoStack.isNotEmpty(),
                            canRedo = redoStack.isNotEmpty(),
                            onUndo = { handleUndo() },
                            onRedo = { handleRedo() },
                            onUndoToSaveCheckpoint = { handleUndoToSaveCheckpoint() },
                            undoCount = undoStack.size,
                            redoCount = redoStack.size,
                            onInsertText = { p, s, h -> insertFormatting(p, s, h) },
                            onAddImageClick = { showImagePickerDialog = true },
                            onSearchReplaceClick = { showSearchReplaceDialog = true },
                            onVoiceTypingClick = { onVoiceTypingClick() },
                            content = currentContent,
                            imagesCount = images.size
                        )
                    } else {
                        Text(
                            text = chapter.title,
                            fontFamily = fontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C2C2C)
                        )
                    }

                    Divider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp)

                    // Images Container (Multiple draggable & customizable images on page)
                    if (images.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!isReadOnly) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Collections,
                                            contentDescription = null,
                                            tint = theme.primaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "الصور المدرجة في الصفحة (${images.size}) • يمكن سحبها وتحريكها بأي اتجاه",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.primaryColor
                                        )
                                    }
                                    TextButton(
                                        onClick = { showImagePickerDialog = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("إضافة صورة أخرى", fontSize = 10.sp)
                                    }
                                }
                            }

                            images.forEach { img ->
                                DraggablePageImage(
                                    image = img,
                                    theme = theme,
                                    isSelected = selectedImageId == img.id,
                                    onSelect = { selectedImageId = img.id },
                                    onUpdateOffset = { newX, newY ->
                                        onUpdateImage(img.copy(offsetX = newX, offsetY = newY))
                                    },
                                    onOpenSettings = { editingImageItem = img },
                                    onDelete = { onDeleteImage(img.id) }
                                )
                            }
                        }
                    }

                    // Main Editable Content
                    if (!isReadOnly) {
                        OutlinedTextField(
                            value = currentContent,
                            onValueChange = { handleTextEdit(it) },
                            placeholder = { Text("ابدأ بكتابة أفكارك وصياغة محتواك هنا...") },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = fontFamily,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * lineSpacingFactor).sp,
                                color = textColor,
                                textAlign = textAlign
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 350.dp)
                                .then(
                                    if (highlightColor != null) {
                                        Modifier.background(highlightColor!!.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                    } else Modifier
                                ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            text = if (chapter.content.isNotBlank()) chapter.content else "(صفحة فارغة)",
                            fontFamily = fontFamily,
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            color = Color(0xFF3C3C3C),
                            textAlign = textAlign
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("(نهاية المستند)", color = Color.Gray)
                    }
                }
            }

            // Bottom Page Number (if position is BOTTOM_*)
            if (pageNumberPosition.startsWith("BOTTOM")) {
                PageNumberView(
                    pageIndex = pageIndex,
                    styleId = pageNumberStyle,
                    position = pageNumberPosition
                )
            } else if (katibViewMode != "text") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${pageIndex + 1}",
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    // Modal Dialogs for Images and Text Tools
    if (showImagePickerDialog) {
        ImagePickerDialog(
            theme = theme,
            availableFiles = availableFiles,
            onDismiss = { showImagePickerDialog = false },
            onImageSelected = { newImg ->
                onAddImage(newImg)
                selectedImageId = newImg.id
            }
        )
    }

    if (editingImageItem != null) {
        PageImageControlDialog(
            image = editingImageItem!!,
            theme = theme,
            onDismiss = { editingImageItem = null },
            onSave = { updated ->
                onUpdateImage(updated)
                editingImageItem = null
            },
            onDelete = {
                onDeleteImage(editingImageItem!!.id)
                editingImageItem = null
            },
            onResetPosition = {
                onUpdateImage(editingImageItem!!.copy(offsetX = 0f, offsetY = 0f))
                editingImageItem = null
            }
        )
    }

    if (showSearchReplaceDialog) {
        SearchAndReplaceDialog(
            theme = theme,
            currentText = currentContent,
            onDismiss = { showSearchReplaceDialog = false },
            onApplyReplace = { replacedText ->
                handleTextEdit(replacedText)
            }
        )
    }
}

@Composable
fun PageNumberView(
    pageIndex: Int,
    styleId: Int,
    position: String
) {
    val text = formatPageNumber(pageIndex, styleId)
    val alignment = when {
        position.endsWith("LEFT") -> Alignment.Start
        position.endsWith("RIGHT") -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = when {
            position.endsWith("LEFT") -> Alignment.CenterStart
            position.endsWith("RIGHT") -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PaperSettingsDialog(
    currentPaperColor: String,
    currentPaperSize: String,
    currentViewMode: String,
    currentNumberStyle: Int,
    currentNumberPosition: String,
    theme: com.example.data.model.AppThemeData,
    onDismiss: () -> Unit,
    onApply: (color: String, size: String, mode: String, numStyle: Int, numPos: String) -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentPaperColor) }
    var selectedSize by remember { mutableStateOf(currentPaperSize) }
    var selectedMode by remember { mutableStateOf(currentViewMode) }
    var selectedNumStyle by remember { mutableIntStateOf(currentNumberStyle) }
    var selectedNumPos by remember { mutableStateOf(currentNumberPosition) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "تخصيص الورقة والصفحة (FR-UI-03..07)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Paper Color (FR-UI-03)
                    Text("1. لون الورقة (FR-UI-03):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paperColorOptions.forEach { opt ->
                            val isChosen = selectedColor.equals(opt.hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(opt.color)
                                    .border(
                                        width = if (isChosen) 3.dp else 1.dp,
                                        color = if (isChosen) theme.primaryColor else Color.Gray.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = opt.hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChosen) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // 2. Paper Size (FR-UI-04)
                    Text("2. مقاس الورقة (FR-UI-04):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paperSizeOptions.forEach { sOpt ->
                            val isChosen = selectedSize == sOpt.id
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedSize = sOpt.id },
                                label = { Text(sOpt.id, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // 3. View Mode: Single vs Double Page (FR-UI-05)
                    Text("3. نمط العرض (FR-UI-05):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == "SINGLE_PAGE",
                            onClick = { selectedMode = "SINGLE_PAGE" },
                            label = { Text("صفحة مفردة", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primaryColor,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedMode == "DOUBLE_PAGE",
                            onClick = { selectedMode = "DOUBLE_PAGE" },
                            label = { Text("صفحتين متجاورتين", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primaryColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    // 4. Numbering Style (FR-UI-06)
                    Text("4. شكل ترقيم الصفحات (6 أشكال - FR-UI-06):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        numberingStyleOptions.forEach { styleOpt ->
                            val isChosen = selectedNumStyle == styleOpt.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedNumStyle = styleOpt.id }
                                    .background(if (isChosen) theme.primaryColor.copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(styleOpt.name, fontSize = 12.sp)
                                Text(styleOpt.sampleText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor)
                            }
                        }
                    }

                    // 5. Numbering Position (FR-UI-07)
                    Text("5. موضع الترقيم (FR-UI-07):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "BOTTOM_CENTER" to "أسفل الوسط",
                            "BOTTOM_RIGHT" to "أسفل اليمين",
                            "BOTTOM_LEFT" to "أسفل اليسار"
                        ).forEach { (posKey, label) ->
                            FilterChip(
                                selected = selectedNumPos == posKey,
                                onClick = { selectedNumPos = posKey },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primaryColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onApply(selectedColor, selectedSize, selectedMode, selectedNumStyle, selectedNumPos)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تطبيق الإعدادات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmotionSuggestionsDialog(
    suggestions: List<EmotionSuggestion>,
    theme: com.example.data.model.AppThemeData,
    onDismiss: () -> Unit,
    onAccept: (EmotionSuggestion) -> Unit,
    onReject: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = Color(0xFFFFA000))
                        Text(
                            text = "لوحة الاقتراحات الذكية (FR-ES-03)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "بناءً على المشاعر التي اخترتها، يقترح النظام التعديلات التالية:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(suggestions, key = { it.id }) { sugg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(sugg.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.primaryColor)
                                Text(sugg.message, fontSize = 12.sp, lineHeight = 18.sp, color = Color.DarkGray)

                                if (sugg.suggestedText != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF5F5F5))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "نص مقترح:\n${sugg.suggestedText}",
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            color = Color.Black
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onDismiss() }) {
                                        Text("تجاهل ⏭️", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TextButton(onClick = { onReject(sugg.id) }) {
                                        Text("رفض ❌", fontSize = 11.sp, color = Color.Red)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Button(
                                        onClick = { onAccept(sugg) },
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("قبول وتطبيق ✅", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun AiAssistantDialog(
    theme: com.example.data.model.AppThemeData,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onInsertIntro: () -> Unit,
    onInsertConclusion: () -> Unit,
    onRefineStyle: () -> Unit,
    onExpandWithAi: () -> Unit,
    onCustomAiPrompt: (String) -> Unit
) {
    var customQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = theme.primaryColor)
                        Text(
                            text = "مساعد التأليف الذكي (Gemini AI Copilot)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = theme.primaryColor)
                            Text("جارٍ معالجة وتوليد النص الذكي...", fontSize = 12.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "اختر أحد الإجراءات الذكية أو اكتب أمراً مخصصاً لنموذج Gemini AI:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Custom prompt input
                    OutlinedTextField(
                        value = customQuery,
                        onValueChange = { customQuery = it },
                        placeholder = { Text("اكتب طلباً للذكاء (مثال: صف المشهد بأسلوب غامض ومؤثر)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            if (customQuery.isNotBlank()) {
                                onCustomAiPrompt(customQuery)
                            }
                        },
                        enabled = customQuery.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنفيذ الأمر المخصص عبر Gemini", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Divider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExpandWithAi,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LibraryAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("توسيع الفصل", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onRefineStyle,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تنقيح وبلاغة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onInsertIntro,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("مقدمة القالب", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onInsertConclusion,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("خاتمة القالب", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RenameTabDialog(
    currentTitle: String,
    theme: com.example.data.model.AppThemeData,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تعديل عنوان التبويب / الفصل", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textColor)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (title.isNotBlank()) onSave(title) },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ")
                    }
                }
            }
        }
    }
}

@Composable
fun ExportDialog(
    theme: com.example.data.model.AppThemeData,
    pageCount: Int = 10,
    initialMetadata: com.example.data.model.ExportAdvancedMetadata = com.example.data.model.ExportAdvancedMetadata(),
    onUpdateMetadata: (com.example.data.model.ExportAdvancedMetadata) -> Unit = {},
    onDismiss: () -> Unit,
    onPreflightCheck: () -> Unit = {},
    onExportPdf: () -> Unit,
    onExportDocx: () -> Unit,
    onExportEpub: () -> Unit,
    onExportTxt: () -> Unit,
    onExportHtml: () -> Unit
) {
    var metaIsbn by remember { mutableStateOf(initialMetadata.isbn) }
    var metaPublisher by remember { mutableStateOf(initialMetadata.publisher) }
    var metaMirrorMargins by remember { mutableStateOf(initialMetadata.mirrorMargins) }
    var metaTrimSize by remember { mutableStateOf(initialMetadata.trimSize) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تصدير الكتاب وحفظه في الهاتف",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Advanced Metadata & Printing Specs (Task 3.4)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("البيانات التعريفية ومعايير الطباعة ⚙️", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.primaryColor)
                            Text("ISBN وهندسة الكعب", fontSize = 10.sp, color = Color.Gray)
                        }

                        OutlinedTextField(
                            value = metaIsbn,
                            onValueChange = {
                                metaIsbn = it
                                onUpdateMetadata(initialMetadata.copy(isbn = it, publisher = metaPublisher, mirrorMargins = metaMirrorMargins, trimSize = metaTrimSize))
                            },
                            label = { Text("رقم الـ ISBN الدولي", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = metaPublisher,
                            onValueChange = {
                                metaPublisher = it
                                onUpdateMetadata(initialMetadata.copy(isbn = metaIsbn, publisher = it, mirrorMargins = metaMirrorMargins, trimSize = metaTrimSize))
                            },
                            label = { Text("دار النشر والتوزيع", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Standard Book Trim Sizes (Task 3.4)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("مقاس الكتاب والقطع القياسي (Trim Size):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "A5" to "A5 (روايات وجيب 148×210)",
                                    "B5" to "B5 (أكاديمي 176×250)",
                                    "ROYAL_OCTAVO" to "Royal Octavo (نشر 6×9\")",
                                    "DIGEST" to "Digest (5.5×8.5\")",
                                    "A4" to "A4 قياسي"
                                ).forEach { (code, label) ->
                                    FilterChip(
                                        selected = metaTrimSize == code,
                                        onClick = {
                                            metaTrimSize = code
                                            onUpdateMetadata(initialMetadata.copy(isbn = metaIsbn, publisher = metaPublisher, mirrorMargins = metaMirrorMargins, trimSize = code))
                                        },
                                        label = { Text(label, fontSize = 10.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("الهوامش الحية للطباعة المزدوجة (Mirror Margins)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("توسيع حاشية كعب التجليد (56pt) للطباعة على الوجهين", fontSize = 9.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = metaMirrorMargins,
                                onCheckedChange = {
                                    metaMirrorMargins = it
                                    onUpdateMetadata(initialMetadata.copy(isbn = metaIsbn, publisher = metaPublisher, mirrorMargins = it, trimSize = metaTrimSize))
                                }
                            )
                        }

                        // Live calculated Spine Thickness
                        val spineMm = com.example.util.PdfExporter.calculateSpineThicknessMm(pageCount, initialMetadata.paperWeightGsm)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("كعب الكتاب المقدر تلقائياً (Spine):", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                                Text(String.format(java.util.Locale.US, "%.2f ملم (%d صفحة)", spineMm, pageCount), fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Pre-flight Validation Card (Task 4.4)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPreflightCheck() }
                        .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(30.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("فحص الجودة والامتثال للنشر (Pre-flight Check) ✈️", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E40AF))
                            Text("التحقق من دقة الصور (300 DPI)، الهوامش المفقودة، واكتمال ISBN قبل التصدير", fontSize = 11.sp, color = Color(0xFF2563EB))
                        }
                    }
                }

                Text(
                    text = "اختر الصيغة المطلوبة لتصدير وحفظ كتابك مباشرة على هاتفك أو مشاركته:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                // PDF Export Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportPdf() }
                        .border(1.dp, Color(0xFFD32F2F).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(30.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تصدير كملف PDF جاهز للطباعة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("يشمل الغلاف وترقيم الصفحات وتنسيقات الهوامش", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // DOCX Export Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportDocx() }
                        .border(1.dp, Color(0xFF1976D2).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F9FF))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(30.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تصدير كملف وورد Word (DOCX)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("قابل للتعديل عبر Microsoft Word و Google Docs", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // EPUB E-Book Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportEpub() }
                        .border(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(30.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تصدير كتاب إلكتروني (EPUB 3.0)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("متوافق مع تطبيقات Apple Books و Google Play", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // TXT Export Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportTxt() }
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(30.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تصدير كملف نصي عادي (TXT)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("نص نقي خفيف ومناسب لجميع الأجهزة والحواسب", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // HTML Export Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportHtml() }
                        .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(30.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تصدير كصفحة ويب كتاب (HTML)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("ملف ويب متكامل بتنسيق عربي فاخر يُفتح بأي متصفح", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TargetWordCountDialog(
    currentGoal: Int,
    theme: com.example.data.model.AppThemeData,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var goalStr by remember { mutableStateOf(currentGoal.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تحديد هدف الكتابة (عدد الكلمات)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = theme.textColor)
                Text("حدد عدد الكلمات الذي تستهدفه لمشروعك بأكمله لتتمكن من تتبع تقدمك.", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(
                    value = goalStr,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) goalStr = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val goal = goalStr.toIntOrNull() ?: 0
                            onSave(goal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                    ) {
                        Text("حفظ الهدف")
                    }
                }
            }
        }
    }
}
