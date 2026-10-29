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
}

