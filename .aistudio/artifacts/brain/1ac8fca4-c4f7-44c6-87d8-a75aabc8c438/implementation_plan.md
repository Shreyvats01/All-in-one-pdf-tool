# Complete Research & Fix Plan: Crash-Free Storage Permissions, In-App File Browser, & Document Scanner

## 1. Deep Research: How Leading Open-Source Android Document & PDF Apps Solve This

We analyzed how mature open-source Android PDF and document tools—**MJ PDF**, **Librera Reader**, **OSS Document Scanner**, **Image-Toolbox (T8RIN)**, and **Fossify File Manager**—handle permissions, file loading, and scanning without ever crashing:

1. **Layered Permission & File Discovery Architecture (Librera / Fossify / MJ PDF)**:
   - **Explicit Runtime Permission Prompt First**: Before scanning local folders or capturing pages, the app checks and requests the exact runtime permissions for the device's Android API level (`READ_EXTERNAL_STORAGE` on Android 8–12, `READ_MEDIA_IMAGES` + `POST_NOTIFICATIONS` on Android 13+, and `CAMERA` for scanning) using `ActivityResultContracts.RequestMultiplePermissions()`.
   - **Dual File Selection (In-App Local Storage Browser + System SAF Picker)**:
     - Users often find Android's system `DocumentsUI` picker confusing or broken on custom OEM ROMs (MIUI/HyperOS, ColorOS, OriginOS).
     - Open-source apps provide an **In-App Device Storage & Downloads Browser** (which queries `MediaStore.Files` and readable directories like `Download/`, `Documents/`, `DCIM/`, and app storage) **alongside** a crash-guarded **System SAF File/Folder Picker** (`OpenMultipleDocuments` / `GetMultipleContents` / `OpenDocumentTree` wrapped in `runCatching`).
2. **Zero-Heap Native `PdfRenderer` on Import (MJ PDF / MuPDF)**:
   - Open-source PDF readers **never** call `PDDocument.load(file)` + `PDFTextStripper` when importing or listing a PDF. Loading a 20–100 MB real-world PDF into Java heap via PDFBox on import causes immediate `OutOfMemoryError` or font table `Error` crashes.
   - Instead, they use Android's C++ native `PdfRenderer(ParcelFileDescriptor)` during import to inspect page count and render cover thumbnails in **< 5 ms with near-zero Java heap allocation**, and use `MemoryUsageSetting.setupTempFileOnly()` whenever PDFBox is invoked for editing tools.
3. **Subsampled Bitmap Pipeline & Camera Fallback (OSS Document Scanner / Image-Toolbox)**:
   - Modern phone cameras capture 12MP–50MP images (48 MB–200 MB uncompressed `ARGB_8888` per page). Decoding or filtering full-size camera/gallery bitmaps on the main thread or passing >1600px bitmaps to Jetpack Compose `Image(bitmap.asImageBitmap())` crashes with `OutOfMemoryError` or `RuntimeException: Canvas: trying to draw too large(...) bitmap`.
   - Production scanners always:
     - Decode bitmaps on `Dispatchers.IO` using `BitmapFactory.Options().inSampleSize` capped to `1600px` max dimension (~3–6 MB max).
     - Configure CameraX `PreviewView` with `ImplementationMode.COMPATIBLE` (`TextureView`) so rounded-corner Compose clipping never crashes hardware `SurfaceView` layers.
     - Verify `cameraProvider.hasCamera(...)` (falling back from back camera to front camera) and provide a **System Camera App (`ActivityResultContracts.TakePicture`)** + **Gallery Picker** fallback so scanning works on 100% of physical phones, tablets, and emulators.

---

## 2. Root-Cause Diagnosis of Current Crashes & Permission Issues

| Issue Reported | Exact Root Cause in Codebase | Fix Strategy |
| :--- | :--- | :--- |
| **App does not ask for storage permission** | `AndroidManifest.xml` only declared `CAMERA`, `VIBRATE`, `USE_BIOMETRIC`, and `POST_NOTIFICATIONS`. No storage/media permissions (`READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES`) were declared, and no runtime permission launcher was triggered when tapping **Import Files**, **Pick Files**, or **Sync Folder**. | Declare version-appropriate storage/media permissions in `AndroidManifest.xml`, add a unified **Permission & File Access Sheet** that triggers `RequestMultiplePermissions()` on tap, and provide both an **In-App Device Storage Browser** (`Downloads`, `Documents`, `MediaStore`) and the **System SAF Picker**. |
| **App automatically crashes when adding a file** | 1. `FolioViewModel.importSingleUriInternal()` called `MultiFormatDocumentEngine.openSession()`, which executed `PDDocument.load(file)` (in-memory) + 80-page `PDFTextStripper` synchronously. Real PDFs throw `OutOfMemoryError` or font `Error` (subclass of `Throwable`, not `Exception`), bypassing `catch (e: Exception)` and crashing the app.<br>2. Launching SAF contracts without `try/catch` or calling `File.readText()` on large/binary files with unknown extensions caused unhandled crashes. | 1. Replace `PDDocument.load()` during file import with fast native `PdfRenderer` page-count inspection.<br>2. Catch `Throwable` (including `OutOfMemoryError`) across all engine and repository boundaries.<br>3. Cap text reading to a safe byte buffer (`256 KB` per chunk) and guard all `ActivityResultLauncher.launch()` calls with `runCatching`. |
| **App automatically crashes when using Scan** | 1. `DocumentScannerScreen` used `PreviewView` default `SurfaceView` mode inside `.clip(RoundedCornerShape(20.dp))` and assumed `DEFAULT_BACK_CAMERA` always exists.<br>2. `image.toBitmap()` and `BitmapFactory.decodeStream()` decoded full 12MP–48MP images on the **Main Thread** without `inSampleSize`, and Compose crashed with `Canvas: trying to draw too large bitmap`.<br>3. `saveScannedBitmapsToDisk()` ran on the **Main UI Thread** and re-filtered bitmaps a second time. | 1. Force `PreviewView.ImplementationMode.COMPATIBLE` (`TextureView`) and safely resolve available camera (`DEFAULT_BACK_CAMERA` → `DEFAULT_FRONT_CAMERA` → Fallback UI).<br>2. Move all image decoding, downsampling (`maxDimension = 1600px`), filtering, and disk saving to `Dispatchers.IO`.<br>3. Add a **System Camera (`TakePicture`)** button alongside **In-App CameraX Capture** and **Import Photos**. |
| **PDF tools fail or crash on filenames with non-ASCII / bullet characters** | `PdfToolkitConversions.convertAnyToPdf()` and `PdfToolkitEngine` passed `'•'` (`\u2022`), `'—'` (`\u2014`), or non-ASCII filenames directly to `PDType1Font.HELVETICA.showText()`, which throws `IllegalArgumentException` under `WinAnsiEncoding`. | Add a central `sanitizePdfBoxText()` helper that normalizes Unicode punctuation (`•` -> `-`, `—` -> `--`) and strips unsupported glyphs before any `PDPageContentStream.showText()` call, and use `MemoryUsageSetting.setupTempFileOnly()` on all `PDDocument.load()` calls. |

---

## 3. Step-by-Step Implementation Plan

### Phase 1: Runtime Storage & Camera Permission Flow + In-App Device File Browser
- **Update `/app/src/main/AndroidManifest.xml`**:
  - Add `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />`
  - Add `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />`
  - Add `<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />`
  - Keep `INTERNET` and `ACCESS_NETWORK_STATE` strictly removed (`tools:node="remove"`).
- **Create `/app/src/main/java/com/example/ui/StoragePermissionAndBrowserSheet.kt`**:
  - When the user taps **Import Files**, **Pick Files**, or **Browse Device**, check if runtime storage/media permissions are granted:
    - If not yet requested, immediately trigger `ActivityResultContracts.RequestMultiplePermissions()` so Android displays the native system permission dialog (*"Allow Folio to access files and media on your device?"*).
  - Present a clean **Add Files & Storage Access Sheet** offering three crash-safe ways to load files:
    1. **Device Storage Browser (Instant In-App List)**: Automatically scans `Downloads`, `Documents`, `Pictures`, and `MediaStore` for PDFs, Office docs, Images, EPUBs, and Text files on the device so the user can tap files to import with one click.
    2. **System File Picker (SAF Multi-Select)**: Launches `OpenMultipleDocuments` (with automatic fallback to `GetMultipleContents` if the device lacks a DocumentsUI activity).
    3. **Connect & Sync Folder**: Launches `OpenDocumentTree` safely inside `runCatching`.

### Phase 2: Crash-Proof File Import & Native `PdfRenderer` Engine
- **Update `/app/src/main/java/com/example/reader/MultiFormatDocumentEngine.kt`**:
  - Add `quickInspectDocument(context, file, format)` used during file import:
    - For PDFs, use native `PdfRenderer(ParcelFileDescriptor)` to read `pageCount` in milliseconds without loading the PDF into PDFBox Java heap.
    - For text/markup/Office files, cap initial preview reading to `128 KB` so huge files never cause `OutOfMemoryError`.
  - Update `openPdfSession()` to use `PDDocument.load(file, password, MemoryUsageSetting.setupTempFileOnly())` and cap synchronous text extraction to the first 15 pages, loading remaining pages lazily.
  - Replace all `catch (e: Exception)` blocks with `catch (t: Throwable)` (catching `OutOfMemoryError` and `Error`) and downsample all raster image decodes via `decodeSampledBitmapFromFile(file, maxDimension = 1440)`.
- **Update `/app/src/main/java/com/example/ui/FolioViewModel.kt`**:
  - Update `importSingleUriInternal()` to:
    - Detect the true file extension using both `OpenableColumns.DISPLAY_NAME` and `MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))` so URIs like `content://.../document/msf:10023` get their proper `.pdf` or `.jpg` extension.
    - Use `quickInspectDocument()` on import so importing 1 or 20 files is instantaneous and never crashes.
    - Wrap the entire import coroutine in `runCatching` (`Throwable`) and report clear status messages via `statusMessage`.

### Phase 3: Memory-Safe CameraX Scanner + System Camera & Gallery Fallbacks
- **Update `/app/src/main/java/com/example/tools/DocumentScannerScreen.kt`**:
  - **Runtime Permission Gate**: Request `Manifest.permission.CAMERA` (and `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` when importing photos) with clear permission status feedback.
  - **Safe `PreviewView` Configuration**:
    - Set `previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE` (`TextureView`) to prevent hardware `SurfaceView` clipping crashes.
    - Bind camera inside `LaunchedEffect(hasCameraPermission, lifecycleOwner)` using a safe selector helper that checks `cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)` and falls back to `DEFAULT_FRONT_CAMERA`.
  - **Three Reliable Capture Methods**:
    1. **In-App CameraX Shutter**: Captures frame, downsamples on `Dispatchers.IO` to `maxDimension = 1600px`, applies scan filter off the main thread, and recycles intermediate bitmaps.
    2. **System Camera App (`ActivityResultContracts.TakePicture`)**: Writes to a `FileProvider` URI in `cacheDir` and decodes with `inSampleSize` on `Dispatchers.IO`—guaranteeing capture even if CameraX is unsupported on a custom ROM.
    3. **Gallery Photo Picker (`GetMultipleContents`)**: Decodes selected images on `Dispatchers.IO` using `inSampleSize` downsampling so 48MP photos never crash Compose `Canvas`.
  - **Async Save PDF**: Move `saveScannedBitmapsToDisk()` off the main thread onto `Dispatchers.IO` with a loading indicator so tapping **Save PDF** never freezes or double-filters bitmaps.

### Phase 4: Bulletproof PDFBox Tool Execution (`WinAnsi` Sanitizer & TempFile Memory)
- **Update `/app/src/main/java/com/example/tools/PdfToolkitEngine.kt` & `/app/src/main/java/com/example/tools/PdfToolkitConversions.kt`**:
  - Add `sanitizePdfText(input: String)` to replace Unicode bullets (`•`), em-dashes (`—`), smart quotes, and non-WinAnsi characters before any `PDPageContentStream.showText()` call.
  - Use `MemoryUsageSetting.setupTempFileOnly()` on `PDDocument.load()` calls and `decodeSampledBitmap()` in `imagesToPdf()`.
  - Catch `Throwable` in `executeTool()` so any low-memory or corrupted-stream error returns a clean `ToolExecutionResult(success = false, errorMessage = ...)` instead of crashing the app.

### Phase 5: Verification & Automated Tests
- Run `compile_applet` to verify zero compilation errors.
- Add Robolectric tests in `ExampleRobolectricTest.kt` verifying:
  - Storage and camera permissions are declared while `INTERNET` and `ACCESS_NETWORK_STATE` remain strictly absent.
  - Importing files with missing extensions or Unicode characters and scanning high-resolution images to PDF complete without exceptions.
- Run `gradle :app:testDebugUnitTest` to confirm all tests pass.
