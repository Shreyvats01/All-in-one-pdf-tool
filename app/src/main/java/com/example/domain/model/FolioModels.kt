package com.example.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.Approval
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.BorderOuter
import androidx.compose.material.icons.outlined.BrandingWatermark
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector

enum class DocumentFormat(val extension: String, val label: String, val mimeType: String) {
    PDF("pdf", "PDF Document", "application/pdf"),
    EPUB("epub", "EPUB eBook", "application/epub+zip"),
    DOCX("docx", "Word Document", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    DOC("doc", "Word 97-2003", "application/msword"),
    XLSX("xlsx", "Excel Spreadsheet", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    XLS("xls", "Excel 97-2003", "application/vnd.ms-excel"),
    PPTX("pptx", "PowerPoint Presentation", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    PPT("ppt", "PowerPoint 97-2003", "application/vnd.ms-powerpoint"),
    ODT("odt", "OpenDocument Text", "application/vnd.oasis.opendocument.text"),
    RTF("rtf", "Rich Text Format", "application/rtf"),
    TXT("txt", "Plain Text", "text/plain"),
    MD("md", "Markdown", "text/markdown"),
    CSV("csv", "CSV Table", "text/csv"),
    HTML("html", "HTML Document", "text/html"),
    CBZ("cbz", "Comic Book Archive", "application/vnd.comicbook+zip"),
    CBR("cbr", "Comic RAR Archive", "application/vnd.comicbook-rar"),
    XPS("xps", "XPS Document", "application/vnd.ms-xpsdocument"),
    IMAGE("jpg", "Raster Image", "image/*");

    companion object {
        fun fromFilename(name: String): DocumentFormat {
            val ext = name.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "pdf" -> PDF
                "epub" -> EPUB
                "docx" -> DOCX
                "doc" -> DOC
                "xlsx" -> XLSX
                "xls" -> XLS
                "pptx" -> PPTX
                "ppt" -> PPT
                "odt" -> ODT
                "rtf" -> RTF
                "txt", "log" -> TXT
                "md", "markdown" -> MD
                "csv", "tsv" -> CSV
                "html", "htm", "xhtml" -> HTML
                "cbz" -> CBZ
                "cbr" -> CBR
                "xps", "oxps" -> XPS
                "jpg", "jpeg", "png", "webp", "bmp", "gif", "tif", "tiff" -> IMAGE
                else -> PDF
            }
        }
    }
}

enum class ReaderViewMode(val label: String) {
    VERTICAL_CONTINUOUS("Vertical Continuous"),
    HORIZONTAL_PAGED("Horizontal Paged"),
    SINGLE_PAGE("Single Page"),
    TWO_PAGE_SPREAD("Two-Page Spread"),
    REFLOW("Reflow Mode")
}

enum class ReaderFitMode(val label: String) {
    FIT_WIDTH("Fit Width"),
    FIT_PAGE("Fit Page"),
    ACTUAL_SIZE("Actual Size (100%)")
}

enum class ReadingThemeMode(val label: String) {
    DAY("Day"),
    NIGHT_INVERTED("Night (Inverted)"),
    SEPIA("Sepia"),
    AUTO_TIME("Auto (Time of Day)")
}

enum class AnnotationType(val label: String) {
    HIGHLIGHT("Highlight"),
    UNDERLINE("Underline"),
    STRIKETHROUGH("Strikethrough"),
    SQUIGGLY("Squiggly"),
    FREEHAND_PEN("Pen"),
    FREEHAND_HIGHLIGHTER("Marker"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle"),
    LINE("Line"),
    ARROW("Arrow"),
    TEXT_BOX("Text Box"),
    STICKY_NOTE("Sticky Note"),
    STAMP("Stamp"),
    SIGNATURE("Signature"),
    FORM_FIELD("Form Field")
}

enum class StampPreset(val label: String) {
    APPROVED("APPROVED"),
    DRAFT("DRAFT"),
    CONFIDENTIAL("CONFIDENTIAL"),
    FINAL("FINAL"),
    REVIEWED("REVIEWED"),
    CUSTOM("CUSTOM")
}

enum class ToolCategory(val title: String) {
    ORGANIZE("Organize PDF"),
    OPTIMIZE("Optimize & OCR"),
    CONVERT_TO_PDF("Convert to PDF"),
    CONVERT_FROM_PDF("Convert from PDF"),
    ANNOTATE_EDIT("Annotate & Edit"),
    EDIT_ENHANCE("Edit & Enhance"),
    SECURITY("Security & Privacy"),
    BATCH("Batch & Automation")
}

enum class PdfToolId(
    val title: String,
    val subtitle: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val acceptsMultipleFiles: Boolean = false,
    val defaultSuffix: String
) {
    // Organize
    MERGE_PDF("Merge PDF", "Combine multiple PDFs in custom order with page ranges", ToolCategory.ORGANIZE, Icons.AutoMirrored.Outlined.MergeType, true, "merged"),
    SPLIT_PDF("Split PDF", "Split by range, every N pages, size, or bookmarks", ToolCategory.ORGANIZE, Icons.AutoMirrored.Outlined.CallSplit, false, "split"),
    REMOVE_PAGES("Remove Pages", "Delete selected pages visually or by range", ToolCategory.ORGANIZE, Icons.Outlined.DeleteSweep, false, "trimmed"),
    EXTRACT_PAGES("Extract Pages", "Pick specific pages into a clean new PDF", ToolCategory.ORGANIZE, Icons.Outlined.ContentCut, false, "extracted"),
    ORGANIZE_PAGES("Organize Pages", "Reorder, rotate, duplicate, or insert blank pages", ToolCategory.ORGANIZE, Icons.Outlined.GridView, false, "organized"),
    SCAN_TO_PDF("Scan to PDF", "CameraX edge detection, crop, filters & offline OCR", ToolCategory.ORGANIZE, Icons.Outlined.DocumentScanner, true, "scanned"),

    // Optimize
    COMPRESS_PDF("Compress PDF", "Reduce size with presets or custom KB target", ToolCategory.OPTIMIZE, Icons.Outlined.Compress, true, "compressed"),
    REPAIR_PDF("Repair PDF", "Rebuild corrupted cross-reference table and streams", ToolCategory.OPTIMIZE, Icons.Outlined.Build, false, "repaired"),
    OCR_PDF("OCR PDF", "Offline searchable text layer & deskew", ToolCategory.OPTIMIZE, Icons.Outlined.Translate, true, "ocr"),

    // Convert to PDF
    IMAGE_TO_PDF("Image to PDF", "JPG, PNG, WebP, TIFF to PDF with margins & layout", ToolCategory.CONVERT_TO_PDF, Icons.Outlined.Collections, true, "images_to_pdf"),
    OFFICE_TO_PDF("Office to PDF", "Word, Excel, PowerPoint & ODT to PDF offline", ToolCategory.CONVERT_TO_PDF, Icons.Outlined.Description, true, "office_to_pdf"),
    TEXT_WEB_TO_PDF("HTML / MD / CSV to PDF", "Convert HTML, Markdown, TXT & CSV with custom typography", ToolCategory.CONVERT_TO_PDF, Icons.Outlined.PostAdd, true, "converted"),

    // Convert from PDF
    PDF_TO_IMAGE("PDF to JPG / PNG", "Export pages at 72, 150, or 300 DPI or ZIP", ToolCategory.CONVERT_FROM_PDF, Icons.Outlined.Image, false, "page_images"),
    PDF_TO_OFFICE("PDF to Word / Excel / PPT", "Reconstruct DOCX, XLSX, or PPTX offline", ToolCategory.CONVERT_FROM_PDF, Icons.AutoMirrored.Outlined.DriveFileMove, false, "reconstructed"),
    PDF_TO_TEXT_MARKUP("PDF to TXT / MD / HTML / RTF", "Export clean structured markup or text", ToolCategory.CONVERT_FROM_PDF, Icons.Outlined.TextFields, false, "exported_text"),
    PDF_TO_PDFA("PDF to PDF/A", "Embed archival metadata and color profile", ToolCategory.CONVERT_FROM_PDF, Icons.Outlined.Inventory2, false, "pdfa"),
    EXTRACT_IMAGES("Extract Images", "Pull all embedded raster images from PDF", ToolCategory.CONVERT_FROM_PDF, Icons.Outlined.FilterNone, false, "extracted_images"),
    EXTRACT_TEXT("Extract Text", "Pull text with optional layout preservation", ToolCategory.CONVERT_FROM_PDF, Icons.Outlined.EditNote, false, "extracted_text"),

    // Annotate & Edit
    ANNOTATE_MARKUP("Annotate & Markup", "Highlights, ink pen, shapes, notes & comments", ToolCategory.ANNOTATE_EDIT, Icons.Outlined.Draw, false, "annotated"),
    STAMPS("Add Stamps", "Approved, Draft, Confidential or custom stamp", ToolCategory.ANNOTATE_EDIT, Icons.Outlined.Approval, false, "stamped"),
    FORM_FILL("Fill Forms", "Detect and fill interactive PDF AcroForm fields", ToolCategory.ANNOTATE_EDIT, Icons.Outlined.Checklist, false, "filled_form"),
    EDIT_PDF_CONTENT("Edit PDF Content", "Add/edit text blocks, insert images & move objects", ToolCategory.ANNOTATE_EDIT, Icons.Outlined.Edit, false, "edited"),

    // Edit & Enhance
    ROTATE_PDF("Rotate PDF", "Rotate all or selected pages by 90°, 180°, or 270°", ToolCategory.EDIT_ENHANCE, Icons.AutoMirrored.Outlined.RotateRight, true, "rotated"),
    ADD_PAGE_NUMBERS("Page Numbers", "Add formatted numbering with position grid", ToolCategory.EDIT_ENHANCE, Icons.Outlined.FormatListNumbered, false, "numbered"),
    WATERMARK_PDF("Watermark", "Text or image watermark, tiled or single", ToolCategory.EDIT_ENHANCE, Icons.Outlined.BrandingWatermark, true, "watermarked"),
    HEADER_FOOTER("Header & Footer", "Add date, filename & page variables", ToolCategory.EDIT_ENHANCE, Icons.Outlined.BorderOuter, false, "header_footer"),
    CROP_PDF("Crop PDF", "Visual margin crop or auto-crop whitespace", ToolCategory.EDIT_ENHANCE, Icons.Outlined.Crop, false, "cropped"),
    RESIZE_NUP_BOOKLET("Resize / N-Up / Booklet", "2, 4, 6 pages per sheet or booklet imposition", ToolCategory.EDIT_ENHANCE, Icons.Outlined.GridView, false, "nup"),
    REMOVE_BLANK_PAGES("Remove Blank Pages", "Auto-detect and remove blank pages with sensitivity", ToolCategory.EDIT_ENHANCE, Icons.Outlined.AutoFixHigh, false, "no_blanks"),
    GRAYSCALE_BW("Grayscale / B&W", "Convert color PDF pages to grayscale or high-contrast B&W", ToolCategory.EDIT_ENHANCE, Icons.Outlined.Contrast, true, "grayscale"),
    EDIT_METADATA("Edit Metadata", "Modify Title, Author, Subject, and Keywords", ToolCategory.EDIT_ENHANCE, Icons.Outlined.Info, false, "metadata"),
    FLATTEN_PDF("Flatten PDF", "Merge annotations and form fields into page streams", ToolCategory.EDIT_ENHANCE, Icons.Outlined.LayersClear, false, "flattened"),

    // Security
    PROTECT_PDF("Protect PDF", "Encrypt with AES-256 open & permission passwords", ToolCategory.SECURITY, Icons.Outlined.Lock, true, "protected"),
    UNLOCK_PDF("Unlock PDF", "Remove password restrictions when password is known", ToolCategory.SECURITY, Icons.Outlined.LockOpen, false, "unlocked"),
    SIGN_PDF("Sign PDF", "Draw, type or apply PKCS#12 digital signature", ToolCategory.SECURITY, Icons.Outlined.Draw, false, "signed"),
    REDACT_PDF("Redact PDF", "Permanently purge sensitive text and areas with verification", ToolCategory.SECURITY, Icons.Outlined.VisibilityOff, false, "redacted"),
    COMPARE_PDF("Compare PDFs", "Side-by-side & overlay visual/text diff report", ToolCategory.SECURITY, Icons.AutoMirrored.Outlined.CompareArrows, true, "diff_report"),

    // Batch
    BATCH_PROCESSOR("Batch Studio", "Run compress, watermark, rotate, protect or OCR on many files", ToolCategory.BATCH, Icons.AutoMirrored.Outlined.Rule, true, "batch")
}

enum class CompressionLevel(val label: String, val jpegQuality: Int, val scaleFactor: Float) {
    LOW("Low (High Quality)", 82, 0.9f),
    MEDIUM("Medium (Balanced)", 60, 0.72f),
    EXTREME("Extreme (Smallest Size)", 35, 0.5f),
    CUSTOM("Custom Target KB", 50, 0.65f)
}

enum class SplitMode(val label: String) {
    BY_RANGE("By Page Range (e.g. 1-3, 4-6)"),
    EVERY_N_PAGES("Every N Pages"),
    BY_FILE_SIZE("By Max File Size (KB)"),
    BY_BOOKMARKS("By Outline / Bookmarks"),
    EXTRACT_ALL("Every Page as Separate PDF")
}

enum class PagePosition(val label: String, val xFactor: Float, val yFactor: Float) {
    TOP_LEFT("Top Left", 0.12f, 0.93f),
    TOP_CENTER("Top Center", 0.50f, 0.93f),
    TOP_RIGHT("Top Right", 0.88f, 0.93f),
    CENTER_LEFT("Center Left", 0.12f, 0.50f),
    CENTER("Center", 0.50f, 0.50f),
    CENTER_RIGHT("Center Right", 0.88f, 0.50f),
    BOTTOM_LEFT("Bottom Left", 0.12f, 0.07f),
    BOTTOM_CENTER("Bottom Center", 0.50f, 0.07f),
    BOTTOM_RIGHT("Bottom Right", 0.88f, 0.07f)
}

enum class NumberFormatStyle(val label: String) {
    PLAIN("1, 2, 3"),
    PAGE_X_OF_N("Page 1 of N"),
    ROMAN("i, ii, iii (Roman)")
}

data class ToolConfiguration(
    val pageRangeSpec: String = "All",
    val addBookmarksPerSource: Boolean = true,
    val splitMode: SplitMode = SplitMode.EVERY_N_PAGES,
    val splitEveryN: Int = 2,
    val splitMaxSizeKb: Int = 500,
    val outputAsZip: Boolean = false,
    val rotateDegrees: Int = 90,
    val insertBlankAfterPage: Int = 0,
    val duplicatePageIndex: Int = 0,
    val customPageOrder: List<Int> = emptyList(),
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val targetSizeKb: Int = 400,
    val ocrLanguage: String = "eng+lat",
    val ocrDeskew: Boolean = true,
    val imagePageSize: String = "A4", // A4, LETTER, FIT
    val imageOrientation: String = "PORTRAIT", // PORTRAIT, LANDSCAPE
    val imageMarginDp: Int = 24,
    val imageQuality: Int = 88,
    val exportImageFormat: String = "JPG", // JPG, PNG
    val exportDpi: Int = 150, // 72, 150, 300
    val exportOfficeFormat: String = "DOCX", // DOCX, XLSX, PPTX
    val exportTextFormat: String = "MD", // TXT, RTF, HTML, MD
    val preserveTextLayout: Boolean = true,
    val pageNumberPosition: PagePosition = PagePosition.BOTTOM_CENTER,
    val pageNumberFormat: NumberFormatStyle = NumberFormatStyle.PAGE_X_OF_N,
    val startPageNumber: Int = 1,
    val skipFirstPage: Boolean = false,
    val fontSizePt: Float = 11f,
    val watermarkText: String = "CONFIDENTIAL",
    val watermarkOpacity: Float = 0.25f,
    val watermarkRotation: Float = -35f,
    val watermarkTiled: Boolean = true,
    val watermarkPosition: PagePosition = PagePosition.CENTER,
    val headerText: String = "{filename}",
    val footerText: String = "{date} • Page {page}",
    val cropMarginPercent: Float = 0.08f,
    val autoCropWhitespace: Boolean = false,
    val nUpPagesPerSheet: Int = 2, // 1, 2, 4, 6
    val bookletMode: Boolean = false,
    val blankPageSensitivity: Float = 0.98f,
    val highContrastBw: Boolean = false,
    val metaTitle: String = "",
    val metaAuthor: String = "",
    val metaSubject: String = "",
    val metaKeywords: String = "",
    val openPassword: String = "",
    val permissionPassword: String = "",
    val allowPrinting: Boolean = true,
    val allowCopying: Boolean = false,
    val allowEditing: Boolean = false,
    val signatureText: String = "Signed via Folio",
    val includeDateStamp: Boolean = true,
    val usePkcs12Certificate: Boolean = true,
    val redactSearchTerms: String = "SSN, Secret, Confidential",
    val redactAreaNormalized: FloatArray = floatArrayOf(0.15f, 0.40f, 0.85f, 0.48f),
    val stampPreset: StampPreset = StampPreset.APPROVED,
    val customStampText: String = "APPROVED",
    val editAddedText: String = "",
    val formFieldUpdates: Map<String, String> = emptyMap(),
    val batchSubTool: PdfToolId = PdfToolId.COMPRESS_PDF
)

data class ToolExecutionResult(
    val success: Boolean,
    val outputFilePaths: List<String>,
    val primaryOutputPath: String,
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val pagesProcessed: Int,
    val summaryReport: String,
    val errorMessage: String? = null
)
