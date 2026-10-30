package com.example.tools

import android.content.Intent
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.DocumentEntity
import com.example.domain.model.CompressionLevel
import com.example.domain.model.NumberFormatStyle
import com.example.domain.model.PagePosition
import com.example.domain.model.PdfToolId
import com.example.domain.model.SplitMode
import com.example.domain.model.StampPreset
import com.example.domain.model.ToolConfiguration
import com.example.domain.model.ToolExecutionResult
import com.example.ui.ActiveToolWizardState
import com.example.ui.DocumentPageThumbnail
import com.example.ui.FolioViewModel
import com.example.ui.SignaturePadDialog
import java.io.File

@Composable
fun UnifiedToolWizardScreen(
    wizardState: ActiveToolWizardState,
    viewModel: FolioViewModel,
    onClose: () -> Unit,
    onOpenResultInReader: (String) -> Unit
) {
    val libraryDocs by viewModel.libraryDocuments.collectAsStateWithLifecycle()
    val signatures by viewModel.signatures.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val fallbackGetContentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleSafUris(uris, openImmediately = false)
        }
    }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleSafUris(uris, openImmediately = false)
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.importSafFolderTree(treeUri)
        }
    }

    var showStorageBrowserSheet by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var showSignaturePadDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (wizardState.step == 2 && !wizardState.isRunning) {
            viewModel.updateWizardStep(1)
        } else {
            onClose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Clean Studio Wizard Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (wizardState.step == 2 && !wizardState.isRunning) {
                        viewModel.updateWizardStep(1)
                    } else {
                        onClose()
                    }
                },
                modifier = Modifier.testTag("wizard_close_button")
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close tool wizard")
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wizardState.tool.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = wizardState.tool.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Compact 3-Step Indicator Pill Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepBadge("1. Select Files", active = wizardState.step == 1, done = wizardState.step > 1, Modifier.weight(1f))
            StepBadge("2. Configure", active = wizardState.step == 2, done = wizardState.step > 2, Modifier.weight(1f))
            StepBadge("3. Result", active = wizardState.step == 3, done = false, Modifier.weight(1f))
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 6.dp)
        )

        when (wizardState.step) {
            1 -> Step1PickFiles(
                tool = wizardState.tool,
                allDocs = libraryDocs,
                selectedDocs = wizardState.selectedDocs,
                onUpdateSelection = { viewModel.updateWizardSelection(it) },
                onReorder = { from, to -> viewModel.reorderWizardFiles(from, to) },
                onRemoveAt = { idx -> viewModel.removeWizardFileAt(idx) },
                onBrowseSaf = { showStorageBrowserSheet = true },
                onNext = { viewModel.updateWizardStep(2) },
                modifier = Modifier.weight(1f)
            )
            2 -> Step2ConfigureTool(
                wizardState = wizardState,
                savedSignaturesCount = signatures.size,
                onUpdateConfig = { transform -> viewModel.updateWizardConfig(transform) },
                onReorder = { from, to -> viewModel.reorderWizardFiles(from, to) },
                onAddMoreFiles = { showStorageBrowserSheet = true },
                onSaveAsPreset = { showSavePresetDialog = true },
                onOpenSignaturePad = { showSignaturePadDialog = true },
                onExecute = { viewModel.executeCurrentWizardTool() },
                onCancelRunning = { viewModel.cancelActiveToolJob() },
                modifier = Modifier.weight(1f)
            )
            3 -> Step3JobResult(
                tool = wizardState.tool,
                result = wizardState.result,
                onOpenInReader = onOpenResultInReader,
                onChainTool = { outPath, nextTool ->
                    viewModel.chainOutputIntoAnotherTool(outPath, nextTool)
                },
                onRenameOutput = { outPath, newName ->
                    viewModel.renameFileOnDiskAndDb(outPath, newName)
                },
                onShareOutput = { filePath ->
                    runCatching {
                        val file = File(filePath)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (file.name.endsWith(".pdf", true)) "application/pdf" else "*/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Output File"))
                        }
                    }
                },
                onDone = onClose,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showStorageBrowserSheet) {
        com.example.ui.StoragePermissionAndBrowserSheet(
            onDismiss = { showStorageBrowserSheet = false },
            onImportUris = { uris ->
                viewModel.importMultipleSafUris(uris, openImmediately = false)
            },
            onImportFiles = { files ->
                viewModel.importLocalFiles(files, openImmediately = false)
            },
            onLaunchSafFilePicker = {
                runCatching {
                    safLauncher.launch(arrayOf("*/*"))
                }.onFailure {
                    runCatching { fallbackGetContentsLauncher.launch("*/*") }
                }
            },
            onLaunchSafFolderPicker = {
                runCatching {
                    folderLauncher.launch(null)
                }.onFailure {
                    runCatching { safLauncher.launch(arrayOf("*/*")) }
                }
            }
        )
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Tool Preset") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("Preset Name (e.g. Monthly Report Stamp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveCurrentConfigAsPreset(presetNameInput)
                    presetNameInput = ""
                    showSavePresetDialog = false
                }) { Text("Save Preset") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSignaturePadDialog) {
        SignaturePadDialog(
            onDismiss = { showSignaturePadDialog = false },
            onSaveSignature = { label, typedText, pointsJson ->
                viewModel.saveSignature(label, typedText, pointsJson)
                viewModel.updateWizardConfig { it.copy(signatureText = "$typedText (Wet-Ink Drawn)") }
                showSignaturePadDialog = false
            }
        )
    }
}

@Composable
private fun StepBadge(label: String, active: Boolean, done: Boolean, modifier: Modifier = Modifier) {
    val bg = when {
        active -> MaterialTheme.colorScheme.primary
        done -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val fg = when {
        active -> MaterialTheme.colorScheme.onPrimary
        done -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (done) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Step1PickFiles(
    tool: PdfToolId,
    allDocs: List<DocumentEntity>,
    selectedDocs: List<DocumentEntity>,
    onUpdateSelection: (List<DocumentEntity>) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onBrowseSaf: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Prominent Device File Picker Banner (Proper weight(1f) prevents vertical button crushing!)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (tool.acceptsMultipleFiles) {
                            "Select 1 or more files"
                        } else {
                            "Select source document"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Pick PDFs, Images, Office, or Text files from device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onBrowseSaf,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("wizard_pick_saf_button")
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Browse Device",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Selected Queue Card Stack (With Reorder & Remove)
        if (selectedDocs.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (tool.acceptsMultipleFiles) {
                            "Selected Queue (${selectedDocs.size} file${if (selectedDocs.size == 1) "" else "s"} in order)"
                        } else {
                            "Selected Source Document"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tool.acceptsMultipleFiles && selectedDocs.size > 1) {
                        Text(
                            text = "Use arrows to reorder",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                selectedDocs.forEachIndexed { idx, doc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val formatNote = if (doc.formatName.equals("PDF", ignoreCase = true)) {
                                "PDF • ${doc.pageCount.coerceAtLeast(1)}p • ${formatBytes(doc.sizeBytes)}"
                            } else {
                                "${doc.formatName} (Auto-converts to PDF) • ${formatBytes(doc.sizeBytes)}"
                            }
                            Text(
                                text = formatNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (tool.acceptsMultipleFiles && selectedDocs.size > 1) {
                            IconButton(
                                onClick = { onReorder(idx, idx - 1) },
                                enabled = idx > 0,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onReorder(idx, idx + 1) },
                                enabled = idx < selectedDocs.lastIndex,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(
                            onClick = { onRemoveAt(idx) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove file", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Workspace Files List or Empty State
        if (allDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "No Files in Workspace Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap \"Browse Device\" above to pick your PDFs, images, or Office documents from Android storage for ${tool.title}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onBrowseSaf,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Files from Device", maxLines = 1, softWrap = false)
                    }
                }
            }
        } else {
            Text(
                text = "Workspace Documents (${allDocs.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allDocs, key = { it.id }) { doc ->
                    val isSelected = selectedDocs.any { it.id == doc.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                val next = if (tool.acceptsMultipleFiles) {
                                    if (isSelected) selectedDocs.filterNot { it.id == doc.id }
                                    else selectedDocs + doc
                                } else {
                                    listOf(doc)
                                }
                                onUpdateSelection(next)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (tool.acceptsMultipleFiles) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val next = if (checked) selectedDocs + doc else selectedDocs.filterNot { it.id == doc.id }
                                    onUpdateSelection(next)
                                }
                            )
                        } else {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onUpdateSelection(listOf(doc)) }
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        DocumentPageThumbnail(
                            filePath = doc.filePath,
                            formatName = doc.formatName,
                            pageIndex = 0,
                            width = 40.dp,
                            height = 52.dp,
                            isSelected = isSelected
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${doc.formatName} • ${doc.pageCount.coerceAtLeast(1)} pages • ${formatBytes(doc.sizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Bottom Continue Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Button(
                onClick = onNext,
                enabled = selectedDocs.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp)
                    .testTag("wizard_next_step_button")
            ) {
                Text(
                    text = "Continue to Configure (${selectedDocs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step2ConfigureTool(
    wizardState: ActiveToolWizardState,
    savedSignaturesCount: Int,
    onUpdateConfig: ((ToolConfiguration) -> ToolConfiguration) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onAddMoreFiles: () -> Unit,
    onSaveAsPreset: () -> Unit,
    onOpenSignaturePad: () -> Unit,
    onExecute: () -> Unit,
    onCancelRunning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = wizardState.config
    val tool = wizardState.tool
    val firstDoc = wizardState.selectedDocs.firstOrNull()
    val totalInputBytes = wizardState.selectedDocs.sumOf { it.sizeBytes }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Input Summary Banner + Save Preset Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${wizardState.selectedDocs.size} file(s) • ${formatBytes(totalInputBytes)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = wizardState.selectedDocs.joinToString(", ") { it.displayName },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (tool.acceptsMultipleFiles) {
                            OutlinedButton(
                                onClick = onAddMoreFiles,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add File", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                            }
                        }
                        OutlinedButton(
                            onClick = onSaveAsPreset,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Preset", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            // Visual Page Thumbnail Selector for Page-Level Tools
            val showVisualPagePicker = firstDoc != null && tool in setOf(
                PdfToolId.SPLIT_PDF,
                PdfToolId.REMOVE_PAGES,
                PdfToolId.EXTRACT_PAGES,
                PdfToolId.ROTATE_PDF,
                PdfToolId.CROP_PDF
            )
            if (showVisualPagePicker && firstDoc != null) {
                VisualPageThumbnailSelector(
                    doc = firstDoc,
                    pageRangeSpec = cfg.pageRangeSpec,
                    onUpdateRangeSpec = { newRange ->
                        onUpdateConfig { it.copy(pageRangeSpec = newRange) }
                    }
                )
            }

            // Dedicated Visual Page Organizer (for ORGANIZE_PAGES)
            if (tool == PdfToolId.ORGANIZE_PAGES && firstDoc != null) {
                VisualPageOrganizerDeck(
                    doc = firstDoc,
                    customOrder = cfg.customPageOrder.ifEmpty { (0 until firstDoc.pageCount.coerceAtLeast(1)).toList() },
                    onOrderChanged = { newOrder ->
                        onUpdateConfig { it.copy(customPageOrder = newOrder) }
                    }
                )
            }

            // Page Range Input (for applicable tools)
            if (tool in listOf(
                    PdfToolId.MERGE_PDF, PdfToolId.SPLIT_PDF, PdfToolId.REMOVE_PAGES,
                    PdfToolId.EXTRACT_PAGES, PdfToolId.ROTATE_PDF, PdfToolId.PDF_TO_IMAGE,
                    PdfToolId.WATERMARK_PDF, PdfToolId.CROP_PDF
                )
            ) {
                OutlinedTextField(
                    value = cfg.pageRangeSpec,
                    onValueChange = { v -> onUpdateConfig { it.copy(pageRangeSpec = v) } },
                    label = { Text("Page Range (e.g. All, 1-3, 5, 2-6)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Tool-Specific Configuration Workspaces
            when (tool) {
                PdfToolId.MERGE_PDF -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Merge Sequence (${wizardState.selectedDocs.size} files)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            wizardState.selectedDocs.forEachIndexed { idx, doc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = doc.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { onReorder(idx, idx - 1) },
                                        enabled = idx > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onReorder(idx, idx + 1) },
                                        enabled = idx < wizardState.selectedDocs.lastIndex,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            SwitchRow(
                                label = "Generate PDF Outline Bookmarks per file",
                                checked = cfg.addBookmarksPerSource,
                                onCheckedChange = { v -> onUpdateConfig { it.copy(addBookmarksPerSource = v) } }
                            )
                        }
                    }
                }

                PdfToolId.SPLIT_PDF -> {
                    Text("Split Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    SplitMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateConfig { it.copy(splitMode = mode) } }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = cfg.splitMode == mode,
                                onClick = { onUpdateConfig { it.copy(splitMode = mode) } }
                            )
                            Text(mode.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (cfg.splitMode == SplitMode.EVERY_N_PAGES) {
                        Text("Split every ${cfg.splitEveryN} page(s)", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = cfg.splitEveryN.toFloat(),
                            onValueChange = { v -> onUpdateConfig { it.copy(splitEveryN = v.toInt().coerceAtLeast(1)) } },
                            valueRange = 1f..10f
                        )
                    }
                    SwitchRow(
                        label = "Bundle split files into a single .ZIP archive",
                        checked = cfg.outputAsZip,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(outputAsZip = v) } }
                    )
                }

                PdfToolId.ORGANIZE_PAGES -> {
                    Text("Blank Page Insertion", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Insert Blank Page After Page #${cfg.insertBlankAfterPage} (0 = None)", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = cfg.insertBlankAfterPage.toFloat(),
                        onValueChange = { v -> onUpdateConfig { it.copy(insertBlankAfterPage = v.toInt()) } },
                        valueRange = 0f..10f
                    )
                }

                PdfToolId.ROTATE_PDF -> {
                    Text("Rotation Angle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(90, 180, 270).forEach { deg ->
                            FilterChip(
                                selected = cfg.rotateDegrees == deg,
                                onClick = { onUpdateConfig { it.copy(rotateDegrees = deg) } },
                                label = { Text("${deg}° Clockwise", maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }

                PdfToolId.COMPRESS_PDF -> {
                    Text("Compression Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    CompressionLevel.entries.forEach { level ->
                        val estBytes = (totalInputBytes * level.scaleFactor).toLong()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (cfg.compressionLevel == level) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (cfg.compressionLevel == level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onUpdateConfig { it.copy(compressionLevel = level) } }
                                .padding(12.dp)
                        ) {
                            RadioButton(
                                selected = cfg.compressionLevel == level,
                                onClick = { onUpdateConfig { it.copy(compressionLevel = level) } }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(level.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Estimated output: ~${formatBytes(estBytes)} (Quality ${level.jpegQuality}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                PdfToolId.OCR_PDF, PdfToolId.SCAN_TO_PDF -> {
                    OutlinedTextField(
                        value = cfg.ocrLanguage,
                        onValueChange = { v -> onUpdateConfig { it.copy(ocrLanguage = v) } },
                        label = { Text("Offline OCR Script (eng, lat, deu, fra, spa)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SwitchRow(
                        label = "Auto-Deskew & Contrast Enhancement",
                        checked = cfg.ocrDeskew,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(ocrDeskew = v) } }
                    )
                }

                PdfToolId.IMAGE_TO_PDF -> {
                    Text("Page Size & Orientation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A4", "LETTER", "FIT").forEach { size ->
                            FilterChip(
                                selected = cfg.imagePageSize == size,
                                onClick = { onUpdateConfig { it.copy(imagePageSize = size) } },
                                label = { Text(size, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PORTRAIT", "LANDSCAPE").forEach { orient ->
                            FilterChip(
                                selected = cfg.imageOrientation == orient,
                                onClick = { onUpdateConfig { it.copy(imageOrientation = orient) } },
                                label = { Text(orient, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }

                PdfToolId.PDF_TO_IMAGE -> {
                    Text("Image Format & Resolution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("JPG", "PNG").forEach { fmt ->
                            FilterChip(
                                selected = cfg.exportImageFormat == fmt,
                                onClick = { onUpdateConfig { it.copy(exportImageFormat = fmt) } },
                                label = { Text(fmt, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(72, 150, 300).forEach { dpi ->
                            FilterChip(
                                selected = cfg.exportDpi == dpi,
                                onClick = { onUpdateConfig { it.copy(exportDpi = dpi) } },
                                label = { Text("$dpi DPI", maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    SwitchRow(
                        label = "Package all exported page images as .ZIP",
                        checked = cfg.outputAsZip,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(outputAsZip = v) } }
                    )
                }

                PdfToolId.PDF_TO_OFFICE -> {
                    Text("Target Office Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DOCX", "XLSX", "PPTX").forEach { fmt ->
                            FilterChip(
                                selected = cfg.exportOfficeFormat == fmt,
                                onClick = { onUpdateConfig { it.copy(exportOfficeFormat = fmt) } },
                                label = { Text(fmt, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }

                PdfToolId.PDF_TO_TEXT_MARKUP -> {
                    Text("Target Markup Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MD", "TXT", "HTML", "RTF").forEach { fmt ->
                            FilterChip(
                                selected = cfg.exportTextFormat == fmt,
                                onClick = { onUpdateConfig { it.copy(exportTextFormat = fmt) } },
                                label = { Text(fmt, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }

                PdfToolId.ADD_PAGE_NUMBERS -> {
                    Text("Numbering Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    NumberFormatStyle.entries.forEach { style ->
                        FilterChip(
                            selected = cfg.pageNumberFormat == style,
                            onClick = { onUpdateConfig { it.copy(pageNumberFormat = style) } },
                            label = { Text(style.label, maxLines = 1, softWrap = false) }
                        )
                    }
                    Text("Position on Page", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            PagePosition.BOTTOM_CENTER, PagePosition.BOTTOM_RIGHT,
                            PagePosition.BOTTOM_LEFT, PagePosition.TOP_RIGHT, PagePosition.TOP_CENTER
                        ).forEach { pos ->
                            FilterChip(
                                selected = cfg.pageNumberPosition == pos,
                                onClick = { onUpdateConfig { it.copy(pageNumberPosition = pos) } },
                                label = { Text(pos.label, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    SwitchRow(
                        label = "Skip Cover / First Page",
                        checked = cfg.skipFirstPage,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(skipFirstPage = v) } }
                    )
                }

                PdfToolId.WATERMARK_PDF -> {
                    OutlinedTextField(
                        value = cfg.watermarkText,
                        onValueChange = { v -> onUpdateConfig { it.copy(watermarkText = v) } },
                        label = { Text("Watermark Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Opacity: ${(cfg.watermarkOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = cfg.watermarkOpacity,
                        onValueChange = { v -> onUpdateConfig { it.copy(watermarkOpacity = v) } },
                        valueRange = 0.08f..0.85f
                    )
                    SwitchRow(
                        label = "Repeat Tiled Across Page",
                        checked = cfg.watermarkTiled,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(watermarkTiled = v) } }
                    )
                }

                PdfToolId.HEADER_FOOTER -> {
                    OutlinedTextField(
                        value = cfg.headerText,
                        onValueChange = { v -> onUpdateConfig { it.copy(headerText = v) } },
                        label = { Text("Header Text ({filename}, {date})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cfg.footerText,
                        onValueChange = { v -> onUpdateConfig { it.copy(footerText = v) } },
                        label = { Text("Footer Text ({page}, {date})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PdfToolId.CROP_PDF -> {
                    Text("Margin Crop Percentage: ${(cfg.cropMarginPercent * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = cfg.cropMarginPercent,
                        onValueChange = { v -> onUpdateConfig { it.copy(cropMarginPercent = v) } },
                        valueRange = 0.02f..0.25f
                    )
                    SwitchRow(
                        label = "Auto-Detect & Trim Whitespace Margins",
                        checked = cfg.autoCropWhitespace,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(autoCropWhitespace = v) } }
                    )
                }

                PdfToolId.RESIZE_NUP_BOOKLET -> {
                    Text("Pages Per Sheet (N-Up)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 4, 6).forEach { n ->
                            FilterChip(
                                selected = cfg.nUpPagesPerSheet == n,
                                onClick = { onUpdateConfig { it.copy(nUpPagesPerSheet = n) } },
                                label = { Text("$n-Up Grid", maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    SwitchRow(
                        label = "Booklet Signature Imposition",
                        checked = cfg.bookletMode,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(bookletMode = v) } }
                    )
                }

                PdfToolId.GRAYSCALE_BW -> {
                    SwitchRow(
                        label = "High-Contrast B&W Threshold (Instead of Soft Grayscale)",
                        checked = cfg.highContrastBw,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(highContrastBw = v) } }
                    )
                }

                PdfToolId.EDIT_METADATA -> {
                    OutlinedTextField(
                        value = cfg.metaTitle,
                        onValueChange = { v -> onUpdateConfig { it.copy(metaTitle = v) } },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cfg.metaAuthor,
                        onValueChange = { v -> onUpdateConfig { it.copy(metaAuthor = v) } },
                        label = { Text("Author") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cfg.metaSubject,
                        onValueChange = { v -> onUpdateConfig { it.copy(metaSubject = v) } },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cfg.metaKeywords,
                        onValueChange = { v -> onUpdateConfig { it.copy(metaKeywords = v) } },
                        label = { Text("Keywords (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PdfToolId.PROTECT_PDF -> {
                    OutlinedTextField(
                        value = cfg.openPassword,
                        onValueChange = { v -> onUpdateConfig { it.copy(openPassword = v) } },
                        label = { Text("User Open Password (AES-256)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_password_input")
                    )
                    OutlinedTextField(
                        value = cfg.permissionPassword,
                        onValueChange = { v -> onUpdateConfig { it.copy(permissionPassword = v) } },
                        label = { Text("Owner Permission Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SwitchRow("Allow Printing", cfg.allowPrinting) { v -> onUpdateConfig { it.copy(allowPrinting = v) } }
                    SwitchRow("Allow Copying Text", cfg.allowCopying) { v -> onUpdateConfig { it.copy(allowCopying = v) } }
                    SwitchRow("Allow Modifying Document", cfg.allowEditing) { v -> onUpdateConfig { it.copy(allowEditing = v) } }
                }

                PdfToolId.UNLOCK_PDF -> {
                    OutlinedTextField(
                        value = cfg.openPassword,
                        onValueChange = { v -> onUpdateConfig { it.copy(openPassword = v) } },
                        label = { Text("Known Document Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PdfToolId.SIGN_PDF -> {
                    if (firstDoc != null) {
                        Text(
                            text = "Live Signature Preview on Document",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box {
                                DocumentPageThumbnail(
                                    filePath = firstDoc.filePath,
                                    formatName = firstDoc.formatName,
                                    pageIndex = (firstDoc.pageCount - 1).coerceAtLeast(0),
                                    width = 116.dp,
                                    height = 154.dp,
                                    isSelected = true
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = cfg.signatureText.ifBlank { "Signed" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenSignaturePad,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_open_signature_pad_button")
                    ) {
                        Icon(Icons.Outlined.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Draw Freehand Wet-Ink Signature ($savedSignaturesCount saved)",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedTextField(
                        value = cfg.signatureText,
                        onValueChange = { v -> onUpdateConfig { it.copy(signatureText = v) } },
                        label = { Text("Signature Text / Signer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SwitchRow("Include Date & Timestamp", cfg.includeDateStamp) { v ->
                        onUpdateConfig { it.copy(includeDateStamp = v) }
                    }
                    SwitchRow("Embed PKCS#12 Cryptographic Digest", cfg.usePkcs12Certificate) { v ->
                        onUpdateConfig { it.copy(usePkcs12Certificate = v) }
                    }
                }

                PdfToolId.REDACT_PDF -> {
                    OutlinedTextField(
                        value = cfg.redactSearchTerms,
                        onValueChange = { v -> onUpdateConfig { it.copy(redactSearchTerms = v) } },
                        label = { Text("Sensitive Terms or Regex to Purge") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Permanent Redaction: Purges matching text and strips hidden metadata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                PdfToolId.STAMPS -> {
                    Text("Stamp Preset", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StampPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = cfg.stampPreset == preset,
                                onClick = { onUpdateConfig { it.copy(stampPreset = preset, customStampText = preset.label) } },
                                label = { Text(preset.label, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    if (cfg.stampPreset == StampPreset.CUSTOM) {
                        OutlinedTextField(
                            value = cfg.customStampText,
                            onValueChange = { v -> onUpdateConfig { it.copy(customStampText = v) } },
                            label = { Text("Custom Stamp Text") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                PdfToolId.EDIT_PDF_CONTENT -> {
                    OutlinedTextField(
                        value = cfg.editAddedText,
                        onValueChange = { v -> onUpdateConfig { it.copy(editAddedText = v) } },
                        label = { Text("Text Block to Insert onto PDF") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PdfToolId.BATCH_PROCESSOR -> {
                    Text("Select Batch Operation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    val subTools = listOf(
                        PdfToolId.COMPRESS_PDF, PdfToolId.WATERMARK_PDF,
                        PdfToolId.ROTATE_PDF, PdfToolId.GRAYSCALE_BW, PdfToolId.FLATTEN_PDF
                    )
                    subTools.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateConfig { it.copy(batchSubTool = sub) } }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = cfg.batchSubTool == sub,
                                onClick = { onUpdateConfig { it.copy(batchSubTool = sub) } }
                            )
                            Text(sub.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                else -> {
                    Text(
                        "Ready to run ${tool.title}. Originals remain untouched; output will be saved to your workspace.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (wizardState.isRunning) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${wizardState.progressStatus} (${wizardState.progressPercent}%)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    TextButton(onClick = onCancelRunning) {
                        Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
                LinearProgressIndicator(
                    progress = { (wizardState.progressPercent / 100f).coerceIn(0.05f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Button(
                onClick = onExecute,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(50.dp)
                    .testTag("wizard_execute_button")
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Run ${tool.title}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun VisualPageThumbnailSelector(
    doc: DocumentEntity,
    pageRangeSpec: String,
    onUpdateRangeSpec: (String) -> Unit
) {
    val pageCount = doc.pageCount.coerceIn(1, 24)
    val selectedZeroBased = remember(pageRangeSpec, pageCount) {
        parseWizardPageRange(pageRangeSpec, pageCount)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tap Pages to Toggle (${selectedZeroBased.size}/$pageCount)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "All",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onUpdateRangeSpec("All") }
                )
                Text(
                    text = "Odd",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val odds = (1..pageCount step 2).joinToString(",")
                        onUpdateRangeSpec(odds)
                    }
                )
                Text(
                    text = "Even",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val evens = (2..pageCount step 2).joinToString(",")
                        if (evens.isNotEmpty()) onUpdateRangeSpec(evens)
                    }
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items((0 until pageCount).toList()) { pageIdx ->
                val isSelected = pageIdx in selectedZeroBased
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        val mutable = selectedZeroBased.toMutableSet()
                        if (isSelected && mutable.size > 1) {
                            mutable.remove(pageIdx)
                        } else {
                            mutable.add(pageIdx)
                        }
                        val newSpec = if (mutable.size == pageCount) {
                            "All"
                        } else {
                            mutable.sorted().joinToString(",") { (it + 1).toString() }
                        }
                        onUpdateRangeSpec(newSpec)
                    }
                ) {
                    DocumentPageThumbnail(
                        filePath = doc.filePath,
                        formatName = doc.formatName,
                        pageIndex = pageIdx,
                        width = 62.dp,
                        height = 84.dp,
                        isSelected = isSelected,
                        badgeText = "p.${pageIdx + 1}"
                    )
                }
            }
        }
    }
}

@Composable
private fun VisualPageOrganizerDeck(
    doc: DocumentEntity,
    customOrder: List<Int>,
    onOrderChanged: (List<Int>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Visual Page Order (${customOrder.size} pages)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(customOrder) { pos, pageIdx ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(6.dp)
                ) {
                    DocumentPageThumbnail(
                        filePath = doc.filePath,
                        formatName = doc.formatName,
                        pageIndex = pageIdx,
                        width = 64.dp,
                        height = 86.dp,
                        isSelected = true,
                        badgeText = "#${pos + 1} (p.${pageIdx + 1})"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = {
                                if (pos > 0) {
                                    val m = customOrder.toMutableList()
                                    val item = m.removeAt(pos)
                                    m.add(pos - 1, item)
                                    onOrderChanged(m)
                                }
                            },
                            enabled = pos > 0,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Move left", modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = {
                                if (pos < customOrder.lastIndex) {
                                    val m = customOrder.toMutableList()
                                    val item = m.removeAt(pos)
                                    m.add(pos + 1, item)
                                    onOrderChanged(m)
                                }
                            },
                            enabled = pos < customOrder.lastIndex,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Move right", modifier = Modifier.size(14.dp))
                        }
                        if (customOrder.size > 1) {
                            IconButton(
                                onClick = {
                                    val m = customOrder.toMutableList()
                                    m.removeAt(pos)
                                    onOrderChanged(m)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove page", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step3JobResult(
    tool: PdfToolId,
    result: ToolExecutionResult?,
    onOpenInReader: (String) -> Unit,
    onChainTool: (String, PdfToolId) -> Unit,
    onRenameOutput: (String, String) -> Unit,
    onShareOutput: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(result?.primaryOutputPath) {
        mutableStateOf(result?.primaryOutputPath?.let { File(it).name } ?: "")
    }

    val saveAsSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destUri ->
        val srcPath = result?.primaryOutputPath ?: return@rememberLauncherForActivityResult
        if (destUri != null) {
            runCatching {
                val srcFile = File(srcPath)
                if (srcFile.exists()) {
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        srcFile.inputStream().use { input -> input.copyTo(out) }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (result == null) {
            Text("Processing...", style = MaterialTheme.typography.titleMedium)
            return@Column
        }

        Icon(
            imageVector = if (result.success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = if (result.success) Color(0xFF15803D) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(60.dp)
        )

        Text(
            text = if (result.success) "${tool.title} Complete" else "${tool.title} Failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Result Summary Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (result.success && result.primaryOutputPath.isNotEmpty()) {
                    DocumentPageThumbnail(
                        filePath = result.primaryOutputPath,
                        formatName = if (result.primaryOutputPath.endsWith(".pdf", true)) "PDF" else "TXT",
                        pageIndex = 0,
                        width = 56.dp,
                        height = 76.dp,
                        isSelected = true
                    )
                    Spacer(Modifier.width(14.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(result.summaryReport, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        "Before: ${formatBytes(result.originalSizeBytes)}  →  After: ${formatBytes(result.outputSizeBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (result.primaryOutputPath.isNotEmpty()) {
                        Text(
                            "Saved as: ${File(result.primaryOutputPath).name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (result.success && result.primaryOutputPath.isNotEmpty()) {
            Button(
                onClick = { onOpenInReader(result.primaryOutputPath) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("result_open_reader_button")
            ) {
                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open in Universal Reader", maxLines = 1, softWrap = false)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onShareOutput(result.primaryOutputPath) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("result_share_button")
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", maxLines = 1, softWrap = false)
                }

                OutlinedButton(
                    onClick = {
                        val defaultName = File(result.primaryOutputPath).name
                        saveAsSafLauncher.launch(defaultName)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("result_save_as_button")
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save to Device", maxLines = 1, softWrap = false)
                }
            }

            OutlinedButton(
                onClick = { showRenameDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result_rename_button")
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Rename Output File", maxLines = 1, softWrap = false)
            }

            if (result.primaryOutputPath.endsWith(".pdf", ignoreCase = true)) {
                Text(
                    "Chain Output into Next Tool",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        PdfToolId.COMPRESS_PDF,
                        PdfToolId.PROTECT_PDF,
                        PdfToolId.WATERMARK_PDF,
                        PdfToolId.SIGN_PDF,
                        PdfToolId.ADD_PAGE_NUMBERS
                    ).filter { it != tool }.forEach { nextTool ->
                        FilterChip(
                            selected = false,
                            onClick = { onChainTool(result.primaryOutputPath, nextTool) },
                            label = { Text(nextTool.title, maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onDone,
            modifier = Modifier.testTag("result_done_button")
        ) {
            Text("Done & Return to Studio")
        }
    }

    if (showRenameDialog && result != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Output File") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Filename") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameOutput(result.primaryOutputPath, renameText)
                    showRenameDialog = false
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun parseWizardPageRange(spec: String, totalPages: Int): Set<Int> {
    if (totalPages <= 0) return emptySet()
    val clean = spec.trim()
    if (clean.isEmpty() || clean.equals("All", ignoreCase = true) || clean == "*") {
        return (0 until totalPages).toSet()
    }
    val result = linkedSetOf<Int>()
    clean.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
        if (part.contains("-")) {
            val bounds = part.split("-").map { it.trim() }
            val start = (bounds.getOrNull(0)?.toIntOrNull() ?: 1).coerceIn(1, totalPages)
            val end = (bounds.getOrNull(1)?.toIntOrNull() ?: totalPages).coerceIn(1, totalPages)
            val range = if (start <= end) start..end else end..start
            range.forEach { result.add(it - 1) }
        } else {
            part.toIntOrNull()?.let { p ->
                if (p in 1..totalPages) result.add(p - 1)
            }
        }
    }
    return if (result.isEmpty()) (0 until totalPages).toSet() else result
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.toInt()} KB"
    val mb = kb / 1024.0
    return String.format(java.util.Locale.US, "%.1f MB", mb)
}
