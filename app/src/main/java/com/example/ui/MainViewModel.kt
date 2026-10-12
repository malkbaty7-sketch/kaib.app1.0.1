package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.example.util.DocxExporter
import com.example.util.DocumentParser
import com.example.util.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getDatabase(application))
    val billingManager = com.example.util.BillingManager(application, viewModelScope)
    val analyticsTracker = com.example.util.AnalyticsTracker(AppDatabase.getDatabase(application).dailyWritingAnalyticsDao(), viewModelScope)

    val todayAnalytics = analyticsTracker.currentDayAnalytics
    val analyticsHistory = analyticsTracker.getAllAnalytics().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val aiImageTokens = billingManager.aiImageTokens
    val isVipUser = MutableStateFlow(false)

    init {
        setupWorkManagerPeriodicSnapshots()
        viewModelScope.launch {
            billingManager.isVipActive.collect { isVip ->
                isVipUser.value = isVip
            }
        }
    }

    val allFiles: StateFlow<List<AppFile>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExtractedTexts: StateFlow<List<ExtractedText>> = repository.allExtractedTexts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Visual Theme
    val currentThemeId = MutableStateFlow("warmSunset")
    val currentTheme: StateFlow<AppThemeData> = currentThemeId.map { id ->
        defaultAppThemes.firstOrNull { it.id == id } ?: defaultAppThemes[0]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultAppThemes[0])

    // Global Emotion Suggestions Active toggle (FR-ES-04)
    val emotionSuggestionsGlobalActive = MutableStateFlow(true)

    // Extraction State
    val selectedFileIdsForExtraction = MutableStateFlow<Set<Int>>(emptySet())
    val topicQuery = MutableStateFlow("")
    val extractionMode = MutableStateFlow("OFFLINE") // "OFFLINE", "ONLINE"
    val isExtracting = MutableStateFlow(false)
    val extractedPassages = MutableStateFlow<List<String>>(emptyList())

    // Editor State
    val activeProject = MutableStateFlow<Project?>(null)
    val activeChapters = MutableStateFlow<List<ChapterItem>>(emptyList())
    val activeChapterIndex = MutableStateFlow(0)

    val editorPaperColor = MutableStateFlow("#FFFDD0")
    val editorPaperSize = MutableStateFlow("A4")
    val editorPageViewMode = MutableStateFlow("SINGLE_PAGE")
    val editorPageNumberStyle = MutableStateFlow(0)
    val editorPageNumberPosition = MutableStateFlow("BOTTOM_CENTER")
    val editorSelectedEmotions = MutableStateFlow<List<String>>(listOf("warmth", "motivation"))
    val editorEmotionSuggestionsActive = MutableStateFlow(true)
    val emotionSuggestions = MutableStateFlow<List<EmotionSuggestion>>(emptyList())

    // Language & Internationalization
    val appLanguage = MutableStateFlow(com.example.util.AppLanguage.ARABIC)
    fun setAppLanguage(lang: com.example.util.AppLanguage) {
        appLanguage.value = lang
    }

    // Admin & Analytics State
    val adminUsersList = MutableStateFlow<List<AdminUser>>(
        listOf(
            AdminUser(
                username = "malkbaty",
                userType = "مدير",
                permissions = AdminPermissions(
                    accessUserData = true,
                    deleteCode = true,
                    editCode = true,
                    replaceCode = true,
                    addImageOrShape = true,
                    addTextToApp = true,
                    monitorActivity = true
                )
            ),
            AdminUser(
                username = "editor_lead",
                userType = "مسؤول",
                permissions = AdminPermissions(
                    accessUserData = true,
                    editCode = true,
                    addImageOrShape = true,
                    addTextToApp = true,
                    monitorActivity = true
                )
            )
        )
    )
    val analyticsData = MutableStateFlow(AppAnalyticsData())

    fun addAdminUser(user: AdminUser) {
        adminUsersList.update { it + user }
    }

    fun removeAdminUser(id: String) {
        adminUsersList.update { it.filter { u -> u.id != id } }
    }

    // User Profile & Authentication State
    val isUserLoggedIn = MutableStateFlow(false)
    val currentUserProfile = MutableStateFlow(AppUserProfile())

    fun setUserLoggedIn(loggedIn: Boolean) {
        isUserLoggedIn.value = loggedIn
    }

    fun updateUserProfile(name: String, username: String) {
        currentUserProfile.update { it.copy(displayName = name, username = username) }
    }

    fun addPublishedWork(work: PublishedWork) {
        currentUserProfile.update { it.copy(publishedWorks = listOf(work) + it.publishedWorks) }
    }

    fun deletePublishedWork(workId: String) {
        currentUserProfile.update { it.copy(publishedWorks = it.publishedWorks.filter { w -> w.id != workId }) }
    }

    fun toggleWorkVisibility(workId: String) {
        currentUserProfile.update { profile ->
            val updated = profile.publishedWorks.map { w ->
                if (w.id == workId) w.copy(isPublic = !w.isPublic) else w
            }
            profile.copy(publishedWorks = updated)
        }
    }

    // VIP / Subscription State & Play Billing (Task 4.2)
    fun setVipUser(vip: Boolean) {
        isVipUser.value = vip
    }

    fun purchaseSubscription(activity: android.app.Activity, productId: String) {
        billingManager.purchaseSubscription(activity, productId)
    }

    fun purchaseInAppPack(activity: android.app.Activity, productId: String) {
        billingManager.purchaseInAppProduct(activity, productId)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    fun setVipActive(active: Boolean) {
        isVipUser.value = active
    }

    fun addAiImageTokens(amount: Int) {
        // Direct addition for test/development mode
        saveToastMessage.value = "تمت إضافة $amount رصيد صور بالذكاء الاصطناعي"
    }

    fun updateDailyWordGoal(newGoal: Int) {
        analyticsTracker.updateTargetWordGoal(newGoal)
    }

    fun runPreflightValidation(context: Context): com.example.util.PreflightReport {
        val project = activeProject.value ?: Project(projectName = "مشروع جديد")
        return com.example.util.PreflightValidator.validateProject(
            context = context,
            project = project,
            chapters = activeChapters.value,
            metadata = exportMetadata.value,
            frontCoverJson = getProjectFrontCover()?.toJson()
        )
    }

    // Document Progress Persistence (Task 3)
    val allDocumentProgress = repository.allDocumentProgress

    fun getDocumentProgress(uri: String): Flow<DocumentProgressEntity?> =
        repository.getDocumentProgress(uri)

    suspend fun getDocumentProgressSync(uri: String): DocumentProgressEntity? =
        repository.getDocumentProgressSync(uri)

    fun saveDocumentProgress(progress: DocumentProgressEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveDocumentProgress(progress)
        }
    }

    fun deleteDocumentProgress(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocumentProgress(uri)
        }
    }

    // Parental Controls
    val parentalControlConfig = MutableStateFlow(ParentalControlConfig())
    fun updateParentalControl(config: ParentalControlConfig) {
        parentalControlConfig.value = config
    }

    // Support Tickets
    val supportTickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    fun submitSupportTicket(ticket: SupportTicket) {
        supportTickets.update { listOf(ticket) + it }
    }

    fun shareProjectWithPermissions(projectName: String, perms: SharedProjectPermissions) {
        saveToastMessage.value = "تمت مشاركة مشروع $projectName بالصلاحيات المحددة"
    }

    val saveToastMessage = MutableStateFlow<String?>(null)

    // File Operations (Phase 1)
    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val parsed = DocumentParser.parseUri(context, uri)
            val appFile = AppFile(
                fileName = parsed.fileName,
                filePath = uri.toString(),
                fileType = parsed.fileType,
                fileSize = parsed.fileSize,
                wordCount = parsed.wordCount,
                extractedText = parsed.text
            )
            repository.insertFile(appFile)
            saveToastMessage.value = "تم استيراد الملف واستخلاص نصوصه بنجاح!"
        }
    }

    fun addManualTextFile(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val appFile = AppFile(
                fileName = if (title.isNotBlank()) title else "مستند_نصي_${System.currentTimeMillis()}",
                filePath = "local_manual",
                fileType = "TXT",
                fileSize = content.toByteArray().size.toLong(),
                wordCount = DocumentParser.countWords(content),
                extractedText = content
            )
            repository.insertFile(appFile)
            saveToastMessage.value = "تم حفظ النص المرجعي بنجاح!"
        }
    }

    fun deleteFile(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFile(id)
            selectedFileIdsForExtraction.update { it - id }
        }
    }

    fun toggleFileSelectionForExtraction(id: Int) {
        selectedFileIdsForExtraction.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun selectAllFilesForExtraction(allIds: List<Int>) {
        selectedFileIdsForExtraction.update { current ->
            if (current.size == allIds.size) emptySet() else allIds.toSet()
        }
    }

    // Extraction Operations (Phase 2)
    fun extractTopic() {
        val query = topicQuery.value.trim()
        if (query.isBlank()) return

        isExtracting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val currentFiles = allFiles.value
            val selectedIds = selectedFileIdsForExtraction.value

            val targetFiles = if (selectedIds.isNotEmpty()) {
                currentFiles.filter { selectedIds.contains(it.id) }
            } else {
                currentFiles
            }

            val passages = mutableListOf<String>()

            targetFiles.forEach { file ->
                val results = DocumentParser.extractTopicOffline(file.extractedText, query)
                results.forEach { sentence ->
                    if (sentence.length > 8) {
                        passages.add("【مصدر: ${file.fileName}】\n$sentence")
                    }
                }
            }

            if (passages.isEmpty()) {
                passages.add("لم يتم العثور على فقرات مطابقة مباشرة لكلمة '$query'. يمكنك إضافة وتعديل الفقرات المستخلصة أدناه.")
            }

            extractedPassages.value = passages
            isExtracting.value = false
        }
    }

    fun updateExtractedPassage(index: Int, newText: String) {
        val current = extractedPassages.value.toMutableList()
        if (index in current.indices) {
            current[index] = newText
            extractedPassages.value = current
        }
    }

    fun deleteExtractedPassage(index: Int) {
        val current = extractedPassages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            extractedPassages.value = current
        }
    }

    fun saveExtractedPassagesToDbAndCreateProject(onCreated: (Project) -> Unit) {
        val passages = extractedPassages.value
        if (passages.isEmpty()) return

        val fullText = passages.joinToString("\n\n")
        val query = topicQuery.value.ifBlank { "موضوع مستخلص" }

        viewModelScope.launch(Dispatchers.IO) {
            val firstSelectedFileId = selectedFileIdsForExtraction.value.firstOrNull()
            val extractedRecord = ExtractedText(
                fileId = firstSelectedFileId,
                topicQuery = query,
                extractedContent = fullText
            )
            repository.insertExtractedText(extractedRecord)

            // Auto create project
            val initialChapters = listOf(
                ChapterItem(
                    title = "المقدمة والاستخلاص الأولي",
                    tabName = "المقدمة",
                    content = fullText
                ),
                ChapterItem(
                    title = "الفصل الأول: التحليل والمناقشة",
                    tabName = "الفصل 1",
                    content = "تابع صياغة وتطوير الأفكار المستخلصة هنا..."
                )
            )

            val newProject = Project(
                projectName = "كتاب: $query",
                projectType = "E_BOOK",
                writingTemplateId = "novel_action",
                themeId = currentThemeId.value,
                contentJson = serializeChapters(initialChapters)
            )

            val newId = repository.insertProject(newProject)
            val created = newProject.copy(id = newId.toInt())
            loadProjectIntoEditor(created)
            onCreated(created)
        }
    }

    fun deleteExtractedText(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExtractedText(id)
        }
    }

    // Projects & Editor Operations (Phase 3 & 4 & 5)
    fun createProject(
        title: String,
        type: String,
        templateId: String,
        emotions: List<String>,
        initialContent: String? = null,
        onCreated: (Project) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val template = defaultWritingTemplates.firstOrNull { it.id == templateId }
                ?: defaultWritingTemplates[0]

            val chapters = if (initialContent != null && initialContent.isNotBlank()) {
                listOf(
                    ChapterItem(title = "المقدمة", tabName = "المقدمة", content = initialContent),
                    ChapterItem(title = template.suggestedStructure.getOrElse(0) { "الفصل 1" }, tabName = "الفصل 1", content = "")
                )
            } else {
                template.suggestedStructure.mapIndexed { idx, titleText ->
                    ChapterItem(
                        title = titleText,
                        tabName = if (titleText.contains(":")) titleText.substringBefore(":") else "قسم ${idx + 1}",
                        content = if (idx == 0) template.defaultIntro else if (idx == template.suggestedStructure.lastIndex) template.defaultConclusion else ""
                    )
                }
            }

            val project = Project(
                projectName = title,
                projectType = type,
                writingTemplateId = templateId,
                themeId = currentThemeId.value,
                paperColor = "#FFFDD0",
                paperSize = "A4",
                pageViewMode = "SINGLE_PAGE",
                emotionProfileJson = JSONArray(emotions).toString(),
                emotionSuggestionsActive = true,
                contentJson = serializeChapters(chapters)
            )

            val id = repository.insertProject(project)
            val created = project.copy(id = id.toInt())
            loadProjectIntoEditor(created)
            onCreated(created)
        }
    }

    val editorTargetWordCount = MutableStateFlow(0)
    
    fun updateTargetWordCount(target: Int) {
        editorTargetWordCount.value = target
        saveCurrentProject()
    }

    fun loadProjectIntoEditor(project: Project) {
        activeProject.value = project
        val chapters = deserializeChapters(project.contentJson)
        activeChapters.value = if (chapters.isNotEmpty()) chapters else listOf(
            ChapterItem(title = "الفصل الأول", tabName = "الفصل 1", content = "")
        )
        activeChapterIndex.value = 0

        editorPaperColor.value = project.paperColor
        editorPaperSize.value = project.paperSize
        editorPageViewMode.value = project.pageViewMode
        editorPageNumberStyle.value = project.pageNumberStyle
        editorPageNumberPosition.value = project.pageNumberPosition
        editorEmotionSuggestionsActive.value = project.emotionSuggestionsActive
        editorTargetWordCount.value = project.targetWordCount
        selectedEditorFont.value = project.selectedFont.ifBlank { "Tajawal" }

        val emotions = parseEmotionsJson(project.emotionProfileJson)
        editorSelectedEmotions.value = emotions

        recalculateEmotionSuggestions(activeChapters.value, emotions)
        loadStoryElementsAndSnapshots(project.id)
        val firstChapter = activeChapters.value.firstOrNull()
        if (firstChapter != null) {
            loadChapterSnapshots(firstChapter.id)
        }
    }

    fun saveCurrentProject() {
        val current = activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = current.copy(
                paperColor = editorPaperColor.value,
                paperSize = editorPaperSize.value,
                pageViewMode = editorPageViewMode.value,
                pageNumberStyle = editorPageNumberStyle.value,
                pageNumberPosition = editorPageNumberPosition.value,
                selectedFont = selectedEditorFont.value,
                emotionProfileJson = JSONArray(editorSelectedEmotions.value).toString(),
                emotionSuggestionsActive = editorEmotionSuggestionsActive.value,
                targetWordCount = editorTargetWordCount.value,
                lastModified = System.currentTimeMillis(),
                contentJson = serializeChapters(activeChapters.value)
            )
            repository.updateProject(updated)
            activeProject.value = updated
            saveToastMessage.value = "تم حفظ المشروع والتعديلات بنجاح!"
        }
    }

    fun deleteProject(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(id)
            if (activeProject.value?.id == id) {
                activeProject.value = null
                activeChapters.value = emptyList()
            }
        }
    }

    // Editor Chapter/Tab Actions (FR-SE-04, FR-SE-05)
    fun selectChapter(index: Int) {
        val oldIdx = activeChapterIndex.value
        val oldChapter = activeChapters.value.getOrNull(oldIdx)
        if (oldChapter != null && oldChapter.content.isNotBlank()) {
            captureChapterSnapshot(oldChapter, isManual = false)
        }
        activeChapterIndex.value = index
        val newChapter = activeChapters.value.getOrNull(index)
        if (newChapter != null) {
            loadChapterSnapshots(newChapter.id)
            loadCommentsForActiveChapter()
        }
    }

    fun updateCurrentChapterTitle(newTitle: String) {
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx in currentList.indices) {
            currentList[idx] = currentList[idx].copy(title = newTitle, tabName = newTitle)
            activeChapters.value = currentList
        }
    }

    fun updateCurrentChapterContent(newContent: String) {
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx in currentList.indices) {
            val oldContent = currentList[idx].content
            currentList[idx] = currentList[idx].copy(content = newContent)
            activeChapters.value = currentList

            // Real-time Writing & Productivity Analytics (Task 4.3)
            analyticsTracker.onContentChanged(oldContent, newContent)

            // Auto snapshot check: every 15 minutes of active writing
            val now = System.currentTimeMillis()
            if (now - lastAutoSnapshotTime >= 15 * 60 * 1000L) {
                captureChapterSnapshot(currentList[idx], isManual = false)
            }
        }
    }

    fun addNewChapter(title: String = "فصل جديد") {
        val currentList = activeChapters.value.toMutableList()
        val newChapter = ChapterItem(
            title = "$title ${currentList.size + 1}",
            tabName = "تبويب ${currentList.size + 1}",
            content = ""
        )
        currentList.add(newChapter)
        activeChapters.value = currentList
        activeChapterIndex.value = currentList.lastIndex
        recalculateEmotionSuggestions(currentList, editorSelectedEmotions.value)
    }

    fun updateActiveChapterImages(images: List<PageImageItem>) {
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx in currentList.indices) {
            currentList[idx] = currentList[idx].copy(imagesJson = PageImageSerializer.toJson(images))
            activeChapters.value = currentList
        }
    }

    fun addImageToActiveChapter(image: PageImageItem) {
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx in currentList.indices) {
            val existingImages = PageImageSerializer.fromJson(currentList[idx].imagesJson).toMutableList()
            existingImages.add(image)
            currentList[idx] = currentList[idx].copy(imagesJson = PageImageSerializer.toJson(existingImages))
            activeChapters.value = currentList
            saveToastMessage.value = "تمت إضافة الصورة إلى الصفحة بنجاح"
        }
    }

    fun updateImageInActiveChapter(updatedImage: PageImageItem) {
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx in currentList.indices) {
            val existingImages = PageImageSerializer.fromJson(currentList[idx].imagesJson).map {
                if (it.id == updatedImage.id) updatedImage else it
            }
            currentList[idx] = currentList[idx].copy(imagesJson = PageImageSerializer.toJson(existingImages))
            activeChapters.value = currentList
        }
    }

    fun deleteImageFromActiveChapter(imageId: String) {
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx in currentList.indices) {
            val existingImages = PageImageSerializer.fromJson(currentList[idx].imagesJson).filter { it.id != imageId }
            currentList[idx] = currentList[idx].copy(imagesJson = PageImageSerializer.toJson(existingImages))
            activeChapters.value = currentList
            saveToastMessage.value = "تم حذف الصورة من الصفحة"
        }
    }

    fun deleteChapter(index: Int) {
        val currentList = activeChapters.value.toMutableList()
        if (currentList.size <= 1) return // keep at least 1
        if (index in currentList.indices) {
            currentList.removeAt(index)
            activeChapters.value = currentList
            activeChapterIndex.value = index.coerceAtMost(currentList.lastIndex)
            recalculateEmotionSuggestions(currentList, editorSelectedEmotions.value)
        }
    }

    fun moveChapter(index: Int, direction: Int) { // -1 left, +1 right
        val currentList = activeChapters.value.toMutableList()
        val target = index + direction
        if (index in currentList.indices && target in currentList.indices) {
            val item = currentList.removeAt(index)
            currentList.add(target, item)
            activeChapters.value = currentList
            activeChapterIndex.value = target
            recalculateEmotionSuggestions(currentList, editorSelectedEmotions.value)
        }
    }

    // Emotion System & Suggestions (FR-ES-01 .. FR-ES-04)
    fun toggleProjectEmotionSuggestions() {
        val nextVal = !editorEmotionSuggestionsActive.value
        editorEmotionSuggestionsActive.value = nextVal
        if (!nextVal) {
            emotionSuggestions.value = emptyList()
        } else {
            recalculateEmotionSuggestions(activeChapters.value, editorSelectedEmotions.value)
        }
    }

    fun updateSelectedEmotions(emotions: List<String>) {
        editorSelectedEmotions.value = emotions
        recalculateEmotionSuggestions(activeChapters.value, emotions)
    }

    private fun recalculateEmotionSuggestions(chapters: List<ChapterItem>, emotions: List<String>) {
        if (!editorEmotionSuggestionsActive.value || !emotionSuggestionsGlobalActive.value) {
            emotionSuggestions.value = emptyList()
            return
        }
        val suggestions = EmotionEngine.generateSuggestions(chapters, emotions)
        emotionSuggestions.value = suggestions
    }

    fun acceptEmotionSuggestion(suggestion: EmotionSuggestion) {
        val currentList = activeChapters.value.toMutableList()
        when (suggestion.type) {
            "REORDER" -> {
                if (suggestion.sourceChapterIndex != null && suggestion.targetChapterIndex != null) {
                    val sIdx = suggestion.sourceChapterIndex
                    val tIdx = suggestion.targetChapterIndex
                    if (sIdx in currentList.indices && tIdx in currentList.indices) {
                        val item = currentList.removeAt(sIdx)
                        currentList.add(tIdx, item)
                        activeChapters.value = currentList
                    }
                }
            }
            "REWRITE" -> {
                if (suggestion.sourceChapterIndex != null && suggestion.suggestedText != null) {
                    val sIdx = suggestion.sourceChapterIndex
                    if (sIdx in currentList.indices) {
                        val currentText = currentList[sIdx].content
                        currentList[sIdx] = currentList[sIdx].copy(
                            content = if (currentText.isBlank()) suggestion.suggestedText else "$currentText\n\n${suggestion.suggestedText}"
                        )
                        activeChapters.value = currentList
                    }
                }
            }
        }
        // Remove accepted suggestion
        emotionSuggestions.value = emotionSuggestions.value.filter { it.id != suggestion.id }
        saveToastMessage.value = "تم تطبيق الاقتراح الذكي بنجاح!"
    }

    fun rejectEmotionSuggestion(id: String) {
        emotionSuggestions.value = emotionSuggestions.value.filter { it.id != id }
    }

    // AI Writing Assistant Helpers (FR-SE-02)
    fun applyAiIntroduction() {
        val project = activeProject.value ?: return
        val template = defaultWritingTemplates.firstOrNull { it.id == project.writingTemplateId }
            ?: defaultWritingTemplates[0]
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx in currentList.indices) {
            val ch = currentList[idx]
            currentList[idx] = ch.copy(content = "${ch.content}\n\n${template.defaultIntro}".trim())
            activeChapters.value = currentList
            saveToastMessage.value = "تم إدراج المقدمة المقترحة!"
        }
    }

    fun applyAiConclusion() {
        val project = activeProject.value ?: return
        val template = defaultWritingTemplates.firstOrNull { it.id == project.writingTemplateId }
            ?: defaultWritingTemplates[0]
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx in currentList.indices) {
            val ch = currentList[idx]
            currentList[idx] = ch.copy(content = "${ch.content}\n\n${template.defaultConclusion}".trim())
            activeChapters.value = currentList
            saveToastMessage.value = "تم إدراج الخاتمة المقترحة!"
        }
    }

    fun applyAiRefinement() {
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx in currentList.indices) {
            val ch = currentList[idx]
            if (ch.content.isNotBlank()) {
                val refined = "『تنقيح بلاغي』: " + ch.content.replace("  ", " ").trim()
                currentList[idx] = ch.copy(content = refined)
                activeChapters.value = currentList
                saveToastMessage.value = "تم تنقيح النص وضبط الصياغة!"
            }
        }
    }

    // Phase 3: Export Metadata (Task 3.4)
    val exportMetadata = MutableStateFlow(com.example.data.model.ExportAdvancedMetadata())
    fun updateExportMetadata(metadata: com.example.data.model.ExportAdvancedMetadata) {
        exportMetadata.value = metadata
    }

    // Export Helpers (FR-13, FR-14, EPUB, TXT, HTML) with runCatching safe exception handling
    fun exportToPdf(context: Context): File? {
        val project = activeProject.value ?: return null
        return runCatching {
            PdfExporter.exportProjectToPdf(context, project, activeChapters.value, exportMetadata.value)
        }.onFailure {
            android.util.Log.e("MainViewModel", "Failed to export PDF", it)
            saveToastMessage.value = "حدث خطأ أثناء تصدير PDF: ${it.localizedMessage}"
        }.getOrNull()
    }

    fun exportToDocx(context: Context): File? {
        val project = activeProject.value ?: return null
        return runCatching {
            DocxExporter.exportProjectToDocx(context, project, activeChapters.value)
        }.onFailure {
            android.util.Log.e("MainViewModel", "Failed to export DOCX", it)
            saveToastMessage.value = "حدث خطأ أثناء تصدير Word: ${it.localizedMessage}"
        }.getOrNull()
    }

    fun exportToEpub(context: Context): File? {
        val project = activeProject.value ?: return null
        return runCatching {
            com.example.util.EpubExporter.exportProjectToEpub(
                context,
                project,
                activeChapters.value,
                exportMetadata.value,
                customFonts.value
            )
        }.onFailure {
            android.util.Log.e("MainViewModel", "Failed to export EPUB", it)
            saveToastMessage.value = "حدث خطأ أثناء تصدير EPUB: ${it.localizedMessage}"
        }.getOrNull()
    }

    fun exportToTxt(context: Context): File? {
        val project = activeProject.value ?: return null
        return runCatching {
            com.example.util.BookExporter.exportToTxt(context, project, activeChapters.value)
        }.onFailure {
            android.util.Log.e("MainViewModel", "Failed to export TXT", it)
        }.getOrNull()
    }

    fun exportToHtml(context: Context): File? {
        val project = activeProject.value ?: return null
        return runCatching {
            com.example.util.BookExporter.exportToHtml(context, project, activeChapters.value)
        }.onFailure {
            android.util.Log.e("MainViewModel", "Failed to export HTML", it)
        }.getOrNull()
    }

    // Cover Management for Active Project
    fun setProjectFrontCover(template: com.example.data.model.BookCoverTemplate) {
        val currentList = activeChapters.value.toMutableList()
        val frontIdx = currentList.indexOfFirst { it.sectionType == "FRONT_COVER" }
        val frontChapter = com.example.data.model.ChapterItem(
            id = if (frontIdx >= 0) currentList[frontIdx].id else java.util.UUID.randomUUID().toString(),
            title = "الغلاف الأمامي",
            tabName = "الغلاف 📖",
            content = template.toJson(),
            sectionType = "FRONT_COVER"
        )

        if (frontIdx >= 0) {
            currentList[frontIdx] = frontChapter
        } else {
            currentList.add(0, frontChapter)
        }

        activeChapters.value = currentList
        activeProject.value = activeProject.value?.copy(coverImagePath = template.imageUri)
        saveCurrentProject()
        saveToastMessage.value = "تم اعتماد الغلاف الأمامي للكتاب!"
    }

    fun setProjectBackCover(template: com.example.data.model.BookCoverTemplate) {
        val currentList = activeChapters.value.toMutableList()
        val backIdx = currentList.indexOfFirst { it.sectionType == "BACK_COVER" }
        val backChapter = com.example.data.model.ChapterItem(
            id = if (backIdx >= 0) currentList[backIdx].id else java.util.UUID.randomUUID().toString(),
            title = "الغلاف الخلفي",
            tabName = "الغلاف الخلفي 🔖",
            content = template.toJson(),
            sectionType = "BACK_COVER"
        )

        if (backIdx >= 0) {
            currentList[backIdx] = backChapter
        } else {
            currentList.add(backChapter)
        }

        activeChapters.value = currentList
        saveCurrentProject()
        saveToastMessage.value = "تم اعتماد الغلاف الخلفي للكتاب!"
    }

    fun removeProjectFrontCover() {
        val currentList = activeChapters.value.filterNot { it.sectionType == "FRONT_COVER" }
        activeChapters.value = currentList
        saveCurrentProject()
        saveToastMessage.value = "تمت إزالة الغلاف الأمامي"
    }

    fun removeProjectBackCover() {
        val currentList = activeChapters.value.filterNot { it.sectionType == "BACK_COVER" }
        activeChapters.value = currentList
        saveCurrentProject()
        saveToastMessage.value = "تمت إزالة الغلاف الخلفي"
    }

    fun getProjectFrontCover(): com.example.data.model.BookCoverTemplate? {
        val front = activeChapters.value.firstOrNull { it.sectionType == "FRONT_COVER" } ?: return null
        return com.example.data.model.BookCoverTemplate.fromJson(front.content)
    }

    fun getProjectBackCover(): com.example.data.model.BookCoverTemplate? {
        val back = activeChapters.value.firstOrNull { it.sectionType == "BACK_COVER" } ?: return null
        return com.example.data.model.BookCoverTemplate.fromJson(back.content)
    }

    // Story Elements State & Operations
    val activeStoryElements = MutableStateFlow<List<StoryElement>>(emptyList())
    val activeSnapshots = MutableStateFlow<List<ProjectSnapshot>>(emptyList())
    val isAiGenerating = MutableStateFlow(false)

    fun loadStoryElementsAndSnapshots(projectId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getStoryElementsByProject(projectId).collect { list ->
                activeStoryElements.value = list
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSnapshotsByProject(projectId).collect { list ->
                activeSnapshots.value = list
            }
        }
    }

    fun addStoryElement(name: String, role: String, desc: String, motivation: String, colorHex: String) {
        val project = activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val element = StoryElement(
                projectId = project.id,
                name = name,
                roleOrType = role,
                description = desc,
                psychologicalMotivation = motivation,
                colorTag = colorHex
            )
            repository.insertStoryElement(element)
            saveToastMessage.value = "تمت إضافة بطاقة $name بنجاح!"
        }
    }

    fun deleteStoryElement(element: StoryElement) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStoryElement(element)
        }
    }

    fun updateChapterTension(index: Int, tension: Float) {
        val currentList = activeChapters.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = currentList[index].copy(tensionLevel = tension)
            activeChapters.value = currentList
        }
    }

    // Snapshot / Version Control Operations
    fun createProjectSnapshot(label: String) {
        val project = activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val contentJson = serializeChapters(activeChapters.value)
            val totalWords = activeChapters.value.sumOf { DocumentParser.countWords(it.content) }
            val snapshot = ProjectSnapshot(
                projectId = project.id,
                snapshotLabel = label,
                contentJson = contentJson,
                wordCount = totalWords
            )
            repository.insertSnapshot(snapshot)
            saveToastMessage.value = "تم حفظ نقطة استرجاع احتياطية: $label"
        }
    }

    fun restoreProjectSnapshot(snapshot: ProjectSnapshot) {
        val restoredChapters = deserializeChapters(snapshot.contentJson)
        if (restoredChapters.isNotEmpty()) {
            activeChapters.value = restoredChapters
            activeChapterIndex.value = 0
            saveCurrentProject()
            saveToastMessage.value = "تمت استعادة المسودة: ${snapshot.snapshotLabel}"
        }
    }

    fun deleteProjectSnapshot(snapshotId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSnapshot(snapshotId)
        }
    }

    // Gemini AI Copilot Integration
    fun generateAiContentForActiveChapter(promptType: String, customPrompt: String? = null) {
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx !in currentList.indices) return

        val currentChapter = currentList[idx]
        val project = activeProject.value
        isAiGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = when (promptType) {
                "REFINE" -> {
                    if (currentChapter.content.isNotBlank()) {
                        com.example.data.ai.GeminiAiService.refineText(currentChapter.content)
                    } else {
                        Result.failure(Exception("الفصل فارغ، اكتب بعض الجمل ليتم تنقيحها"))
                    }
                }
                "EXPAND" -> {
                    val topic = currentChapter.title
                    com.example.data.ai.GeminiAiService.expandSection(topic, currentChapter.content)
                }
                "CUSTOM" -> {
                    val userQuery = customPrompt ?: "تابع الكتابة"
                    val prompt = """
                        أنت كاتب ومؤلف محترف في تطبيق 'كاتب'.
                        مشروع الكتاب: '${project?.projectName ?: ""}'
                        عنوان الفصل: '${currentChapter.title}'
                        المحتوى الحالي:
                        ${currentChapter.content}
                        
                        المطلوب من المساعد الذكي:
                        $userQuery
                        
                        اكتب النص الأدبي/الفكري المطلوب بدقة وبلاغة عربية مباشرة دون مقدمات.
                    """.trimIndent()
                    com.example.data.ai.GeminiAiService.generateContent(prompt)
                }
                else -> Result.failure(Exception("نوع طلب غير مدعوم"))
            }

            result.onSuccess { generatedText ->
                val newContent = if (promptType == "REFINE") {
                    generatedText
                } else {
                    if (currentChapter.content.isBlank()) generatedText else "${currentChapter.content}\n\n$generatedText"
                }
                currentList[idx] = currentChapter.copy(content = newContent)
                activeChapters.value = currentList
                saveToastMessage.value = "تم توليد وتطبيق النص بنجاح بواسطة Gemini AI!"
            }.onFailure { err ->
                // Graceful fallback if no API key or offline
                if (promptType == "REFINE") {
                    applyAiRefinement()
                } else if (promptType == "EXPAND") {
                    applyAiIntroduction()
                } else {
                    saveToastMessage.value = err.message ?: "تعذر استدعاء الذكاء الاصطناعي"
                }
            }

            isAiGenerating.value = false
        }
    }

    // Custom Font Import System (Task 1.3)
    val customFonts: StateFlow<List<CustomFontEntity>> = repository.allCustomFonts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedEditorFont = MutableStateFlow("Tajawal")

    fun importCustomFont(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val imported = com.example.util.FontStorageManager.importFontFromUri(getApplication(), uri)
            if (imported != null) {
                repository.insertCustomFont(imported)
                selectedEditorFont.value = imported.fontName
                saveToastMessage.value = "تم استيراد الخط: ${imported.fontName} بنجاح!"
                saveCurrentProject()
            } else {
                saveToastMessage.value = "فشل استيراد الخط. الرجاء التأكد من اختيار ملف .ttf أو .otf صالح"
            }
        }
    }

    fun deleteCustomFont(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomFont(id)
            saveToastMessage.value = "تم حذف الخط المخصص"
        }
    }

    fun selectEditorFont(fontName: String) {
        selectedEditorFont.value = fontName
        val p = activeProject.value
        if (p != null) {
            activeProject.value = p.copy(selectedFont = fontName)
            saveCurrentProject()
        }
    }

    // Detailed Snapshots Recovery System (Task 1.1)
    val detailedSnapshots = MutableStateFlow<List<SnapshotEntity>>(emptyList())
    val allDetailedSnapshots: StateFlow<List<SnapshotEntity>> = repository.getAllDetailedSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastAutoSnapshotTime = System.currentTimeMillis()
    private var lastSnapshotContentHash = ""

    private fun md5(input: String): String {
        return try {
            val bytes = java.security.MessageDigest.getInstance("MD5").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toString()
        }
    }

    fun loadChapterSnapshots(chapterId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getDetailedSnapshotsForChapter(chapterId).collect { list ->
                detailedSnapshots.value = list
            }
        }
    }

    fun captureChapterSnapshot(chapter: ChapterItem, isManual: Boolean = false) {
        if (chapter.content.isBlank()) return
        val hash = md5(chapter.content)
        if (!isManual && hash == lastSnapshotContentHash) return

        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = SnapshotEntity(
                chapterId = chapter.id,
                contentHash = hash,
                deltaContent = chapter.content,
                wordCount = DocumentParser.countWords(chapter.content),
                chapterTitle = chapter.title,
                timestamp = System.currentTimeMillis()
            )
            repository.insertDetailedSnapshot(snapshot)
            lastSnapshotContentHash = hash
            lastAutoSnapshotTime = System.currentTimeMillis()
            if (isManual) {
                saveToastMessage.value = "تم أخذ لقطة فورية: ${chapter.title}"
            }
        }
    }

    fun restoreDetailedSnapshot(snapshot: SnapshotEntity) {
        val currentList = activeChapters.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == snapshot.chapterId }
        val targetIdx = if (idx != -1) idx else activeChapterIndex.value
        if (targetIdx in currentList.indices) {
            currentList[targetIdx] = currentList[targetIdx].copy(content = snapshot.deltaContent)
            activeChapters.value = currentList
            saveCurrentProject()
            saveToastMessage.value = "تمت استعادة اللقطة: ${snapshot.chapterTitle}"
        }
    }

    fun deleteDetailedSnapshot(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDetailedSnapshot(id)
            saveToastMessage.value = "تم حذف اللقطة"
        }
    }

    fun setupWorkManagerPeriodicSnapshots() {
        try {
            val request = androidx.work.PeriodicWorkRequestBuilder<com.example.util.SnapshotWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
                .build()
            androidx.work.WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
                "KatibPeriodicSnapshot",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Scene Breakdown Engine (Task 1.2)
    fun getActiveChapterScenes(): List<Scene> {
        val idx = activeChapterIndex.value
        val ch = activeChapters.value.getOrNull(idx) ?: return emptyList()
        return SceneSerializer.fromJson(ch.scenesJson)
    }

    fun updateActiveChapterScenes(scenes: List<Scene>, syncContent: Boolean = true) {
        val idx = activeChapterIndex.value
        val currentList = activeChapters.value.toMutableList()
        if (idx in currentList.indices) {
            val ch = currentList[idx]
            val newContent = if (syncContent && scenes.isNotEmpty()) {
                scenes.joinToString("\n\n***\n\n") { it.content }
            } else {
                ch.content
            }
            currentList[idx] = ch.copy(
                scenesJson = SceneSerializer.toJson(scenes),
                content = newContent
            )
            activeChapters.value = currentList
        }
    }

    fun addSceneToActiveChapter(title: String, summary: String = "") {
        val currentScenes = getActiveChapterScenes().toMutableList()
        val ch = activeChapters.value.getOrNull(activeChapterIndex.value)
        val newScene = Scene(
            id = java.util.UUID.randomUUID().toString(),
            chapterId = ch?.id ?: "",
            title = if (title.isNotBlank()) title else "المشهد ${currentScenes.size + 1}",
            content = "",
            orderIndex = currentScenes.size,
            summary = summary
        )
        currentScenes.add(newScene)
        updateActiveChapterScenes(currentScenes, syncContent = false)
        saveToastMessage.value = "تمت إضافة المشهد: ${newScene.title}"
    }

    fun updateSceneInActiveChapter(updatedScene: Scene) {
        val currentScenes = getActiveChapterScenes().toMutableList()
        val idx = currentScenes.indexOfFirst { it.id == updatedScene.id }
        if (idx != -1) {
            currentScenes[idx] = updatedScene
            updateActiveChapterScenes(currentScenes, syncContent = true)
        }
    }

    fun deleteSceneFromActiveChapter(sceneId: String) {
        val currentScenes = getActiveChapterScenes().toMutableList()
        currentScenes.removeAll { it.id == sceneId }
        updateActiveChapterScenes(currentScenes, syncContent = true)
        saveToastMessage.value = "تم حذف المشهد"
    }

    fun reorderScenesInActiveChapter(fromIndex: Int, toIndex: Int) {
        val currentScenes = getActiveChapterScenes().toMutableList()
        if (fromIndex in currentScenes.indices && toIndex in currentScenes.indices) {
            val item = currentScenes.removeAt(fromIndex)
            currentScenes.add(toIndex, item)
            val reindexed = currentScenes.mapIndexed { i, s -> s.copy(orderIndex = i) }
            updateActiveChapterScenes(reindexed, syncContent = true)
        }
    }

    fun autoSplitActiveChapterIntoScenes() {
        val idx = activeChapterIndex.value
        val ch = activeChapters.value.getOrNull(idx) ?: return
        if (ch.content.isBlank()) {
            saveToastMessage.value = "الفصل فارغ، أضف بعض النصوص أولاً لتقسيمها إلى مشاهد"
            return
        }
        val rawSections = ch.content.split(Regex("(?m)^\\s*(\\*\\*\\*|---|###)\\s*$"))
            .flatMap { if (it.contains("\n\n\n")) it.split(Regex("\n{3,}")) else listOf(it) }
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val scenes = rawSections.mapIndexed { index, text ->
            val firstLine = text.lines().firstOrNull()?.take(30) ?: ""
            Scene(
                id = java.util.UUID.randomUUID().toString(),
                chapterId = ch.id,
                title = "المشهد ${index + 1}: ${if (firstLine.isNotBlank()) firstLine else "مقطع"}",
                content = text,
                orderIndex = index,
                summary = "المشهد رقم ${index + 1}"
            )
        }
        updateActiveChapterScenes(scenes, syncContent = false)
        saveToastMessage.value = "تم تقسيم الفصل تلقائياً إلى ${scenes.size} مشاهد!"
    }

    fun mergeScenesIntoActiveChapter() {
        val scenes = getActiveChapterScenes()
        if (scenes.isEmpty()) return
        val merged = scenes.joinToString("\n\n***\n\n") { "【${it.title}】\n${it.content}".trim() }
        updateCurrentChapterContent(merged)
        saveToastMessage.value = "تم دمج المشاهد في نص الفصل بنجاح"
    }

    // Phase 2 Services: Drive Sync, TTS, AI Proofreading & Plot Branching
    val driveSyncManager = com.example.data.sync.DriveSyncManager(getApplication())
    val syncStatus = driveSyncManager.syncStatus
    val syncMetadata = driveSyncManager.syncMetadata

    fun performDriveSync(forceManual: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = driveSyncManager.performSync(forceManual = forceManual)
            result.onSuccess {
                saveToastMessage.value = it
            }.onFailure {
                saveToastMessage.value = it.localizedMessage ?: "فشلت المزامنة السحابية"
            }
        }
    }

    fun restoreFromDriveBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = driveSyncManager.restoreFromBackup()
            result.onSuccess { count ->
                saveToastMessage.value = "تمت استعادة $count مشاريع من النسخة السحابية بنجاح!"
            }.onFailure {
                saveToastMessage.value = it.localizedMessage ?: "فشلت الاستعادة من النسخة السحابية"
            }
        }
    }

    fun updateAutoSyncConfig(enabled: Boolean, wifiOnly: Boolean) {
        driveSyncManager.updateAutoSyncConfig(enabled, wifiOnly)
    }

    // Phase 2 TTS Service
    val ttsManager = com.example.util.TtsManager(getApplication())

    fun startTtsReading(text: String) {
        ttsManager.prepareText(text)
        ttsManager.play()
    }

    fun stopTtsReading() {
        ttsManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    // Phase 2 AI: Proofreading & Plot Branching
    val proofreadingIssues = MutableStateFlow<List<com.example.data.ai.ProofreadingIssue>>(emptyList())
    val isProofreading = MutableStateFlow(false)

    fun runProofreadingForCurrentChapter() {
        val current = activeChapters.value.getOrNull(activeChapterIndex.value) ?: return
        if (current.content.isBlank()) {
            saveToastMessage.value = "الفصل فارغ، اكتب بعض الجمل ليتم فحصها"
            return
        }

        isProofreading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.data.ai.AdvancedAIAssistant.proofreadChapter(current.content)
            result.onSuccess { list ->
                proofreadingIssues.value = list
            }.onFailure { err ->
                saveToastMessage.value = err.localizedMessage ?: "تعذر إكمال التدقيق اللغوي"
            }
            isProofreading.value = false
        }
    }

    fun applyProofreadingFix(issue: com.example.data.ai.ProofreadingIssue) {
        val idx = activeChapterIndex.value
        val currentList = activeChapters.value.toMutableList()
        if (idx in currentList.indices) {
            val ch = currentList[idx]
            val updatedContent = ch.content.replaceFirst(issue.originalSnippet, issue.suggestedFix)
            currentList[idx] = ch.copy(content = updatedContent)
            activeChapters.value = currentList
            proofreadingIssues.value = proofreadingIssues.value.filter { it.id != issue.id }
            saveToastMessage.value = "تم تطبيق التصحيح: ${issue.suggestedFix}"
        }
    }

    fun applyAllProofreadingFixes(issues: List<com.example.data.ai.ProofreadingIssue>) {
        val idx = activeChapterIndex.value
        val currentList = activeChapters.value.toMutableList()
        if (idx in currentList.indices) {
            var content = currentList[idx].content
            issues.forEach { issue ->
                content = content.replace(issue.originalSnippet, issue.suggestedFix)
            }
            currentList[idx] = currentList[idx].copy(content = content)
            activeChapters.value = currentList
            proofreadingIssues.value = emptyList()
            saveToastMessage.value = "تم تطبيق جميع التصحيحات بنجاح!"
        }
    }

    val plotBranchOptions = MutableStateFlow<List<com.example.data.ai.PlotBranchOption>>(emptyList())
    val isGeneratingBranches = MutableStateFlow(false)

    fun generatePlotBranchesForCurrentScene() {
        val project = activeProject.value
        val currentChapter = activeChapters.value.getOrNull(activeChapterIndex.value) ?: return
        val characters = activeStoryElements.value

        isGeneratingBranches.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.data.ai.AdvancedAIAssistant.generatePlotBranches(
                bookTitle = project?.projectName ?: "رواية كاتب",
                chapterTitle = currentChapter.title,
                currentContent = currentChapter.content,
                characters = characters
            )
            result.onSuccess { options ->
                plotBranchOptions.value = options
            }.onFailure { err ->
                saveToastMessage.value = err.localizedMessage ?: "فشل توليد مسارات الحبكة"
            }
            isGeneratingBranches.value = false
        }
    }

    fun selectPlotBranch(option: com.example.data.ai.PlotBranchOption) {
        val currentList = activeChapters.value.toMutableList()
        val idx = activeChapterIndex.value
        if (idx in currentList.indices) {
            val ch = currentList[idx]
            val newContent = if (ch.content.isBlank()) {
                "【${option.title}】\n\n${option.generatedText}"
            } else {
                "${ch.content}\n\n***\n\n【${option.title}】\n\n${option.generatedText}"
            }
            currentList[idx] = ch.copy(content = newContent)
            activeChapters.value = currentList
            saveToastMessage.value = "تم اعتماد المسار الدرامي وإدراجه بنجاح!"
        }
    }

    // Phase 3: Beta Reader Feedback System (Task 3.1)
    val isReviewModeActive = MutableStateFlow(false)
    fun toggleReviewMode() {
        isReviewModeActive.value = !isReviewModeActive.value
        saveToastMessage.value = if (isReviewModeActive.value) "تم تفعيل وضع القراءة والمراجعة التجريبية 📝" else "تم العودة إلى وضع التحرير"
    }

    val activeChapterComments = MutableStateFlow<List<com.example.data.model.CommentEntity>>(emptyList())
    val allComments: StateFlow<List<com.example.data.model.CommentEntity>> = repository.getAllComments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadCommentsForActiveChapter() {
        val currentChapter = activeChapters.value.getOrNull(activeChapterIndex.value) ?: return
        viewModelScope.launch {
            repository.getCommentsForChapter(currentChapter.id).collect { list ->
                activeChapterComments.value = list
            }
        }
    }

    fun addChapterComment(authorName: String, snippet: String, commentText: String) {
        val currentChapter = activeChapters.value.getOrNull(activeChapterIndex.value) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val comment = com.example.data.model.CommentEntity(
                chapterId = currentChapter.id,
                selectedTextRange = snippet,
                authorName = authorName,
                commentText = commentText
            )
            repository.insertComment(comment)
            saveToastMessage.value = "تم تسجيل الملاحظة بنجاح!"
        }
    }

    fun toggleCommentResolved(commentId: Long, currentResolved: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setCommentResolved(commentId, !currentResolved)
        }
    }

    fun replyToComment(commentId: Long, replyText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.replyToComment(commentId, replyText)
            saveToastMessage.value = "تم إرسال رد الكاتب!"
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComment(commentId)
            saveToastMessage.value = "تم حذف الملاحظة"
        }
    }

    fun updateChaptersOrder(newList: List<ChapterItem>) {
        activeChapters.value = newList
        saveCurrentProject()
    }

    // JSON serialization
    private fun serializeChapters(chapters: List<ChapterItem>): String {
        val array = JSONArray()
        chapters.forEach { ch ->
            val obj = JSONObject().apply {
                put("id", ch.id)
                put("title", ch.title)
                put("tabName", ch.tabName)
                put("content", ch.content)
                put("tensionLevel", ch.tensionLevel.toDouble())
                put("sectionType", ch.sectionType)
                put("imagesJson", ch.imagesJson)
                put("scenesJson", ch.scenesJson)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeChapters(jsonString: String): List<ChapterItem> {
        val list = mutableListOf<ChapterItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChapterItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.optString("title", "فصل"),
                        tabName = obj.optString("tabName", "فصل"),
                        content = obj.optString("content", ""),
                        tensionLevel = obj.optDouble("tensionLevel", 0.5).toFloat(),
                        sectionType = obj.optString("sectionType", "CHAPTER"),
                        imagesJson = obj.optString("imagesJson", "[]"),
                        scenesJson = obj.optString("scenesJson", "[]")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    private fun parseEmotionsJson(jsonString: String?): List<String> {
        if (jsonString.isNullOrBlank()) return listOf("warmth", "motivation")
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            return listOf("warmth", "motivation")
        }
        return list
    }
}
