package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.core.SampleSeedGenerator
import com.example.core.VaultCryptoManager
import com.example.domain.model.PdfToolId
import com.example.domain.model.ToolConfiguration
import com.example.tools.PdfToolkitEngine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun verifyAppNameAndZeroNetworkPermissions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Folio", context.getString(R.string.app_name))

        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requestedPermissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()
        assertFalse(
            "INTERNET permission must NOT be declared",
            requestedPermissions.contains(android.Manifest.permission.INTERNET)
        )
        assertFalse(
            "ACCESS_NETWORK_STATE permission must NOT be declared",
            requestedPermissions.contains(android.Manifest.permission.ACCESS_NETWORK_STATE)
        )
    }

    @Test
    fun verifyNonDestructivePdfMergeAndRotate() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SampleSeedGenerator.ensurePdfBoxInitialized(context)

        val tempDir = File(context.cacheDir, "test_pdfs").apply { mkdirs() }
        val pdf1 = File(tempDir, "doc1.pdf")
        val pdf2 = File(tempDir, "doc2.pdf")

        PDDocument().use { doc ->
            doc.addPage(PDPage())
            doc.addPage(PDPage())
            doc.save(pdf1)
        }
        PDDocument().use { doc ->
            doc.addPage(PDPage())
            doc.save(pdf2)
        }
        val origLength1 = pdf1.length()

        // 1. Test Merge PDF
        val mergeResult = PdfToolkitEngine.executeTool(
            context = context,
            tool = PdfToolId.MERGE_PDF,
            inputFiles = listOf(pdf1, pdf2),
            config = ToolConfiguration()
        )
        assertTrue("Merge PDF failed: ${mergeResult.errorMessage}", mergeResult.success)
        assertEquals(3, mergeResult.pagesProcessed)
        assertEquals("Original file must remain untouched", origLength1, pdf1.length())

        // 2. Test Rotate PDF
        val rotateResult = PdfToolkitEngine.executeTool(
            context = context,
            tool = PdfToolId.ROTATE_PDF,
            inputFiles = listOf(File(mergeResult.primaryOutputPath)),
            config = ToolConfiguration(rotateDegrees = 90)
        )
        assertTrue("Rotate PDF failed: ${rotateResult.errorMessage}", rotateResult.success)
        assertTrue("Rotated output file must exist", File(rotateResult.primaryOutputPath).exists())
    }

    @Test
    fun verifyVaultEncryptionRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val plainFile = File(context.cacheDir, "secret_note.txt")
        plainFile.writeText("Confidential Offline Payload 2026")

        val encrypted = VaultCryptoManager.encryptFileToVault(context, plainFile, "1234")
        assertTrue("Encrypted vault file must exist", encrypted.exists())

        val decrypted = VaultCryptoManager.decryptVaultFileToTemp(context, encrypted, "txt", "1234")
        assertEquals("Confidential Offline Payload 2026", decrypted.readText())
    }

    @Test
    fun verifyRuntimeReleaseIntegrityAuditAndVersioning() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report = com.example.ui.inspectRuntimeReleaseIntegrity(context)

        assertTrue(
            "Runtime offline audit must confirm strictly offline APK (permissions=${report.requestedPermissions})",
            report.isStrictlyOffline
        )
        assertFalse(report.hasInternetPermission)
        assertFalse(report.hasNetworkStatePermission)
        assertTrue("Version name must be populated", report.versionName.isNotBlank())
        assertTrue("Version code must be >= 1", report.versionCode >= 1)
    }

    @Test
    fun verifyNonPdfAutoConversionInMergeAndSignTools() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SampleSeedGenerator.ensurePdfBoxInitialized(context)

        val tempDir = File(context.cacheDir, "test_mixed_formats").apply { mkdirs() }
        val mdFile = File(tempDir, "notes.md").apply {
            writeText("# Project Summary\n\nAll non-PDF files auto-convert cleanly.")
        }
        val csvFile = File(tempDir, "budget.csv").apply {
            writeText("Department,Allocation\nEngineering,120000\nDesign,85000")
        }

        // Merge a Markdown and CSV file directly in MERGE_PDF
        val mergeResult = PdfToolkitEngine.executeTool(
            context = context,
            tool = PdfToolId.MERGE_PDF,
            inputFiles = listOf(mdFile, csvFile),
            config = ToolConfiguration()
        )
        assertTrue("Merging .md and .csv must succeed via auto-conversion: ${mergeResult.errorMessage}", mergeResult.success)
        assertTrue("Merged PDF output must exist", File(mergeResult.primaryOutputPath).exists())

        // Sign a Markdown file directly in SIGN_PDF
        val signResult = PdfToolkitEngine.executeTool(
            context = context,
            tool = PdfToolId.SIGN_PDF,
            inputFiles = listOf(mdFile),
            config = ToolConfiguration(signatureText = "Approved by User")
        )
        assertTrue("Signing .md file must succeed via auto-conversion: ${signResult.errorMessage}", signResult.success)
        assertTrue("Signed PDF output must exist", File(signResult.primaryOutputPath).exists())
    }

    @Test
    fun verifyUnicodeAndBulletPointsDoNotCrashPdfConversion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SampleSeedGenerator.ensurePdfBoxInitialized(context)

        val tempDir = File(context.cacheDir, "test_unicode").apply { mkdirs() }
        // File with non-WinAnsi characters (bullets •, em-dashes —, curly quotes “”, non-ASCII)
        val unicodeFile = File(tempDir, "Report_•_2026_—_Final.txt").apply {
            writeText("• Executive Summary — 2026\n• Item 1: “Smart Scan” & PDF Merge\n• Status: ✓ Completed")
        }

        val result = PdfToolkitEngine.executeTool(
            context = context,
            tool = PdfToolId.TEXT_WEB_TO_PDF,
            inputFiles = listOf(unicodeFile),
            config = ToolConfiguration()
        )
        assertTrue("Unicode filename and bullet text must not crash PDFBox: ${result.errorMessage}", result.success)
        assertTrue("Generated PDF must exist", File(result.primaryOutputPath).exists())
    }

    @Test
    fun verifyLargeCameraImageDownsamplingAndScannerSave() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SampleSeedGenerator.ensurePdfBoxInitialized(context)

        // Create a large 2400x3200 bitmap to verify downsampling and scan filter never OOM or exceed Canvas limits
        val largeBmp = android.graphics.Bitmap.createBitmap(2400, 3200, android.graphics.Bitmap.Config.ARGB_8888)
        val filtered = com.example.tools.applyScanFilter(
            source = largeBmp,
            filter = com.example.tools.ScanFilterMode.ENHANCED,
            cropMargins = true
        )
        assertTrue("Filtered scan bitmap width must be downsampled <= 1600px", filtered.width <= 1600)
        assertTrue("Filtered scan bitmap height must be downsampled <= 1600px", filtered.height <= 1600)

        val savedFiles = com.example.tools.saveScannedBitmapsToDisk(context, listOf(filtered))
        assertEquals(1, savedFiles.size)
        assertTrue(savedFiles.first().exists())

        val scanPdfResult = PdfToolkitEngine.executeTool(
            context = context,
            tool = PdfToolId.SCAN_TO_PDF,
            inputFiles = savedFiles,
            config = ToolConfiguration()
        )
        assertTrue("SCAN_TO_PDF must succeed: ${scanPdfResult.errorMessage}", scanPdfResult.success)
        assertTrue(File(scanPdfResult.primaryOutputPath).exists())
    }
}

