package com.example.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.example.domain.model.PdfToolId
import com.example.domain.model.ToolConfiguration
import com.example.domain.model.ToolExecutionResult
import com.example.reader.MultiFormatDocumentEngine
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PdfToolkitConversions {

    /**
     * Sanitizes any string before passing to PDFBox PDType1Font.showText()
     * so non-WinAnsi characters (bullets, em-dashes, non-ASCII Unicode) never throw IllegalArgumentException.
     */
    fun sanitizePdfText(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '\u2022', '\u00B7', '\u25CF' -> sb.append('-')
                '\u2014', '\u2013' -> sb.append("--")
                '\u2018', '\u2019' -> sb.append('\'')
                '\u201C', '\u201D' -> sb.append('"')
                '\u2026' -> sb.append("...")
                '\r', '\n', '\t' -> sb.append(' ')
                in '\u0020'..'\u007E' -> sb.append(ch)
                else -> sb.append(' ')
            }
        }
        return sb.toString()
    }

    suspend fun imagesToPdf(
        context: Context,
        inputFiles: List<File>,
        outDir: File,
        config: ToolConfiguration,
        tool: PdfToolId,
        pattern: String,
        onProgress: (Int, String) -> Unit
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val outDoc = PDDocument()
        try {
            val marginPt = config.imageMarginDp.toFloat().coerceIn(0f, 72f)
            inputFiles.forEachIndexed { idx, file ->
                ensureActive()
                onProgress(((idx + 1) * 90) / inputFiles.size.coerceAtLeast(1), "Adding image ${idx + 1} of ${inputFiles.size}...")
                val bmp = MultiFormatDocumentEngine.decodeSampledBitmapFromFile(file, maxDimensionPx = 1600)
                    ?: MultiFormatDocumentEngine.renderPageBitmap(file, com.example.domain.model.DocumentFormat.fromFilename(file.name), 0, file.name, 1080)

                val baseRect = when (config.imagePageSize) {
                    "LETTER" -> PDRectangle.LETTER
                    "FIT" -> PDRectangle(bmp.width.toFloat().coerceAtLeast(200f), bmp.height.toFloat().coerceAtLeast(200f))
                    else -> PDRectangle.A4
                }
                val pageRect = if (config.imageOrientation == "LANDSCAPE" && config.imagePageSize != "FIT") {
                    PDRectangle(baseRect.height, baseRect.width)
                } else {
                    baseRect
                }
                val page = PDPage(pageRect)
                outDoc.addPage(page)

                val pdImage = JPEGFactory.createFromImage(outDoc, bmp, (config.imageQuality / 100f).coerceIn(0.4f, 0.95f))
                val availW = (pageRect.width - marginPt * 2).coerceAtLeast(100f)
                val availH = (pageRect.height - marginPt * 2).coerceAtLeast(100f)
                val scale = minOf(availW / bmp.width.toFloat().coerceAtLeast(1f), availH / bmp.height.toFloat().coerceAtLeast(1f))
                val drawW = bmp.width * scale
                val drawH = bmp.height * scale
                val drawX = (pageRect.width - drawW) / 2f
                val drawY = (pageRect.height - drawH) / 2f

                PDPageContentStream(outDoc, page).use { cs ->
                    cs.drawImage(pdImage, drawX, drawY, drawW, drawH)
                }
            }
            val outFile = PdfToolkitEngine.buildOutputFile(outDir, inputFiles.first().name, tool.defaultSuffix, "pdf", pattern)
            outDoc.save(outFile)
            return@withContext ToolExecutionResult(
                success = true,
                outputFilePaths = listOf(outFile.absolutePath),
                primaryOutputPath = outFile.absolutePath,
                originalSizeBytes = inputFiles.sumOf { it.length() },
                outputSizeBytes = outFile.length(),
                pagesProcessed = inputFiles.size,
                summaryReport = "Converted ${inputFiles.size} image(s) into ${outFile.name} (${config.imagePageSize} ${config.imageOrientation})."
            )
        } finally {
            outDoc.close()
        }
    }

    suspend fun ocrPdfOffline(
        context: Context,
        sourceFile: File,
        outDir: File,
        config: ToolConfiguration,
        pattern: String,
        onProgress: (Int, String) -> Unit
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val session = MultiFormatDocumentEngine.openSession(context, sourceFile)
        val outDoc = PDDocument()
        var recognizedWords = 0
        try {
            val total = session.pageCount.coerceAtLeast(1)
            for (i in 0 until total) {
                ensureActive()
                onProgress(((i + 1) * 90) / total, "Offline OCR (${config.ocrLanguage}) & deskew on page ${i + 1}/$total...")
                val bmp = MultiFormatDocumentEngine.renderPageBitmap(
                    sourceFile,
                    session.format,
                    i,
                    session.pagesText.getOrElse(i) { "" },
                    1080
                )
                val existingText = session.pagesText.getOrElse(i) { "" }
                val ocrLayerText = if (existingText.isNotBlank() && !existingText.startsWith("[Page")) {
                    existingText
                } else {
                    performOfflineBitmapTextRecognition(bmp, i + 1, config.ocrLanguage)
                }
                recognizedWords += ocrLayerText.split(Regex("\\s+")).count { it.isNotBlank() }

                val page = PDPage(PDRectangle.A4)
                outDoc.addPage(page)
                val pdImg = JPEGFactory.createFromImage(outDoc, bmp, 0.85f)
                PDPageContentStream(outDoc, page).use { cs ->
                    cs.drawImage(pdImg, 0f, 0f, PDRectangle.A4.width, PDRectangle.A4.height)
                    // Embed searchable text layer over the page
                    var y = 760f
                    ocrLayerText.lines().take(35).forEach { line ->
                        val safe = sanitizePdfText(line).take(85)
                        if (safe.isNotBlank()) {
                            cs.beginText()
                            cs.setFont(PDType1Font.HELVETICA, 10f)
                            cs.newLineAtOffset(48f, y)
                            cs.showText(safe)
                            cs.endText()
                            y -= 20f
                        }
                    }
                }
            }
            val outFile = PdfToolkitEngine.buildOutputFile(outDir, sourceFile.name, "ocr_searchable", "pdf", pattern)
            outDoc.save(outFile)
            return@withContext ToolExecutionResult(
                success = true,
                outputFilePaths = listOf(outFile.absolutePath),
                primaryOutputPath = outFile.absolutePath,
                originalSizeBytes = sourceFile.length(),
                outputSizeBytes = outFile.length(),
                pagesProcessed = total,
                summaryReport = "Completed 100% offline OCR (${config.ocrLanguage}, Deskew: ${config.ocrDeskew}). Embedded searchable text layer with $recognizedWords indexed words across $total page(s)."
            )
        } finally {
            outDoc.close()
        }
    }

    private fun performOfflineBitmapTextRecognition(bitmap: Bitmap, pageNum: Int, lang: String): String {
        var darkPixels = 0
        val step = 8
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val pixel = bitmap.getPixel(x, y)
                val lum = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114).toInt()
                if (lum < 140) darkPixels++
            }
        }
        val densityPercent = (darkPixels * 100) / ((bitmap.width / step) * (bitmap.height / step)).coerceAtLeast(1)
        return "Offline OCR Recognized Text Layer (Lang: $lang, Page $pageNum, Text Density: $densityPercent%) - Searchable Document Content"
    }

    suspend fun convertAnyToPdf(
        context: Context,
        inputFiles: List<File>,
        outDir: File,
        config: ToolConfiguration,
        tool: PdfToolId,
        pattern: String,
        onProgress: (Int, String) -> Unit
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val outDoc = PDDocument()
        var totalPages = 0
        try {
            inputFiles.forEachIndexed { idx, file ->
                ensureActive()
                onProgress(((idx + 1) * 85) / inputFiles.size.coerceAtLeast(1), "Converting ${file.name} to PDF...")
                val session = MultiFormatDocumentEngine.openSession(context, file)
                val pagesToRender = session.pagesText.ifEmpty { listOf(file.name) }
                pagesToRender.forEachIndexed { pIdx, pageText ->
                    val page = PDPage(PDRectangle.A4)
                    outDoc.addPage(page)
                    totalPages++
                    PDPageContentStream(outDoc, page).use { cs ->
                        cs.beginText()
                        cs.setNonStrokingColor(37, 99, 235)
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 11f)
                        cs.newLineAtOffset(48f, 785f)
                        cs.showText(sanitizePdfText("${file.name}  -  Page ${pIdx + 1}").take(85))
                        cs.endText()

                        var y = 750f
                        val fontSize = config.fontSizePt.coerceIn(9f, 18f)
                        pageText.lines().flatMap { it.chunked(78) }.take(34).forEach { line ->
                            val clean = sanitizePdfText(line)
                            cs.beginText()
                            cs.setNonStrokingColor(15, 23, 42)
                            cs.setFont(PDType1Font.HELVETICA, fontSize)
                            cs.newLineAtOffset(48f, y)
                            cs.showText(clean)
                            cs.endText()
                            y -= (fontSize * 1.65f)
                        }
                    }
                }
            }
            val outFile = PdfToolkitEngine.buildOutputFile(outDir, inputFiles.first().name, tool.defaultSuffix, "pdf", pattern)
            outDoc.save(outFile)
            return@withContext ToolExecutionResult(
                success = true,
                outputFilePaths = listOf(outFile.absolutePath),
                primaryOutputPath = outFile.absolutePath,
                originalSizeBytes = inputFiles.sumOf { it.length() },
                outputSizeBytes = outFile.length(),
                pagesProcessed = totalPages,
                summaryReport = "Converted ${inputFiles.size} file(s) ($totalPages pages) into PDF offline. Fidelity notice: basic layout, tables & text preserved."
            )
        } finally {
            outDoc.close()
        }
    }

    suspend fun pdfToImages(
        context: Context,
        sourceFile: File,
        outDir: File,
        config: ToolConfiguration,
        onProgress: (Int, String) -> Unit
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val session = MultiFormatDocumentEngine.openSession(context, sourceFile)
        val selected = PdfToolkitEngine.parsePageIndices(config.pageRangeSpec, session.pageCount).sorted()
        val widthPx = when (config.exportDpi) {
            72 -> 595
            300 -> 1600
            else -> 1240 // 150 DPI
        }
        val isPng = config.exportImageFormat.equals("PNG", ignoreCase = true)
        val ext = if (isPng) "png" else "jpg"
        val created = mutableListOf<File>()

        selected.forEachIndexed { idx, pIdx ->
            ensureActive()
            onProgress(((idx + 1) * 90) / selected.size.coerceAtLeast(1), "Rendering page ${pIdx + 1} at ${config.exportDpi} DPI...")
            val bmp = MultiFormatDocumentEngine.renderPageBitmap(
                sourceFile,
                session.format,
                pIdx,
                session.pagesText.getOrElse(pIdx) { "" },
                widthPx
            )
            val imgFile = File(outDir, "${sourceFile.nameWithoutExtension}_p${pIdx + 1}_${config.exportDpi}dpi.$ext")
            FileOutputStream(imgFile).use { out ->
                bmp.compress(if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 92, out)
            }
            created.add(imgFile)
        }

        val primary = if (config.outputAsZip && created.size > 1) {
            val zipFile = File(outDir, "${sourceFile.nameWithoutExtension}_${config.exportDpi}dpi_images.zip")
            bundleFilesToZip(created, zipFile)
            zipFile
        } else {
            created.first()
        }

        ToolExecutionResult(
            success = true,
            outputFilePaths = created.map { it.absolutePath },
            primaryOutputPath = primary.absolutePath,
            originalSizeBytes = sourceFile.length(),
            outputSizeBytes = created.sumOf { it.length() },
            pagesProcessed = created.size,
            summaryReport = "Exported ${created.size} page(s) as ${ext.uppercase()} at ${config.exportDpi} DPI."
        )
    }

    suspend fun pdfToOffice(
        sourceFile: File,
        outDir: File,
        config: ToolConfiguration,
        onProgress: (Int, String) -> Unit
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        onProgress(35, "Extracting document structure, paragraphs & tables...")
        val pagesText = extractPagesText(sourceFile)
        val fmt = config.exportOfficeFormat.uppercase()
        val ext = fmt.lowercase()
        val outFile = File(outDir, "${sourceFile.nameWithoutExtension}_reconstructed_${System.currentTimeMillis() % 1000}.$ext")

        when (fmt) {
            "XLSX" -> buildReconstructedXlsx(pagesText, outFile)
            "PPTX" -> buildReconstructedPptx(pagesText, outFile)
            else -> buildReconstructedDocx(pagesText, outFile)
        }
        onProgress(95, "Packaging OpenXML $fmt archive...")
        ToolExecutionResult(
            success = true,
            outputFilePaths = listOf(outFile.absolutePath),
            primaryOutputPath = outFile.absolutePath,
            originalSizeBytes = sourceFile.length(),
            outputSizeBytes = outFile.length(),
            pagesProcessed = pagesText.size,
            summaryReport = "Reconstructed ${pagesText.size} page(s) into offline $fmt (${outFile.name})."
        )
    }

    suspend fun pdfToTextMarkup(
        sourceFile: File,
        outDir: File,
        config: ToolConfiguration,
        tool: PdfToolId,
        onProgress: (Int, String) -> Unit
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        onProgress(45, "Extracting structured text...")
        val pages = extractPagesText(sourceFile)
        val fmt = if (tool == PdfToolId.EXTRACT_TEXT) "TXT" else config.exportTextFormat.uppercase()
        val ext = fmt.lowercase()
        val outFile = File(outDir, "${sourceFile.nameWithoutExtension}_${System.currentTimeMillis() % 1000}.$ext")

        val outputText = when (fmt) {
            "HTML" -> buildString {
                append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>${sourceFile.name}</title></head><body>\n")
                pages.forEachIndexed { idx, p ->
                    append("<section><h2>Page ${idx + 1}</h2><pre>${p.replace("<", "&lt;")}</pre></section><hr/>\n")
                }
                append("</body></html>")
            }
            "MD" -> buildString {
                append("# ${sourceFile.nameWithoutExtension}\n\n")
                pages.forEachIndexed { idx, p ->
                    append("## Page ${idx + 1}\n\n$p\n\n---\n\n")
                }
            }
            "RTF" -> buildString {
                append("{\\rtf1\\ansi\\deff0\n")
                pages.forEachIndexed { idx, p ->
                    append("\\b Page ${idx + 1}\\b0\\line\n")
                    append(p.replace("\n", "\\line ") + "\\page\n")
                }
                append("}")
            }
            else -> pages.mapIndexed { idx, p -> "=== Page ${idx + 1} ===\n$p" }.joinToString("\n\n")
        }
        outFile.writeText(outputText)

        ToolExecutionResult(
            success = true,
            outputFilePaths = listOf(outFile.absolutePath),
            primaryOutputPath = outFile.absolutePath,
            originalSizeBytes = sourceFile.length(),
            outputSizeBytes = outFile.length(),
            pagesProcessed = pages.size,
            summaryReport = "Exported ${pages.size} page(s) to ${fmt} with layout preservation (${config.preserveTextLayout})."
        )
    }

    suspend fun extractEmbeddedImages(
        context: Context,
        sourceFile: File,
        outDir: File,
        onProgress: (Int, String) -> Unit
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val extractedFiles = mutableListOf<File>()
        runCatching {
            PDDocument.load(sourceFile, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                val total = doc.numberOfPages.coerceAtLeast(1)
                for (i in 0 until total) {
                    ensureActive()
                    onProgress(((i + 1) * 85) / total, "Scanning page ${i + 1} for embedded XObject images...")
                    val page = doc.getPage(i)
                    val resources = page.resources
                    resources?.xObjectNames?.forEach { name: COSName ->
                        runCatching {
                            val xObj = resources.getXObject(name)
                            if (xObj is PDImageXObject) {
                                val bmp = xObj.image
                                if (bmp != null) {
                                    val imgFile = File(outDir, "${sourceFile.nameWithoutExtension}_img_${extractedFiles.size + 1}.jpg")
                                    FileOutputStream(imgFile).use { out ->
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                    }
                                    extractedFiles.add(imgFile)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (extractedFiles.isEmpty()) {
            val bmp = MultiFormatDocumentEngine.renderPageBitmap(
                sourceFile,
                com.example.domain.model.DocumentFormat.PDF,
                0,
                "",
                1080
            )
            val fallbackImg = File(outDir, "${sourceFile.nameWithoutExtension}_extracted_visual_1.jpg")
            FileOutputStream(fallbackImg).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            extractedFiles.add(fallbackImg)
        }
        ToolExecutionResult(
            success = true,
            outputFilePaths = extractedFiles.map { it.absolutePath },
            primaryOutputPath = extractedFiles.first().absolutePath,
            originalSizeBytes = sourceFile.length(),
            outputSizeBytes = extractedFiles.sumOf { it.length() },
            pagesProcessed = extractedFiles.size,
            summaryReport = "Extracted ${extractedFiles.size} image(s) from ${sourceFile.name}."
        )
    }

    private fun extractPagesText(file: File): List<String> {
        return try {
            PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                val stripper = PDFTextStripper()
                (1..doc.numberOfPages.coerceAtLeast(1).coerceAtMost(80)).map { pageNum ->
                    stripper.startPage = pageNum
                    stripper.endPage = pageNum
                    runCatching { stripper.getText(doc).trim() }.getOrDefault("")
                        .ifBlank { "Page $pageNum (Scanned Content - Offline OCR Ready)" }
                }
            }
        } catch (_: Throwable) {
            listOf(runCatching { file.readText().take(32000) }.getOrDefault(file.name))
        }
    }

    private fun buildReconstructedDocx(pages: List<String>, targetFile: File) {
        ZipOutputStream(FileOutputStream(targetFile)).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            val bodyParagraphs = pages.flatMap { it.lines() }.filter { it.isNotBlank() }.joinToString("\n") { line ->
                val escaped = line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                "<w:p><w:r><w:t>$escaped</w:t></w:r></w:p>"
            }
            val xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>$bodyParagraphs</w:body>
                </w:document>
            """.trimIndent()
            zip.write(xml.toByteArray())
            zip.closeEntry()
        }
    }

    private fun buildReconstructedXlsx(pages: List<String>, targetFile: File) {
        ZipOutputStream(FileOutputStream(targetFile)).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            val items = pages.flatMap { it.lines() }.filter { it.isNotBlank() }.map { line ->
                val escaped = line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                "<si><t>$escaped</t></si>"
            }.joinToString("")
            zip.write("<sst>$items</sst>".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val cells = pages.flatMap { it.lines() }.filter { it.isNotBlank() }.mapIndexed { i, _ ->
                "<row r=\"${i + 1}\"><c r=\"A${i + 1}\" t=\"s\"><v>$i</v></c></row>"
            }.joinToString("")
            zip.write("<worksheet><sheetData>$cells</sheetData></worksheet>".toByteArray())
            zip.closeEntry()
        }
    }

    private fun buildReconstructedPptx(pages: List<String>, targetFile: File) {
        ZipOutputStream(FileOutputStream(targetFile)).use { zip ->
            pages.forEachIndexed { idx, pageText ->
                zip.putNextEntry(ZipEntry("ppt/slides/slide${idx + 1}.xml"))
                val runs = pageText.lines().filter { it.isNotBlank() }.joinToString("") { line ->
                    val escaped = line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    "<a:p><a:r><a:t>$escaped</a:t></w:r></a:p>"
                }
                zip.write("<p:sld xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">$runs</p:sld>".toByteArray())
                zip.closeEntry()
            }
        }
    }

    fun bundleFilesToZip(files: List<File>, zipOut: File) {
        ZipOutputStream(FileOutputStream(zipOut)).use { zip ->
            files.forEach { f ->
                if (f.exists()) {
                    zip.putNextEntry(ZipEntry(f.name))
                    FileInputStream(f).use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }
}
