package com.example.reader

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import androidx.core.content.FileProvider
import com.example.core.SampleSeedGenerator
import com.example.domain.model.DocumentFormat
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class OutlineEntry(
    val title: String,
    val pageIndex: Int,
    val level: Int = 0
)

data class FormFieldEntry(
    val name: String,
    val value: String,
    val fieldType: String // TEXT, CHECKBOX, CHOICE
)

data class ParsedDocumentSession(
    val file: File,
    val format: DocumentFormat,
    val pageCount: Int,
    val pagesText: List<String>,
    val outline: List<OutlineEntry>,
    val formFields: List<FormFieldEntry>,
    val isPasswordProtected: Boolean = false,
    val isCorrupted: Boolean = false,
    val errorPlainMessage: String? = null
)

data class QuickDocumentInspection(
    val pageCount: Int,
    val previewSnippet: String
)

object MultiFormatDocumentEngine {

    private val bitmapCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(28 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    /**
     * Lightweight, zero-OOM document inspection used during file import.
     * Uses Android's native C++ PdfRenderer for instant page count without loading the entire PDF into Java heap.
     */
    suspend fun quickInspectDocument(
        context: Context,
        file: File,
        format: DocumentFormat
    ): QuickDocumentInspection = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0L) {
            return@withContext QuickDocumentInspection(1, file.name)
        }
        try {
            when (format) {
                DocumentFormat.PDF -> {
                    var pages = 1
                    runCatching {
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                            PdfRenderer(pfd).use { renderer ->
                                pages = renderer.pageCount.coerceAtLeast(1)
                            }
                        }
                    }.onFailure {
                        // Fallback to temp-file backed PDDocument if native PdfRenderer fails
                        runCatching {
                            SampleSeedGenerator.ensurePdfBoxInitialized(context)
                            PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                                pages = doc.numberOfPages.coerceAtLeast(1)
                            }
                        }
                    }
                    QuickDocumentInspection(pageCount = pages, previewSnippet = file.name)
                }
                DocumentFormat.IMAGE -> QuickDocumentInspection(pageCount = 1, previewSnippet = "Image ${file.name}")
                else -> {
                    val snippet = readBoundedUtf8Text(file, maxBytes = 16 * 1024)
                    val estimatedPages = (file.length() / 2200L).toInt().coerceIn(1, 500)
                    QuickDocumentInspection(
                        pageCount = estimatedPages,
                        previewSnippet = snippet.take(1200).ifBlank { file.name }
                    )
                }
            }
        } catch (_: Throwable) {
            QuickDocumentInspection(1, file.name)
        }
    }

    suspend fun openSession(
        context: Context,
        file: File,
        password: String = ""
    ): ParsedDocumentSession = withContext(Dispatchers.IO) {
        SampleSeedGenerator.ensurePdfBoxInitialized(context)
        val format = DocumentFormat.fromFilename(file.name)
        if (!file.exists()) {
            return@withContext ParsedDocumentSession(
                file = file,
                format = format,
                pageCount = 1,
                pagesText = listOf("File not found at ${file.name}."),
                outline = emptyList(),
                formFields = emptyList(),
                errorPlainMessage = "This file was moved or deleted from storage."
            )
        }

        try {
            when (format) {
                DocumentFormat.PDF -> openPdfSession(file, password)
                DocumentFormat.EPUB -> openEpubSession(file)
                DocumentFormat.DOCX, DocumentFormat.ODT -> openZipXmlTextSession(file, format)
                DocumentFormat.XLSX -> openXlsxSession(file)
                DocumentFormat.PPTX -> openPptxSession(file)
                DocumentFormat.CBZ, DocumentFormat.CBR -> openComicArchiveSession(file, format)
                DocumentFormat.XPS -> openZipXmlTextSession(file, format)
                DocumentFormat.IMAGE -> ParsedDocumentSession(
                    file = file,
                    format = format,
                    pageCount = 1,
                    pagesText = listOf("Image File: ${file.name} (${file.length() / 1024} KB)"),
                    outline = listOf(OutlineEntry(file.name, 0)),
                    formFields = emptyList()
                )
                else -> openPlainOrMarkupSession(file, format)
            }
        } catch (t: Throwable) {
            val msg = t.message?.lowercase() ?: ""
            val isPasswordError = msg.contains("password") || msg.contains("decrypt") || msg.contains("encrypted")
            // Even if PDFBox text extraction fails or OOMs on an enormous PDF, try native PdfRenderer for page count so visual rendering still works!
            val fallbackPageCount = runCatching {
                if (format == DocumentFormat.PDF) {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { it.pageCount.coerceAtLeast(1) }
                    }
                } else 1
            }.getOrDefault(1)

            ParsedDocumentSession(
                file = file,
                format = format,
                pageCount = fallbackPageCount,
                pagesText = List(fallbackPageCount.coerceAtMost(50)) {
                    "[Page ${it + 1} — Visual page ready]"
                },
                outline = emptyList(),
                formFields = emptyList(),
                isPasswordProtected = isPasswordError,
                isCorrupted = !isPasswordError && fallbackPageCount <= 1,
                errorPlainMessage = if (isPasswordError) {
                    "This PDF is password-protected. Enter the password to continue."
                } else if (fallbackPageCount > 1) {
                    null
                } else {
                    "This file appears damaged or encrypted. Run Repair PDF or Unlock PDF to recover it."
                }
            )
        }
    }

    private fun openPdfSession(file: File, password: String): ParsedDocumentSession {
        val memSetting = MemoryUsageSetting.setupTempFileOnly()
        val doc = if (password.isNotEmpty()) {
            PDDocument.load(file, password, memSetting)
        } else {
            PDDocument.load(file, memSetting)
        }
        doc.use { pdDoc ->
            val totalPages = pdDoc.numberOfPages.coerceAtLeast(1)
            val stripper = PDFTextStripper()
            val pagesText = ArrayList<String>(totalPages.coerceAtMost(120))
            // Cap initial synchronous text extraction to 25 pages for fast, low-memory startup
            val extractLimit = totalPages.coerceAtMost(25)
            for (i in 1..extractLimit) {
                stripper.startPage = i
                stripper.endPage = i
                val pageStr = runCatching { stripper.getText(pdDoc).trim() }.getOrDefault("")
                pagesText.add(pageStr.ifEmpty { "[Page $i — Scanned or visual page. Run Offline OCR to make text selectable.]" })
            }
            for (i in (extractLimit + 1)..totalPages.coerceAtMost(200)) {
                pagesText.add("[Page $i — Loaded on demand]")
            }

            // Extract outline / ToC safely
            val outlineList = mutableListOf<OutlineEntry>()
            runCatching {
                val rootOutline = pdDoc.documentCatalog?.documentOutline
                if (rootOutline != null) {
                    collectOutline(rootOutline, pdDoc, 0, outlineList)
                }
            }
            if (outlineList.isEmpty()) {
                for (i in 0 until totalPages.coerceAtMost(100)) {
                    val firstLine = pagesText.getOrNull(i)?.lineSequence()?.firstOrNull { it.isNotBlank() }?.take(48) ?: "Page ${i + 1}"
                    outlineList.add(OutlineEntry(firstLine, i, 0))
                }
            }

            // Detect fillable AcroForm fields + structured form lines safely
            val formFields = mutableListOf<FormFieldEntry>()
            runCatching {
                val acroForm = pdDoc.documentCatalog?.acroForm
                acroForm?.fields?.forEach { field ->
                    formFields.add(
                        FormFieldEntry(
                            name = field.fullyQualifiedName ?: "Field",
                            value = field.valueAsString ?: "",
                            fieldType = field.fieldType ?: "TEXT"
                        )
                    )
                }
            }
            pagesText.take(25).forEach { text ->
                val regex = Regex("""Form Field \[([^\]]+)]:\s*(.*)""")
                regex.findAll(text).forEach { match ->
                    formFields.add(
                        FormFieldEntry(
                            name = match.groupValues[1],
                            value = match.groupValues[2].trim(),
                            fieldType = "TEXT"
                        )
                    )
                }
            }

            return ParsedDocumentSession(
                file = file,
                format = DocumentFormat.PDF,
                pageCount = totalPages,
                pagesText = pagesText,
                outline = outlineList,
                formFields = formFields
            )
        }
    }

    private fun collectOutline(
        node: PDOutlineNode,
        doc: PDDocument,
        level: Int,
        out: MutableList<OutlineEntry>
    ) {
        if (level > 4 || out.size > 150) return
        var current: PDOutlineItem? = node.firstChild
        while (current != null && out.size <= 150) {
            val page = runCatching { current.findDestinationPage(doc) }.getOrNull()
            val pageIdx = if (page != null) doc.pages.indexOf(page).coerceAtLeast(0) else 0
            out.add(OutlineEntry(current.title ?: "Section", pageIdx, level))
            collectOutline(current, doc, level + 1, out)
            current = current.nextSibling
        }
    }

    private fun openEpubSession(file: File): ParsedDocumentSession {
        val chapters = mutableListOf<String>()
        val outline = mutableListOf<OutlineEntry>()
        ZipInputStream(FileInputStream(file)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null && chapters.size < 80) {
                if (!entry.isDirectory && (entry.name.endsWith(".xhtml") || entry.name.endsWith(".html") || entry.name.endsWith(".htm"))) {
                    val rawHtml = readBoundedStreamText(zip, 64 * 1024)
                    val title = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(rawHtml)?.groupValues?.get(1)
                        ?: Regex("<h1[^>]*>(.*?)</h1>", RegexOption.IGNORE_CASE).find(rawHtml)?.groupValues?.get(1)
                        ?: "Chapter ${chapters.size + 1}"
                    val cleanText = stripHtmlTags(rawHtml)
                    if (cleanText.isNotBlank()) {
                        outline.add(OutlineEntry(title.trim(), chapters.size, 0))
                        chapters.add("$title\n\n$cleanText")
                    }
                }
                entry = zip.nextEntry
            }
        }
        if (chapters.isEmpty()) chapters.add("Empty EPUB container.")
        return ParsedDocumentSession(
            file = file,
            format = DocumentFormat.EPUB,
            pageCount = chapters.size,
            pagesText = chapters,
            outline = outline,
            formFields = emptyList()
        )
    }

    private fun openZipXmlTextSession(file: File, format: DocumentFormat): ParsedDocumentSession {
        val paragraphs = mutableListOf<String>()
        runCatching {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && paragraphs.size < 60) {
                    if (!entry.isDirectory && (entry.name.endsWith(".xml") || entry.name.endsWith(".fpage"))) {
                        val xml = readBoundedStreamText(zip, 96 * 1024)
                        val extracted = xml
                            .replace(Regex("</w:p>|</text:p>"), "\n\n")
                            .replace(Regex("<[^>]+>"), " ")
                            .replace(Regex("[ \\t]+"), " ")
                            .trim()
                        if (extracted.isNotBlank() && (entry.name.contains("document") || entry.name.contains("content") || entry.name.contains("Pages"))) {
                            paragraphs.add(extracted)
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        }
        val combined = if (paragraphs.isNotEmpty()) {
            paragraphs.joinToString("\n\n")
        } else {
            stripHtmlTags(readBoundedUtf8Text(file, 128 * 1024))
        }
        val pages = paginateText(combined, 1400)
        return ParsedDocumentSession(
            file = file,
            format = format,
            pageCount = pages.size,
            pagesText = pages,
            outline = pages.mapIndexed { idx, _ -> OutlineEntry("Page ${idx + 1}", idx) },
            formFields = emptyList()
        )
    }

    private fun openXlsxSession(file: File): ParsedDocumentSession {
        val sharedStrings = mutableListOf<String>()
        val sheets = mutableListOf<String>()
        runCatching {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && sheets.size < 30) {
                    val content = readBoundedStreamText(zip, 96 * 1024)
                    if (entry.name.endsWith("sharedStrings.xml")) {
                        Regex("<t[^>]*>(.*?)</t>").findAll(content).take(200).forEach {
                            sharedStrings.add(it.groupValues[1])
                        }
                    } else if (entry.name.contains("worksheets/sheet")) {
                        val values = Regex("<v>(.*?)</v>").findAll(content).take(250).map { it.groupValues[1] }.toList()
                        sheets.add("Spreadsheet Grid (${entry.name.substringAfterLast('/')}):\n\n" + values.joinToString("  |  "))
                    }
                    entry = zip.nextEntry
                }
            }
        }
        val pages = if (sheets.isNotEmpty()) {
            val sharedHeader = if (sharedStrings.isNotEmpty()) "Shared Labels:\n" + sharedStrings.joinToString(" - ") + "\n\n" else ""
            sheets.map { sharedHeader + it }
        } else {
            listOf("Spreadsheet: ${file.name}")
        }
        return ParsedDocumentSession(
            file = file,
            format = DocumentFormat.XLSX,
            pageCount = pages.size,
            pagesText = pages,
            outline = pages.mapIndexed { idx, _ -> OutlineEntry("Sheet ${idx + 1}", idx) },
            formFields = emptyList()
        )
    }

    private fun openPptxSession(file: File): ParsedDocumentSession {
        val slides = mutableListOf<String>()
        runCatching {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && slides.size < 60) {
                    if (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) {
                        val xml = readBoundedStreamText(zip, 64 * 1024)
                        val texts = Regex("<a:t>(.*?)</a:t>").findAll(xml).map { it.groupValues[1] }.toList()
                        slides.add("Slide ${slides.size + 1}\n\n" + texts.joinToString("\n- "))
                    }
                    entry = zip.nextEntry
                }
            }
        }
        if (slides.isEmpty()) slides.add("Presentation: ${file.name}")
        return ParsedDocumentSession(
            file = file,
            format = DocumentFormat.PPTX,
            pageCount = slides.size,
            pagesText = slides,
            outline = slides.mapIndexed { i, _ -> OutlineEntry("Slide ${i + 1}", i) },
            formFields = emptyList()
        )
    }

    private fun openComicArchiveSession(file: File, format: DocumentFormat): ParsedDocumentSession {
        var imageCount = 0
        runCatching {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.lowercase().let {
                            it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp")
                        }) {
                        imageCount++
                    }
                    entry = zip.nextEntry
                }
            }
        }
        val count = imageCount.coerceAtLeast(1)
        return ParsedDocumentSession(
            file = file,
            format = format,
            pageCount = count,
            pagesText = List(count) { "Comic Panel Page ${it + 1}" },
            outline = List(count) { OutlineEntry("Panel ${it + 1}", it) },
            formFields = emptyList()
        )
    }

    private fun openPlainOrMarkupSession(file: File, format: DocumentFormat): ParsedDocumentSession {
        val raw = readBoundedUtf8Text(file, 128 * 1024)
        val processed = when (format) {
            DocumentFormat.HTML -> stripHtmlTags(raw)
            DocumentFormat.RTF -> raw.replace(Regex("""\\[a-z]+\d*\s?|[{}]"""), "").trim()
            DocumentFormat.CSV -> raw.lines().joinToString("\n") { line ->
                line.split(",").joinToString("   |   ") { it.trim() }
            }
            else -> raw
        }
        val pages = paginateText(processed, 1500)
        return ParsedDocumentSession(
            file = file,
            format = format,
            pageCount = pages.size,
            pagesText = pages,
            outline = pages.mapIndexed { i, p ->
                val heading = p.lineSequence().firstOrNull { it.isNotBlank() }?.take(40) ?: "Page ${i + 1}"
                OutlineEntry(heading, i)
            },
            formFields = emptyList()
        )
    }

    private fun readBoundedUtf8Text(file: File, maxBytes: Int): String {
        return runCatching {
            FileInputStream(file).use { input ->
                readBoundedStreamText(input, maxBytes)
            }
        }.getOrDefault("")
    }

    private fun readBoundedStreamText(input: java.io.InputStream, maxBytes: Int): String {
        return runCatching {
            val buffer = ByteArray(maxBytes.coerceIn(1024, 256 * 1024))
            var totalRead = 0
            while (totalRead < buffer.size) {
                val read = input.read(buffer, totalRead, buffer.size - totalRead)
                if (read <= 0) break
                totalRead += read
            }
            if (totalRead <= 0) "" else String(buffer, 0, totalRead, Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun stripHtmlTags(input: String): String {
        return input
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</p>|</div>|</h1>|</h2>|</h3>|</li>|<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    private fun paginateText(text: String, charsPerPage: Int): List<String> {
        if (text.isBlank()) return listOf("(Empty Document)")
        val pages = mutableListOf<String>()
        var index = 0
        while (index < text.length && pages.size < 120) {
            val end = (index + charsPerPage).coerceAtMost(text.length)
            pages.add(text.substring(index, end))
            index = end
        }
        return pages
    }

    suspend fun renderPageBitmap(
        file: File,
        format: DocumentFormat,
        pageIndex: Int,
        pageText: String,
        targetWidthPx: Int = 1080
    ): Bitmap = withContext(Dispatchers.IO) {
        val safeWidth = targetWidthPx.coerceIn(240, 1600)
        val cacheKey = "${file.absolutePath}:${file.lastModified()}:$pageIndex:$safeWidth"
        bitmapCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        val rendered = try {
            when (format) {
                DocumentFormat.PDF -> renderNativePdfPage(file, pageIndex, safeWidth)
                DocumentFormat.IMAGE -> renderRasterImageFile(file, safeWidth)
                else -> renderTextDocumentPage(file.name, pageIndex, pageText, safeWidth)
            }
        } catch (_: Throwable) {
            renderTextDocumentPage(file.name, pageIndex, pageText.ifBlank { file.name }, safeWidth.coerceAtMost(720))
        }
        runCatching { bitmapCache.put(cacheKey, rendered) }
        rendered
    }

    private fun renderNativePdfPage(file: File, pageIndex: Int, targetWidthPx: Int): Bitmap {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val safeIndex = pageIndex.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
                    renderer.openPage(safeIndex).use { page ->
                        val aspect = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                        val width = targetWidthPx.coerceIn(240, 1600)
                        val height = (width * aspect).toInt().coerceIn(300, 2200)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, Matrix(), PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (_: Throwable) {
            renderTextDocumentPage(file.name, pageIndex, "PDF Page ${pageIndex + 1} (${file.name})", targetWidthPx)
        }
    }

    fun decodeSampledBitmapFromFile(file: File, maxDimensionPx: Int = 1440): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            var sampleSize = 1
            while ((srcW / sampleSize) > maxDimensionPx || (srcH / sampleSize) > maxDimensionPx) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
        } catch (_: Throwable) {
            null
        }
    }

    private fun renderRasterImageFile(file: File, targetWidthPx: Int): Bitmap {
        val decoded = decodeSampledBitmapFromFile(file, targetWidthPx.coerceIn(360, 1440))
        if (decoded != null) {
            val aspect = decoded.height.toFloat() / decoded.width.toFloat().coerceAtLeast(1f)
            val width = targetWidthPx.coerceIn(240, 1440)
            val height = (width * aspect).toInt().coerceIn(240, 2048)
            return if (decoded.width == width && decoded.height == height) {
                decoded
            } else {
                Bitmap.createScaledBitmap(decoded, width, height, true)
            }
        }
        return renderTextDocumentPage(file.name, 0, "Image Preview: ${file.name}", targetWidthPx)
    }

    private fun renderTextDocumentPage(
        title: String,
        pageIndex: Int,
        pageText: String,
        targetWidthPx: Int
    ): Bitmap {
        val safeW = targetWidthPx.coerceIn(240, 1440)
        val height = (safeW * 1.414f).toInt().coerceIn(320, 2048)
        val bitmap = Bitmap.createBitmap(safeW, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#FFFFFF"))

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2563EB")
            textSize = safeW * 0.025f
            isFakeBoldText = true
        }
        val margin = safeW * 0.07f
        canvas.drawText("$title  •  Page ${pageIndex + 1}", margin, margin, headerPaint)

        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 2f
        }
        canvas.drawLine(margin, margin + 18f, safeW - margin, margin + 18f, dividerPaint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = safeW * 0.031f
        }
        val contentWidth = (safeW - margin * 2).toInt().coerceAtLeast(160)
        val safeSlice = pageText.take(2200)
        val staticLayout = StaticLayout.Builder
            .obtain(safeSlice, 0, safeSlice.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(5f, 1.32f)
            .build()

        canvas.save()
        canvas.translate(margin, margin + 56f)
        staticLayout.draw(canvas)
        canvas.restore()
        return bitmap
    }

    fun shareDocumentOffline(context: Context, file: File) {
        if (!file.exists()) return
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val format = DocumentFormat.fromFilename(file.name)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${file.name}"))
        }
    }

    fun printDocumentOffline(context: Context, session: ParsedDocumentSession) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Folio_${session.file.name}"
        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(session.file.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(session.pageCount)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (destination == null) {
                        callback?.onWriteFailed("Invalid print destination")
                        return
                    }
                    try {
                        if (session.format == DocumentFormat.PDF) {
                            FileInputStream(session.file).use { input ->
                                FileOutputStream(destination.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } else {
                            val pdfDoc = PdfDocument()
                            session.pagesText.forEachIndexed { index, text ->
                                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
                                val page = pdfDoc.startPage(pageInfo)
                                val bmp = renderTextDocumentPage(session.file.name, index, text, 595)
                                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                                pdfDoc.finishPage(page)
                            }
                            FileOutputStream(destination.fileDescriptor).use { out ->
                                pdfDoc.writeTo(out)
                            }
                            pdfDoc.close()
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (t: Throwable) {
                        callback?.onWriteFailed(t.message ?: "Print error")
                    }
                }
            },
            null
        )
    }
}
