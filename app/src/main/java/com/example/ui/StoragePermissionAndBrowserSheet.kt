package com.example.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DeviceBrowserItem(
    val id: String,
    val displayName: String,
    val extension: String,
    val sizeBytes: Long,
    val sourceFolder: String,
    val uri: Uri?,
    val file: File?
)

enum class BrowserCategoryFilter(val label: String) {
    ALL("All Files"),
    PDF("PDFs"),
    IMAGES("Images"),
    DOCS("Office & Text")
}

fun requiredStoragePermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

fun hasStoragePermissionGranted(context: Context): Boolean {
    return requiredStoragePermissions().all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoragePermissionAndBrowserSheet(
    onDismiss: () -> Unit,
    onImportUris: (List<Uri>) -> Unit,
    onImportFiles: (List<File>) -> Unit,
    onLaunchSafFilePicker: () -> Unit,
    onLaunchSafFolderPicker: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var permissionGranted by remember {
        mutableStateOf(hasStoragePermissionGranted(context))
    }
    var isScanning by remember { mutableStateOf(false) }
    var discoveredItems by remember { mutableStateOf<List<DeviceBrowserItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf(BrowserCategoryFilter.ALL) }
    val selectedIds = remember { mutableStateListOf<String>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result.values.any { it } || hasStoragePermissionGranted(context)
        scope.launch {
            isScanning = true
            discoveredItems = scanDeviceStorageFiles(context)
            isScanning = false
        }
    }

    // Automatically request runtime storage permission on open if not yet granted, and scan device files
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            runCatching {
                permissionLauncher.launch(requiredStoragePermissions())
            }
        }
        isScanning = true
        discoveredItems = scanDeviceStorageFiles(context)
        isScanning = false
    }

    val filteredItems = remember(discoveredItems, selectedCategory) {
        discoveredItems.filter { item ->
            when (selectedCategory) {
                BrowserCategoryFilter.ALL -> true
                BrowserCategoryFilter.PDF -> item.extension == "pdf"
                BrowserCategoryFilter.IMAGES -> item.extension in setOf("jpg", "jpeg", "png", "webp")
                BrowserCategoryFilter.DOCS -> item.extension !in setOf("pdf", "jpg", "jpeg", "png", "webp")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add Files from Device",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Browse local storage or use the Android system file picker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("storage_sheet_close_button")) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            // Runtime Storage Permission Banner (always visible if permission is not granted)
            if (!permissionGranted) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Storage & Media Permission",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = "Allow Folio to scan Downloads, Documents & Photos directly on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        Button(
                            onClick = {
                                runCatching {
                                    permissionLauncher.launch(requiredStoragePermissions())
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("grant_storage_permission_button")
                        ) {
                            Text(
                                text = "Allow Access",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // System Picker Quick Actions Row (SAF File Picker + SAF Folder Sync)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onLaunchSafFilePicker()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_system_saf_picker_button")
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "System File Picker",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onLaunchSafFolderPicker()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_system_folder_picker_button")
                ) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Connect Folder",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // In-App Device Storage Browser Filter Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BrowserCategoryFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedCategory == filter,
                            onClick = { selectedCategory = filter },
                            label = { Text(filter.label, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            isScanning = true
                            discoveredItems = scanDeviceStorageFiles(context)
                            isScanning = false
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Rescan device storage")
                }
            }

            // Device File List
            if (isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                        Text(
                            text = "Scanning device Downloads, Documents & Media...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Direct Files Found in Public Folders",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Use \"System File Picker\" above to select PDFs or documents from any folder on your phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isSelected) selectedIds.remove(item.id) else selectedIds.add(item.id)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!selectedIds.contains(item.id)) selectedIds.add(item.id)
                                    } else {
                                        selectedIds.remove(item.id)
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when {
                                    item.extension == "pdf" -> Icons.Outlined.PictureAsPdf
                                    item.extension in setOf("jpg", "jpeg", "png", "webp") -> Icons.Outlined.Image
                                    else -> Icons.Outlined.Description
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.extension.uppercase()} • ${formatBrowserBytes(item.sizeBytes)} • ${item.sourceFolder}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Confirm Import Selected Button
            if (selectedIds.isNotEmpty()) {
                Button(
                    onClick = {
                        val chosen = discoveredItems.filter { selectedIds.contains(it.id) }
                        val uris = chosen.mapNotNull { it.uri }
                        val files = chosen.filter { it.uri == null }.mapNotNull { it.file }
                        onDismiss()
                        if (uris.isNotEmpty()) onImportUris(uris)
                        if (files.isNotEmpty()) onImportFiles(files)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("import_selected_browser_files_button")
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Import Selected (${selectedIds.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

suspend fun scanDeviceStorageFiles(context: Context): List<DeviceBrowserItem> = withContext(Dispatchers.IO) {
    val results = LinkedHashMap<String, DeviceBrowserItem>()
    val supportedExtensions = setOf(
        "pdf", "epub", "docx", "doc", "xlsx", "xls", "pptx", "ppt",
        "odt", "txt", "md", "csv", "html", "htm", "rtf", "cbz", "jpg", "jpeg", "png", "webp"
    )

    // 1. Scan standard public directories (Download, Documents, Pictures, DCIM) when readable
    val publicDirs = listOf(
        Environment.DIRECTORY_DOWNLOADS to "Downloads",
        Environment.DIRECTORY_DOCUMENTS to "Documents",
        Environment.DIRECTORY_PICTURES to "Pictures",
        Environment.DIRECTORY_DCIM to "Camera"
    )
    publicDirs.forEach { (dirType, label) ->
        runCatching {
            val root = Environment.getExternalStoragePublicDirectory(dirType)
            if (root != null && root.exists() && root.canRead()) {
                root.walkTopDown().maxDepth(3).take(120).forEach { file ->
                    if (file.isFile && file.length() > 0L) {
                        val ext = file.extension.lowercase()
                        if (ext in supportedExtensions) {
                            val key = file.absolutePath
                            results[key] = DeviceBrowserItem(
                                id = key,
                                displayName = file.name,
                                extension = ext,
                                sizeBytes = file.length(),
                                sourceFolder = label,
                                uri = null,
                                file = file
                            )
                        }
                    }
                }
            }
        }
    }

    // 2. Query MediaStore.Files for indexed documents & images
    runCatching {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")
        context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            var count = 0
            while (cursor.moveToNext() && count < 80) {
                val id = if (idCol >= 0) cursor.getLong(idCol) else continue
                val name = (if (nameCol >= 0) cursor.getString(nameCol) else null) ?: continue
                val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                val mime = (if (mimeCol >= 0) cursor.getString(mimeCol) else null) ?: ""
                if (size <= 0L) continue

                val ext = name.substringAfterLast('.', "").lowercase().ifBlank {
                    when {
                        mime.contains("pdf") -> "pdf"
                        mime.startsWith("image/jpeg") -> "jpg"
                        mime.startsWith("image/png") -> "png"
                        mime.startsWith("image/webp") -> "webp"
                        else -> ""
                    }
                }
                if (ext in supportedExtensions) {
                    val contentUri = ContentUris.withAppendedId(queryUri, id)
                    val key = contentUri.toString()
                    if (!results.containsKey(key) && results.values.none { it.displayName == name && it.sizeBytes == size }) {
                        results[key] = DeviceBrowserItem(
                            id = key,
                            displayName = if (name.contains('.')) name else "$name.$ext",
                            extension = ext,
                            sizeBytes = size,
                            sourceFolder = "Device MediaStore",
                            uri = contentUri,
                            file = null
                        )
                        count++
                    }
                }
            }
        }
    }

    // 3. Query MediaStore.Images.Media specifically on Android 13+ where READ_MEDIA_IMAGES grants photo access
    runCatching {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        val imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        context.contentResolver.query(imagesUri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
            var count = 0
            while (cursor.moveToNext() && count < 40) {
                val id = if (idCol >= 0) cursor.getLong(idCol) else continue
                val name = (if (nameCol >= 0) cursor.getString(nameCol) else null) ?: "Photo_$id.jpg"
                val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                if (size <= 0L) continue
                val ext = name.substringAfterLast('.', "jpg").lowercase()
                val contentUri = ContentUris.withAppendedId(imagesUri, id)
                val key = contentUri.toString()
                if (!results.containsKey(key) && results.values.none { it.displayName == name && it.sizeBytes == size }) {
                    results[key] = DeviceBrowserItem(
                        id = key,
                        displayName = name,
                        extension = ext,
                        sizeBytes = size,
                        sourceFolder = "Device Photos",
                        uri = contentUri,
                        file = null
                    )
                    count++
                }
            }
        }
    }

    results.values.toList()
}

private fun formatBrowserBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    return if (kb < 1024.0) {
        String.format(java.util.Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
    } else {
        String.format(java.util.Locale.US, "%.1f MB", kb / 1024.0)
    }
}
