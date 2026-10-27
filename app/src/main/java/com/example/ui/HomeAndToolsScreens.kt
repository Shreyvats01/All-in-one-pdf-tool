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
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Compress
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
            "Universal Offline Reader",
            "Read PDF, EPUB, Word, Excel, PowerPoint, ODT, Markdown, CSV, Comics, and Images with 800% crisp zoom, Reflow mode, and offline Read Aloud.",
            "Zero network permissions • Instant cold start"
        ),
        Triple(
            "31 Non-Destructive PDF Tools",
            "Merge, split, compress, OCR, scan with CameraX edge detection, convert to/from Office, watermark, redact, and digitally sign in 3 taps.",
            "Pick files → Configure → Result"
        ),
        Triple(
            "Complete Document Sovereignty",
            "Every operation runs 100% on your device. Original files are never altered, and sensitive records stay encrypted in your Biometric Private Vault.",
            "No cloud • No telemetry • No ads"
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
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Folio.", style = MaterialTheme.typography.headlineMedium)
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
                    Spacer(Modifier.height(32.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.height(14.dp))
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
                    Text(if (pagerState.currentPage < pages.lastIndex) "Continue" else "Enter Folio")
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
            .height(190.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(surface)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        when (slideIndex) {
            0 -> {
                // Slide 0: Architectural spread & typography lines
                drawRoundRect(
                    color = Color(0xFFFAF8F4),
                    topLeft = Offset(cx - 110.dp.toPx(), cy - 64.dp.toPx()),
                    size = Size(104.dp.toPx(), 128.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFFFAF8F4),
                    topLeft = Offset(cx + 6.dp.toPx(), cy - 64.dp.toPx()),
                    size = Size(104.dp.toPx(), 128.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                for (i in 0..4) {
                    val y = cy - 40.dp.toPx() + i * 18.dp.toPx()
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
                // Slide 1: 3-Step Bento studio tiles
                for (col in 0..2) {
                    val x = cx - 125.dp.toPx() + col * 88.dp.toPx()
                    drawRoundRect(
                        color = if (col == 1) primary else Color(0xFFFAF8F4),
                        topLeft = Offset(x, cy - 48.dp.toPx()),
                        size = Size(74.dp.toPx(), 96.dp.toPx()),
                        cornerRadius = CornerRadius(14.dp.toPx())
                    )
                }
            }
            else -> {
                // Slide 2: Cryptographic Vault concentric shield
                drawCircle(
                    color = primary.copy(alpha = 0.15f),
                    radius = 64.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = primary,
                    radius = 42.dp.toPx(),
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
    onOpenDocument: (DocumentEntity) -> Unit,
    onToggleFavourite: (DocumentEntity) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
    onQuickToolClick: (PdfToolId) -> Unit,
    onPickSafFile: () -> Unit,
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier.fillMaxSize().testTag("home_screen_list")
    ) {
        // Editorial Masthead + SAF Open Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Folio", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "Offline Document Desk & PDF Studio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onPickSafFile,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("home_open_saf_button")
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open File")
                }
            }
        }

        // Understated Search Pill (History chips only reveal when focused)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onRecordSearch(it)
                    },
                    placeholder = { Text("Search titles, #tags, or full text...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
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
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(term, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // "Continue Reading" Editorial Showcase Card with Live Page-1 Cover Thumbnail
        if (lastOpenedDoc != null && searchQuery.isBlank()) {
            item {
                val progressFraction = ((lastOpenedDoc.lastPage + 1).toFloat() / lastOpenedDoc.pageCount.coerceAtLeast(1).toFloat()).coerceIn(0.05f, 1f)
                Surface(
                    onClick = { onOpenDocument(lastOpenedDoc) },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                        .testTag("continue_reading_card")
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DocumentPageThumbnail(
                            filePath = lastOpenedDoc.filePath,
                            formatName = lastOpenedDoc.formatName,
                            pageIndex = lastOpenedDoc.lastPage,
                            width = 68.dp,
                            height = 92.dp
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "p. ${lastOpenedDoc.lastPage + 1} / ${lastOpenedDoc.pageCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = lastOpenedDoc.displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${lastOpenedDoc.formatName} • ${(progressFraction * 100).toInt()}% completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Resume", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Minimalist Quick Actions Dock (Scan, Merge, Compress, Sign)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick Studio", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val quickActions = listOf(
                        Triple(PdfToolId.SCAN_TO_PDF, "Scan", Icons.Outlined.DocumentScanner),
                        Triple(PdfToolId.MERGE_PDF, "Merge", Icons.AutoMirrored.Outlined.MergeType),
                        Triple(PdfToolId.COMPRESS_PDF, "Compress", Icons.Outlined.Compress),
                        Triple(PdfToolId.SIGN_PDF, "Sign", Icons.Outlined.Draw)
                    )
                    quickActions.forEach { (toolId, label, icon) ->
                        Surface(
                            onClick = { onQuickToolClick(toolId) },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                                .testTag("quick_action_${label.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Recent Files / Favourites Segmented Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Documents", style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showOnlyFavourites,
                        onClick = { showOnlyFavourites = false },
                        label = { Text("Recent (${documents.size})") },
                        modifier = Modifier.testTag("home_tab_recent")
                    )
                    FilterChip(
                        selected = showOnlyFavourites,
                        onClick = { showOnlyFavourites = true },
                        label = { Text("Starred (${favourites.size})") },
                        modifier = Modifier.testTag("home_tab_favourites")
                    )
                }
            }
        }

        if (filteredDocs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No matching documents", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tap 'Open File' to import from your device via SAF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .testTag("doc_row_${doc.id}")
    ) {
        Surface(
            onClick = onClick,
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = doc.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${doc.formatName}  •  ${doc.pageCount} p.  •  ${(doc.sizeBytes / 1024).coerceAtLeast(1)} KB" +
                            if (doc.tagsCsv.isNotBlank()) "  •  #${doc.tagsCsv.replace(",", " #")}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onToggleFavourite) {
                    Icon(
                        if (doc.isFavourite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Toggle favourite",
                        tint = if (doc.isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Move to trash",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

    val recentTools = remember(recentToolIds) {
        recentToolIds.mapNotNull { name -> runCatching { PdfToolId.valueOf(name) }.getOrNull() }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize().testTag("tools_screen_list")
    ) {
        // Editorial Masthead
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Toolkit", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "31 offline instruments • Non-destructive workflow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (jobs.isNotEmpty()) {
                    FilterChip(
                        selected = showQueueDetails,
                        onClick = { showQueueDetails = !showQueueDetails },
                        label = { Text("Queue (${jobs.size})") }
                    )
                }
            }
        }

        // Searchable Tool Input + Single-Row Category Filter Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tools (Merge, OCR, Redact, PDF/A, N-Up)...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tools_search_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All (${allTools.size})") }
                        )
                    }
                    items(ToolCategory.entries.size) { idx ->
                        val cat = ToolCategory.entries[idx]
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.title) }
                        )
                    }
                }
            }
        }

        // Process Queue Accordion
        if (showQueueDetails && jobs.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Process Queue & History", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onClearJobs) { Text("Clear Finished") }
                        }
                        jobs.take(5).forEach { job ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("${job.title} • ${job.status}", style = MaterialTheme.typography.labelLarge)
                                    Text(job.summaryMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        // Pinned Recently Used Tools (Refined monochrome + indigo pills)
        if (searchQuery.isBlank() && selectedCategory == null && recentTools.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pinned Recent", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentTools) { tool ->
                            Surface(
                                onClick = { onSelectTool(tool) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        tool.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(tool.title, style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Reusable Presets
        if (searchQuery.isBlank() && selectedCategory == null && presets.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Saved Presets", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(presets, key = { it.id }) { preset ->
                            Surface(
                                onClick = { onSelectPreset(preset) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .width(220.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(preset.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(preset.configSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { onDeletePreset(preset.id) }, modifier = Modifier.size(26.dp)) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Delete preset", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Uniform 2-Column Bento Tool Grid (Never stretches odd orphan items to 100% width)
        val categoriesToShow = if (selectedCategory != null) listOf(selectedCategory!!) else ToolCategory.entries
        categoriesToShow.forEach { category ->
            val categoryTools = filteredTools.filter { it.category == category }
            if (categoryTools.isNotEmpty()) {
                item(key = category.name) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(category.title, style = MaterialTheme.typography.headlineMedium)
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
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .height(78.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .testTag("tool_card_${tool.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
