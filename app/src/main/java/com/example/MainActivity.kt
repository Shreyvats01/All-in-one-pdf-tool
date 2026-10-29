package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SignatureEntity
import com.example.domain.model.PdfToolId
import com.example.reader.UniversalReaderScreen
import com.example.tools.DocumentScannerScreen
import com.example.tools.UnifiedToolWizardScreen
import com.example.ui.FilesScreen
import com.example.ui.FolioViewModel
import com.example.ui.HomeScreen
import com.example.ui.MainTab
import com.example.ui.OnboardingOverlay
import com.example.ui.SettingsScreen
import com.example.ui.TactilePinPad
import com.example.ui.ToolsScreen
import com.example.ui.theme.FolioTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val viewModel: FolioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            LaunchedEffect(settings.hideInRecents) {
                if (settings.hideInRecents) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(settings.keepScreenOnInReader) {
                if (settings.keepScreenOnInReader) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            val darkTheme = when (settings.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            FolioTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor
            ) {
                FolioAppRoot(
                    viewModel = viewModel,
                    onRequestBiometricAuth = { callback ->
                        promptBiometricUnlock(callback)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    viewModel.importSafUri(uri, openImmediately = true)
                }
            }
            Intent.ACTION_SEND -> {
                val sharedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                sharedUri?.let { uri ->
                    val mime = intent.type ?: ""
                    val targetTool = if (mime.startsWith("image/")) PdfToolId.IMAGE_TO_PDF else PdfToolId.OFFICE_TO_PDF
                    viewModel.importSafUri(uri, openImmediately = false, launchTool = targetTool)
                }
            }
        }
    }

    private fun promptBiometricUnlock(onResult: (Boolean) -> Unit) {
        runCatching {
            val executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onResult(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onResult(false)
                    }

                    override fun onAuthenticationFailed() {
                        onResult(false)
                    }
                }
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Folio Private Vault")
                .setSubtitle("Verify your identity offline")
                .setNegativeButtonText("Use PIN")
                .build()
            biometricPrompt.authenticate(promptInfo)
        }.onFailure {
            onResult(false)
        }
    }
}

@Composable
fun FolioAppRoot(
    viewModel: FolioViewModel,
    onRequestBiometricAuth: ((Boolean) -> Unit) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val libraryDocs by viewModel.libraryDocuments.collectAsStateWithLifecycle()
    val favouriteDocs by viewModel.favouriteDocuments.collectAsStateWithLifecycle()
    val trashedDocs by viewModel.trashedDocuments.collectAsStateWithLifecycle()
    val vaultDocs by viewModel.vaultedDocuments.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val presets by viewModel.toolPresets.collectAsStateWithLifecycle()
    val batchJobs by viewModel.batchJobs.collectAsStateWithLifecycle()
    val signatures by viewModel.signatures.collectAsStateWithLifecycle()
    val storageBreakdown by viewModel.storageBreakdown.collectAsStateWithLifecycle()

    val activeDocument by viewModel.activeDocument.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val readerLoading by viewModel.readerLoading.collectAsStateWithLifecycle()
    val toolWizard by viewModel.toolWizard.collectAsStateWithLifecycle()
    val showScanner by viewModel.showScannerScreen.collectAsStateWithLifecycle()
    val lastTrashed by viewModel.lastTrashedDocument.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Multi-file SAF Picker (allows picking 1 or more files with persistent read permission)
    val safMultiDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleSafUris(uris, openImmediately = uris.size == 1)
        }
    }

    // Folder Tree SAF Picker (allows granting persistent directory access)
    val safFolderTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.importSafFolderTree(treeUri)
        }
    }

    // Status message feedback
    LaunchedEffect(statusMessage) {
        val msg = statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = msg,
            duration = SnackbarDuration.Short
        )
        viewModel.clearStatusMessage()
    }

    // Snackbar Undo when a document is moved to Trash
    LaunchedEffect(lastTrashed) {
        val trashed = lastTrashed ?: return@LaunchedEffect
        val res = snackbarHostState.showSnackbar(
            message = "Moved '${trashed.displayName}' to Trash",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (res == SnackbarResult.ActionPerformed) {
            viewModel.undoLastTrash()
        }
    }

    // App Lock Startup Gate (Tactile 4-dot PIN Keypad + Biometric)
    var appUnlocked by remember(settings.appLockEnabled) { mutableStateOf(!settings.appLockEnabled) }

    if (!appUnlocked) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                TactilePinPad(
                    title = "Folio App Lock",
                    subtitle = "Enter your 4-digit offline PIN or use Biometric authentication",
                    expectedPin = settings.appLockPin,
                    onUnlocked = {
                        appUnlocked = true
                    },
                    onBiometricClick = {
                        onRequestBiometricAuth { ok ->
                            if (ok) appUnlocked = true
                        }
                    },
                    modifier = Modifier.testTag("app_lock_pin_input")
                )
            }
        }
        return
    }

    // 1. Optional Onboarding
    if (!settings.onboardingCompleted) {
        OnboardingOverlay(
            onComplete = {
                viewModel.preferences.update { it.copy(onboardingCompleted = true) }
            }
        )
        return
    }

    // 2. Full-screen CameraX Scanner
    if (showScanner) {
        DocumentScannerScreen(
            onCancel = { viewModel.closeScannerScreen() },
            onFinishScan = { files, runOcr ->
                viewModel.onScannerPagesCaptured(files, runOcr)
            }
        )
        return
    }

    // 3. Full-screen 3-Step Unified Tool Wizard
    val currentWizard = toolWizard
    if (currentWizard != null) {
        UnifiedToolWizardScreen(
            wizardState = currentWizard,
            viewModel = viewModel,
            onClose = { viewModel.closeToolWizard() },
            onOpenResultInReader = { outputPath ->
                viewModel.closeToolWizard()
                val match = libraryDocs.firstOrNull { it.filePath == outputPath }
                if (match != null) {
                    viewModel.openDocument(match)
                }
            }
        )
        return
    }

    // 4. Full-screen Universal Document Reader
    val currentDoc = activeDocument
    if (currentDoc != null) {
        UniversalReaderScreen(
            document = currentDoc,
            session = activeSession,
            isLoading = readerLoading,
            viewModel = viewModel,
            onBack = { viewModel.closeReader() },
            onOpenInTool = { toolId ->
                val docToProcess = currentDoc
                viewModel.closeReader()
                viewModel.startToolFlow(toolId, listOf(docToProcess))
            }
        )
        return
    }

    // 5. Adaptive Main Workspace Shell (NavigationRail on Wide Tablets, Standard M3 NavigationBar on Phones)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        NavTabItem.entries.forEach { item ->
                            val selected = currentTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.filledIcon else item.outlinedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${item.tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding()
                    ) {
                        Spacer(Modifier.height(16.dp))
                        NavTabItem.entries.forEach { item ->
                            val selected = currentTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        if (selected) item.filledIcon else item.outlinedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${item.tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 920.dp).fillMaxSize()) {
                        when (currentTab) {
                            MainTab.HOME -> HomeScreen(
                                documents = libraryDocs,
                                favourites = favouriteDocs,
                                searchHistory = settings.searchHistory,
                                connectedFolderName = settings.connectedFolderName,
                                onOpenDocument = { viewModel.openDocument(it) },
                                onToggleFavourite = { viewModel.toggleFavourite(it) },
                                onDeleteDocument = { viewModel.moveToTrash(it) },
                                onQuickToolClick = { toolId -> viewModel.startToolFlow(toolId) },
                                onPickSafFile = { safMultiDocumentLauncher.launch(arrayOf("*/*")) },
                                onConnectFolder = { safFolderTreeLauncher.launch(null) },
                                onRecordSearch = { q -> viewModel.preferences.recordSearchQuery(q) }
                            )

                            MainTab.TOOLS -> ToolsScreen(
                                recentToolIds = settings.recentToolIds,
                                presets = presets,
                                jobs = batchJobs,
                                onSelectTool = { toolId -> viewModel.startToolFlow(toolId) },
                                onSelectPreset = { preset -> viewModel.applyToolPreset(preset) },
                                onDeletePreset = { id ->
                                    scope.launch { viewModel.repository.deletePreset(id) }
                                },
                                onRetryJob = { job -> viewModel.retryBatchJob(job) },
                                onClearJobs = {
                                    scope.launch { viewModel.repository.clearFinishedJobs() }
                                }
                            )

                            MainTab.FILES -> FilesScreen(
                                libraryDocs = libraryDocs,
                                vaultDocs = vaultDocs,
                                trashedDocs = trashedDocs,
                                folders = folders,
                                tags = tags,
                                appLockPin = settings.appLockPin,
                                connectedFolderName = settings.connectedFolderName,
                                onOpenDocument = { viewModel.openDocument(it) },
                                onPickSafFile = { safMultiDocumentLauncher.launch(arrayOf("*/*")) },
                                onConnectFolder = { safFolderTreeLauncher.launch(null) },
                                onCreateFolder = { name ->
                                    scope.launch { viewModel.repository.createFolder(name) }
                                },
                                onCreateTag = { name ->
                                    scope.launch { viewModel.repository.createTag(name) }
                                },
                                onRenameDoc = { doc, newName -> viewModel.renameDocument(doc, newName) },
                                onDuplicateDoc = { doc -> viewModel.duplicateDocument(doc) },
                                onAssignFolderAndTags = { doc, fId, tagsCsv ->
                                    viewModel.assignFolderAndTags(doc, fId, tagsCsv)
                                },
                                onMoveToVault = { doc -> viewModel.moveDocumentToVault(doc) },
                                onRestoreFromVault = { doc -> viewModel.restoreDocumentFromVault(doc) },
                                onMoveToTrash = { doc -> viewModel.moveToTrash(doc) },
                                onRestoreFromTrash = { doc -> viewModel.restoreFromTrash(doc) },
                                onDeletePermanently = { doc -> viewModel.deletePermanently(doc) },
                                onEmptyTrash = { viewModel.emptyTrash() },
                                onRequestBiometricUnlock = onRequestBiometricAuth
                            )

                            MainTab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                storageBreakdown = storageBreakdown,
                                signatures = signatures,
                                onUpdateSettings = { transform -> viewModel.preferences.update(transform) },
                                onAddSignature = { sigText ->
                                    scope.launch {
                                        viewModel.repository.saveSignature(
                                            SignatureEntity(
                                                label = "Saved Signature",
                                                typedText = sigText
                                            )
                                        )
                                    }
                                },
                                onDeleteSignature = { id ->
                                    scope.launch { viewModel.repository.deleteSignature(id) }
                                },
                                onClearCache = { viewModel.clearCacheAndTemp() }
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class NavTabItem(
    val tab: MainTab,
    val label: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
) {
    HOME(MainTab.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    TOOLS(MainTab.TOOLS, "Tools", Icons.Outlined.Build, Icons.Filled.Build),
    FILES(MainTab.FILES, "Files", Icons.Outlined.Folder, Icons.Filled.Folder),
    SETTINGS(MainTab.SETTINGS, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
}
