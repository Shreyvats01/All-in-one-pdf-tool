package com.example.core

import android.content.Context
import com.example.data.local.DocumentEntity
import com.example.data.local.FolioRepository
import com.example.data.local.SignatureEntity
import com.example.data.local.ToolPresetEntity
import com.example.domain.model.DocumentFormat
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleSeedGenerator {

    private var pdfBoxInitialized = false

    fun ensurePdfBoxInitialized(context: Context) {
        if (!pdfBoxInitialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            pdfBoxInitialized = true
        }
    }

    suspend fun seedIfNeeded(context: Context, repository: FolioRepository) = withContext(Dispatchers.IO) {
        ensurePdfBoxInitialized(context)
        repository.purgeTrashOlderThan30Days()

        val existingDocs = repository.libraryDocuments.first()
        if (existingDocs.isNotEmpty()) return@withContext

        val libDir = File(context.filesDir, "library").apply { mkdirs() }
        File(context.filesDir, "outputs").mkdirs()

        // 1. Seed folders & colour-free tags
        val workFolderId = repository.createFolder("Work & Architecture")
        val financeFolderId = repository.createFolder("Financial Reports")
        val researchFolderId = repository.createFolder("Field Research")

        repository.createTag("essential")
        repository.createTag("review")
        repository.createTag("contract")
        repository.createTag("archive")

        // 2. Generate real 6-page PDF: Folio_Architecture_Guide.pdf
        val archFile = File(libDir, "Folio_Architecture_Guide.pdf")
        val archSummary = buildArchitectureGuidePdf(archFile)
        repository.upsertDocument(
            DocumentEntity(
                displayName = "Folio_Architecture_Guide.pdf",
                filePath = archFile.absolutePath,
                formatName = DocumentFormat.PDF.name,
                sizeBytes = archFile.length(),
                pageCount = 6,
                lastPage = 1,
                isFavourite = true,
                folderId = workFolderId,
                tagsCsv = "essential,review",
                indexedContent = archSummary
            )
        )

        // 3. Generate real 4-page PDF: Quarterly_Financial_Report.pdf
        val finFile = File(libDir, "Quarterly_Financial_Report.pdf")
        val finSummary = buildFinancialReportPdf(finFile)
        repository.upsertDocument(
            DocumentEntity(
                displayName = "Quarterly_Financial_Report.pdf",
                filePath = finFile.absolutePath,
                formatName = DocumentFormat.PDF.name,
                sizeBytes = finFile.length(),
                pageCount = 4,
                lastPage = 0,
                isFavourite = true,
                folderId = financeFolderId,
                tagsCsv = "contract,archive",
                indexedContent = finSummary
            )
        )

        // 4. Generate real valid EPUB eBook: Field_Research_Handbook.epub
        val epubFile = File(libDir, "Field_Research_Handbook.epub")
        val epubText = buildSampleEpub(epubFile)
        repository.upsertDocument(
            DocumentEntity(
                displayName = "Field_Research_Handbook.epub",
                filePath = epubFile.absolutePath,
                formatName = DocumentFormat.EPUB.name,
                sizeBytes = epubFile.length(),
                pageCount = 3,
                folderId = researchFolderId,
                tagsCsv = "essential",
                indexedContent = epubText
            )
        )

        // 5. Generate real valid DOCX document: Service_Agreement_Draft.docx
        val docxFile = File(libDir, "Service_Agreement_Draft.docx")
        val docxText = buildSampleDocx(docxFile)
        repository.upsertDocument(
            DocumentEntity(
                displayName = "Service_Agreement_Draft.docx",
                filePath = docxFile.absolutePath,
                formatName = DocumentFormat.DOCX.name,
                sizeBytes = docxFile.length(),
                pageCount = 2,
                folderId = workFolderId,
                tagsCsv = "contract,review",
                indexedContent = docxText
            )
        )

        // 6. Generate Markdown & CSV files
        val mdFile = File(libDir, "Release_Notes_2026.md")
        val mdContent = """
            # Folio Offline Document Suite — Release Notes
            
            ## 1. Zero-Network Architecture
            Folio is engineered to operate 100% on-device. Neither `INTERNET` nor `ACCESS_NETWORK_STATE` permissions are requested in the AndroidManifest.
            
            ## 2. Universal Reader Capabilities
            - Crisp tiled rendering with up to 800% zoom
            - Reflow mode with custom typography and line spacing
            - Full-text search with hit counters and match highlighting
            - Offline Text-to-Speech (TTS) with speed control
            
            ## 3. Non-Destructive PDF Toolkit
            Every tool writes a new output file in your workspace without modifying the original source document.
        """.trimIndent()
        mdFile.writeText(mdContent)
        repository.upsertDocument(
            DocumentEntity(
                displayName = "Release_Notes_2026.md",
                filePath = mdFile.absolutePath,
                formatName = DocumentFormat.MD.name,
                sizeBytes = mdFile.length(),
                pageCount = 1,
                folderId = researchFolderId,
                tagsCsv = "archive",
                indexedContent = mdContent
            )
        )

        val csvFile = File(libDir, "Department_Budget_Matrix.csv")
        val csvContent = """
            Department,Q1 Allocation,Q2 Allocation,Q3 Allocation,Q4 Allocation,Status
            Core R&D,${'$'}145000,${'$'}152000,${'$'}160000,${'$'}168000,Approved
            Security Audit,${'$'}48000,${'$'}52000,${'$'}49000,${'$'}55000,Approved
            Design Systems,${'$'}64000,${'$'}68000,${'$'}71000,${'$'}74000,Review
            Field Operations,${'$'}92000,${'$'}88000,${'$'}95000,${'$'}102000,Approved
        """.trimIndent()
        csvFile.writeText(csvContent)
        repository.upsertDocument(
            DocumentEntity(
                displayName = "Department_Budget_Matrix.csv",
                filePath = csvFile.absolutePath,
                formatName = DocumentFormat.CSV.name,
                sizeBytes = csvFile.length(),
                pageCount = 1,
                folderId = financeFolderId,
                tagsCsv = "review",
                indexedContent = csvContent
            )
        )

        // 7. Seed Reusable Tool Presets
        repository.savePreset(
            ToolPresetEntity(
                name = "Email Ready Compress (Medium)",
                toolIdName = "COMPRESS_PDF",
                configSummary = "Medium preset • 150 DPI downsample • Strip unused objects",
                configJson = "compression=MEDIUM;dpi=150"
            )
        )
        repository.savePreset(
            ToolPresetEntity(
                name = "Confidential Tiled Watermark",
                toolIdName = "WATERMARK_PDF",
                configSummary = "Text: CONFIDENTIAL • Opacity 25% • -35° Tiled",
                configJson = "text=CONFIDENTIAL;opacity=0.25;tiled=true"
            )
        )
        repository.savePreset(
            ToolPresetEntity(
                name = "Booklet 2-Up Print Layout",
                toolIdName = "RESIZE_NUP_BOOKLET",
                configSummary = "2 pages per sheet • Landscape imposition",
                configJson = "nup=2;booklet=true"
            )
        )

        // 8. Seed Default Saved Signature
        repository.saveSignature(
            SignatureEntity(
                label = "Executive Signature",
                isInitials = false,
                typedText = "Alex Morgan",
                strokePointsJson = "0.10,0.65;0.22,0.35;0.30,0.60;0.45,0.40;0.62,0.58;0.85,0.45"
            )
        )
    }

    private fun buildArchitectureGuidePdf(targetFile: File): String {
        val doc = PDDocument()
        val fullText = StringBuilder()
        val chapters = listOf(
            "Chapter 1: Executive Overview" to listOf(
                "Folio is a 100% offline document reader and comprehensive PDF studio.",
                "All rendering, conversion, encryption, and OCR run locally on your device.",
                "Original source files are never modified; tools always create a new output file.",
                "Use the 3-step workflow: Pick files -> Configure -> Result."
            ),
            "Chapter 2: Universal Reader Engine" to listOf(
                "Supports PDF, EPUB, DOCX, XLSX, PPTX, ODT, RTF, TXT, MD, CSV, HTML, CBZ, XPS.",
                "Pinch to zoom up to 800% with crisp on-demand region rendering.",
                "Switch seamlessly between Vertical, Horizontal, Single, Two-Page, and Reflow modes.",
                "Includes Day, Night (Inverted), and Sepia reading themes plus Offline TTS."
            ),
            "Chapter 3: Non-Destructive PDF Organization" to listOf(
                "Merge multiple PDFs with drag-to-reorder and custom per-file page ranges.",
                "Split documents by range, every N pages, max file size, or outline bookmarks.",
                "Organize pages visually: reorder, rotate, duplicate, delete, or insert blank pages.",
                "Scan physical paperwork with CameraX edge detection and perspective correction."
            ),
            "Chapter 4: Optimization, Repair & Offline OCR" to listOf(
                "Compress PDFs with Low, Medium, Extreme presets or a custom KB target.",
                "Repair corrupted cross-reference tables and damaged PDF object streams.",
                "Run offline OCR with English + Latin recognition to add a searchable text layer."
            ),
            "Chapter 5: Security, Redaction & Digital Signatures" to listOf(
                "Protect PDFs with AES-256 open and permission passwords.",
                "Permanently redact confidential text and regions with structural stream purging.",
                "Sign documents with drawn/typed signatures and local PKCS#12 cryptographic digests."
            ),
            "Chapter 6: Interactive Form Verification Sheet" to listOf(
                "Form Field [Project_Code]: FOLIO-2026-OFFLINE",
                "Form Field [Security_Clearance]: Internal Confidential",
                "Form Field [Reviewer_Status]: Approved for Production",
                "Confidential Reference ID: SSN-990-44-1029 (Test Redaction Target)"
            )
        )

        val outline = PDDocumentOutline()
        doc.documentCatalog.documentOutline = outline

        chapters.forEachIndexed { index, (title, lines) ->
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)

            val bookmark = PDOutlineItem()
            bookmark.title = title
            val dest = PDPageFitWidthDestination()
            dest.page = page
            bookmark.destination = dest
            outline.addLast(bookmark)

            PDPageContentStream(doc, page).use { cs ->
                // Header bar
                cs.setNonStrokingColor(241, 239, 234)
                cs.addRect(36f, 760f, 523f, 44f)
                cs.fill()

                cs.beginText()
                cs.setNonStrokingColor(79, 91, 213)
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11f)
                cs.newLineAtOffset(48f, 778f)
                cs.showText("FOLIO ARCHITECTURAL HANDBOOK • EDITION 2026")
                cs.endText()

                // Chapter Title
                cs.beginText()
                cs.setNonStrokingColor(24, 24, 27)
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20f)
                cs.newLineAtOffset(48f, 710f)
                cs.showText(title)
                cs.endText()

                // Hairline divider
                cs.setStrokingColor(214, 211, 201)
                cs.setLineWidth(1f)
                cs.moveTo(48f, 692f)
                cs.lineTo(545f, 692f)
                cs.stroke()

                var y = 655f
                lines.forEach { paragraph ->
                    cs.beginText()
                    cs.setNonStrokingColor(38, 40, 45)
                    cs.setFont(PDType1Font.HELVETICA, 13f)
                    cs.newLineAtOffset(48f, y)
                    cs.showText("• $paragraph")
                    cs.endText()
                    y -= 34f
                    fullText.append(paragraph).append(" ")
                }

                // Visual architectural box on each page
                cs.setNonStrokingColor(224, 228, 255)
                cs.addRect(48f, 340f, 497f, 120f)
                cs.fill()

                cs.beginText()
                cs.setNonStrokingColor(25, 33, 108)
                cs.setFont(PDType1Font.HELVETICA_BOLD, 13f)
                cs.newLineAtOffset(68f, 420f)
                cs.showText("Offline Verification Node #${index + 1}")
                cs.endText()

                cs.beginText()
                cs.setNonStrokingColor(40, 45, 90)
                cs.setFont(PDType1Font.HELVETICA, 11f)
                cs.newLineAtOffset(68f, 392f)
                cs.showText("Zero network calls • Non-destructive output • Scoped Storage compliant")
                cs.endText()

                // Footer
                cs.beginText()
                cs.setNonStrokingColor(110, 114, 125)
                cs.setFont(PDType1Font.HELVETICA, 10f)
                cs.newLineAtOffset(265f, 42f)
                cs.showText("Page ${index + 1} of ${chapters.size}")
                cs.endText()
            }
            fullText.append(title).append(" ")
        }

        doc.documentInformation.title = "Folio Architecture Guide"
        doc.documentInformation.author = "Folio Systems Engineering"
        doc.documentInformation.subject = "Offline Document Reader & PDF Toolkit Reference"
        doc.documentInformation.keywords = "PDF, Offline, Reader, OCR, Security, Architecture"

        doc.save(targetFile)
        doc.close()
        return fullText.toString()
    }

    private fun buildFinancialReportPdf(targetFile: File): String {
        val doc = PDDocument()
        val summary = StringBuilder()
        val pages = listOf(
            "Q1-Q4 Consolidated Revenue Summary" to listOf(
                "Total Annual Recurring Revenue: $14,820,000 (+24.6% YoY)",
                "Operating Margin: 31.4% across all regional divisions",
                "Gross Retention Rate: 96.2% with zero cloud infrastructure overhead",
                "Audit Status: Verified by Internal Compliance Committee"
            ),
            "Departmental Expense Allocation" to listOf(
                "Core Document Engine R&D: $3,420,000",
                "Offline OCR & Computer Vision: $1,890,000",
                "Security & Cryptographic Vault: $1,120,000",
                "Quality Assurance & Accessibility: $940,000"
            ),
            "Regional Performance Breakdown" to listOf(
                "North America Enterprise Licensing: $6,500,000",
                "European Union Privacy-First Sector: $5,120,000",
                "Asia-Pacific Mobile Field Teams: $3,200,000"
            ),
            "Sign-Off & Executive Attestation" to listOf(
                "Prepared by: Office of the Chief Financial Officer",
                "Secret Audit Token: SECRET-FIN-8841 (Use Redact tool to purge)",
                "Signature Line: _______________________________"
            )
        )

        pages.forEachIndexed { idx, (heading, rows) ->
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                cs.beginText()
                cs.setNonStrokingColor(79, 91, 213)
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12f)
                cs.newLineAtOffset(48f, 775f)
                cs.showText("QUARTERLY FINANCIAL REPORT • FY 2026")
                cs.endText()

                cs.beginText()
                cs.setNonStrokingColor(24, 24, 27)
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18f)
                cs.newLineAtOffset(48f, 735f)
                cs.showText(heading)
                cs.endText()

                var y = 680f
                rows.forEach { row ->
                    cs.setNonStrokingColor(243, 241, 236)
                    cs.addRect(48f, y - 10f, 495f, 32f)
                    cs.fill()

                    cs.beginText()
                    cs.setNonStrokingColor(30, 32, 36)
                    cs.setFont(PDType1Font.HELVETICA, 12f)
                    cs.newLineAtOffset(60f, y)
                    cs.showText(row)
                    cs.endText()
                    y -= 44f
                    summary.append(row).append(" ")
                }

                cs.beginText()
                cs.setNonStrokingColor(120, 120, 128)
                cs.setFont(PDType1Font.HELVETICA, 10f)
                cs.newLineAtOffset(260f, 40f)
                cs.showText("Financial Report • Page ${idx + 1}")
                cs.endText()
            }
            summary.append(heading).append(" ")
        }

        doc.documentInformation.title = "Quarterly Financial Report FY2026"
        doc.documentInformation.author = "Finance Division"
        doc.save(targetFile)
        doc.close()
        return summary.toString()
    }

    private fun buildSampleEpub(targetFile: File): String {
        val chapters = listOf(
            "Chapter 1: Field Readiness" to "Welcome to the Field Research Handbook. This EPUB document is rendered 100% offline with support for reflowable typography, adjustable margins, line height, and offline text-to-speech.",
            "Chapter 2: Document Preservation" to "When archiving physical records in the field, use the Scan to PDF tool with perspective correction and B&W High Contrast filtering, followed by PDF/A archival export.",
            "Chapter 3: Cryptographic Security" to "Sensitive field notes should be protected with AES-256 encryption or stored inside Folio's Biometric Private Vault."
        )
        ZipOutputStream(FileOutputStream(targetFile)).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(
                """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
                """.trimIndent().toByteArray()
            )
            zip.closeEntry()

            chapters.forEachIndexed { i, (title, body) ->
                zip.putNextEntry(ZipEntry("OEBPS/chapter${i + 1}.xhtml"))
                zip.write(
                    """
                    <html xmlns="http://www.w3.org/1999/xhtml">
                    <head><title>$title</title></head>
                    <body>
                      <h1>$title</h1>
                      <p>$body</p>
                    </body>
                    </html>
                    """.trimIndent().toByteArray()
                )
                zip.closeEntry()
            }
        }
        return chapters.joinToString(" ") { "${it.first} ${it.second}" }
    }

    private fun buildSampleDocx(targetFile: File): String {
        val text1 = "MASTER SERVICE AGREEMENT — OFFLINE EDITION"
        val text2 = "1. Scope of Services: Provider shall deliver complete offline document management capabilities with zero external network transmission."
        val text3 = "2. Data Sovereignty: All files remain strictly under local device custody. Original files are never modified in place."
        val text4 = "3. Term & Signatures: Effective immediately upon digital or handwritten signature execution."
        ZipOutputStream(FileOutputStream(targetFile)).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            val xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>$text1</w:t></w:r></w:p>
                    <w:p><w:r><w:t>$text2</w:t></w:r></w:p>
                    <w:p><w:r><w:t>$text3</w:t></w:r></w:p>
                    <w:p><w:r><w:t>$text4</w:t></w:r></w:p>
                  </w:body>
                </w:document>
            """.trimIndent()
            zip.write(xml.toByteArray())
            zip.closeEntry()
        }
        return "$text1 $text2 $text3 $text4"
    }
}
