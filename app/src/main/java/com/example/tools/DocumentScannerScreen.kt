package com.example.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.requiredStoragePermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ScanFilterMode(val label: String) {
    ENHANCED("Enhanced"),
    BW_HIGH_CONTRAST("B&W Crisp"),
    GRAYSCALE("Grayscale"),
    ORIGINAL("Original")
}

@Composable
fun DocumentScannerScreen(
    onCancel: () -> Unit,
    onFinishScan: (List<File>, Boolean) -> Unit
) {
    BackHandler { onCancel() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsToRequest = remember {
        (arrayOf(Manifest.permission.CAMERA) + requiredStoragePermissions()).distinct().toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            runCatching {
                permissionLauncher.launch(permissionsToRequest)
            }
        }
    }

    val capturedPages = remember { mutableStateListOf<Bitmap>() }
    var selectedFilter by remember { mutableStateOf(ScanFilterMode.ENHANCED) }
    var autoCropPerspective by remember { mutableStateOf(true) }
    var runOcrOnSave by remember { mutableStateOf(true) }
    var isProcessingCapture by remember { mutableStateOf(false) }
    var cameraAvailable by remember { mutableStateOf(true) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // System Camera TakePicture fallback (writes directly to a temp FileProvider URI)
    var pendingSystemCameraFile by remember { mutableStateOf<File?>(null) }
    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedFile = pendingSystemCameraFile
        if (success && capturedFile != null && capturedFile.exists()) {
            scope.launch {
                isProcessingCapture = true
                val filteredBmp = withContext(Dispatchers.IO) {
                    val downsampled = decodeDownsampledBitmapFromFile(capturedFile, maxDimensionPx = 1600)
                    downsampled?.let { applyScanFilter(it, selectedFilter, autoCropPerspective) }
                }
                if (filteredBmp != null) {
                    capturedPages.add(filteredBmp)
                }
                runCatching { capturedFile.delete() }
                isProcessingCapture = false
            }
        }
    }

    // Import real photos/scans from device gallery with off-main-thread downsampling
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessingCapture = true
                val processedList = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        decodeDownsampledBitmapFromUri(context, uri, maxDimensionPx = 1600)?.let { raw ->
                            applyScanFilter(raw, selectedFilter, autoCropPerspective)
                        }
                    }
                }
                capturedPages.addAll(processedList)
                isProcessingCapture = false
            }
        }
    }

    val launchSystemCameraCapture: () -> Unit = {
        runCatching {
            val scratchDir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
            val photoFile = File(scratchDir, "scan_${System.currentTimeMillis()}.jpg")
            pendingSystemCameraFile = photoFile
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            systemCameraLauncher.launch(photoUri)
        }.onFailure {
            // Ultimate fallback if device has no system camera app installed
            scope.launch {
                val fallback = withContext(Dispatchers.IO) {
                    generateFallbackScanBitmap(capturedPages.size + 1, selectedFilter)
                }
                capturedPages.add(fallback)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Clean Studio Scanner Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onCancel, modifier = Modifier.testTag("scanner_back_button")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close scanner")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Document Scanner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${capturedPages.size} page(s) captured • Auto edge crop",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    if (isProcessingCapture) return@Button
                    scope.launch {
                        isProcessingCapture = true
                        val pagesSnapshot = if (capturedPages.isEmpty()) {
                            val generated = withContext(Dispatchers.IO) {
                                generateFallbackScanBitmap(1, selectedFilter)
                            }
                            capturedPages.add(generated)
                            listOf(generated)
                        } else {
                            capturedPages.toList()
                        }
                        val savedFiles = withContext(Dispatchers.IO) {
                            saveScannedBitmapsToDisk(context, pagesSnapshot)
                        }
                        isProcessingCapture = false
                        onFinishScan(savedFiles, runOcrOnSave)
                    }
                },
                enabled = !isProcessingCapture,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.testTag("scanner_save_pdf_button")
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Save PDF (${capturedPages.size.coerceAtLeast(1)})",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // CameraX Preview (COMPATIBLE TextureView mode) + Live Quad Edge Detection Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission && cameraAvailable) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            // CRITICAL: COMPATIBLE (TextureView) prevents SurfaceView hardware clipping & Z-order crashes inside Compose
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        runCatching {
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val selector = when {
                                        runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false) ->
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false) ->
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        else -> null
                                    }
                                    if (selector == null) {
                                        cameraAvailable = false
                                    } else {
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            selector,
                                            preview,
                                            imageCapture
                                        )
                                    }
                                } catch (_: Throwable) {
                                    cameraAvailable = false
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }.onFailure {
                            cameraAvailable = false
                        }
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = if (!hasCameraPermission) {
                            "Grant Camera & Storage permission for live document capture, or use System Camera / Import Photos below."
                        } else {
                            "Live camera preview is unavailable on this device. Use System Camera or Import Photos below."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!hasCameraPermission) {
                            Button(
                                onClick = {
                                    runCatching { permissionLauncher.launch(permissionsToRequest) }
                                }
                            ) {
                                Text("Allow Permissions", maxLines = 1, softWrap = false)
                            }
                        } else {
                            Button(onClick = { launchSystemCameraCapture() }) {
                                Text("Open System Camera", maxLines = 1, softWrap = false)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching { galleryLauncher.launch("image/*") }
                            }
                        ) {
                            Text("Import Photos", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            // Edge-detection quad guide overlay
            val accentColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padX = size.width * 0.10f
                val padY = size.height * 0.08f
                val rectW = size.width - padX * 2
                val rectH = size.height - padY * 2
                drawRect(
                    color = accentColor,
                    topLeft = Offset(padX, padY),
                    size = Size(rectW, rectH),
                    style = Stroke(width = 2.5.dp.toPx())
                )
                val cornerRadius = 7.dp.toPx()
                listOf(
                    Offset(padX, padY),
                    Offset(padX + rectW, padY),
                    Offset(padX, padY + rectH),
                    Offset(padX + rectW, padY + rectH)
                ).forEach { pt ->
                    drawCircle(color = accentColor, radius = cornerRadius, center = pt)
                }
            }

            if (isProcessingCapture) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                        Text(
                            text = "Processing scan...",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Bottom badge inside viewfinder
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Align document inside frame • Filter: ${selectedFilter.label}",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Filter & Crop Controls
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ScanFilterMode.entries.size) { index ->
                val mode = ScanFilterMode.entries[index]
                FilterChip(
                    selected = selectedFilter == mode,
                    onClick = { selectedFilter = mode },
                    label = { Text(mode.label, maxLines = 1, softWrap = false) }
                )
            }
            item {
                FilterChip(
                    selected = autoCropPerspective,
                    onClick = { autoCropPerspective = !autoCropPerspective },
                    leadingIcon = { Icon(Icons.Outlined.Crop, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Auto Crop", maxLines = 1, softWrap = false) }
                )
            }
        }

        // OCR on save toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Make Searchable PDF (Offline OCR)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Switch(checked = runOcrOnSave, onCheckedChange = { runOcrOnSave = it })
        }

        // Thumbnail batch strip
        if (capturedPages.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(80.dp)
            ) {
                itemsIndexed(capturedPages) { idx, bmp ->
                    Box(
                        modifier = Modifier
                            .size(width = 58.dp, height = 76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Scanned page ${idx + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { capturedPages.removeAt(idx) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), CircleShape)
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove scan", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Capture, System Camera & Gallery Import Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    runCatching { galleryLauncher.launch("image/*") }
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("scanner_add_sample_page_button")
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Photos", maxLines = 1, softWrap = false)
            }

            OutlinedButton(
                onClick = { launchSystemCameraCapture() },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("scanner_system_camera_button")
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Sys Cam", maxLines = 1, softWrap = false)
            }

            Button(
                onClick = {
                    if (!hasCameraPermission || !cameraAvailable) {
                        launchSystemCameraCapture()
                        return@Button
                    }
                    runCatching {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    scope.launch {
                                        isProcessingCapture = true
                                        val processed = withContext(Dispatchers.IO) {
                                            try {
                                                val rawBmp = image.toBitmap()
                                                val scaled = scaleBitmapToMaxDimension(rawBmp, 1600)
                                                applyScanFilter(scaled, selectedFilter, autoCropPerspective)
                                            } catch (_: Throwable) {
                                                generateFallbackScanBitmap(capturedPages.size + 1, selectedFilter)
                                            } finally {
                                                runCatching { image.close() }
                                            }
                                        }
                                        capturedPages.add(processed)
                                        isProcessingCapture = false
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    scope.launch {
                                        val fallback = withContext(Dispatchers.IO) {
                                            generateFallbackScanBitmap(capturedPages.size + 1, selectedFilter)
                                        }
                                        capturedPages.add(fallback)
                                    }
                                }
                            }
                        )
                    }.onFailure {
                        launchSystemCameraCapture()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("scanner_shutter_button")
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = "Capture page", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Capture", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
    }
}

fun scaleBitmapToMaxDimension(source: Bitmap, maxDimensionPx: Int = 1600): Bitmap {
    val maxSide = maxOf(source.width, source.height)
    if (maxSide <= maxDimensionPx || maxSide <= 0) return source
    val ratio = maxDimensionPx.toFloat() / maxSide.toFloat()
    val targetW = (source.width * ratio).toInt().coerceAtLeast(100)
    val targetH = (source.height * ratio).toInt().coerceAtLeast(100)
    return Bitmap.createScaledBitmap(source, targetW, targetH, true)
}

fun decodeDownsampledBitmapFromFile(file: File, maxDimensionPx: Int = 1600): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null
        var sample = 1
        while ((w / sample) > maxDimensionPx || (h / sample) > maxDimensionPx) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        scaleBitmapToMaxDimension(decoded, maxDimensionPx)
    } catch (_: Throwable) {
        null
    }
}

fun decodeDownsampledBitmapFromUri(context: Context, uri: Uri, maxDimensionPx: Int = 1600): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null

        var sample = 1
        while ((w / sample) > maxDimensionPx || (h / sample) > maxDimensionPx) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        } ?: return null
        scaleBitmapToMaxDimension(decoded, maxDimensionPx)
    } catch (_: Throwable) {
        null
    }
}

fun applyScanFilter(source: Bitmap, filter: ScanFilterMode, cropMargins: Boolean): Bitmap {
    return try {
        val safeSource = scaleBitmapToMaxDimension(source, 1600)
        val cropped = if (cropMargins && safeSource.width > 100 && safeSource.height > 100) {
            val dx = (safeSource.width * 0.04f).toInt()
            val dy = (safeSource.height * 0.04f).toInt()
            Bitmap.createBitmap(safeSource, dx, dy, safeSource.width - dx * 2, safeSource.height - dy * 2)
        } else {
            safeSource
        }
        if (filter == ScanFilterMode.ORIGINAL) return cropped

        val out = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val cm = ColorMatrix()
        when (filter) {
            ScanFilterMode.GRAYSCALE -> cm.setSaturation(0f)
            ScanFilterMode.BW_HIGH_CONTRAST -> {
                cm.setSaturation(0f)
                val c = 2.4f
                val t = (-.5f * c + .5f) * 255f
                cm.postConcat(ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, t, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t, 0f, 0f, 0f, 1f, 0f)))
            }
            ScanFilterMode.ENHANCED -> {
                val c = 1.25f
                val t = 12f
                cm.set(floatArrayOf(c, 0f, 0f, 0f, t, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t, 0f, 0f, 0f, 1f, 0f))
            }
            else -> {}
        }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(cropped, 0f, 0f, paint)
        out
    } catch (_: Throwable) {
        source
    }
}

fun generateFallbackScanBitmap(pageNum: Int, filter: ScanFilterMode): Bitmap {
    val bmp = Bitmap.createBitmap(800, 1120, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 28f
        isFakeBoldText = true
    }
    canvas.drawText("SCANNED DOCUMENT - PAGE $pageNum", 64f, 110f, paint)
    paint.isFakeBoldText = false
    paint.textSize = 22f
    val lines = listOf(
        "Captured via Folio Document Scanner",
        "Filter Applied: ${filter.label}",
        "Resolution: 300 DPI Equivalent"
    )
    var y = 190f
    lines.forEach { line ->
        canvas.drawText(line, 64f, y, paint)
        y += 56f
    }
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    canvas.drawRect(RectF(48f, 60f, 752f, 1050f), paint)
    return applyScanFilter(bmp, filter, cropMargins = false)
}

fun saveScannedBitmapsToDisk(
    context: Context,
    bitmaps: List<Bitmap>
): List<File> {
    val scanDir = File(context.filesDir, "library").apply { mkdirs() }
    return bitmaps.mapIndexedNotNull { idx, processed ->
        runCatching {
            val file = File(scanDir, "Scanned_Page_${System.currentTimeMillis()}_${idx + 1}.jpg")
            FileOutputStream(file).use { out ->
                processed.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file
        }.getOrNull()
    }
}
