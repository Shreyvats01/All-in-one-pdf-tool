package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.VaultCryptoManager
import com.example.domain.model.DocumentFormat
import com.example.reader.MultiFormatDocumentEngine
import java.io.File

/**
 * Renders an asynchronous miniature paper cover or page thumbnail backed by MultiFormatDocumentEngine's LRU cache.
 */
@Composable
fun DocumentPageThumbnail(
    filePath: String,
    formatName: String,
    pageIndex: Int = 0,
    width: Dp = 46.dp,
    height: Dp = 62.dp,
    isSelected: Boolean = false,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(filePath, pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(filePath, pageIndex) {
        val file = File(filePath)
        if (file.exists()) {
            val fmt = runCatching { DocumentFormat.valueOf(formatName) }
                .getOrDefault(DocumentFormat.fromFilename(file.name))
            bitmap = MultiFormatDocumentEngine.renderPageBitmap(
                file = file,
                format = fmt,
                pageIndex = pageIndex,
                pageText = file.name,
                targetWidthPx = 360
            )
        }
    }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "thumb_border"
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFAF8F4))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        val currentBmp = bitmap
        if (currentBmp != null && !currentBmp.isRecycled) {
            Image(
                bitmap = currentBmp.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1} thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = formatName.take(4),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Minimalist tactile numeric keypad + 4-dot indicator for App Lock and Encrypted Private Vault.
 */
@Composable
fun TactilePinPad(
    title: String,
    subtitle: String,
    expectedPin: String,
    onUnlocked: () -> Unit,
    onBiometricClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var enteredPin by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    fun appendDigit(digit: String) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        hasError = false
        if (enteredPin.length < expectedPin.length.coerceAtLeast(4)) {
            enteredPin += digit
            if (enteredPin == expectedPin) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onUnlocked()
            } else if (enteredPin.length >= expectedPin.length) {
                hasError = true
                enteredPin = ""
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (hasError) "Incorrect PIN. Try again (default: $expectedPin)." else subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Animated PIN Dots
        val dotCount = expectedPin.length.coerceAtLeast(4)
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            repeat(dotCount) { idx ->
                val filled = idx < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                hasError -> MaterialTheme.colorScheme.error
                                filled -> MaterialTheme.colorScheme.primary
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }

        // 3x4 Tactile Keypad
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("BIO", "0", "DEL")
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        Surface(
                            onClick = {
                                when (key) {
                                    "BIO" -> onBiometricClick()
                                    "DEL" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                    else -> appendDigit(key)
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("pin_key_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (key) {
                                    "BIO" -> Icon(Icons.Outlined.Fingerprint, contentDescription = "Biometric unlock", tint = MaterialTheme.colorScheme.primary)
                                    "DEL" -> Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Backspace", modifier = Modifier.size(20.dp))
                                    else -> Text(key, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Freehand & Typed Signature Pad Modal Dialog.
 */
@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSaveSignature: (label: String, typedText: String, strokePointsJson: String) -> Unit
) {
    var signerName by remember { mutableStateOf("Alex Morgan") }
    val drawnPoints = remember { mutableStateListOf<Offset>() }
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Digital Signature", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = signerName,
                    onValueChange = { signerName = it },
                    label = { Text("Signer Name / Initials") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Draw your signature below with finger or stylus:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFAF8F4))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { pt -> drawnPoints.add(pt) },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        drawnPoints.add(change.position)
                                    }
                                )
                            }
                    ) {
                        // Baseline rule
                        drawLine(
                            color = Color(0xFFD5D0C5),
                            start = Offset(24.dp.toPx(), size.height * 0.75f),
                            end = Offset(size.width - 24.dp.toPx(), size.height * 0.75f),
                            strokeWidth = 1.dp.toPx()
                        )
                        if (drawnPoints.size > 1) {
                            val path = Path()
                            drawnPoints.forEachIndexed { idx, pt ->
                                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                            }
                            drawPath(path, color = primaryColor, style = Stroke(width = 3.dp.toPx()))
                        }
                    }
                    TextButton(
                        onClick = { drawnPoints.clear() },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("Clear")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val serialized = drawnPoints.take(40).joinToString(";") { "${it.x},${it.y}" }
                    onSaveSignature("Signature", signerName.ifBlank { "Authorized Signer" }, serialized)
                    onDismiss()
                }
            ) {
                Text("Save Signature")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Segmented visual storage bar for Settings Storage & Cache Manager.
 */
@Composable
fun SegmentedStorageBar(
    breakdown: VaultCryptoManager.StorageBreakdown,
    modifier: Modifier = Modifier
) {
    val total = breakdown.totalBytes.coerceAtLeast(1L).toFloat()
    val libFraction = (breakdown.libraryBytes / total).coerceIn(0.08f, 0.8f)
    val outFraction = (breakdown.outputBytes / total).coerceIn(0.05f, 0.8f)
    val vaultFraction = (breakdown.vaultBytes / total).coerceIn(0.04f, 0.8f)
    val cacheFraction = (breakdown.cacheAndTempBytes / total).coerceIn(0.04f, 0.8f)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(Modifier.weight(libFraction).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            Box(Modifier.weight(outFraction).fillMaxHeight().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)))
            Box(Modifier.weight(vaultFraction).fillMaxHeight().background(MaterialTheme.colorScheme.onSurfaceVariant))
            Box(Modifier.weight(cacheFraction).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StorageLegendItem("Library", "${breakdown.libraryBytes / 1024} KB", MaterialTheme.colorScheme.primary)
            StorageLegendItem("Outputs", "${breakdown.outputBytes / 1024} KB", MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
            StorageLegendItem("Vault", "${breakdown.vaultBytes / 1024} KB", MaterialTheme.colorScheme.onSurfaceVariant)
            StorageLegendItem("Cache", "${breakdown.cacheAndTempBytes / 1024} KB", MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun StorageLegendItem(label: String, sizeText: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sizeText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
