package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BatchJobEntity
import com.example.data.local.DocumentEntity
import com.example.data.local.ToolPresetEntity
import com.example.domain.model.PdfToolId
import com.example.domain.model.ToolCategory
import kotlinx.coroutines.launch

@Composable
fun OnboardingOverlay(
    onComplete: () -> Unit
) {
    val pages = listOf(
        Triple(
            "Universal Document Reader",
            "Open PDFs, Word, Excel, PowerPoint, EPUB, Markdown, CSV, and Images with crisp zoom and offline Read Aloud.",
            "100% On-Device"
        ),
        Triple(
            "Visual PDF Studio",
            "Scan with CameraX, merge multiple files, split pages visually, compress, and sign documents with zero cloud uploads.",
            "Non-Destructive"
        ),
        Triple(
            "Direct Device File Access",
            "Grant access to your device files or folders via Android Storage Access Framework and encrypted Private Vault.",
            "Private & Secure"
        )
    )
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Folio Studio", style = MaterialTheme.typography.headlineMedium)
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip")
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIdx ->
                val (title, body, badge) = pages[pageIdx]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OnboardingEditorialIllustration(slideIndex = pageIdx)
                    Spacer(Modifier.height(28.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == i) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == i) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Text(if (pagerState.currentPage < pages.lastIndex) "Continue" else "Get Started")
                }
            }
        }
    }
}

@Composable
private fun OnboardingEditorialIllustration(slideIndex: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val ink = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        when (slideIndex) {
            0 -> {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx - 110.dp.toPx(), cy - 60.dp.toPx()),
                    size = Size(104.dp.toPx(), 120.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx + 6.dp.toPx(), cy - 60.dp.toPx()),
                    size = Size(104.dp.toPx(), 120.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                for (i in 0..4) {
                    val y = cy - 38.dp.toPx() + i * 18.dp.toPx()
                    drawLine(
                        color = if (i == 0) primary else ink.copy(alpha = 0.45f),
                        start = Offset(cx - 94.dp.toPx(), y),
                        end = Offset(cx - 22.dp.toPx(), y),
                        strokeWidth = if (i == 0) 4.dp.toPx() else 2.dp.toPx()
                    )
                    drawLine(
                        color = ink.copy(alpha = 0.45f),
                        start = Offset(cx + 22.dp.toPx(), y),
                        end = Offset(cx + 94.dp.toPx(), y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            1 -> {
                for (col in 0..2) {
                    val x = cx - 125.dp.toPx() + col * 88.dp.toPx()
                    drawRoundRect(
                        color = if (col == 1) primary else Color.White,
                        topLeft = Offset(x, cy - 46.dp.toPx()),
                        size = Size(74.dp.toPx(), 92.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                }
            }
            else -> {
                drawCircle(
                    color = primary.copy(alpha = 0.15f),
                    radius = 60.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = primary,
                    radius = 40.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = primary,
                    radius = 12.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    documents: List<DocumentEntity>,
    favourites: List<DocumentEntity>,
    searchHistory: List<String>,
    connectedFolderName: String = "",
    onOpenDocument: (DocumentEntity) -> Unit,
    onToggleFavourite: (DocumentEntity) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
    onQuickToolClick: (PdfToolId) -> Unit,
    onPickSafFile: () -> Unit,
    onConnectFolder: () -> Unit = {},
    onRecordSearch: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var showOnlyFavourites by remember { mutableStateOf(false) }

    val baseList = if (showOnlyFavourites) favourites else documents
    val filteredDocs = remember(baseList, searchQuery) {
        if (searchQuery.isBlank()) baseList
        else baseList.filter {
            it.displayName.contains(searchQuery.trim(), ignoreCase = true) ||
                it.tagsCsv.contains(searchQuery.trim(), ignoreCase = true) ||
                it.indexedContent.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    val lastOpenedDoc = documents.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().testTag("home_screen_list")
    ) {
        // 1. Clean Top Header (Weight(1f) on Title Column so Button NEVER wraps or gets squeezed)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Folio",
                        style = MaterialTheme.typography.displayMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (connectedFolderName.isNotBlank()) "Folder: $connectedFolderName"
                        else "PDF Studio & Document Workspace",
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
                    modifier = Modifier.testTag("home_open_saf_button")
                ) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import Files", maxLines = 1, softWrap = false)
                }
            }
        }

        // 2. Search Bar (Only shows history when focused)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onRecordSearch(it)
                    },
                    placeholder = {
                        Text(
                            "Search documents or text...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { searchFocused = it.isFocused }
                        .testTag("home_search_input")
                )

                AnimatedVisibility(visible = (searchFocused || searchQuery.isNotEmpty()) && searchHistory.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchHistory) { term ->
                            Surface(
                                onClick = { searchQuery = term },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(term, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Studio Grid (4 Balanced Cards: Scan, Merge, Split, Sign — Never wraps mid-word)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick Studio",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val quickActions = listOf(
                        Triple(PdfToolId.SCAN_TO_PDF, "Scan", Icons.Outlined.DocumentScanner),
                        Triple(PdfToolId.MERGE_PDF, "Merge", Icons.AutoMirrored.Outlined.MergeType),
                        Triple(PdfToolId.SPLIT_PDF, "Split", Icons.AutoMirrored.Outlined.CallSplit),
                        Triple(PdfToolId.SIGN_PDF, "Sign", Icons.Outlined.Draw)
                    )
                    quickActions.forEach { (toolId, label, icon) ->
                        Surface(
                            onClick = { onQuickToolClick(toolId) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .testTag("quick_action_${label.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Continue Reading Card (Only when user has imported real documents)
        if (lastOpenedDoc != null && searchQuery.isBlank()) {
            item {
                val progressFraction = ((lastOpenedDoc.lastPage + 1).toFloat() / lastOpenedDoc.pageCount.coerceAtLeast(1).toFloat()).coerceIn(0.05f, 1f)
                Surface(
                    onClick = { onOpenDocument(lastOpenedDoc) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                        .testTag("continue_reading_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DocumentPageThumbnail(
                            filePath = lastOpenedDoc.filePath,
                            formatName = lastOpenedDoc.formatName,
                            pageIndex = lastOpenedDoc.lastPage,
                            width = 56.dp,
                            height = 76.dp
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CONTINUE READING",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Page ${lastOpenedDoc.lastPage + 1} of ${lastOpenedDoc.pageCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = lastOpenedDoc.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${lastOpenedDoc.formatName} • ${(lastOpenedDoc.sizeBytes / 1024).coerceAtLeast(1)} KB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Resume",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Documents Section Header & Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Files",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showOnlyFavourites,
                        onClick = { showOnlyFavourites = false },
                        label = { Text("All (${documents.size})", maxLines = 1, softWrap = false) },
                        modifier = Modifier.testTag("home_tab_recent")
                    )
                    FilterChip(
                        selected = showOnlyFavourites,
                        onClick = { showOnlyFavourites = true },
                        label = { Text("Starred (${favourites.size})", maxLines = 1, softWrap = false) },
                        modifier = Modifier.testTag("home_tab_favourites")
                    )
                }
            }
        }

        // 6. Empty State Prompting User for Real File / Folder Permission OR Document List
        if (filteredDocs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
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
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching files found"
                            else "Open Your Device Files",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try a different keyword or import more files from your device."
                            else "Select PDFs, Images, Word, Excel, or eBooks from your device storage, or grant folder access to sync an entire directory.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onPickSafFile,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Pick Files", maxLines = 1, softWrap = false)
                            }
                            OutlinedButton(
                                onClick = onConnectFolder,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sync Folder", maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredDocs, key = { it.id }) { doc ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                onToggleFavourite(doc)
                                false
                            }
                            SwipeToDismissBoxValue.EndToStart -> {
                                onDeleteDocument(doc)
                                true
                            }
                            SwipeToDismissBoxValue.Settled -> false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Star, contentDescription = "Favourite", tint = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Trash", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                ) {
                    DocumentRowCard(
                        doc = doc,
                        onClick = { onOpenDocument(doc) },
                        onToggleFavourite = { onToggleFavourite(doc) },
                        onDelete = { onDeleteDocument(doc) }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentRowCard(
    doc: DocumentEntity,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag("doc_row_${doc.id}")
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = doc.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${doc.formatName} • ${doc.pageCount} p. • ${(doc.sizeBytes / 1024).coerceAtLeast(1)} KB" +
                        if (doc.tagsCsv.isNotBlank()) " • #${doc.tagsCsv.replace(",", " #")}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavourite, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (doc.isFavourite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Toggle favourite",
                    tint = if (doc.isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Move to trash",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ToolsScreen(
    recentToolIds: List<String>,
    presets: List<ToolPresetEntity>,
    jobs: List<BatchJobEntity>,
    onSelectTool: (PdfToolId) -> Unit,
    onSelectPreset: (ToolPresetEntity) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onRetryJob: (BatchJobEntity) -> Unit,
    onClearJobs: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ToolCategory?>(null) }
    var showQueueDetails by remember { mutableStateOf(false) }

    val allTools = PdfToolId.entries
    val filteredTools = remember(searchQuery, selectedCategory) {
        allTools.filter { tool ->
            val matchesCat = selectedCategory == null || tool.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                tool.title.contains(searchQuery.trim(), ignoreCase = true) ||
                tool.subtitle.contains(searchQuery.trim(), ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().testTag("tools_screen_list")
    ) {
        // 1. Clean Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "PDF Studio",
                        style = MaterialTheme.typography.displayMedium,
                        maxLines = 1
                    )
                    Text(
                        text = "Select any tool to process device files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (jobs.isNotEmpty()) {
                    FilterChip(
                        selected = showQueueDetails,
                        onClick = { showQueueDetails = !showQueueDetails },
                        label = { Text("History (${jobs.size})", maxLines = 1, softWrap = false) }
                    )
                }
            }
        }

        // 2. Search Input + Horizontally Scrollable Category Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search tools (Merge, Split, Sign, Scan, OCR)...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tools_search_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All (${allTools.size})", maxLines = 1, softWrap = false) }
                        )
                    }
                    items(ToolCategory.entries.size) { idx ->
                        val cat = ToolCategory.entries[idx]
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.title, maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }
        }

        // 3. Process History Accordion
        if (showQueueDetails && jobs.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Operations", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onClearJobs) { Text("Clear") }
                        }
                        jobs.take(5).forEach { job ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${job.title} • ${job.status}",
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        job.summaryMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onRetryJob(job) }) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Run again")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Saved Reusable Presets (Only if user saved any)
        if (searchQuery.isBlank() && selectedCategory == null && presets.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Saved Presets", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(presets, key = { it.id }) { preset ->
                            Surface(
                                onClick = { onSelectPreset(preset) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .width(210.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(preset.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(preset.configSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { onDeletePreset(preset.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Delete preset", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Uniform 2-Column Studio Tool Grid
        val categoriesToShow = if (selectedCategory != null) listOf(selectedCategory!!) else ToolCategory.entries
        categoriesToShow.forEach { category ->
            val categoryTools = filteredTools.filter { it.category == category }
            if (categoryTools.isNotEmpty()) {
                item(key = category.name) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val rows = categoryTools.chunked(2)
                        rows.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { tool ->
                                    BentoToolTile(
                                        tool = tool,
                                        onClick = { onSelectTool(tool) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
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
private fun BentoToolTile(
    tool: PdfToolId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp,
        modifier = modifier
            .height(74.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .testTag("tool_card_${tool.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
