# Folio — 100% Offline Universal Document Reader & PDF Toolkit

Folio is a privacy-first, 100% offline Android document reader and comprehensive non-destructive PDF studio built with Kotlin, Jetpack Compose, Material 3, Room, and PdfBox-Android.

## Hard Offline & Non-Destructive Guarantees
- **Zero Network Permissions**: Neither `android.permission.INTERNET` nor `android.permission.ACCESS_NETWORK_STATE` is declared, and `tools:node="remove"` is applied in `AndroidManifest.xml` to strip any transitive network permissions.
- **Bundled Assets**: The `Inter` typography family (`res/font/inter.ttf`), custom vector illustrations, and PDF/OCR engines are bundled directly in the APK.
- **Scoped Storage (SAF)**: External files are accessed strictly through Android's Storage Access Framework (`ActivityResultContracts.OpenDocument` / `CreateDocument`) without requesting broad storage permissions.
- **Non-Destructive Processing**: Every tool writes a fresh output file in the workspace output directory (`{name}_{tool}.pdf`) and never mutates the original file in place.

## Package Structure
- `com.example.ui`: Editorial design system (`Color.kt`, `Type.kt`, `Theme.kt`), `FolioViewModel`, `HomeScreen`, `ToolsScreen`, `FilesScreen`, `SettingsScreen`, and 3-screen `OnboardingOverlay`.
- `com.example.domain.model`: Domain models (`DocumentFormat`, `PdfToolId` covering 31 tools, `ToolConfiguration`, `ReaderViewMode`, `ReadingThemeMode`).
- `com.example.data.local`: Room database (`FolioDatabase`, `FolioDao`, `FolioRepository`), entities (`DocumentEntity`, `FolderEntity`, `TagEntity`, `BookmarkEntity`, `AnnotationEntity`, `ToolPresetEntity`, `BatchJobEntity`, `SignatureEntity`), and `FolioPreferences`.
- `com.example.reader`: `MultiFormatDocumentEngine` (native `PdfRenderer` with LRU bitmap caching + parsers for EPUB, DOCX, XLSX, PPTX, ODT, RTF, TXT, MD, CSV, HTML, CBZ, XPS, and Images) and `UniversalReaderScreen`.
- `com.example.tools`: `PdfToolkitEngine`, `PdfToolkitConversions`, `UnifiedToolWizardScreen` (`Pick → Configure → Result`), and `DocumentScannerScreen` (CameraX + edge overlay + perspective crop + filters).
- `com.example.core`: `VaultCryptoManager` (AES-256-GCM Private Vault encryption & PKCS#12 SHA-256 digest generation) and `SampleSeedGenerator`.

## Build & Test Steps
1. Compile Debug APK:
   ```bash
   gradle assembleDebug
   ```
2. Run Unit & Robolectric CUJ Tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

## Libraries & Permissive Licenses
| Library | License | Purpose |
| :--- | :--- | :--- |
| AndroidX Jetpack Compose & Material 3 | Apache-2.0 | UI, adaptive layouts, spring animations |
| AndroidX `PdfRenderer` | Apache-2.0 | Hardware-accelerated tiled PDF page rendering |
| `com.tom-roush:pdfbox-android:2.0.27.0` | Apache-2.0 | PDF merge, split, compress, encrypt (AES-256), watermark, redact, PDF/A, metadata, forms |
| AndroidX Room (`2.7.0`) + KSP | Apache-2.0 | Local SQLite persistence for library, tags, bookmarks, annotations, presets & jobs |
| AndroidX CameraX (`1.5.0`) | Apache-2.0 | Document scanner preview, capture, and image filtering |
| AndroidX Biometric (`1.1.0`) | Apache-2.0 | Biometric & PIN App Lock and Private Vault authentication |
| AndroidX WorkManager (`2.10.0`) | Apache-2.0 | Background processing support |

## Offline Trade-Offs & Known Limitations
- **Office <-> PDF Conversion**: Executed 100% offline via OpenXML (`DOCX`, `XLSX`, `PPTX`, `ODT`) stream parsers and `PdfBox-Android` layout builders. Text, tables, headings, and pagination are preserved; complex proprietary desktop macros or floating SmartArt shapes may render with simplified styling.
- **Offline OCR**: Uses a bundled English + Latin character/density recognition pipeline with deskew pre-processing to embed a searchable text layer offline without downloading remote ML models.
