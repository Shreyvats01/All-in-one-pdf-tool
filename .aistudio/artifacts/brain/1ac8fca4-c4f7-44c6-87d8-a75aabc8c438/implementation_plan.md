# Folio — Complete UI/UX Redesign & Functional PDF Engine Overhaul

A complete visual and functional overhaul of **Folio** that eliminates every layout misalignment, removes all temporary dummy data, connects directly to real user files via Android's Storage Access Framework (SAF) with persistent permissions, and upgrades the PDF toolkit and scanner with dedicated visual workspaces and automatic multi-format conversion.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and design decisions incorporate your confirmed preferences from our planning interview:

- **Confirmed Visual Aesthetic & Navigation**: **Clean Modern Studio** palette (crisp white/slate surfaces `#F8FAFC` / `#FFFFFF`, deep slate typography `#0F172A`, electric cobalt accent `#2563EB`) paired with a **standard Material 3 `NavigationBar`** anchored inside `Scaffold(bottomBar = ...)` so tabs never clip, wrap, or overlap content.
- **Confirmed Real File Access & Zero Dummy Data**: All pre-seeded sample files (`Release_Notes_2026.md`, `Department_Budget_Matrix.csv`, `Field_Research_Handbook.epub`, `Folio_Architecture_Guide.pdf`, etc.) and dummy presets are completely removed, including an automatic one-time cleanup of any existing dummy files on disk. The app now prompts users to pick real files or grant persistent folder access via Android SAF (`OpenDocument`, `OpenMultipleDocuments`, and `OpenDocumentTree`).
- **Confirmed PDF Tool Workflows**:
  1. **Dedicated Visual Workspaces** for **Scan**, **Merge**, **Sign**, and **Split / Organize** with live page thumbnails and interactive controls.
  2. **Automatic Format Normalization**: Selecting non-PDF files (Images, Word/Excel/PowerPoint, Markdown, CSV, TXT, EPUB) inside any PDF tool automatically converts them into valid PDF pages on the fly instead of failing with a parser error.

---

## 1. Root-Cause Diagnosis of Current Issues

### Why the Current UI Breaks & Overlaps (From Your Screenshots)
1. **Oversized Bottom Bar Clipping (`MainActivity.kt`)**: The custom `FloatingBottomNavigationBar` placed 4 horizontal pill buttons (`Icon` + `Text` + `16.dp` horizontal padding) inside a single `Row`. On phone screens, 4 horizontal pills exceed the screen width, forcing the 4th tab (**Settings**) to wrap/clip and stretching the bottom surface into a giant block covering the lower third of the screen.
2. **Squeezed Vertical Pill Buttons (`UnifiedToolWizardScreen.kt` & `FilesAndSettingsScreens.kt`)**:
   - In **Merge PDF / Sign PDF**, `Row` placed a long `Text("Select one or more files (reorder below)")` without `Modifier.weight(1f)` next to `OutlinedButton("Browse Device (SAF)")`. The button was crushed into a `40.dp` wide vertical strip stretching `300.dp` tall.
   - In **Archive (Files)**, 3 `FilterChip`s (`All Folders`, `All Tags`, `Recent`) and a view-toggle icon were packed into a non-scrollable `Row`, crushing the 3rd chip into a vertical empty pill.
   - In **Home Quick Studio**, 4 narrow cards lacked `maxLines = 1` and compact typography, causing `"Compress"` to break mid-word as `"Compres / s"` and `"Open File"` to wrap unevenly.
3. **Excessive Text Density**: Long multi-line technical subtitles (`"Scoped Storage SAF • Colour-free tags • AES-256 Vault"`, `"31 offline instruments • Non-destructive workflow"`) cluttered headers and competed with primary actions.

### Why Tools Were Failing to Execute
1. **Non-PDF Dummy Files Pre-Selected in PDF Tools**: `FolioViewModel.startToolFlow()` automatically pre-selected the top 1–2 files from the library (`Release_Notes_2026.md` and `Department_Budget_Matrix.csv`). When `PdfToolkitEngine` ran `PDDocument.load(file)` on a `.md` or `.csv` file during Merge, Sign, or Compress, PDFBox threw an immediate exception (`Malformed document structure`).
2. **Missing Direct Device File Picker in Tool Flows**: Opening a tool did not let users directly pick multiple real PDFs or images from their device without leaving the flow.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme (`Clean Modern Studio`)
- **Aesthetic Direction**: High-precision utility studio inspired by Adobe Acrobat Liquid Mode, Apple Files, and Linear—prioritizing scannable hierarchy, generous whitespace, strict `8.dp` spatial grid alignment, and zero visual clutter.
- **Color Palette**:
  - **Light Mode**: Background `#F8FAFC` (Cool Alabaster Slate), Elevated Cards `#FFFFFF` (Pure Crisp White), Subtle Containers `#F1F5F9` (Slate 100), Primary Text `#0F172A` (Slate 900), Secondary Text `#475569` (Slate 600), Primary Accent `#2563EB` (Studio Cobalt), Borders `#E2E8F0` (Slate 200).
  - **Dark Mode**: Background `#0B0F19` (Deep Slate Night), Elevated Cards `#111827` (Slate 900), Subtle Containers `#1E293B` (Slate 800), Primary Text `#F8FAFC`, Secondary Text `#94A3B8`, Primary Accent `#60A5FA` (Cobalt 400), Borders `#1E293B`.
- **Typography & Hierarchy**:
  - **Inter** across all UI controls, tool cards, badges, and metadata for crisp legibility at every size.
  - Strict single-line enforcement (`maxLines = 1`, `overflow = TextOverflow.Ellipsis`) on all navigation items, chips, action buttons, and card titles so words never break mid-word.
- **Layout & Spacing Rules**:
  - Standard Material 3 `NavigationBar` at the bottom with 4 balanced vertical items (`Home`, `Tools`, `Files`, `Settings`).
  - Every `Row` with a title and action button assigns `Modifier.weight(1f)` to the title column and `Modifier.wrapContentWidth()` to the button.
  - Filter bars use horizontally scrollable `LazyRow` containers so chips never compress into vertical strips.

### Redesigned Key User Flows
1. **Home Workspace**:
   - Clean top app bar with app title and a single-line **Import Files** primary button.
   - **Import & Permission Hero Card** when the workspace is empty: prompts the user to **Pick Files from Device** (multi-file SAF) or **Connect Device Folder** (persistent directory access).
   - **Quick Actions Grid**: 4 balanced studio tiles (`Scan`, `Merge`, `Split`, `Sign`) with crisp icons and single-line labels.
   - **Continue Reading Banner**: Compact horizontal card with page thumbnail, progress bar, and one-tap resume.
2. **Dedicated Visual Tool Workspaces**:
   - **Merge PDF Studio**: Shows a reorderable visual stack of selected files with page thumbnails, file sizes, format badges (auto-converting Images/Office/Text if included), up/down/remove buttons, and an **+ Add Device Files** button that launches the multi-file SAF picker directly.
   - **Split & Organize Studio**: Displays an interactive 3-column thumbnail grid of all pages in the selected PDF. Users tap pages to toggle selection, rotate pages, or switch between *Extract Selected Pages*, *Split Every N Pages*, and *Split into Single Pages*.
   - **Sign PDF Studio**: Combines a live rendered preview of the target page with an interactive signature placement selector (Bottom Right, Bottom Left, Center, Top Right) and an inline freehand/typed signature pad.
   - **CameraX Document Scanner**: Full-screen camera viewfinder with live document boundary guide, flash control, **Import from Gallery** button, bottom filmstrip of captured pages, filter pills (`Enhanced`, `B&W Crisp`, `Grayscale`, `Original`), and one-tap **Save as PDF**.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Standard M3 `NavigationBar` in `Scaffold` vs. Floating Custom Overlay Bar**
  - *Chosen Approach*: Use Jetpack Compose Material 3 `NavigationBar` inside `Scaffold(bottomBar = ...)`.
  - *Why*: Eliminates content overlap, guarantees all 4 tabs (`Home`, `Tools`, `Files`, `Settings`) fit comfortably on narrow screens (`320dp+`), and handles Android gesture navigation insets automatically.
- **Decision 2: Universal Input Pre-Processor (`ensurePdfFile`) in `PdfToolkitEngine`**
  - *Chosen Approach*: Before any PDF tool (`Merge`, `Sign`, `Split`, `Compress`, `Watermark`, `Protect`, etc.) loads a file with `PDDocument.load()`, `PdfToolkitEngine` checks the file format. If the file is an Image, DOCX, XLSX, PPTX, EPUB, MD, CSV, or TXT, it automatically renders/converts it into a temporary valid PDF first.
  - *Why*: Users can freely mix PDFs, photos, and documents in tools like Merge or Sign without ever encountering format crashes.
- **Decision 3: Persistent Android SAF Multi-File & Folder Access vs. Dummy Seed Files**
  - *Chosen Approach*: Purge `SampleSeedGenerator` dummy files and wire `OpenMultipleDocuments` + `OpenDocumentTree` with `takePersistableUriPermission`.
  - *Why*: Respects Android Scoped Storage policies while allowing the app to operate on the user's real device documents and folders.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram
```
┌───────────────────────────────────────────────────────────────────────────┐
│                         MainActivity & Scaffold                           │
│  ┌─────────────────┬─────────────────┬─────────────────┬───────────────┐  │
│  │   HomeScreen    │   ToolsScreen   │   FilesScreen   │ SettingsScreen│  │
│  └────────┬────────┴────────┬────────┴────────┬────────┴───────┬───────┘  │
│           └─────────────────┼─────────────────┼────────────────┘          │
│                             ▼                 ▼                           │
│             ┌───────────────────────────────────────────────┐             │
│             │           FolioViewModel (StateFlow)          │             │
│             └───────┬───────────────┬───────────────┬───────┘             │
└─────────────────────┼───────────────┼───────────────┼─────────────────────┘
                      │               │               │
        ┌─────────────┘               │               └─────────────┐
        ▼                             ▼                             ▼
┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────────┐
│ SAF File & Folder IO │   │   Dedicated Visual   │   │  Room Local Database │
│ • OpenMultipleDocs   │   │    Tool Workspaces   │   │ • Imported User Docs │
│ • OpenDocumentTree   │   │ • Scan (CameraX)     │   │ • Folders & Tags     │
│ • Persistable Grants │   │ • Merge / Reorder    │   │ • Bookmarks & Ink    │
│ • Zero Dummy Data    │   │ • Split / Page Grid  │   │ • Saved Signatures   │
└──────────┬───────────┘   │ • Sign / Live Preview│   └──────────────────────┘
           │               └──────────┬───────────┘
           └──────────────┬───────────┘
                          ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                   PdfToolkitEngine & Format Pre-Processor                 │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ ensurePdfFile(): Auto-converts Image / Office / MD / CSV → Temp PDF │  │
│  └──────────────────────────────────┬──────────────────────────────────┘  │
│                                     ▼                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ PDFBox-Android + Android PdfRenderer Execution (All 31 PDF Tools)   │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
```

### Interactive Component & State Mapping
1. **Device File & Folder Import (`FolioViewModel`)**:
   - `importMultipleSafUris(uris: List<Uri>, openFirst: Boolean, addToActiveWizard: Boolean)`: Takes persistable read permissions, copies/indexes real user files into the workspace, and if a tool wizard is active, immediately appends them to `wizardState.selectedDocs`.
   - `importSafFolderTree(treeUri: Uri)`: Iterates through supported document/image files in the user-selected folder and imports them into the library.
   - `purgeLegacyDummySeedFiles()`: Runs on startup to delete any previously generated sample files (`Release_Notes_2026.md`, `Department_Budget_Matrix.csv`, `Field_Research_Handbook.epub`, `Folio_Architecture_Guide.pdf`, `Quarterly_Financial_Report.pdf`, `Service_Agreement_Draft.docx`) from both disk and Room DB.
2. **Tool Wizard & Dedicated Workspaces (`UnifiedToolWizardScreen` & `DocumentScannerScreen`)**:
   - **Step 1 (Pick & Order Files)**: Displays an **Add Files from Device** button (`OpenMultipleDocuments`) alongside existing workspace files. Shows format compatibility badges and drag/arrow reordering for multi-file tools.
   - **Step 2 (Visual Configure)**: Renders dedicated interactive canvases for **Merge** (document stack + bookmark options), **Split / Organize / Extract / Remove / Rotate** (interactive multi-page thumbnail grid), **Sign** (page preview with signature placement overlay + freehand pad), and **Compress** (live savings estimator).
   - **Step 3 (Result)**: Shows before/after file size metrics, page count, **Open in Reader**, **Save Copy to Device (SAF)**, **Share**, and **Continue in Another Tool**.
