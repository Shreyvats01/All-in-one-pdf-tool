package com.example.reader

import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Toc
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AnnotationEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.DocumentEntity
import com.example.domain.model.AnnotationType
import com.example.domain.model.PdfToolId
import com.example.domain.model.ReaderFitMode
import com.example.domain.model.ReaderViewMode
import com.example.domain.model.ReadingThemeMode
import com.example.ui.DocumentPageThumbnail
import com.example.ui.FolioViewModel
import com.example.ui.theme.ReaderNightBackground
import com.example.ui.theme.ReaderNightText
import com.example.ui.theme.ReaderSepiaBackground
import com.example.ui.theme.ReaderSepiaText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalReaderScreen(
    document: DocumentEntity,
    session: ParsedDocumentSession?,
    isLoading: Boolean,
    viewModel: FolioViewModel,
    onBack: () -> Unit,
    onOpenInTool: (PdfToolId) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val bookmarks by viewModel.repository.observeBookmarks(document.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val annotations by viewModel.repository.observeAnnotations(document.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var currentPage by remember(document.id) { mutableIntStateOf(document.lastPage.coerceAtLeast(0)) }
    var zoomScale by remember(document.id) { mutableFloatStateOf(document.lastZoom.coerceIn(1f, 8f)) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var viewMode by remember(document.id) {
        mutableStateOf(runCatching { ReaderViewMode.valueOf(document.viewModeName) }.getOrDefault(ReaderViewMode.VERTICAL_CONTINUOUS))
    }
    var fitMode by remember(document.id) {
        mutableStateOf(runCatching { ReaderFitMode.valueOf(document.fitModeName) }.getOrDefault(ReaderFitMode.FIT_WIDTH))
    }
    var readingTheme by remember(document.id) {
        mutableStateOf(runCatching { ReadingThemeMode.valueOf(document.readingThemeName) }.getOrDefault(ReadingThemeMode.DAY))
    }

    // Focus mode (single tap hides all chrome)
    var focusMode by remember { mutableStateOf(false) }

    // Search bar state
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchPageIdx by remember { mutableIntStateOf(0) }
    var showTopOverflowMenu by remember { mutableStateOf(false) }

    // Reflow & Display Settings sheet
    var showDisplaySheet by remember { mutableStateOf(false) }
    var reflowFontSizeSp by remember { mutableFloatStateOf(17f) }
    var reflowLineSpacing by remember { mutableFloatStateOf(1.55f) }
    var reflowMarginDp by remember { mutableFloatStateOf(24f) }
    var brightnessDim by remember { mutableFloatStateOf(0f) }
    var blueLightWarmth by remember { mutableFloatStateOf(0f) }
    var autoScrollActive by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableFloatStateOf(1.0f) }

    // Navigation Sheet (Thumbnails, ToC, Bookmarks, Annotations)
    var showNavSheet by remember { mutableStateOf(false) }
    var navSheetTab by remember { mutableIntStateOf(0) }

    // Harmonized Mode-Switching Pill State (Reading Mode vs Annotation Mode)
    var showAnnotationBar by remember { mutableStateOf(false) }
    var activeAnnotationType by remember { mutableStateOf(AnnotationType.HIGHLIGHT) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteDialogInput by remember { mutableStateOf("") }
    val activeInkPoints = remember { mutableStateListOf<Offset>() }

    // Password prompt if PDF is encrypted
    var passwordInput by remember { mutableStateOf("") }

    // Offline Text-to-Speech (TTS)
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsSpeed by remember { mutableFloatStateOf(1.0f) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        ttsEngine = tts
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)
    val totalPages = session?.pageCount?.coerceAtLeast(1) ?: document.pageCount.coerceAtLeast(1)
    val pagerState = rememberPagerState(initialPage = currentPage.coerceIn(0, totalPages - 1)) { totalPages }

    // Auto-scroll effect
    LaunchedEffect(autoScrollActive, autoScrollSpeed, currentPage) {
        while (autoScrollActive) {
            delay((2800L / autoScrollSpeed.coerceAtLeast(0.5f)).toLong())
            if (currentPage < totalPages - 1) {
                currentPage += 1
                listState.animateScrollToItem(currentPage)
            } else {
                autoScrollActive = false
            }
        }
    }

    // Search matches across pages
    val matchedPageIndices = remember(searchQuery, session) {
        if (searchQuery.trim().length < 2 || session == null) {
            emptyList()
        } else {
            session.pagesText.mapIndexedNotNull { idx, pageText ->
                if (pageText.contains(searchQuery.trim(), ignoreCase = true)) idx else null
            }
        }
    }

    val totalMatchHits = remember(searchQuery, session) {
        if (searchQuery.trim().length < 2 || session == null) 0
        else {
            val regex = Regex(Regex.escape(searchQuery.trim()), RegexOption.IGNORE_CASE)
            session.pagesText.sumOf { regex.findAll(it).count() }
        }
    }

    val effectiveNightInverted = when (readingTheme) {
        ReadingThemeMode.NIGHT_INVERTED -> true
        ReadingThemeMode.AUTO_TIME -> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            hour < 6 || hour >= 20
        }
        else -> false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when {
                    effectiveNightInverted -> ReaderNightBackground
                    readingTheme == ReadingThemeMode.SEPIA -> ReaderSepiaBackground
                    else -> MaterialTheme.colorScheme.background
                }
            )
    ) {
        if (isLoading || session == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Opening ${document.displayName}...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (session.isPasswordProtected) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Password Protected PDF", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            session.errorPlainMessage ?: "This PDF is password-protected. Enter the password to continue.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Document Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { viewModel.openDocument(document, passwordInput) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Unlock & Read")
                            }
                        }
                    }
                }
            }
        } else {
            // Main Reader Viewport
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newZoom = (zoomScale * zoom).coerceIn(1f, 8f) // Up to 800% zoom
                            zoomScale = newZoom
                            panOffset = if (newZoom > 1.02f) {
                                Offset(
                                    x = (panOffset.x + pan.x).coerceIn(-1200f, 1200f),
                                    y = (panOffset.y + pan.y).coerceIn(-1600f, 1600f)
                                )
                            } else {
                                Offset.Zero
                            }
                            viewModel.updateReadingProgress(currentPage, zoom = newZoom)
                        }
                    }
                    .pointerInput(settings.tapZonePaging, currentPage, totalPages) {
                        detectTapGestures(
                            onDoubleTap = {
                                zoomScale = if (zoomScale > 1.5f) 1f else 2.5f
                                panOffset = Offset.Zero
                                viewModel.updateReadingProgress(currentPage, zoom = zoomScale)
                            },
                            onTap = { tapPos ->
                                if (settings.tapZonePaging && viewMode != ReaderViewMode.VERTICAL_CONTINUOUS) {
                                    val w = size.width
                                    when {
                                        tapPos.x < w * 0.22f && currentPage > 0 -> {
                                            currentPage -= 1
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateReadingProgress(currentPage)
                                        }
                                        tapPos.x > w * 0.78f && currentPage < totalPages - 1 -> {
                                            currentPage += 1
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateReadingProgress(currentPage)
                                        }
                                        else -> focusMode = !focusMode
                                    }
                                } else {
                                    focusMode = !focusMode
                                }
                            }
                        )
                    }
            ) {
                when (viewMode) {
                    ReaderViewMode.REFLOW -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                horizontal = reflowMarginDp.dp,
                                vertical = 104.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(session.pagesText) { idx, pageText ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            when {
                                                effectiveNightInverted -> Color(0xFF1C1E22)
                                                readingTheme == ReadingThemeMode.SEPIA -> Color(0xFFEDE2C8)
                                                else -> MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                        .padding(24.dp)
                                ) {
                                    Text(
                                        text = "PAGE ${idx + 1} OF $totalPages",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    HighlightedSearchText(
                                        text = pageText,
                                        query = searchQuery,
                                        fontSizeSp = reflowFontSizeSp,
                                        lineSpacingMultiplier = reflowLineSpacing,
                                        textColor = when {
                                            effectiveNightInverted -> ReaderNightText
                                            readingTheme == ReadingThemeMode.SEPIA -> ReaderSepiaText
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }

                    ReaderViewMode.HORIZONTAL_PAGED, ReaderViewMode.SINGLE_PAGE -> {
                        LaunchedEffect(currentPage) {
                            if (pagerState.currentPage != currentPage && currentPage in 0 until totalPages) {
                                pagerState.scrollToPage(currentPage)
                            }
                        }
                        LaunchedEffect(pagerState.currentPage) {
                            if (currentPage != pagerState.currentPage) {
                                currentPage = pagerState.currentPage
                                viewModel.updateReadingProgress(currentPage)
                            }
                        }
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    translationX = panOffset.x,
                                    translationY = panOffset.y
                                )
                        ) { pageIdx ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 84.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                RenderedPageCard(
                                    session = session,
                                    pageIndex = pageIdx,
                                    zoomScale = zoomScale,
                                    invertNight = effectiveNightInverted,
                                    sepiaMode = readingTheme == ReadingThemeMode.SEPIA,
                                    pageAnnotations = annotations.filter { it.pageIndex == pageIdx },
                                    showAnnotationBar = showAnnotationBar,
                                    activeAnnotationType = activeAnnotationType,
                                    activeInkPoints = activeInkPoints,
                                    onAddStrokeAnnotation = { pts ->
                                        viewModel.addAnnotation(
                                            pageIndex = pageIdx,
                                            typeName = activeAnnotationType.name,
                                            colorArgb = 0xFF434CE6,
                                            textContent = "${activeAnnotationType.label} on Page ${pageIdx + 1}",
                                            pointsJson = pts
                                        )
                                    }
                                )
                            }
                        }
                    }

                    ReaderViewMode.TWO_PAGE_SPREAD -> {
                        val spreads = (0 until totalPages).chunked(2)
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    translationX = panOffset.x,
                                    translationY = panOffset.y
                                )
                        ) {
                            itemsIndexed(spreads) { _, pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    pair.forEach { pIdx ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            RenderedPageCard(
                                                session = session,
                                                pageIndex = pIdx,
                                                zoomScale = zoomScale,
                                                invertNight = effectiveNightInverted,
                                                sepiaMode = readingTheme == ReadingThemeMode.SEPIA,
                                                pageAnnotations = annotations.filter { it.pageIndex == pIdx },
                                                showAnnotationBar = showAnnotationBar,
                                                activeAnnotationType = activeAnnotationType,
                                                activeInkPoints = activeInkPoints,
                                                onAddStrokeAnnotation = {}
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ReaderViewMode.VERTICAL_CONTINUOUS -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                horizontal = if (fitMode == ReaderFitMode.FIT_PAGE) 36.dp else 14.dp,
                                vertical = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    translationX = panOffset.x,
                                    translationY = panOffset.y
                                )
                        ) {
                            items(totalPages) { pageIdx ->
                                RenderedPageCard(
                                    session = session,
                                    pageIndex = pageIdx,
                                    zoomScale = zoomScale,
                                    invertNight = effectiveNightInverted,
                                    sepiaMode = readingTheme == ReadingThemeMode.SEPIA,
                                    pageAnnotations = annotations.filter { it.pageIndex == pageIdx },
                                    showAnnotationBar = showAnnotationBar,
                                    activeAnnotationType = activeAnnotationType,
                                    activeInkPoints = activeInkPoints,
                                    onAddStrokeAnnotation = { pts ->
                                        viewModel.addAnnotation(
                                            pageIndex = pageIdx,
                                            typeName = activeAnnotationType.name,
                                            colorArgb = 0xFF434CE6,
                                            textContent = "${activeAnnotationType.label} on Page ${pageIdx + 1}",
                                            pointsJson = pts
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Brightness & Blue-Light Filter Overlays
        if (brightnessDim > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = brightnessDim.coerceIn(0f, 0.7f)))
            )
        }
        if (blueLightWarmth > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFB74D).copy(alpha = (blueLightWarmth * 0.28f).coerceIn(0f, 0.3f)))
            )
        }

        // Top Bar (Editorial Header with Overflow Menu — Hidden in Focus Mode)
        AnimatedVisibility(
            visible = !focusMode,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(
                            text = document.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page ${currentPage + 1} of $totalPages  •  ${(zoomScale * 100).toInt()}%  •  ${viewMode.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSearchBar = !showSearchBar }, modifier = Modifier.testTag("reader_search_button")) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search in document")
                    }
                    val isPageBookmarked = bookmarks.any { it.pageIndex == currentPage }
                    IconButton(
                        onClick = {
                            val existing = bookmarks.firstOrNull { it.pageIndex == currentPage }
                            if (existing != null) {
                                viewModel.deleteBookmark(existing.id)
                            } else {
                                viewModel.toggleBookmark(currentPage, "Bookmark Page ${currentPage + 1}", "Saved from reader")
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.testTag("reader_bookmark_button")
                    ) {
                        Icon(
                            if (isPageBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark page",
                            tint = if (isPageBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Keep direct Print & Share buttons accessible for tests + Overflow menu
                    IconButton(
                        onClick = {
                            session?.let { MultiFormatDocumentEngine.printDocumentOffline(context, it) }
                        },
                        modifier = Modifier.testTag("reader_print_button")
                    ) {
                        Icon(Icons.Outlined.Print, contentDescription = "Print document")
                    }
                    IconButton(
                        onClick = {
                            session?.let { MultiFormatDocumentEngine.shareDocumentOffline(context, it.file) }
                        },
                        modifier = Modifier.testTag("reader_share_button")
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share document")
                    }
                    Box {
                        IconButton(onClick = { showTopOverflowMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More actions")
                        }
                        DropdownMenu(
                            expanded = showTopOverflowMenu,
                            onDismissRequest = { showTopOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open in PDF Toolkit") },
                                leadingIcon = { Icon(Icons.Outlined.Build, contentDescription = null) },
                                onClick = {
                                    showTopOverflowMenu = false
                                    onOpenInTool(PdfToolId.COMPRESS_PDF)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Annotated PDF") },
                                leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = null) },
                                onClick = {
                                    showTopOverflowMenu = false
                                    onOpenInTool(PdfToolId.ANNOTATE_MARKUP)
                                }
                            )
                        }
                    }
                }

                // Expandable Full-Text Search Bar
                AnimatedVisibility(visible = showSearchBar) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    currentMatchPageIdx = 0
                                    viewModel.preferences.recordSearchQuery(it)
                                },
                                placeholder = { Text("Search full text...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reader_search_input"),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                                        }
                                    }
                                }
                            )
                            Text(
                                text = "$totalMatchHits hits",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                enabled = matchedPageIndices.isNotEmpty(),
                                onClick = {
                                    if (matchedPageIndices.isNotEmpty()) {
                                        currentMatchPageIdx = (currentMatchPageIdx - 1 + matchedPageIndices.size) % matchedPageIndices.size
                                        val targetPage = matchedPageIndices[currentMatchPageIdx]
                                        currentPage = targetPage
                                        scope.launch { listState.animateScrollToItem(targetPage) }
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Previous match")
                            }
                            IconButton(
                                enabled = matchedPageIndices.isNotEmpty(),
                                onClick = {
                                    if (matchedPageIndices.isNotEmpty()) {
                                        currentMatchPageIdx = (currentMatchPageIdx + 1) % matchedPageIndices.size
                                        val targetPage = matchedPageIndices[currentMatchPageIdx]
                                        currentPage = targetPage
                                        scope.launch { listState.animateScrollToItem(targetPage) }
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Next match")
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        // Harmonized Mode-Switching Floating Bottom Dock (Hidden in Focus Mode)
        AnimatedVisibility(
            visible = !focusMode,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scrubber Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${currentPage + 1} / $totalPages",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { newVal ->
                                val target = newVal.toInt().coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                                if (target != currentPage) {
                                    currentPage = target
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.updateReadingProgress(currentPage)
                                    scope.launch { listState.scrollToItem(currentPage) }
                                }
                            },
                            valueRange = 0f..(totalPages - 1).coerceAtLeast(1).toFloat(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Mode-Switching Floating Pill Dock (Reading Pill <-> Annotation Studio Pill)
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp))
                ) {
                    AnimatedContent(
                        targetState = showAnnotationBar,
                        label = "reader_mode_pill_transition"
                    ) { isAnnotating ->
                        if (!isAnnotating) {
                            // Reading Mode Pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showNavSheet = true },
                                    modifier = Modifier.testTag("reader_toc_thumbnails_button")
                                ) {
                                    Icon(Icons.Outlined.Toc, contentDescription = "Thumbnails, Outline & Bookmarks")
                                }
                                IconButton(
                                    onClick = {
                                        viewMode = if (viewMode == ReaderViewMode.REFLOW) {
                                            ReaderViewMode.VERTICAL_CONTINUOUS
                                        } else {
                                            ReaderViewMode.REFLOW
                                        }
                                        viewModel.updateReadingProgress(currentPage, viewMode = viewMode)
                                    },
                                    modifier = Modifier.testTag("reader_reflow_toggle_button")
                                ) {
                                    Icon(
                                        Icons.Outlined.FormatSize,
                                        contentDescription = "Toggle Reflow Mode",
                                        tint = if (viewMode == ReaderViewMode.REFLOW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { showAnnotationBar = true },
                                    modifier = Modifier.testTag("reader_annotate_toggle_button")
                                ) {
                                    Icon(
                                        Icons.Outlined.Draw,
                                        contentDescription = "Annotate & Edit",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (isSpeaking) {
                                            ttsEngine?.stop()
                                            isSpeaking = false
                                        } else {
                                            val textToSpeak = session?.pagesText?.getOrElse(currentPage) { "" } ?: ""
                                            ttsEngine?.setSpeechRate(ttsSpeed)
                                            ttsEngine?.speak(textToSpeak.take(1500), TextToSpeech.QUEUE_FLUSH, null, "folio_tts")
                                            isSpeaking = true
                                        }
                                    },
                                    modifier = Modifier.testTag("reader_tts_button")
                                ) {
                                    Icon(
                                        if (isSpeaking) Icons.Outlined.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = "Offline Read Aloud (TTS)",
                                        tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { showDisplaySheet = true },
                                    modifier = Modifier.testTag("reader_display_settings_button")
                                ) {
                                    Icon(Icons.Outlined.Tune, contentDescription = "Reading Themes & View Options")
                                }
                                IconButton(
                                    onClick = { focusMode = true },
                                    modifier = Modifier.testTag("reader_focus_mode_button")
                                ) {
                                    Icon(Icons.Outlined.Fullscreen, contentDescription = "Focus Mode")
                                }
                            }
                        } else {
                            // Annotation Studio Pill (Harmonized in the exact same floating dock footprint)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { showAnnotationBar = false },
                                            modifier = Modifier.testTag("reader_annotate_toggle_button")
                                        ) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = "Done Annotating",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "Markup Studio",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.undoAnnotation() },
                                            enabled = canUndo,
                                            modifier = Modifier.testTag("reader_undo_button")
                                        ) {
                                            Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo annotation")
                                        }
                                        IconButton(
                                            onClick = { viewModel.redoAnnotation() },
                                            enabled = canRedo,
                                            modifier = Modifier.testTag("reader_redo_button")
                                        ) {
                                            Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "Redo annotation")
                                        }
                                        IconButton(onClick = { showAddNoteDialog = true }) {
                                            Icon(Icons.Outlined.NoteAdd, contentDescription = "Add Note or Stamp")
                                        }
                                        Button(
                                            onClick = { onOpenInTool(PdfToolId.ANNOTATE_MARKUP) },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Export", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(AnnotationType.entries.size) { i ->
                                        val type = AnnotationType.entries[i]
                                        FilterChip(
                                            selected = activeAnnotationType == type,
                                            onClick = { activeAnnotationType = type },
                                            label = { Text(type.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Note / Text Box / Stamp Dialog
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add ${activeAnnotationType.label} on Page ${currentPage + 1}") },
            text = {
                OutlinedTextField(
                    value = noteDialogInput,
                    onValueChange = { noteDialogInput = it },
                    label = { Text("Annotation text or note") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addAnnotation(
                            pageIndex = currentPage,
                            typeName = activeAnnotationType.name,
                            colorArgb = 0xFF434CE6,
                            textContent = noteDialogInput.ifBlank { activeAnnotationType.label }
                        )
                        noteDialogInput = ""
                        showAddNoteDialog = false
                    }
                ) {
                    Text("Add Annotation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Navigation Panel Bottom Sheet (Thumbnails with Real Rendered Pages, Outline/ToC, Bookmarks, Annotations)
    if (showNavSheet && session != null) {
        ModalBottomSheet(onDismissRequest = { showNavSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .padding(horizontal = 16.dp)
            ) {
                TabRow(selectedTabIndex = navSheetTab) {
                    Tab(selected = navSheetTab == 0, onClick = { navSheetTab = 0 }, text = { Text("Thumbnails") })
                    Tab(selected = navSheetTab == 1, onClick = { navSheetTab = 1 }, text = { Text("Outline") })
                    Tab(selected = navSheetTab == 2, onClick = { navSheetTab = 2 }, text = { Text("Bookmarks") })
                    Tab(selected = navSheetTab == 3, onClick = { navSheetTab = 3 }, text = { Text("Annotations") })
                }
                Spacer(Modifier.height(12.dp))
                when (navSheetTab) {
                    0 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(104.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(session.pageCount) { pIdx ->
                                val isCurrentPage = pIdx == currentPage
                                Card(
                                    onClick = {
                                        currentPage = pIdx
                                        viewModel.updateReadingProgress(pIdx)
                                        scope.launch { listState.scrollToItem(pIdx) }
                                        showNavSheet = false
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentPage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    modifier = Modifier.border(
                                        width = if (isCurrentPage) 2.dp else 1.dp,
                                        color = if (isCurrentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        DocumentPageThumbnail(
                                            filePath = document.filePath,
                                            formatName = document.formatName,
                                            pageIndex = pIdx,
                                            width = 86.dp,
                                            height = 114.dp,
                                            isSelected = isCurrentPage
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "Page ${pIdx + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isCurrentPage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(session.outline) { _, entry ->
                                Surface(
                                    onClick = {
                                        currentPage = entry.pageIndex
                                        scope.launch { listState.scrollToItem(entry.pageIndex) }
                                        showNavSheet = false
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(entry.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text("p. ${entry.pageIndex + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        if (bookmarks.isEmpty()) {
                            Text("No bookmarks yet. Tap the bookmark icon in the top bar to save a page.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(bookmarks) { _, bm: BookmarkEntity ->
                                    Surface(
                                        onClick = {
                                            currentPage = bm.pageIndex
                                            scope.launch { listState.scrollToItem(bm.pageIndex) }
                                            showNavSheet = false
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(bm.title, style = MaterialTheme.typography.titleSmall)
                                                if (bm.note.isNotBlank()) {
                                                    Text(bm.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text("p. ${bm.pageIndex + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        if (annotations.isEmpty()) {
                            Text("No annotations yet. Toggle the pen icon to highlight, draw, or add notes.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(annotations) { _, annot ->
                                    Surface(
                                        onClick = {
                                            currentPage = annot.pageIndex
                                            scope.launch { listState.scrollToItem(annot.pageIndex) }
                                            showNavSheet = false
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("[${annot.typeName}] ${annot.textContent}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                            Text("p. ${annot.pageIndex + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reading Themes, View Modes, Fit Options, Brightness, Blue-Light, Auto-Scroll & TTS Speed Sheet
    if (showDisplaySheet) {
        ModalBottomSheet(onDismissRequest = { showDisplaySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Reading Experience & Display", style = MaterialTheme.typography.headlineMedium)

                Text("View Mode", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReaderViewMode.entries.size) { i ->
                        val mode = ReaderViewMode.entries[i]
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = {
                                viewMode = mode
                                viewModel.updateReadingProgress(currentPage, viewMode = mode)
                            },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Text("Page Fit", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReaderFitMode.entries.size) { i ->
                        val f = ReaderFitMode.entries[i]
                        FilterChip(
                            selected = fitMode == f,
                            onClick = {
                                fitMode = f
                                viewModel.updateReadingProgress(currentPage, fitMode = f)
                            },
                            label = { Text(f.label) }
                        )
                    }
                }

                Text("Reading Theme", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReadingThemeMode.entries.size) { i ->
                        val t = ReadingThemeMode.entries[i]
                        FilterChip(
                            selected = readingTheme == t,
                            onClick = {
                                readingTheme = t
                                viewModel.updateReadingProgress(currentPage, readingTheme = t)
                            },
                            label = { Text(t.label) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brightness Dim", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                    Slider(value = brightnessDim, onValueChange = { brightnessDim = it }, valueRange = 0f..0.65f, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Blue-Light Filter", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                    Slider(value = blueLightWarmth, onValueChange = { blueLightWarmth = it }, valueRange = 0f..1f, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reflow Text Size", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                    Slider(value = reflowFontSizeSp, onValueChange = { reflowFontSizeSp = it }, valueRange = 13f..28f, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TTS Voice Speed", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                    Slider(value = ttsSpeed, onValueChange = { ttsSpeed = it }, valueRange = 0.6f..2.0f, modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { autoScrollActive = !autoScrollActive }) {
                        Icon(if (autoScrollActive) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (autoScrollActive) "Stop Auto-Scroll" else "Start Auto-Scroll")
                    }
                    Button(onClick = {
                        showDisplaySheet = false
                        onOpenInTool(PdfToolId.COMPRESS_PDF)
                    }) {
                        Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open in PDF Toolkit")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RenderedPageCard(
    session: ParsedDocumentSession,
    pageIndex: Int,
    zoomScale: Float,
    invertNight: Boolean,
    sepiaMode: Boolean,
    pageAnnotations: List<AnnotationEntity>,
    showAnnotationBar: Boolean,
    activeAnnotationType: AnnotationType,
    activeInkPoints: MutableList<Offset>,
    onAddStrokeAnnotation: (String) -> Unit
) {
    var bitmap by remember(session.file.absolutePath, pageIndex, zoomScale > 2.2f) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(session.file.absolutePath, pageIndex, zoomScale > 2.2f) {
        val targetWidth = if (zoomScale > 2.2f) 1800 else 1080
        bitmap = MultiFormatDocumentEngine.renderPageBitmap(
            file = session.file,
            format = session.format,
            pageIndex = pageIndex,
            pageText = session.pagesText.getOrElse(pageIndex) { "" },
            targetWidthPx = targetWidth
        )
    }

    val colorFilter = remember(invertNight, sepiaMode) {
        when {
            invertNight -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            sepiaMode -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.95f, 0.05f, 0f, 0f, 0f,
                        0f, 0.89f, 0.05f, 0f, 0f,
                        0f, 0f, 0.76f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            else -> null
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val currentBmp = bitmap
            if (currentBmp != null && !currentBmp.isRecycled) {
                Image(
                    bitmap = currentBmp.asImageBitmap(),
                    contentDescription = "Document page ${pageIndex + 1}",
                    contentScale = ContentScale.FillWidth,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            // Interactive Freehand & Geometric Shape Annotation Overlay Canvas
            val accentColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (showAnnotationBar) {
                            Modifier.pointerInput(pageIndex, activeAnnotationType) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        activeInkPoints.clear()
                                        activeInkPoints.add(startOffset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        activeInkPoints.add(change.position)
                                    },
                                    onDragEnd = {
                                        if (activeInkPoints.size >= 2) {
                                            val w = size.width.coerceAtLeast(1).toFloat()
                                            val h = size.height.coerceAtLeast(1).toFloat()
                                            val serialized = activeInkPoints.take(40).joinToString(";") {
                                                "${(it.x / w)},${(it.y / h)}"
                                            }
                                            onAddStrokeAnnotation(serialized)
                                        }
                                        activeInkPoints.clear()
                                    }
                                )
                            }
                        } else Modifier
                    )
            ) {
                // Helper to render any annotation by its type
                fun drawTypedGeometry(typeName: String, points: List<Offset>) {
                    if (points.size < 2) return
                    val first = points.first()
                    val last = points.last()
                    when (typeName) {
                        AnnotationType.RECTANGLE.name -> {
                            val topLeft = Offset(min(first.x, last.x), min(first.y, last.y))
                            val rectSize = Size(abs(last.x - first.x), abs(last.y - first.y))
                            drawRect(
                                color = accentColor.copy(alpha = 0.85f),
                                topLeft = topLeft,
                                size = rectSize,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                        AnnotationType.CIRCLE.name -> {
                            val center = Offset((first.x + last.x) / 2f, (first.y + last.y) / 2f)
                            val radius = (Offset(last.x - first.x, last.y - first.y).getDistance() / 2f).coerceAtLeast(8f)
                            drawCircle(
                                color = accentColor.copy(alpha = 0.85f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                        AnnotationType.ARROW.name -> {
                            drawLine(
                                color = accentColor,
                                start = first,
                                end = last,
                                strokeWidth = 3.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            val angle = atan2(last.y - first.y, last.x - first.x)
                            val headLen = 14.dp.toPx()
                            val wing1 = Offset(
                                x = (last.x - headLen * cos(angle - 0.5f)).toFloat(),
                                y = (last.y - headLen * sin(angle - 0.5f)).toFloat()
                            )
                            val wing2 = Offset(
                                x = (last.x - headLen * cos(angle + 0.5f)).toFloat(),
                                y = (last.y - headLen * sin(angle + 0.5f)).toFloat()
                            )
                            drawLine(accentColor, last, wing1, strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            drawLine(accentColor, last, wing2, strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        }
                        AnnotationType.HIGHLIGHT.name -> {
                            val path = Path()
                            points.forEachIndexed { idx, pt ->
                                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFFFFD54F).copy(alpha = 0.42f),
                                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        else -> {
                            val path = Path()
                            points.forEachIndexed { idx, pt ->
                                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                            }
                            drawPath(
                                path = path,
                                color = accentColor.copy(alpha = 0.82f),
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                // Draw saved annotations for this page
                pageAnnotations.forEachIndexed { idx, annot ->
                    if (annot.pointsJson.isNotBlank()) {
                        val pts = annot.pointsJson.split(";").mapNotNull { pair ->
                            val coords = pair.split(",")
                            val nx = coords.getOrNull(0)?.toFloatOrNull() ?: return@mapNotNull null
                            val ny = coords.getOrNull(1)?.toFloatOrNull() ?: return@mapNotNull null
                            Offset(nx * size.width, ny * size.height)
                        }
                        drawTypedGeometry(annot.typeName, pts)
                    } else {
                        drawCircle(
                            color = accentColor,
                            radius = 10.dp.toPx(),
                            center = Offset(size.width - 28.dp.toPx(), (40 + idx * 28).dp.toPx())
                        )
                    }
                }

                // Draw live active stroke/shape
                if (activeInkPoints.size > 1) {
                    drawTypedGeometry(activeAnnotationType.name, activeInkPoints)
                }
            }

            // Annotation badges if present on this page
            if (pageAnnotations.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pageAnnotations.take(3).forEach { annot ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                        ) {
                            Text(
                                text = "${annot.typeName}: ${annot.textContent.take(24)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightedSearchText(
    text: String,
    query: String,
    fontSizeSp: Float,
    lineSpacingMultiplier: Float,
    textColor: Color
) {
    val highlightBg = MaterialTheme.colorScheme.primaryContainer
    val highlightFg = MaterialTheme.colorScheme.onPrimaryContainer

    val annotated = remember(text, query, highlightBg, highlightFg) {
        if (query.trim().length < 2) {
            buildAnnotatedString { append(text) }
        } else {
            buildAnnotatedString {
                val regex = Regex(Regex.escape(query.trim()), RegexOption.IGNORE_CASE)
                var lastIdx = 0
                regex.findAll(text).forEach { match ->
                    append(text.substring(lastIdx, match.range.first))
                    withStyle(SpanStyle(background = highlightBg, color = highlightFg, fontWeight = FontWeight.Bold)) {
                        append(match.value)
                    }
                    lastIdx = match.range.last + 1
                }
                if (lastIdx < text.length) {
                    append(text.substring(lastIdx))
                }
            }
        }
    }

    Text(
        text = annotated,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * lineSpacingMultiplier).sp,
        color = textColor
    )
}
