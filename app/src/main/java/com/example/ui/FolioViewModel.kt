package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.SampleSeedGenerator
import com.example.core.VaultCryptoManager
import com.example.data.local.AnnotationEntity
import com.example.data.local.BatchJobEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.DocumentEntity
import com.example.data.local.FolderEntity
import com.example.data.local.FolioDatabase
import com.example.data.local.FolioPreferences
import com.example.data.local.FolioRepository
import com.example.data.local.SignatureEntity
import com.example.data.local.TagEntity
import com.example.data.local.ToolPresetEntity
import com.example.data.local.UserSettingsState
import com.example.domain.model.DocumentFormat
import com.example.domain.model.PdfToolId
import com.example.domain.model.ReaderFitMode
import com.example.domain.model.ReaderViewMode
import com.example.domain.model.ReadingThemeMode
import com.example.domain.model.ToolConfiguration
import com.example.domain.model.ToolExecutionResult
import com.example.reader.MultiFormatDocumentEngine
import com.example.reader.ParsedDocumentSession
import com.example.tools.PdfToolkitEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class MainTab {
    HOME, TOOLS, FILES, SETTINGS
}

data class ActiveToolWizardState(
    val tool: PdfToolId,
    val step: Int = 1, // 1 = Pick Files, 2 = Configure, 3 = Result
    val selectedDocs: List<DocumentEntity> = emptyList(),
    val config: ToolConfiguration = ToolConfiguration(),
    val isRunning: Boolean = false,
    val progressPercent: Int = 0,
    val progressStatus: String = "",
    val result: ToolExecutionResult? = null
)

class FolioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FolioDatabase.getInstance(application)
    val repository = FolioRepository(database.folioDao())
    val preferences = FolioPreferences(application)

    val settings: StateFlow<UserSettingsState> = preferences.settings

    val libraryDocuments: StateFlow<List<DocumentEntity>> = repository.libraryDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favouriteDocuments: StateFlow<List<DocumentEntity>> = repository.favouriteDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedDocuments: StateFlow<List<DocumentEntity>> = repository.trashedDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultedDocuments: StateFlow<List<DocumentEntity>> = repository.vaultedDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val toolPresets: StateFlow<List<ToolPresetEntity>> = repository.toolPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batchJobs: StateFlow<List<BatchJobEntity>> = repository.batchJobs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val signatures: StateFlow<List<SignatureEntity>> = repository.signatures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Reader state
    private val _activeDocument = MutableStateFlow<DocumentEntity?>(null)
    val activeDocument: StateFlow<DocumentEntity?> = _activeDocument.asStateFlow()

    private val _activeSession = MutableStateFlow<ParsedDocumentSession?>(null)
    val activeSession: StateFlow<ParsedDocumentSession?> = _activeSession.asStateFlow()

    private val _readerLoading = MutableStateFlow(false)
    val readerLoading: StateFlow<Boolean> = _readerLoading.asStateFlow()

    // Undo / Redo stack for annotations
    private val undoStack = ArrayDeque<AnnotationEntity>()
    private val redoStack = ArrayDeque<AnnotationEntity>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // 3-Step Tool Wizard & Camera Scanner state
    private val _toolWizard = MutableStateFlow<ActiveToolWizardState?>(null)
    val toolWizard: StateFlow<ActiveToolWizardState?> = _toolWizard.asStateFlow()

    private val _showScannerScreen = MutableStateFlow(false)
    val showScannerScreen: StateFlow<Boolean> = _showScannerScreen.asStateFlow()

    private var activeToolJob: Job? = null

    // Last deleted document for Snackbar Undo
    private val _lastTrashedDocument = MutableStateFlow<DocumentEntity?>(null)
    val lastTrashedDocument: StateFlow<DocumentEntity?> = _lastTrashedDocument.asStateFlow()

    // Status message for file/folder imports
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _storageBreakdown = MutableStateFlow(VaultCryptoManager.calculateStorageBreakdown(application))
    val storageBreakdown: StateFlow<VaultCryptoManager.StorageBreakdown> = _storageBreakdown.asStateFlow()

    init {
        viewModelScope.launch {
            SampleSeedGenerator.initializeWorkspaceAndPurgeDummyFiles(getApplication(), repository)
            refreshStorageStats()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun refreshStorageStats() {
        _storageBreakdown.value = VaultCryptoManager.calculateStorageBreakdown(getApplication())
    }

    fun clearCacheAndTemp() {
        VaultCryptoManager.clearCacheAndTempFiles(getApplication())
        refreshStorageStats()
    }

    // --- Document Open & Reader Operations ---

    fun openDocument(doc: DocumentEntity, password: String = "") {
        viewModelScope.launch {
            _readerLoading.value = true
            val context = getApplication<Application>()
            val sourceFile = if (doc.isVaulted) {
                VaultCryptoManager.decryptVaultFileToTemp(
                    context,
                    File(doc.filePath),
                    doc.formatName.lowercase(),
                    settings.value.appLockPin
                )
            } else {
                File(doc.filePath)
            }
            val session = MultiFormatDocumentEngine.openSession(context, sourceFile, password)
            val updatedDoc = doc.copy(
                lastOpenedAt = System.currentTimeMillis(),
                pageCount = session.pageCount.coerceAtLeast(doc.pageCount)
            )
            repository.updateDocument(updatedDoc)
            _activeDocument.value = updatedDoc
            _activeSession.value = session
            undoStack.clear()
            redoStack.clear()
            updateUndoRedoFlags()
            _readerLoading.value = false
        }
    }

    fun closeReader() {
        _activeDocument.value = null
        _activeSession.value = null
    }

    fun updateReadingProgress(
        pageIndex: Int,
        zoom: Float? = null,
        viewMode: ReaderViewMode? = null,
        fitMode: ReaderFitMode? = null,
        readingTheme: ReadingThemeMode? = null
    ) {
        val current = _activeDocument.value ?: return
        val updated = current.copy(
            lastPage = pageIndex.coerceIn(0, (current.pageCount - 1).coerceAtLeast(0)),
            lastZoom = zoom ?: current.lastZoom,
            viewModeName = viewMode?.name ?: current.viewModeName,
            fitModeName = fitMode?.name ?: current.fitModeName,
            readingThemeName = readingTheme?.name ?: current.readingThemeName,
            lastOpenedAt = System.currentTimeMillis()
        )
        _activeDocument.value = updated
        viewModelScope.launch {
            repository.updateDocument(updated)
        }
    }

    fun addAnnotation(
        pageIndex: Int,
        typeName: String,
        colorArgb: Long,
        strokeWidth: Float = 4f,
        opacity: Float = 0.85f,
        textContent: String = "",
        pointsJson: String = ""
    ) {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch {
            val entity = AnnotationEntity(
                documentId = doc.id,
                pageIndex = pageIndex,
                typeName = typeName,
                colorArgb = colorArgb,
                strokeWidth = strokeWidth,
                opacity = opacity,
                textContent = textContent,
                pointsJson = pointsJson
            )
            val insertedId = repository.addAnnotation(entity)
            undoStack.addLast(entity.copy(id = insertedId))
            redoStack.clear()
            updateUndoRedoFlags()
        }
    }

    fun undoAnnotation() {
        if (undoStack.isEmpty()) return
        val last = undoStack.removeLast()
        viewModelScope.launch {
            repository.removeAnnotation(last.id)
            redoStack.addLast(last)
            updateUndoRedoFlags()
        }
    }

    fun redoAnnotation() {
        if (redoStack.isEmpty()) return
        val item = redoStack.removeLast()
        viewModelScope.launch {
            val newId = repository.addAnnotation(item.copy(id = 0))
            undoStack.addLast(item.copy(id = newId))
            updateUndoRedoFlags()
        }
    }

    private fun updateUndoRedoFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun toggleBookmark(pageIndex: Int, title: String, note: String = "") {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch {
            repository.addBookmark(
                BookmarkEntity(
                    documentId = doc.id,
                    pageIndex = pageIndex,
                    title = title.ifBlank { "Page ${pageIndex + 1}" },
                    note = note
                )
            )
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            repository.removeBookmark(bookmarkId)
        }
    }

    // --- SAF File & Folder Import (With Persistent User Permission Grants) ---

    fun importSafUri(uri: Uri, openImmediately: Boolean = false, launchTool: PdfToolId? = null) {
        importMultipleSafUris(listOf(uri), openImmediately = openImmediately, launchTool = launchTool)
    }

    fun importMultipleSafUris(
        uris: List<Uri>,
        openImmediately: Boolean = false,
        launchTool: PdfToolId? = null
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val importedDocs = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    importSingleUriInternal(context, uri)
                }
            }
            if (importedDocs.isEmpty()) return@launch
            refreshStorageStats()

            if (launchTool != null) {
                startToolFlow(launchTool, importedDocs)
            } else if (_toolWizard.value != null) {
                val current = _toolWizard.value!!
                val updatedSelection = if (current.tool.acceptsMultipleFiles) {
                    (current.selectedDocs + importedDocs).distinctBy { it.id }
                } else {
                    listOf(importedDocs.first())
                }
                _toolWizard.value = current.copy(selectedDocs = updatedSelection)
                _statusMessage.value = "Added ${importedDocs.size} file(s) to ${current.tool.title}"
            } else if (openImmediately && importedDocs.size == 1) {
                openDocument(importedDocs.first())
            } else {
                _statusMessage.value = "Imported ${importedDocs.size} file(s) from device"
            }
        }
    }

    fun importSafFolderTree(treeUri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            runCatching {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            }
            val importedCount = withContext(Dispatchers.IO) {
                val rootDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0
                val folderTitle = rootDir.name ?: "Connected Folder"
                preferences.update {
                    it.copy(
                        customOutputFolderUri = treeUri.toString(),
                        connectedFolderName = folderTitle
                    )
                }
                val folderEntityId = repository.createFolder(folderTitle)
                var count = 0
                val supportedExts = setOf(
                    "pdf", "epub", "docx", "doc", "xlsx", "xls", "pptx", "ppt",
                    "odt", "txt", "md", "csv", "html", "htm", "rtf", "cbz", "jpg", "jpeg", "png", "webp"
                )
                rootDir.listFiles().take(40).forEach { docFile ->
                    if (docFile.isFile) {
                        val name = docFile.name ?: return@forEach
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext in supportedExts) {
                            val imported = importSingleUriInternal(context, docFile.uri, preferredName = name, folderId = folderEntityId)
                            if (imported != null) count++
                        }
                    }
                }
                count
            }
            refreshStorageStats()
            _statusMessage.value = if (importedCount > 0) {
                "Synced $importedCount file(s) from selected folder"
            } else {
                "Folder connected. Pick files or add documents to sync."
            }
        }
    }

    private suspend fun importSingleUriInternal(
        context: Context,
        uri: Uri,
        preferredName: String? = null,
        folderId: Long? = null
    ): DocumentEntity? {
        val contentResolver = context.contentResolver
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        var displayName = preferredName ?: "Document_${System.currentTimeMillis() % 10000}.pdf"
        if (preferredName == null) {
            runCatching {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                }
            }
        }
        val safeFileName = displayName.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
        val libDir = File(context.filesDir, "library").apply { mkdirs() }
        val destFile = File(libDir, "${System.currentTimeMillis() % 100000}_$safeFileName")
        runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        if (!destFile.exists() || destFile.length() == 0L) return null

        val session = MultiFormatDocumentEngine.openSession(context, destFile)
        val format = DocumentFormat.fromFilename(displayName)
        val newDoc = DocumentEntity(
            displayName = displayName,
            filePath = destFile.absolutePath,
            uriString = uri.toString(),
            formatName = format.name,
            sizeBytes = destFile.length(),
            pageCount = session.pageCount.coerceAtLeast(1),
            folderId = folderId,
            indexedContent = session.pagesText.take(10).joinToString(" ").take(2500)
        )
        val id = repository.upsertDocument(newDoc)
        return newDoc.copy(id = id)
    }

    fun exportFileToSafUri(sourcePath: String, targetUri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val src = File(sourcePath)
            if (!src.exists()) return@launch
            runCatching {
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    src.inputStream().use { input -> input.copyTo(out) }
                }
                _statusMessage.value = "Saved ${src.name} to device storage"
            }
        }
    }

    // --- Library Management (Favourite, Rename, Duplicate, Tag, Vault, Trash) ---

    fun toggleFavourite(doc: DocumentEntity) {
        viewModelScope.launch {
            val updated = doc.copy(isFavourite = !doc.isFavourite)
            repository.updateDocument(updated)
            if (_activeDocument.value?.id == doc.id) {
                _activeDocument.value = updated
            }
        }
    }

    fun renameDocument(doc: DocumentEntity, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val ext = doc.displayName.substringAfterLast('.', doc.formatName.lowercase())
            val finalName = if (clean.contains(".")) clean else "$clean.$ext"
            val updated = doc.copy(displayName = finalName)
            repository.updateDocument(updated)
            if (_activeDocument.value?.id == doc.id) {
                _activeDocument.value = updated
            }
        }
    }

    fun renameFileOnDiskAndDb(filePath: String, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getDocumentByPath(filePath)
            if (existing != null) {
                renameDocument(existing, clean)
            }
        }
    }

    fun saveSignature(label: String, typedText: String, strokePointsJson: String) {
        viewModelScope.launch {
            repository.saveSignature(
                SignatureEntity(
                    label = label.ifBlank { "Signature" },
                    typedText = typedText.ifBlank { "Signed via Folio" },
                    strokePointsJson = strokePointsJson
                )
            )
        }
    }

    fun duplicateDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            val src = File(doc.filePath)
            if (!src.exists()) return@launch
            val copyFile = File(src.parentFile, "${src.nameWithoutExtension}_copy_${System.currentTimeMillis() % 1000}.${src.extension}")
            src.copyTo(copyFile, overwrite = true)
            repository.upsertDocument(
                doc.copy(
                    id = 0,
                    displayName = "${doc.displayName.substringBeforeLast('.')}_copy.${src.extension}",
                    filePath = copyFile.absolutePath,
                    createdAt = System.currentTimeMillis(),
                    lastOpenedAt = System.currentTimeMillis()
                )
            )
            refreshStorageStats()
        }
    }

    fun assignFolderAndTags(doc: DocumentEntity, folderId: Long?, tagsCsv: String) {
        viewModelScope.launch {
            repository.updateDocument(doc.copy(folderId = folderId, tagsCsv = tagsCsv))
        }
    }

    fun moveDocumentToVault(doc: DocumentEntity) {
        viewModelScope.launch {
            val src = File(doc.filePath)
            if (!src.exists()) return@launch
            val encryptedFile = VaultCryptoManager.encryptFileToVault(
                getApplication(),
                src,
                settings.value.appLockPin
            )
            repository.updateDocument(
                doc.copy(
                    filePath = encryptedFile.absolutePath,
                    isVaulted = true
                )
            )
            refreshStorageStats()
        }
    }

    fun restoreDocumentFromVault(doc: DocumentEntity) {
        viewModelScope.launch {
            val vaultFile = File(doc.filePath)
            if (!vaultFile.exists()) return@launch
            val unlocked = VaultCryptoManager.decryptVaultFileToTemp(
                getApplication(),
                vaultFile,
                doc.formatName.lowercase(),
                settings.value.appLockPin
            )
            val libDir = File(getApplication<Application>().filesDir, "library").apply { mkdirs() }
            val restoredFile = File(libDir, doc.displayName)
            unlocked.copyTo(restoredFile, overwrite = true)
            repository.updateDocument(
                doc.copy(
                    filePath = restoredFile.absolutePath,
                    isVaulted = false
                )
            )
            refreshStorageStats()
        }
    }

    fun moveToTrash(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.moveToTrash(doc)
            _lastTrashedDocument.value = doc
        }
    }

    fun undoLastTrash() {
        val doc = _lastTrashedDocument.value ?: return
        viewModelScope.launch {
            repository.restoreFromTrash(doc)
            _lastTrashedDocument.value = null
        }
    }

    fun restoreFromTrash(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.restoreFromTrash(doc)
        }
    }

    fun deletePermanently(doc: DocumentEntity) {
        viewModelScope.launch {
            runCatching { File(doc.filePath).delete() }
            repository.deletePermanently(doc.id)
            refreshStorageStats()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            refreshStorageStats()
        }
    }

    // --- 3-Step Unified Tool Flow (Pick -> Configure -> Result) ---

    fun startToolFlow(tool: PdfToolId, preselectedDocs: List<DocumentEntity> = emptyList()) {
        preferences.recordToolUsed(tool.name)
        if (tool == PdfToolId.SCAN_TO_PDF && preselectedDocs.isEmpty()) {
            _showScannerScreen.value = true
            return
        }
        val defaultOrder = if (preselectedDocs.isNotEmpty()) {
            (0 until preselectedDocs.first().pageCount.coerceAtLeast(1)).toList()
        } else {
            emptyList()
        }
        _toolWizard.value = ActiveToolWizardState(
            tool = tool,
            step = if (preselectedDocs.isNotEmpty()) 2 else 1,
            selectedDocs = preselectedDocs,
            config = ToolConfiguration(customPageOrder = defaultOrder)
        )
    }

    fun openScannerScreen() {
        _showScannerScreen.value = true
    }

    fun closeScannerScreen() {
        _showScannerScreen.value = false
    }

    fun onScannerPagesCaptured(capturedImageFiles: List<File>, runOcrOnSave: Boolean) {
        _showScannerScreen.value = false
        if (capturedImageFiles.isEmpty()) return
        viewModelScope.launch {
            val savedEntities = mutableListOf<DocumentEntity>()
            capturedImageFiles.forEachIndexed { idx, imgFile ->
                val doc = DocumentEntity(
                    displayName = "Scan_Page_${idx + 1}.jpg",
                    filePath = imgFile.absolutePath,
                    formatName = DocumentFormat.IMAGE.name,
                    sizeBytes = imgFile.length(),
                    pageCount = 1
                )
                val id = repository.upsertDocument(doc)
                savedEntities.add(doc.copy(id = id))
            }
            _toolWizard.value = ActiveToolWizardState(
                tool = PdfToolId.SCAN_TO_PDF,
                step = 2,
                selectedDocs = savedEntities,
                config = ToolConfiguration(ocrDeskew = runOcrOnSave)
            )
            executeCurrentWizardTool(chainOcr = runOcrOnSave)
        }
    }

    fun updateWizardSelection(docs: List<DocumentEntity>) {
        val current = _toolWizard.value ?: return
        val firstPages = docs.firstOrNull()?.pageCount ?: 1
        _toolWizard.value = current.copy(
            selectedDocs = docs,
            config = current.config.copy(customPageOrder = (0 until firstPages).toList())
        )
    }

    fun reorderWizardFiles(fromIndex: Int, toIndex: Int) {
        val current = _toolWizard.value ?: return
        val mutable = current.selectedDocs.toMutableList()
        if (fromIndex in mutable.indices && toIndex in mutable.indices) {
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            _toolWizard.value = current.copy(selectedDocs = mutable)
        }
    }

    fun removeWizardFileAt(index: Int) {
        val current = _toolWizard.value ?: return
        val mutable = current.selectedDocs.toMutableList()
        if (index in mutable.indices) {
            mutable.removeAt(index)
            _toolWizard.value = current.copy(selectedDocs = mutable)
        }
    }

    fun updateWizardStep(step: Int) {
        val current = _toolWizard.value ?: return
        _toolWizard.value = current.copy(step = step.coerceIn(1, 3))
    }

    fun updateWizardConfig(transform: (ToolConfiguration) -> ToolConfiguration) {
        val current = _toolWizard.value ?: return
        _toolWizard.value = current.copy(config = transform(current.config))
    }

    fun applyToolPreset(preset: ToolPresetEntity) {
        val tool = runCatching { PdfToolId.valueOf(preset.toolIdName) }.getOrDefault(PdfToolId.COMPRESS_PDF)
        _toolWizard.value = ActiveToolWizardState(
            tool = tool,
            step = 1,
            selectedDocs = emptyList(),
            config = ToolConfiguration()
        )
    }

    fun saveCurrentConfigAsPreset(presetName: String) {
        val current = _toolWizard.value ?: return
        if (presetName.isBlank()) return
        viewModelScope.launch {
            repository.savePreset(
                ToolPresetEntity(
                    name = presetName.trim(),
                    toolIdName = current.tool.name,
                    configSummary = "${current.tool.title} • Range: ${current.config.pageRangeSpec}",
                    configJson = "range=${current.config.pageRangeSpec};rotate=${current.config.rotateDegrees}"
                )
            )
        }
    }

    fun executeCurrentWizardTool(chainOcr: Boolean = false) {
        val current = _toolWizard.value ?: return
        if (current.selectedDocs.isEmpty()) return

        activeToolJob?.cancel()
        activeToolJob = viewModelScope.launch {
            _toolWizard.value = current.copy(
                isRunning = true,
                progressPercent = 5,
                progressStatus = "Starting ${current.tool.title}..."
            )
            val inputFiles = current.selectedDocs.map { File(it.filePath) }
            val firstDocId = current.selectedDocs.first().id
            val docAnnotations = repository.getAnnotationsSync(firstDocId)

            val jobRecord = BatchJobEntity(
                title = "${current.tool.title} (${inputFiles.first().name})",
                toolIdName = current.tool.name,
                status = "RUNNING",
                progressPercent = 10,
                inputFilesCount = inputFiles.size,
                beforeBytes = inputFiles.sumOf { if (it.exists()) it.length() else 0L }
            )
            val jobId = repository.insertJob(jobRecord)

            var execResult = PdfToolkitEngine.executeTool(
                context = getApplication(),
                tool = current.tool,
                inputFiles = inputFiles,
                config = current.config,
                annotations = docAnnotations,
                outputPattern = settings.value.outputNamingPattern,
                onProgress = { pct, status ->
                    _toolWizard.value = _toolWizard.value?.copy(
                        progressPercent = pct,
                        progressStatus = status
                    )
                }
            )

            if (chainOcr && execResult.success && execResult.primaryOutputPath.endsWith(".pdf")) {
                execResult = PdfToolkitEngine.executeTool(
                    context = getApplication(),
                    tool = PdfToolId.OCR_PDF,
                    inputFiles = listOf(File(execResult.primaryOutputPath)),
                    config = current.config,
                    outputPattern = settings.value.outputNamingPattern
                )
            }

            if (execResult.success && execResult.primaryOutputPath.isNotEmpty()) {
                val outFile = File(execResult.primaryOutputPath)
                if (outFile.exists()) {
                    val format = DocumentFormat.fromFilename(outFile.name)
                    val session = MultiFormatDocumentEngine.openSession(getApplication(), outFile)
                    repository.upsertDocument(
                        DocumentEntity(
                            displayName = outFile.name,
                            filePath = outFile.absolutePath,
                            formatName = format.name,
                            sizeBytes = outFile.length(),
                            pageCount = session.pageCount,
                            indexedContent = session.pagesText.take(8).joinToString(" ").take(2000)
                        )
                    )
                }
                repository.updateJob(
                    jobRecord.copy(
                        id = jobId,
                        status = "COMPLETED",
                        progressPercent = 100,
                        outputPath = execResult.primaryOutputPath,
                        afterBytes = execResult.outputSizeBytes,
                        summaryMessage = execResult.summaryReport
                    )
                )
                postCompletionNotification(current.tool.title, execResult.summaryReport)
            } else {
                repository.updateJob(
                    jobRecord.copy(
                        id = jobId,
                        status = "FAILED",
                        progressPercent = 0,
                        summaryMessage = execResult.errorMessage ?: "Failed"
                    )
                )
            }

            refreshStorageStats()
            _toolWizard.value = _toolWizard.value?.copy(
                step = 3,
                isRunning = false,
                progressPercent = 100,
                result = execResult
            )
        }
    }

    fun cancelActiveToolJob() {
        activeToolJob?.cancel()
        activeToolJob = null
        val current = _toolWizard.value ?: return
        _toolWizard.value = current.copy(
            isRunning = false,
            progressPercent = 0,
            progressStatus = "Cancelled by user"
        )
    }

    fun closeToolWizard() {
        activeToolJob?.cancel()
        _toolWizard.value = null
    }

    fun chainOutputIntoAnotherTool(outputPath: String, nextTool: PdfToolId) {
        viewModelScope.launch {
            val existing = repository.getDocumentByPath(outputPath)
            val doc = existing ?: run {
                val f = File(outputPath)
                val entity = DocumentEntity(
                    displayName = f.name,
                    filePath = f.absolutePath,
                    formatName = DocumentFormat.fromFilename(f.name).name,
                    sizeBytes = f.length(),
                    pageCount = 1
                )
                val id = repository.upsertDocument(entity)
                entity.copy(id = id)
            }
            startToolFlow(nextTool, listOf(doc))
        }
    }

    fun retryBatchJob(job: BatchJobEntity) {
        val tool = runCatching { PdfToolId.valueOf(job.toolIdName) }.getOrDefault(PdfToolId.COMPRESS_PDF)
        startToolFlow(tool, libraryDocuments.value.take(1))
    }

    private fun postCompletionNotification(toolTitle: String, summary: String) {
        runCatching {
            val context = getApplication<Application>()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "folio_jobs",
                    "Folio Offline Jobs",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                nm.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(context, "folio_jobs")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("$toolTitle Complete")
                .setContentText(summary)
                .setAutoCancel(true)
                .build()
            nm.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        }
    }
}
