package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.VaultCryptoManager
import com.example.data.local.DocumentEntity
import com.example.data.local.FolderEntity
import com.example.data.local.SignatureEntity
import com.example.data.local.TagEntity
import com.example.data.local.UserSettingsState
import com.example.reader.MultiFormatDocumentEngine
import java.io.File

enum class FilesSectionTab(val label: String) {
    LIBRARY("Library"),
    VAULT("Private Vault"),
    TRASH("Trash (30d)")
}

enum class SortByOption(val label: String) {
    DATE("Recent"),
    NAME("Name"),
    SIZE("Size"),
    TYPE("Format")
}

@Composable
fun FilesScreen(
    libraryDocs: List<DocumentEntity>,
    vaultDocs: List<DocumentEntity>,
    trashedDocs: List<DocumentEntity>,
    folders: List<FolderEntity>,
    tags: List<TagEntity>,
    appLockPin: String,
    onOpenDocument: (DocumentEntity) -> Unit,
    onPickSafFile: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateTag: (String) -> Unit,
    onRenameDoc: (DocumentEntity, String) -> Unit,
    onDuplicateDoc: (DocumentEntity) -> Unit,
    onAssignFolderAndTags: (DocumentEntity, Long?, String) -> Unit,
    onMoveToVault: (DocumentEntity) -> Unit,
    onRestoreFromVault: (DocumentEntity) -> Unit,
    onMoveToTrash: (DocumentEntity) -> Unit,
    onRestoreFromTrash: (DocumentEntity) -> Unit,
    onDeletePermanently: (DocumentEntity) -> Unit,
    onEmptyTrash: () -> Unit,
    onRequestBiometricUnlock: ((Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var sectionTab by remember { mutableStateOf(FilesSectionTab.LIBRARY) }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortByOption.DATE) }
    var isGridView by remember { mutableStateOf(false) }

    // Consolidated Toolbar Dropdown states
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Vault lock state
    var isVaultUnlocked by remember { mutableStateOf(false) }

    // Dialog states
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    var docToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var docToTag by remember { mutableStateOf<DocumentEntity?>(null) }
    var tagInputText by remember { mutableStateOf("") }

    val filteredLibrary = remember(libraryDocs, selectedFolderId, selectedTag, sortOption) {
        libraryDocs
            .filter { doc -> selectedFolderId == null || doc.folderId == selectedFolderId }
            .filter { doc -> selectedTag == null || doc.tagsCsv.split(",").any { it.equals(selectedTag, ignoreCase = true) } }
            .let { list ->
                when (sortOption) {
                    SortByOption.NAME -> list.sortedBy { it.displayName.lowercase() }
                    SortByOption.DATE -> list.sortedByDescending { it.lastOpenedAt }
                    SortByOption.SIZE -> list.sortedByDescending { it.sizeBytes }
                    SortByOption.TYPE -> list.sortedBy { it.formatName }
                }
            }
    }

    val activeFolderName = folders.firstOrNull { it.id == selectedFolderId }?.name ?: "All Folders"
    val activeTagName = selectedTag?.let { "#$it" } ?: "All Tags"

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().testTag("files_screen_list")
    ) {
        // Editorial Masthead
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Archive", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "Scoped Storage SAF • Colour-free tags • AES-256 Vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onPickSafFile,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("files_saf_import_button")
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import SAF")
                }
            }
        }

        // Sleek 3-Segment Section Switcher (Library | Private Vault | Trash)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilesSectionTab.entries.forEach { tab ->
                        val selected = sectionTab == tab
                        Surface(
                            onClick = { sectionTab = tab },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            tonalElevation = if (selected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("files_tab_${tab.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        when (sectionTab) {
            FilesSectionTab.LIBRARY -> {
                // Single Consolidated Filter & Sort Toolbar (replaces the 4 stacked chip rows)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            // Folder Dropdown Pill
                            Box {
                                FilterChip(
                                    selected = selectedFolderId != null,
                                    onClick = { folderMenuExpanded = true },
                                    leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                    label = { Text(activeFolderName, maxLines = 1) }
                                )
                                DropdownMenu(expanded = folderMenuExpanded, onDismissRequest = { folderMenuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("All Folders") },
                                        onClick = { selectedFolderId = null; folderMenuExpanded = false }
                                    )
                                    folders.forEach { folder ->
                                        DropdownMenuItem(
                                            text = { Text(folder.name) },
                                            onClick = { selectedFolderId = folder.id; folderMenuExpanded = false }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("+ New Folder") },
                                        onClick = { folderMenuExpanded = false; showNewFolderDialog = true }
                                    )
                                }
                            }

                            // Tag Dropdown Pill
                            Box {
                                FilterChip(
                                    selected = selectedTag != null,
                                    onClick = { tagMenuExpanded = true },
                                    leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                    label = { Text(activeTagName, maxLines = 1) }
                                )
                                DropdownMenu(expanded = tagMenuExpanded, onDismissRequest = { tagMenuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("All Tags") },
                                        onClick = { selectedTag = null; tagMenuExpanded = false }
                                    )
                                    tags.forEach { tag ->
                                        DropdownMenuItem(
                                            text = { Text("#${tag.name}") },
                                            onClick = { selectedTag = tag.name; tagMenuExpanded = false }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("+ New Tag") },
                                        onClick = { tagMenuExpanded = false; showNewTagDialog = true }
                                    )
                                }
                            }

                            // Sort Dropdown Pill
                            Box {
                                FilterChip(
                                    selected = false,
                                    onClick = { sortMenuExpanded = true },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                    label = { Text(sortOption.label) }
                                )
                                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                                    SortByOption.entries.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text("Sort by ${opt.label}") },
                                            onClick = { sortOption = opt; sortMenuExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = { isGridView = !isGridView }, modifier = Modifier.testTag("files_grid_toggle_button")) {
                            Icon(
                                if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = "Toggle Grid or List view"
                            )
                        }
                    }
                }

                if (isGridView) {
                    val gridRows = filteredLibrary.chunked(2)
                    items(gridRows.size) { rIdx ->
                        val pair = gridRows[rIdx]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { doc ->
                                Surface(
                                    onClick = { onOpenDocument(doc) },
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DocumentPageThumbnail(
                                            filePath = doc.filePath,
                                            formatName = doc.formatName,
                                            pageIndex = 0,
                                            width = 88.dp,
                                            height = 120.dp
                                        )
                                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(doc.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${doc.pageCount} p. • ${doc.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(filteredLibrary, key = { it.id }) { doc ->
                        LibraryDocumentItemWithMenu(
                            doc = doc,
                            onOpen = { onOpenDocument(doc) },
                            onRename = {
                                docToRename = doc
                                renameText = doc.displayName.substringBeforeLast('.')
                            },
                            onDuplicate = { onDuplicateDoc(doc) },
                            onEditTags = {
                                docToTag = doc
                                tagInputText = doc.tagsCsv
                            },
                            onMoveToVault = { onMoveToVault(doc) },
                            onShare = { MultiFormatDocumentEngine.shareDocumentOffline(context, File(doc.filePath)) },
                            onTrash = { onMoveToTrash(doc) }
                        )
                    }
                }
            }

            FilesSectionTab.VAULT -> {
                if (!isVaultUnlocked) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                        ) {
                            TactilePinPad(
                                title = "Encrypted Private Vault",
                                subtitle = "AES-256-GCM encryption at rest. Enter PIN or use Biometrics.",
                                expectedPin = appLockPin,
                                onUnlocked = { isVaultUnlocked = true },
                                onBiometricClick = {
                                    onRequestBiometricUnlock { success ->
                                        if (success) isVaultUnlocked = true
                                    }
                                }
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Unlocked Vault (${vaultDocs.size} files)", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = { isVaultUnlocked = false }) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Lock Vault")
                            }
                        }
                    }
                    if (vaultDocs.isEmpty()) {
                        item {
                            Text(
                                "No files in Private Vault yet. Open the menu (⋮) on any Library file and select 'Encrypt to Private Vault'.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(vaultDocs, key = { it.id }) { vDoc ->
                            Surface(
                                onClick = { onOpenDocument(vDoc) },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(vDoc.displayName, style = MaterialTheme.typography.titleSmall)
                                        Text("AES-256-GCM Encrypted • ${vDoc.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    TextButton(onClick = { onRestoreFromVault(vDoc) }) {
                                        Text("Restore to Library")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FilesSectionTab.TRASH -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Items in Trash auto-purge after 30 days.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (trashedDocs.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onEmptyTrash,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Empty Trash")
                            }
                        }
                    }
                }
                if (trashedDocs.isEmpty()) {
                    item {
                        Text("Trash is empty.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(trashedDocs, key = { it.id }) { tDoc ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(tDoc.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text("${tDoc.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onRestoreFromTrash(tDoc) }) {
                                    Icon(Icons.Outlined.RestoreFromTrash, contentDescription = "Restore")
                                }
                                IconButton(onClick = { onDeletePermanently(tDoc) }) {
                                    Icon(Icons.Outlined.DeleteForever, contentDescription = "Delete permanently", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) onCreateFolder(newFolderName.trim())
                    newFolderName = ""
                    showNewFolderDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") } }
        )
    }

    if (showNewTagDialog) {
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            title = { Text("Create Colour-Free Tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Tag Name (e.g. confidential)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newTagName.isNotBlank()) onCreateTag(newTagName.trim().lowercase())
                    newTagName = ""
                    showNewTagDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewTagDialog = false }) { Text("Cancel") } }
        )
    }

    docToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { docToRename = null },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameDoc(target, renameText)
                    docToRename = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { docToRename = null }) { Text("Cancel") } }
        )
    }

    docToTag?.let { target ->
        AlertDialog(
            onDismissRequest = { docToTag = null },
            title = { Text("Edit Colour-Free Tags") },
            text = {
                OutlinedTextField(
                    value = tagInputText,
                    onValueChange = { tagInputText = it },
                    label = { Text("Comma-separated tags (e.g. review,contract)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onAssignFolderAndTags(target, target.folderId, tagInputText)
                    docToTag = null
                }) { Text("Save Tags") }
            },
            dismissButton = { TextButton(onClick = { docToTag = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LibraryDocumentItemWithMenu(
    doc: DocumentEntity,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onEditTags: () -> Unit,
    onMoveToVault: () -> Unit,
    onShare: () -> Unit,
    onTrash: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onOpen,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DocumentPageThumbnail(
                    filePath = doc.filePath,
                    formatName = doc.formatName,
                    pageIndex = 0,
                    width = 44.dp,
                    height = 58.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(doc.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${doc.formatName}  •  ${doc.pageCount} p.  •  ${(doc.sizeBytes / 1024).coerceAtLeast(1)} KB" +
                            if (doc.tagsCsv.isNotBlank()) "  •  #${doc.tagsCsv.replace(",", " #")}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Document actions")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null) },
                            onClick = { menuExpanded = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Copy") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = { menuExpanded = false; onDuplicate() }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Tags") },
                            leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                            onClick = { menuExpanded = false; onEditTags() }
                        )
                        DropdownMenuItem(
                            text = { Text("Encrypt to Private Vault") },
                            leadingIcon = { Icon(Icons.Outlined.EnhancedEncryption, contentDescription = null) },
                            onClick = { menuExpanded = false; onMoveToVault() }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Offline") },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = { menuExpanded = false; onShare() }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onTrash() }
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun SettingsScreen(
    settings: UserSettingsState,
    storageBreakdown: VaultCryptoManager.StorageBreakdown,
    signatures: List<SignatureEntity>,
    onUpdateSettings: ((UserSettingsState) -> UserSettingsState) -> Unit,
    onAddSignature: (String) -> Unit,
    onDeleteSignature: (Long) -> Unit,
    onClearCache: () -> Unit
) {
    var showSignaturePad by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf(settings.appLockPin) }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize().testTag("settings_screen_list")
    ) {
        item {
            Column {
                Text("Settings", style = MaterialTheme.typography.displayMedium)
                Text(
                    "Editorial appearance, output rules, biometric vault & storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Appearance & Typography
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Appearance & Palette", style = MaterialTheme.typography.headlineMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Alabaster Light", "DARK" to "Obsidian Dark").forEach { (code, label) ->
                            FilterChip(
                                selected = settings.themeMode == code,
                                onClick = { onUpdateSettings { it.copy(themeMode = code) } },
                                label = { Text(label) },
                                modifier = Modifier.testTag("theme_chip_${code.lowercase()}")
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Material You Dynamic Colour", style = MaterialTheme.typography.titleSmall)
                            Text("Use wallpaper accent instead of Muted Royal Indigo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = { d -> onUpdateSettings { it.copy(dynamicColor = d) } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reduce Motion (Accessibility)", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = settings.reduceMotion,
                            onCheckedChange = { r -> onUpdateSettings { it.copy(reduceMotion = r) } }
                        )
                    }
                }
            }
        }

        // 2. Output Naming & Reader Defaults
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Output Naming & Reader", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = settings.outputNamingPattern,
                        onValueChange = { p -> onUpdateSettings { it.copy(outputNamingPattern = p) } },
                        label = { Text("Auto-Naming Template ({name}_{tool} or {name}_{date})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Keep Screen On While Reading", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = settings.keepScreenOnInReader,
                            onCheckedChange = { k -> onUpdateSettings { it.copy(keepScreenOnInReader = k) } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume-Key Page Turning", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = settings.volumeKeyPaging,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(volumeKeyPaging = v) } }
                        )
                    }
                }
            }
        }

        // 3. Security, App Lock & Freehand Signatures
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Security, Vault & Signatures", style = MaterialTheme.typography.headlineMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Biometric / PIN App Lock on Startup", style = MaterialTheme.typography.titleSmall)
                            Text("Auto-lock timeout: ${settings.autoLockTimeoutMinutes} minutes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.appLockEnabled,
                            onCheckedChange = { e -> onUpdateSettings { it.copy(appLockEnabled = e) } },
                            modifier = Modifier.testTag("settings_app_lock_switch")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Vault & App Lock PIN", style = MaterialTheme.typography.titleSmall)
                            Text("Masked (${"•".repeat(settings.appLockPin.length.coerceAtLeast(4))})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = {
                            newPinInput = settings.appLockPin
                            showChangePinDialog = true
                        }) {
                            Text("Change PIN")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Hide Content in Recents (FLAG_SECURE)", style = MaterialTheme.typography.titleSmall)
                            Text("Masks app preview in system task switcher", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.hideInRecents,
                            onCheckedChange = { h -> onUpdateSettings { it.copy(hideInRecents = h) } }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saved Signatures", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { showSignaturePad = true }) {
                            Icon(Icons.Outlined.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Draw Signature")
                        }
                    }
                    signatures.forEach { sig ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${sig.label}: ${sig.typedText}", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onDeleteSignature(sig.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete signature")
                            }
                        }
                    }
                }
            }
        }

        // 4. Segmented Storage & Cache Manager
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Storage & Cache Manager", style = MaterialTheme.typography.headlineMedium)
                    SegmentedStorageBar(breakdown = storageBreakdown)
                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.fillMaxWidth().testTag("settings_clear_cache_button")
                    ) {
                        Text("Clear Bitmap Cache & Temporary Scratch")
                    }
                }
            }
        }

        // 5. About & 100% Offline Attestation
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Folio v1.0 — 100% Offline Guarantee", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Permissions Audit: INTERNET and ACCESS_NETWORK_STATE are strictly omitted. Playfair Display & Inter typography, AndroidX PdfRenderer, PdfBox-Android (Apache 2.0), CameraX, and Room run locally on-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onSaveSignature = { _, typedText, _ ->
                onAddSignature(typedText)
            }
        )
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Update App Lock & Vault PIN") },
            text = {
                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) newPinInput = it },
                    label = { Text("4–8 Digit Numeric PIN") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newPinInput.length >= 4) {
                        onUpdateSettings { it.copy(appLockPin = newPinInput) }
                    }
                    showChangePinDialog = false
                }) { Text("Save PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
            }
        )
    }
}
