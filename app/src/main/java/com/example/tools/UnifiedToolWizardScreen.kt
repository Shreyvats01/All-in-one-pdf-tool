package com.example.tools

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.DocumentEntity
import com.example.domain.model.CompressionLevel
import com.example.domain.model.DocumentFormat
import com.example.domain.model.NumberFormatStyle
import com.example.domain.model.PagePosition
import com.example.domain.model.PdfToolId
import com.example.domain.model.SplitMode
import com.example.domain.model.StampPreset
import com.example.reader.MultiFormatDocumentEngine
import com.example.ui.ActiveToolWizardState
import com.example.ui.DocumentPageThumbnail
import com.example.ui.FolioViewModel
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedToolWizardScreen(
    wizardState: ActiveToolWizardState,
    viewModel: FolioViewModel,
    onClose: () -> Unit,
    onOpenResultInReader: (String) -> Unit
) {
    BackHandler {
        if (wizardState.step > 1 && !wizardState.isRunning) {
            viewModel.updateWizardStep(wizardState.step - 1)
        } else {
            onClose()
        }
    }

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val allLibraryDocs by viewModel.libraryDocuments.collectAsStateWithLifecycle()
    val signatures by viewModel.signatures.collectAsStateWithLifecycle()

    val safFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importSafUri(uri, openImmediately = false)
        }
    }

    val safSaveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val outPath = wizardState.result?.primaryOutputPath ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            viewModel.exportFileToSafUri(outPath, uri)
        }
    }

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var showRenameResultDialog by remember { mutableStateOf(false) }
    var renameResultInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Editorial Header with 3-Step Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("wizard_close_button")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close tool")
                }
                Column {
                    Text(wizardState.tool.title, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        wizardState.tool.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (wizardState.step == 2) {
                IconButton(onClick = { showSavePresetDialog = true }) {
                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save configuration as preset")
                }
            }
        }

        // Stepper Pill Row (Step 1: Pick files -> Step 2: Configure -> Step 3: Result)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1 to "1. Pick Files", 2 to "2. Configure", 3 to "3. Result").forEach { (stepNum, label) ->
                val isCurrent = wizardState.step == stepNum
                val isDone = wizardState.step > stepNum
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isDone -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = stepNum < wizardState.step && !wizardState.isRunning) {
                            viewModel.updateWizardStep(stepNum)
                        }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.onPrimary
                            isDone -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Step Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (wizardState.step) {
                1 -> Step1PickFiles(
                    wizardState = wizardState,
                    allLibraryDocs = allLibraryDocs,
                    onPickFromSaf = { safFilePicker.launch(arrayOf("*/*")) },
                    onToggleSelect = { doc ->
                        val current = wizardState.selectedDocs
                        val updated = if (wizardState.tool.acceptsMultipleFiles) {
                            if (current.any { it.id == doc.id }) current.filterNot { it.id == doc.id }
                            else current + doc
                        } else {
                            listOf(doc)
                        }
                        viewModel.updateWizardSelection(updated)
                    },
                    onMoveUp = { idx -> viewModel.reorderWizardFiles(idx, idx - 1) },
                    onMoveDown = { idx -> viewModel.reorderWizardFiles(idx, idx + 1) },
                    onNextStep = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.updateWizardStep(2)
                    }
                )

                2 -> Step2ConfigureTool(
                    wizardState = wizardState,
                    savedSignatures = signatures.map { it.typedText.ifBlank { it.label } },
                    viewModel = viewModel,
                    onRunTool = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.executeCurrentWizardTool()
                    }
                )

                3 -> Step3ResultScreen(
                    wizardState = wizardState,
                    onOpenInReader = { path -> onOpenResultInReader(path) },
                    onShare = { path -> MultiFormatDocumentEngine.shareDocumentOffline(context, File(path)) },
                    onSaveAsSaf = { path ->
                        val file = File(path)
                        safSaveAsLauncher.launch(file.name)
                    },
                    onRenameClick = {
                        val file = File(wizardState.result?.primaryOutputPath ?: "")
                        renameResultInput = file.nameWithoutExtension
                        showRenameResultDialog = true
                    },
                    onChainTool = { nextTool ->
                        val outPath = wizardState.result?.primaryOutputPath ?: return@Step3ResultScreen
                        viewModel.chainOutputIntoAnotherTool(outPath, nextTool)
                    },
                    onDone = onClose
                )
            }
        }
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Reusable Preset") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("Preset Name (e.g. Monthly Archive)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveCurrentConfigAsPreset(presetNameInput)
                    presetNameInput = ""
                    showSavePresetDialog = false
                }) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameResultDialog) {
        AlertDialog(
            onDismissRequest = { showRenameResultDialog = false },
            title = { Text("Rename Output File") },
            text = {
                OutlinedTextField(
                    value = renameResultInput,
                    onValueChange = { renameResultInput = it },
                    label = { Text("New filename") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val outPath = wizardState.result?.primaryOutputPath ?: ""
                    val doc = allLibraryDocs.firstOrNull { it.filePath == outPath }
                    if (doc != null) {
                        viewModel.renameDocument(doc, renameResultInput)
                    }
                    showRenameResultDialog = false
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameResultDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Step1PickFiles(
    wizardState: ActiveToolWizardState,
    allLibraryDocs: List<DocumentEntity>,
    onPickFromSaf: () -> Unit,
    onToggleSelect: (DocumentEntity) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onNextStep: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (wizardState.tool.acceptsMultipleFiles) "Select one or more files (reorder below)"
                else "Select source document",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedButton(onClick = onPickFromSaf, modifier = Modifier.testTag("wizard_pick_saf_button")) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Browse Device (SAF)")
            }
        }

        Spacer(Modifier.height(10.dp))

        // Selected files reorderable queue (when multiple files selected)
        if (wizardState.selectedDocs.size > 1) {
            Text(
                "Execution Order (${wizardState.selectedDocs.size} files selected):",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            wizardState.selectedDocs.forEachIndexed { idx, sel ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}. ${sel.displayName}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { onMoveUp(idx) }, enabled = idx > 0, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up")
                        }
                        IconButton(onClick = { onMoveDown(idx) }, enabled = idx < wizardState.selectedDocs.lastIndex, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down")
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(allLibraryDocs) { _, doc ->
                val isSelected = wizardState.selectedDocs.any { it.id == doc.id }
                Card(
                    onClick = { onToggleSelect(doc) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect(doc) })
                        Spacer(Modifier.width(8.dp))
                        DocumentPageThumbnail(
                            filePath = doc.filePath,
                            formatName = doc.formatName,
                            pageIndex = 0,
                            width = 40.dp,
                            height = 54.dp,
                            isSelected = isSelected
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${doc.formatName} • ${doc.pageCount} pages • ${doc.sizeBytes / 1024} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onNextStep,
            enabled = wizardState.selectedDocs.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("wizard_next_step_button")
        ) {
            Text("Continue to Configure (${wizardState.selectedDocs.size})")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step2ConfigureTool(
    wizardState: ActiveToolWizardState,
    savedSignatures: List<String>,
    viewModel: FolioViewModel,
    onRunTool: () -> Unit
) {
    val cfg = wizardState.config
    val tool = wizardState.tool
    val firstDoc = wizardState.selectedDocs.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Non-destructive badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = "Non-destructive mode active: Original file(s) will not be modified. A new output file will be created.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Visual Thumbnail Page Picker + Range Input for page-targeted tools
            if (tool in listOf(
                    PdfToolId.MERGE_PDF, PdfToolId.SPLIT_PDF, PdfToolId.REMOVE_PAGES,
                    PdfToolId.EXTRACT_PAGES, PdfToolId.ROTATE_PDF, PdfToolId.WATERMARK_PDF,
                    PdfToolId.CROP_PDF, PdfToolId.PDF_TO_IMAGE, PdfToolId.ORGANIZE_PAGES
                )
            ) {
                OutlinedTextField(
                    value = cfg.pageRangeSpec,
                    onValueChange = { spec -> viewModel.updateWizardConfig { it.copy(pageRangeSpec = spec) } },
                    label = { Text("Page Selection / Range (e.g. All, 1-3, 5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (firstDoc != null && firstDoc.pageCount > 1) {
                    Text(
                        "Visual Page Picker (Tap thumbnail to toggle page):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    val selectedSet = PdfToolkitEngine.parsePageIndices(cfg.pageRangeSpec, firstDoc.pageCount)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(firstDoc.pageCount.coerceAtMost(24)) { pageZeroIdx ->
                            val isPicked = selectedSet.contains(pageZeroIdx)
                            Card(
                                onClick = {
                                    val mutable = selectedSet.toMutableSet()
                                    if (isPicked && mutable.size > 1) mutable.remove(pageZeroIdx) else mutable.add(pageZeroIdx)
                                    val newSpec = mutable.sorted().joinToString(",") { "${it + 1}" }
                                    viewModel.updateWizardConfig { it.copy(pageRangeSpec = newSpec) }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPicked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                modifier = Modifier.border(
                                    width = if (isPicked) 2.dp else 1.dp,
                                    color = if (isPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(14.dp)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    DocumentPageThumbnail(
                                        filePath = firstDoc.filePath,
                                        formatName = firstDoc.formatName,
                                        pageIndex = pageZeroIdx,
                                        width = 68.dp,
                                        height = 92.dp,
                                        isSelected = isPicked
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Page ${pageZeroIdx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isPicked) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isPicked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tool-Specific Controls
            when (tool) {
                PdfToolId.MERGE_PDF -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Create Outline Bookmark per Source PDF")
                        Switch(
                            checked = cfg.addBookmarksPerSource,
                            onCheckedChange = { b -> viewModel.updateWizardConfig { it.copy(addBookmarksPerSource = b) } }
                        )
                    }
                }

                PdfToolId.SPLIT_PDF -> {
                    Text("Split Mode", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SplitMode.entries.forEach { mode ->
                            FilterChip(
                                selected = cfg.splitMode == mode,
                                onClick = { viewModel.updateWizardConfig { it.copy(splitMode = mode) } },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    if (cfg.splitMode == SplitMode.EVERY_N_PAGES) {
                        Text("Split every ${cfg.splitEveryN} page(s)")
                        Slider(
                            value = cfg.splitEveryN.toFloat(),
                            onValueChange = { v -> viewModel.updateWizardConfig { it.copy(splitEveryN = v.toInt().coerceAtLeast(1)) } },
                            valueRange = 1f..10f
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bundle output files as single .ZIP archive")
                        Switch(
                            checked = cfg.outputAsZip,
                            onCheckedChange = { z -> viewModel.updateWizardConfig { it.copy(outputAsZip = z) } }
                        )
                    }
                }

                PdfToolId.ORGANIZE_PAGES -> {
                    Text("Page Order & Actions (Tap to rotate/duplicate/insert blank)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 90, 180, 270).forEach { deg ->
                            FilterChip(
                                selected = cfg.rotateDegrees == deg,
                                onClick = { viewModel.updateWizardConfig { it.copy(rotateDegrees = deg) } },
                                label = { Text("Rotate $deg°") }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            viewModel.updateWizardConfig { it.copy(customPageOrder = it.customPageOrder.reversed()) }
                        }) {
                            Text("Reverse Page Order")
                        }
                        OutlinedButton(onClick = {
                            viewModel.updateWizardConfig { it.copy(insertBlankAfterPage = 1) }
                        }) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Insert Blank Page")
                        }
                    }
                }

                PdfToolId.COMPRESS_PDF -> {
                    Text("Compression Preset", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompressionLevel.entries.forEach { level ->
                            FilterChip(
                                selected = cfg.compressionLevel == level,
                                onClick = { viewModel.updateWizardConfig { it.copy(compressionLevel = level) } },
                                label = { Text(level.label) }
                            )
                        }
                    }
                    val origKb = ((firstDoc?.sizeBytes ?: 120000L) / 1024).coerceAtLeast(10)
                    val estKb = (origKb * cfg.compressionLevel.scaleFactor).toLong().coerceAtLeast(8)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Estimated Savings Preview", style = MaterialTheme.typography.titleSmall)
                            Text("Before: $origKb KB  →  Estimated After: ~$estKb KB")
                        }
                    }
                }

                PdfToolId.OCR_PDF -> {
                    Text("Bundled Offline OCR Language Pack", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("eng+lat" to "English + Latin (Bundled)", "eng" to "English Fast", "lat+deu+fra+spa" to "Western European Latin").forEach { (code, label) ->
                            FilterChip(
                                selected = cfg.ocrLanguage == code,
                                onClick = { viewModel.updateWizardConfig { it.copy(ocrLanguage = code) } },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Deskew crooked scanned pages")
                        Switch(
                            checked = cfg.ocrDeskew,
                            onCheckedChange = { d -> viewModel.updateWizardConfig { it.copy(ocrDeskew = d) } }
                        )
                    }
                }

                PdfToolId.IMAGE_TO_PDF, PdfToolId.SCAN_TO_PDF -> {
                    Text("Page Size & Orientation", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A4", "LETTER", "FIT").forEach { sz ->
                            FilterChip(
                                selected = cfg.imagePageSize == sz,
                                onClick = { viewModel.updateWizardConfig { it.copy(imagePageSize = sz) } },
                                label = { Text(sz) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PORTRAIT", "LANDSCAPE").forEach { orient ->
                            FilterChip(
                                selected = cfg.imageOrientation == orient,
                                onClick = { viewModel.updateWizardConfig { it.copy(imageOrientation = orient) } },
                                label = { Text(orient) }
                            )
                        }
                    }
                    Text("Page Margin: ${cfg.imageMarginDp} pt")
                    Slider(
                        value = cfg.imageMarginDp.toFloat(),
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(imageMarginDp = v.toInt()) } },
                        valueRange = 0f..64f
                    )
                }

                PdfToolId.OFFICE_TO_PDF, PdfToolId.TEXT_WEB_TO_PDF -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "Offline Conversion Notice: Text, tables, headings, and basic pagination are preserved 100% offline. Complex custom macros or floating shapes may vary.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text("Typography Font Size: ${cfg.fontSizePt.toInt()} pt")
                    Slider(
                        value = cfg.fontSizePt,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(fontSizePt = v) } },
                        valueRange = 9f..18f
                    )
                }

                PdfToolId.PDF_TO_IMAGE -> {
                    Text("Image Format & Resolution (DPI)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("JPG", "PNG").forEach { fmt ->
                            FilterChip(
                                selected = cfg.exportImageFormat == fmt,
                                onClick = { viewModel.updateWizardConfig { it.copy(exportImageFormat = fmt) } },
                                label = { Text(fmt) }
                            )
                        }
                        listOf(72, 150, 300).forEach { dpi ->
                            FilterChip(
                                selected = cfg.exportDpi == dpi,
                                onClick = { viewModel.updateWizardConfig { it.copy(exportDpi = dpi) } },
                                label = { Text("$dpi DPI") }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Export all pages into a single .ZIP file")
                        Switch(
                            checked = cfg.outputAsZip,
                            onCheckedChange = { z -> viewModel.updateWizardConfig { it.copy(outputAsZip = z) } }
                        )
                    }
                }

                PdfToolId.PDF_TO_OFFICE -> {
                    Text("Target Office OpenXML Format", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DOCX" to "Word (.docx)", "XLSX" to "Excel (.xlsx)", "PPTX" to "PowerPoint (.pptx)").forEach { (code, label) ->
                            FilterChip(
                                selected = cfg.exportOfficeFormat == code,
                                onClick = { viewModel.updateWizardConfig { it.copy(exportOfficeFormat = code) } },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                PdfToolId.PDF_TO_TEXT_MARKUP, PdfToolId.EXTRACT_TEXT -> {
                    Text("Target Markup Format", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MD", "TXT", "HTML", "RTF").forEach { fmt ->
                            FilterChip(
                                selected = cfg.exportTextFormat == fmt,
                                onClick = { viewModel.updateWizardConfig { it.copy(exportTextFormat = fmt) } },
                                label = { Text(fmt) }
                            )
                        }
                    }
                }

                PdfToolId.ROTATE_PDF -> {
                    Text("Rotation Angle", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(90, 180, 270).forEach { deg ->
                            FilterChip(
                                selected = cfg.rotateDegrees == deg,
                                onClick = { viewModel.updateWizardConfig { it.copy(rotateDegrees = deg) } },
                                label = { Text("$deg° Clockwise") }
                            )
                        }
                    }
                }

                PdfToolId.ADD_PAGE_NUMBERS -> {
                    Text("Numbering Format", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberFormatStyle.entries.forEach { style ->
                            FilterChip(
                                selected = cfg.pageNumberFormat == style,
                                onClick = { viewModel.updateWizardConfig { it.copy(pageNumberFormat = style) } },
                                label = { Text(style.label) }
                            )
                        }
                    }
                    Text("Position Grid", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PagePosition.entries.forEach { pos ->
                            FilterChip(
                                selected = cfg.pageNumberPosition == pos,
                                onClick = { viewModel.updateWizardConfig { it.copy(pageNumberPosition = pos) } },
                                label = { Text(pos.label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Skip First Cover Page")
                        Switch(
                            checked = cfg.skipFirstPage,
                            onCheckedChange = { s -> viewModel.updateWizardConfig { it.copy(skipFirstPage = s) } }
                        )
                    }
                }

                PdfToolId.WATERMARK_PDF -> {
                    OutlinedTextField(
                        value = cfg.watermarkText,
                        onValueChange = { t -> viewModel.updateWizardConfig { it.copy(watermarkText = t) } },
                        label = { Text("Watermark Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Opacity: ${(cfg.watermarkOpacity * 100).toInt()}%")
                    Slider(
                        value = cfg.watermarkOpacity,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(watermarkOpacity = v) } },
                        valueRange = 0.1f..0.85f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tiled Grid Across Entire Page")
                        Switch(
                            checked = cfg.watermarkTiled,
                            onCheckedChange = { t -> viewModel.updateWizardConfig { it.copy(watermarkTiled = t) } }
                        )
                    }
                }

                PdfToolId.HEADER_FOOTER -> {
                    OutlinedTextField(
                        value = cfg.headerText,
                        onValueChange = { h -> viewModel.updateWizardConfig { it.copy(headerText = h) } },
                        label = { Text("Header Template (supports {filename}, {date}, {page})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cfg.footerText,
                        onValueChange = { f -> viewModel.updateWizardConfig { it.copy(footerText = f) } },
                        label = { Text("Footer Template") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PdfToolId.CROP_PDF -> {
                    Text("Crop Margin Inset: ${(cfg.cropMarginPercent * 100).toInt()}%")
                    Slider(
                        value = cfg.cropMarginPercent,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(cropMarginPercent = v) } },
                        valueRange = 0.02f..0.25f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-crop surrounding whitespace margins")
                        Switch(
                            checked = cfg.autoCropWhitespace,
                            onCheckedChange = { a -> viewModel.updateWizardConfig { it.copy(autoCropWhitespace = a) } }
                        )
                    }
                }

                PdfToolId.RESIZE_NUP_BOOKLET -> {
                    Text("Pages per Sheet (N-Up)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 4, 6).forEach { n ->
                            FilterChip(
                                selected = cfg.nUpPagesPerSheet == n,
                                onClick = { viewModel.updateWizardConfig { it.copy(nUpPagesPerSheet = n) } },
                                label = { Text("$n Pages / Sheet") }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Booklet Fold Imposition Mode")
                        Switch(
                            checked = cfg.bookletMode,
                            onCheckedChange = { b -> viewModel.updateWizardConfig { it.copy(bookletMode = b) } }
                        )
                    }
                }

                PdfToolId.REMOVE_BLANK_PAGES -> {
                    Text("Blank Detection Sensitivity: ${(cfg.blankPageSensitivity * 100).toInt()}%")
                    Slider(
                        value = cfg.blankPageSensitivity,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(blankPageSensitivity = v) } },
                        valueRange = 0.80f..1.0f
                    )
                }

                PdfToolId.GRAYSCALE_BW -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("High-Contrast Pure Black & White (Archival Scan look)")
                        Switch(
                            checked = cfg.highContrastBw,
                            onCheckedChange = { bw -> viewModel.updateWizardConfig { it.copy(highContrastBw = bw) } }
                        )
                    }
                }

                PdfToolId.EDIT_METADATA -> {
                    OutlinedTextField(
                        value = cfg.metaTitle,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(metaTitle = v) } },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cfg.metaAuthor,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(metaAuthor = v) } },
                        label = { Text("Author") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cfg.metaSubject,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(metaSubject = v) } },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cfg.metaKeywords,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(metaKeywords = v) } },
                        label = { Text("Keywords (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PdfToolId.PROTECT_PDF, PdfToolId.UNLOCK_PDF -> {
                    OutlinedTextField(
                        value = cfg.openPassword,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(openPassword = v) } },
                        label = { Text(if (tool == PdfToolId.PROTECT_PDF) "AES-256 Open Password" else "Known Password to Unlock") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("wizard_password_input")
                    )
                    if (tool == PdfToolId.PROTECT_PDF) {
                        OutlinedTextField(
                            value = cfg.permissionPassword,
                            onValueChange = { v -> viewModel.updateWizardConfig { it.copy(permissionPassword = v) } },
                            label = { Text("Permissions / Owner Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = cfg.allowPrinting,
                                onClick = { viewModel.updateWizardConfig { it.copy(allowPrinting = !it.allowPrinting) } },
                                label = { Text("Allow Print") }
                            )
                            FilterChip(
                                selected = cfg.allowCopying,
                                onClick = { viewModel.updateWizardConfig { it.copy(allowCopying = !it.allowCopying) } },
                                label = { Text("Allow Copy") }
                            )
                            FilterChip(
                                selected = cfg.allowEditing,
                                onClick = { viewModel.updateWizardConfig { it.copy(allowEditing = !it.allowEditing) } },
                                label = { Text("Allow Edit") }
                            )
                        }
                    }
                }

                PdfToolId.SIGN_PDF -> {
                    OutlinedTextField(
                        value = cfg.signatureText,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(signatureText = v) } },
                        label = { Text("Signer Name / Signature Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (savedSignatures.isNotEmpty()) {
                        Text("Saved Signatures:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            savedSignatures.forEach { sig ->
                                FilterChip(
                                    selected = cfg.signatureText == sig,
                                    onClick = { viewModel.updateWizardConfig { it.copy(signatureText = sig) } },
                                    label = { Text(sig) }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Attach Local PKCS#12 SHA-256 Digital Certificate")
                        Switch(
                            checked = cfg.usePkcs12Certificate,
                            onCheckedChange = { c -> viewModel.updateWizardConfig { it.copy(usePkcs12Certificate = c) } }
                        )
                    }
                }

                PdfToolId.REDACT_PDF -> {
                    OutlinedTextField(
                        value = cfg.redactSearchTerms,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(redactSearchTerms = v) } },
                        label = { Text("Sensitive Terms or Patterns to Permanently Purge") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "Verification Step Enabled: Matching text tokens and regions are permanently removed from the underlying PDF object stream—not merely covered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                PdfToolId.STAMPS -> {
                    Text("Stamp Preset", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StampPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = cfg.stampPreset == preset,
                                onClick = { viewModel.updateWizardConfig { it.copy(stampPreset = preset) } },
                                label = { Text(preset.label) }
                            )
                        }
                    }
                }

                PdfToolId.EDIT_PDF_CONTENT -> {
                    OutlinedTextField(
                        value = cfg.editAddedText,
                        onValueChange = { v -> viewModel.updateWizardConfig { it.copy(editAddedText = v) } },
                        label = { Text("Text Block to Insert / Update") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PdfToolId.BATCH_PROCESSOR -> {
                    Text("Select Batch Operation to Run Across All Selected Files:", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            PdfToolId.COMPRESS_PDF,
                            PdfToolId.WATERMARK_PDF,
                            PdfToolId.ROTATE_PDF,
                            PdfToolId.GRAYSCALE_BW,
                            PdfToolId.PROTECT_PDF,
                            PdfToolId.OCR_PDF
                        ).forEach { sub ->
                            FilterChip(
                                selected = cfg.batchSubTool == sub,
                                onClick = { viewModel.updateWizardConfig { it.copy(batchSubTool = sub) } },
                                label = { Text(sub.title) }
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        "Ready to run ${tool.title} on ${wizardState.selectedDocs.size} selected file(s).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Running Progress & Cancel Bar or Start Button
        if (wizardState.isRunning) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(wizardState.progressStatus, style = MaterialTheme.typography.bodyMedium)
                        Text("${wizardState.progressPercent}%", style = MaterialTheme.typography.labelLarge)
                    }
                    LinearProgressIndicator(
                        progress = { wizardState.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { viewModel.cancelActiveToolJob() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel Job")
                    }
                }
            }
        } else {
            Button(
                onClick = onRunTool,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("wizard_execute_button")
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Run ${tool.title}", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step3ResultScreen(
    wizardState: ActiveToolWizardState,
    onOpenInReader: (String) -> Unit,
    onShare: (String) -> Unit,
    onSaveAsSaf: (String) -> Unit,
    onRenameClick: () -> Unit,
    onChainTool: (PdfToolId) -> Unit,
    onDone: () -> Unit
) {
    val res = wizardState.result
    if (res == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (res.success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Inline Rendered Output Thumbnail Preview
                if (res.success && res.primaryOutputPath.isNotEmpty()) {
                    val outFile = File(res.primaryOutputPath)
                    val extFormat = DocumentFormat.fromFilename(outFile.name).name
                    DocumentPageThumbnail(
                        filePath = outFile.absolutePath,
                        formatName = extFormat,
                        pageIndex = 0,
                        width = 74.dp,
                        height = 102.dp
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (res.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (res.success) "${wizardState.tool.title} Complete" else "Operation Needs Attention",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = if (res.success) res.summaryReport else (res.errorMessage ?: "Failed"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (res.success) {
                        Text(
                            text = "Before: ${res.originalSizeBytes / 1024} KB  •  Output: ${res.outputSizeBytes / 1024} KB  •  ${res.pagesProcessed} page(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (res.success && res.primaryOutputPath.isNotEmpty()) {
            val outFile = File(res.primaryOutputPath)
            Text("Output File: ${outFile.name}", style = MaterialTheme.typography.titleSmall)

            // Primary Actions Row: Open, Share, Save As, Rename
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onOpenInReader(res.primaryOutputPath) },
                    modifier = Modifier.weight(1f).testTag("result_open_reader_button")
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open & Preview")
                }
                OutlinedButton(
                    onClick = { onShare(res.primaryOutputPath) },
                    modifier = Modifier.weight(1f).testTag("result_share_button")
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSaveAsSaf(res.primaryOutputPath) },
                    modifier = Modifier.weight(1f).testTag("result_save_as_button")
                ) {
                    Icon(Icons.Outlined.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save As (SAF)")
                }
                OutlinedButton(
                    onClick = onRenameClick,
                    modifier = Modifier.weight(1f).testTag("result_rename_button")
                ) {
                    Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rename")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // "Use in another tool" Chaining Section
            Text("Chain Output Into Another Tool", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    PdfToolId.COMPRESS_PDF,
                    PdfToolId.WATERMARK_PDF,
                    PdfToolId.SIGN_PDF,
                    PdfToolId.PROTECT_PDF,
                    PdfToolId.ADD_PAGE_NUMBERS,
                    PdfToolId.PDF_TO_PDFA
                ).forEach { nextTool ->
                    FilterChip(
                        selected = false,
                        onClick = { onChainTool(nextTool) },
                        label = { Text(nextTool.title) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().testTag("result_done_button")
        ) {
            Text("Back to Workspace")
        }
    }
}
