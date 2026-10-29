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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
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
import androidx.compose.material.icons.outlined.UploadFile
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
import androidx.compose.ui.draw.clip
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
    VAULT("Vault"),
    TRASH("Trash")
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
    connectedFolderName: String = "",
    onOpenDocument: (DocumentEntity) -> Unit,
    onPickSafFile: () -> Unit,
    onConnectFolder: () -> Unit = {},
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().testTag("files_screen_list")
    ) {
        // 1. Clean Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Files & Vault",
                        style = MaterialTheme.typography.displayMedium,
                        maxLines = 1
                    )
                    Text(
                        text = if (connectedFolderName.isNotBlank()) "Synced folder: $connectedFolderName"
                        else "Device files, folders & AES-256 Vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onPickSafFile,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("files_saf_import_button")
                ) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import", maxLines = 1, softWrap = false)
                }
            }
        }

        // 2. Clean 3-Segment Section Switcher (Library | Vault | Trash)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilesSectionTab.entries.forEach { tab ->
                        val selected = sectionTab == tab
                        val count = when (tab) {
                            FilesSectionTab.LIBRARY -> libraryDocs.size
                            FilesSectionTab.VAULT -> vaultDocs.size
                            FilesSectionTab.TRASH -> trashedDocs.size
                        }
                        Surface(
                            onClick = { sectionTab = tab },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            tonalElevation = if (selected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("files_tab_${tab.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "${tab.label} ($count)",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
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
                // 3. Horizontally Scrollable Filter & Sort Toolbar (Prevents any chip from squeezing vertically!)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Folder Dropdown Pill
                            item {
                                Box {
                                    FilterChip(
                                        selected = selectedFolderId != null,
                                        onClick = { folderMenuExpanded = true },
                                        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(activeFolderName, maxLines = 1, softWrap = false) }
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
                            }

                            // Tag Dropdown Pill
                            item {
                                Box {
                                    FilterChip(
                                        selected = selectedTag != null,
                                        onClick = { tagMenuExpanded = true },
                                        leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(activeTagName, maxLines = 1, softWrap = false) }
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
                            }

                            // Sort Dropdown Pill
                            item {
                                Box {
                                    FilterChip(
                                        selected = false,
                                        onClick = { sortMenuExpanded = true },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(sortOption.label, maxLines = 1, softWrap = false) }
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

                            // Sync Device Folder Pill
                            item {
                                FilterChip(
                                    selected = connectedFolderName.isNotBlank(),
                                    onClick = onConnectFolder,
                                    leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = {
                                        Text(
                                            if (connectedFolderName.isNotBlank()) "Sync: $connectedFolderName" else "Connect Folder",
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier.testTag("files_grid_toggle_button")
                        ) {
                            Icon(
                                if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = "Toggle Grid or List view"
                            )
                        }
                    }
                }

                if (filteredLibrary.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text("No Files in This View", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Import files from your device or grant permission to sync a device folder.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onPickSafFile,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Pick Files", maxLines = 1, softWrap = false)
                                    }
                                    OutlinedButton(
                                        onClick = onConnectFolder,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Sync Folder", maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                } else if (isGridView) {
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
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
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
                                            width = 84.dp,
                                            height = 114.dp
                                        )
                                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(doc.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${doc.formatName} • ${doc.pageCount} p. • ${(doc.sizeBytes / 1024).coerceAtLeast(1)} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
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
                            Text("Unlocked Vault (${vaultDocs.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = { isVaultUnlocked = false }) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Lock Vault", maxLines = 1, softWrap = false)
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
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(vDoc.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("AES-256-GCM Encrypted • ${(vDoc.sizeBytes / 1024).coerceAtLeast(1)} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    TextButton(onClick = { onRestoreFromVault(vDoc) }) {
                                        Text("Restore", maxLines = 1, softWrap = false)
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
                                Text("Empty Trash", maxLines = 1, softWrap = false)
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
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(tDoc.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${(tDoc.sizeBytes / 1024).coerceAtLeast(1)} KB", style = MaterialTheme.typography.bodySmall)
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
            title = { Text("Create Tag") },
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
            title = { Text("Edit Tags") },
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

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentPageThumbnail(
                filePath = doc.filePath,
                formatName = doc.formatName,
                pageIndex = 0,
                width = 42.dp,
                height = 56.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(doc.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${doc.formatName} • ${doc.pageCount} p. • ${(doc.sizeBytes / 1024).coerceAtLeast(1)} KB" +
                        if (doc.tagsCsv.isNotBlank()) " • #${doc.tagsCsv.replace(",", " #")}" else "",
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().testTag("settings_screen_list")
    ) {
        item {
            Column {
                Text("Settings", style = MaterialTheme.typography.displayMedium)
                Text(
                    "Theme, output rules, security vault & storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 1. Appearance & Typography
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.headlineMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Studio Light", "DARK" to "Slate Dark").forEach { (code, label) ->
                            FilterChip(
                                selected = settings.themeMode == code,
                                onClick = { onUpdateSettings { it.copy(themeMode = code) } },
                                label = { Text(label, maxLines = 1, softWrap = false) },
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
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Material You Dynamic Color", style = MaterialTheme.typography.titleSmall)
                            Text("Use system wallpaper palette", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Reduce Motion", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
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
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Output Naming & Reader", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = settings.outputNamingPattern,
                        onValueChange = { p -> onUpdateSettings { it.copy(outputNamingPattern = p) } },
                        label = { Text("Auto-Naming Template ({name}_{tool})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Keep Screen On While Reading", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
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
                        Text("Volume-Key Page Turning", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
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
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Security, Vault & Signatures", style = MaterialTheme.typography.headlineMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Biometric / PIN App Lock", style = MaterialTheme.typography.titleSmall)
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
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Vault & App Lock PIN", style = MaterialTheme.typography.titleSmall)
                            Text("Masked (${"•".repeat(settings.appLockPin.length.coerceAtLeast(4))})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = {
                            newPinInput = settings.appLockPin
                            showChangePinDialog = true
                        }) {
                            Text("Change PIN", maxLines = 1, softWrap = false)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Hide Content in Recents", style = MaterialTheme.typography.titleSmall)
                            Text("Masks preview in task switcher", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Saved Signatures", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Button(onClick = { showSignaturePad = true }) {
                            Icon(Icons.Outlined.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Signature", maxLines = 1, softWrap = false)
                        }
                    }
                    signatures.forEach { sig ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${sig.label}: ${sig.typedText}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Storage & Cache Manager", style = MaterialTheme.typography.headlineMedium)
                    SegmentedStorageBar(breakdown = storageBreakdown)
                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.fillMaxWidth().testTag("settings_clear_cache_button")
                    ) {
                        Text("Clear Bitmap Cache & Temporary Scratch", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // 5. About & 100% Offline Runtime Attestation
        item {
            val context = LocalContext.current
            var liveAuditReport by remember { mutableStateOf<OfflineReleaseAuditReport?>(null) }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = "Folio v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                                Text(
                                    text = com.example.BuildConfig.APPLICATION_ID,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "100% OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        "All PDF tools, CameraX scanning, and storage run 100% locally on your device with zero network permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val report = liveAuditReport
                    if (report != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (report.isStrictlyOffline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (report.isStrictlyOffline) "Runtime APK Audit Passed: 0 Network Permissions"
                                    else "Warning: Network Permission Detected",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (report.isStrictlyOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "INTERNET: ${if (report.hasInternetPermission) "Present" else "Stripped"} • ACCESS_NETWORK_STATE: ${if (report.hasNetworkStatePermission) "Present" else "Stripped"}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { liveAuditReport = inspectRuntimeReleaseIntegrity(context) },
                        modifier = Modifier.fillMaxWidth().testTag("settings_verify_offline_button")
                    ) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (liveAuditReport == null) "Verify Offline APK Integrity" else "Re-Run Offline Audit", maxLines = 1)
                    }
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
