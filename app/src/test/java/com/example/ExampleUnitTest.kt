package com.example

import com.example.domain.model.CompressionLevel
import com.example.domain.model.DocumentFormat
import com.example.domain.model.PdfToolId
import com.example.domain.model.ToolConfiguration
import com.example.tools.PdfToolkitEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun documentFormat_detectsAllSupportedFormats() {
        assertEquals(DocumentFormat.PDF, DocumentFormat.fromFilename("contract.pdf"))
        assertEquals(DocumentFormat.EPUB, DocumentFormat.fromFilename("book.epub"))
        assertEquals(DocumentFormat.DOCX, DocumentFormat.fromFilename("report.docx"))
        assertEquals(DocumentFormat.XLSX, DocumentFormat.fromFilename("budget.xlsx"))
        assertEquals(DocumentFormat.PPTX, DocumentFormat.fromFilename("slides.pptx"))
        assertEquals(DocumentFormat.MD, DocumentFormat.fromFilename("notes.md"))
        assertEquals(DocumentFormat.CSV, DocumentFormat.fromFilename("data.csv"))
        assertEquals(DocumentFormat.CBZ, DocumentFormat.fromFilename("comic.cbz"))
        assertEquals(DocumentFormat.IMAGE, DocumentFormat.fromFilename("scan.tiff"))
    }

    @Test
    fun parsePageIndices_handlesRangesAndListsCorrectly() {
        val allPages = PdfToolkitEngine.parsePageIndices("All", 6)
        assertEquals(setOf(0, 1, 2, 3, 4, 5), allPages)

        val customRange = PdfToolkitEngine.parsePageIndices("1-3, 5", 6)
        assertEquals(setOf(0, 1, 2, 4), customRange)
    }

    @Test
    fun allThirtyOnePdfTools_areRegistered() {
        assertTrue(PdfToolId.entries.size >= 31)
        val config = ToolConfiguration(compressionLevel = CompressionLevel.EXTREME)
        assertEquals(CompressionLevel.EXTREME, config.compressionLevel)
    }
}
