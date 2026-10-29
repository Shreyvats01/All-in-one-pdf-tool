package com.example.core

import android.content.Context
import com.example.data.local.FolioRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

object SampleSeedGenerator {

    private var pdfBoxInitialized = false

    private val LEGACY_DUMMY_FILES = setOf(
        "Folio_Architecture_Guide.pdf",
        "Quarterly_Financial_Report.pdf",
        "Field_Research_Handbook.epub",
        "Service_Agreement_Draft.docx",
        "Release_Notes_2026.md",
        "Department_Budget_Matrix.csv"
    )

    private val LEGACY_DUMMY_PRESETS = setOf(
        "Email Ready Compress (Medium)",
        "Confidential Tiled Watermark",
        "Booklet 2-Up Print Layout"
    )

    fun ensurePdfBoxInitialized(context: Context) {
        if (!pdfBoxInitialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            pdfBoxInitialized = true
        }
    }

    /**
     * Initializes PDFBox, creates required local directories, purges expired trash,
     * and removes any legacy dummy sample files/presets so the workspace only contains real user files.
     */
    suspend fun initializeWorkspaceAndPurgeDummyFiles(
        context: Context,
        repository: FolioRepository
    ) = withContext(Dispatchers.IO) {
        ensurePdfBoxInitialized(context)
        repository.purgeTrashOlderThan30Days()

        val libDir = File(context.filesDir, "library").apply { mkdirs() }
        File(context.filesDir, "outputs").mkdirs()

        // Purge any legacy pre-seeded dummy files from Room and disk
        val allDocs = repository.libraryDocuments.first() + repository.trashedDocuments.first()
        allDocs.forEach { doc ->
            if (doc.displayName in LEGACY_DUMMY_FILES) {
                runCatching { File(doc.filePath).delete() }
                repository.deletePermanently(doc.id)
            }
        }
        LEGACY_DUMMY_FILES.forEach { dummyName ->
            runCatching { File(libDir, dummyName).delete() }
        }

        // Purge any legacy pre-seeded dummy presets
        val existingPresets = repository.toolPresets.first()
        existingPresets.forEach { preset ->
            if (preset.name in LEGACY_DUMMY_PRESETS) {
                repository.deletePreset(preset.id)
            }
        }
    }

    suspend fun seedIfNeeded(context: Context, repository: FolioRepository) {
        initializeWorkspaceAndPurgeDummyFiles(context, repository)
    }
}
