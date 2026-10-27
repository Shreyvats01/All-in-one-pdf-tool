# Folio — Premium Editorial UI/UX Overhaul & Design System Upgrade

This plan transforms **Folio** from a functional Material 3 utility into a flagship editorial document workspace—resolving every layout overload, design mismatch, and visual gap identified in the Design Audit while updating `DESIGN_AUDIT.md` with the structured screen-by-screen blueprint for **Home**, **Tools**, **Files**, **Reader**, and **Settings**.

## User Review & Critical Decisions

> [!IMPORTANT]
> Your confirmed aesthetic, typographic, and layout choices below govern this redesign.

- **Confirmed Decision 1 — Premium Color Palette**: **Warm Alabaster Paper (`#FAF8F4`), Deep Obsidian Ink (`#141518`), and Muted Royal Indigo (`#434CE6` Light / `#8B95F6` Dark)** with warm parchment surfaces (`#F2EFE9`), subtle stone hairlines (`#E4E0D8`), and true charcoal-obsidian dark mode (`#111215` / `#181A1F`)—eliminating heavy borders-on-boxes in favor of tonal layering and whitespace.
- **Confirmed Decision 2 — Editorial Typography Pairing**: Bundle local **Playfair Display** (`res/font/playfair_display.ttf`) for editorial display headers and section titles paired with **Inter** (`res/font/inter.ttf`) for crisp UI controls, metadata, and tabular numerals.
- **Confirmed Decision 3 — Decluttered Tools Grid**: Replace the overloaded 2-line description cards and full-width orphan stretching with **horizontal Category Filter Pills** + **uniform-height 2-column Bento tiles** (`Icon + 1-line title`, with an empty spacer slot whenever a category has an odd item count so cards never stretch to 100% width).

---

## 1. Overview & Core Concept

- **What It Does**: Elevates Folio's visual craft across all screens and modals—introducing real rendered page-1 document cover thumbnails, swipe-to-action file rows, a consolidated single-bar filter header in Files, harmonized floating Reader/Annotation toolbars, visual page thumbnail selection in the 3-Step Tool Wizard, a tactile numeric PIN pad for App Lock/Vault, a freehand Signature Drawing Pad, and a segmented visual storage bar.
- **Target Audience / Persona**: Discerning readers, designers, legal/financial professionals, and researchers who expect the calm typographic elegance of *Readwise Reader*, *Things 3*, and *Craft* combined with a 100% offline PDF studio.
- **Key Value**: Eliminates visual clutter ("chip fatigue" and cramped toolbars), reduces cognitive load through generous whitespace and clear typographic hierarchy, and makes every interaction—from page flipping to page extraction—tactile and visually grounded.

---

## 2. User Experience & Visual Design (Screen-by-Screen Audit & Upgrade Blueprint)

### Visual Identity & Theme Tokens

- **Light Mode (Warm Alabaster & Ink)**:
  - `--background`: `#FAF8F4` (Warm Alabaster Paper)
  - `--surface`: `#F3F0EA` (Soft Ivory Stone)
  - `--surface-container`: `#EAE6DD` (Warm Parchment Container)
  - `--foreground`: `#141518` (Deep Obsidian Ink)
  - `--muted-foreground`: `#636670` (Warm Slate Muted Ink)
  - `--outline-variant`: `#E2DDD3` (1dp Hairline Divider)
  - `--primary`: `#434CE6` (Muted Royal Indigo)
  - `--primary-container`: `#E2E5FF` (Soft Indigo Wash)
- **Dark Mode (True Obsidian Charcoal)**:
  - `--background`: `#111215` (Deep Obsidian Charcoal—never pure `#000000`)
  - `--surface`: `#181A1F` (Elevated Graphite)
  - `--surface-container`: `#20232A` (Slate Surface Container)
  - `--foreground`: `#F4F2ED` (Warm Alabaster Text)
  - `--muted-foreground`: `#9BA0AE` (Muted Silver-Slate)
  - `--outline-variant`: `#292D36` (Subtle Hairline Border)
  - `--primary`: `#8B95F6` (Luminous Muted Indigo)
  - `--primary-container`: `#262C6A` (Deep Indigo Container)
- **Typography Scale**:
  - `displayLarge` / `displayMedium` / `headlineLarge`: **Playfair Display** (SemiBold/Bold, tight `-0.4sp` tracking) for screen titles and editorial hero headers.
  - `titleMedium` / `bodyLarge` / `labelMedium`: **Inter** with generous `1.5x` line height and tabular figures for page counts, file sizes, and progress metrics.

---

### Screen 1: Home (`HomeScreen`)
- **Intended Minimal/Premium Look**: A calm, magazine-like reading desk. Generous top breathing room, a Playfair Display `"Folio"` masthead, an understated search pill, a rich **"Continue Reading" Editorial Showcase Card** featuring a real rendered page-1 miniature cover with a subtle paper edge, a refined 4-icon Quick Actions dock, and borderless file rows separated by 1dp hairline dividers.
- **Layout Overloads Fixed**:
  - *Before*: Search history chips were permanently rendered below the search bar, and every file row had a heavy `1.dp` border, filled box, star icon, and delete icon.
  - *After*: Search history chips appear **only when the search field is active/focused**. File rows use clean hairline dividers, real miniature paper cover thumbnails, and `SwipeToDismissBox` gestures (swipe right to Favourite, swipe left to Trash) plus a single subtle `⋯` menu.
- **Design Mismatches Fixed**:
  - Replaces generic 4-letter text squares (`PDF`, `DOCX`) with asynchronous rendered miniature page previews (`PaperThumbnailPreview`).

### Screen 2: Tools (`ToolsScreen`)
- **Intended Minimal/Premium Look**: A precision Swiss instrument panel. Pinned recent tools appear as sleek monochrome-and-indigo pills at the top, followed by a single horizontal **Category Filter Bar** (`All`, `Organize`, `Optimize`, `Convert To`, `Convert From`, `Annotate`, `Enhance`, `Security`) and a uniform 2-column Bento grid.
- **Layout Overloads Fixed**:
  - *Before*: 31 cards all showed 2-line descriptions simultaneously, and categories with an odd number of tools (3 or 5) stretched the final orphan card across 100% of the screen width.
  - *After*: Each Bento tile has a fixed, uniform height (`84.dp`) displaying the thin-line icon + crisp 1-line title + subtle subtitle truncation, and odd-numbered rows automatically insert an invisible `Spacer(Modifier.weight(1f))` so no card ever stretches to double width.

### Screen 3: Files (`FilesScreen` — List & Grid)
- **Intended Minimal/Premium Look**: An archival library shelf. Top segmented switcher (`Library` • `Private Vault` • `Trash`), a single unified **Filter & Sort Bar** combining Folder/Tag selection with a compact Sort dropdown and Grid/List toggle, and real vertical document cover cards in Grid mode.
- **Layout Overloads Fixed**:
  - *Before*: Stacked 4 consecutive horizontal rows of `FilterChip`s (`Section`, `Folders`, `Tags`, `Sort by`) consuming ~190dp at the top of the screen.
  - *After*: Consolidates `Section` into a sleek 3-segment pill bar and merges `Folder`, `Tag`, `Sort`, and `Grid/List` into a single compact toolbar row with dropdown/sheet pickers—reclaiming over 120dp of vertical whitespace.
- **Design Mismatches Fixed**:
  - Grid mode now displays true vertical aspect-ratio document cover cards with a `⋯` action button, and Private Vault uses a tactile **Numeric PIN Keypad** with animated dot indicators instead of a raw text field.

### Screen 4: Universal Reader (`UniversalReaderScreen` with Reading & Editing Toolbars)
- **Intended Minimal/Premium Look**: The document page is the undisputed hero. Chrome floats lightly with rounded pill geometry (`24.dp`–`28.dp` corners) and identical height/elevation tokens whether in Reading mode or Annotation mode.
- **Layout Overloads Fixed**:
  - *Before*: Opening search, the full-width bottom page slider, the 15-chip annotation bar, and the bottom icon bar consumed ~40% of vertical screen space.
  - *After*: Replaces the bulky double-decker bottom dock with a **Floating Reader Pill Dock** and a compact **Contextual Annotation Floating Pill** that replaces (rather than stacks on top of) the reading dock when Annotate mode is active.
- **Design Mismatches Fixed**:
  - Harmonizes the Top Bar, Reading Pill, and Annotation Pill with matching tonal elevation and icon sizing.
  - Upgrades the **Thumbnails Tab** in the Navigation Sheet to render **real asynchronous page bitmaps** instead of generic book icons.
  - Upgrades the **Annotation Canvas** so `Highlight` draws a wide translucent marker band, `Rectangle` and `Circle` draw crisp geometric shapes, `Arrow` draws a directional vector, and `Pen` draws smooth freehand ink.

### Screen 5: Settings (`SettingsScreen`) & 3-Step Tool Wizard (`UnifiedToolWizardScreen`)
- **Intended Minimal/Premium Look**:
  - **Settings**: Clean grouped editorial sections with a **Segmented Visual Storage Bar** (color-coded by Library, Outputs, Vault, and Cache), a hidden/masked PIN change dialog, and an interactive **Freehand Signature Drawing Pad Modal** where users can draw their signature with a finger/stylus.
  - **3-Step Tool Wizard**: Step 2 now features a **Visual Page Thumbnail Grid Picker** (showing rendered miniature page previews with selection checkmarks and rotation badges) instead of plain `[Page 1]` text chips, and Step 3 displays an **Inline Rendered Output Page Preview** right inside the Result screen.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Playfair Display + Inter Local Font Pairing**
  - *Chosen Approach*: Install `Playfair Display` via `font-util` into `app/src/main/res/font/playfair_display.ttf` and pair it with `Inter` in `Type.kt`.
  - *Why*: Gives Folio an unmistakable high-craft editorial personality on mastheads, onboarding headers, and hero cards while keeping dense tool controls and page numbers ultra-legible in Inter.
- **Decision 2 — Asynchronous Paper Thumbnail Component (`DocumentPageThumbnail`)**
  - *Chosen Approach*: Build a reusable Compose component backed by `MultiFormatDocumentEngine`'s memory-safe `LruCache` that renders miniature page bitmaps off the main thread (`Dispatchers.IO`) with a warm paper background, subtle border, and shimmer placeholder.
  - *Why*: Eliminates the "developer placeholder" look across Home, Files (List & Grid), Reader Thumbnails sheet, Tool Wizard Step 2 page picker, and Tool Wizard Step 3 result preview.
- **Decision 3 — Unified Mode-Switching Floating Dock in Reader**
  - *Chosen Approach*: Instead of stacking the Annotation Toolbar on top of the Bottom AppBar and Scrubber, switch cleanly between the **Reading Floating Pill** and the **Editing/Annotation Floating Pill** using `AnimatedContent`.
  - *Why*: Preserves maximum vertical reading canvas and eliminates toolbar clashing.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Upgrade Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     PREMIUM EDITORIAL DESIGN SYSTEM LAYER                    │
│  • Typography: Playfair Display (Headers) + Inter (UI/Body/Tabular Numerals) │
│  • Palette: Warm Alabaster (#FAF8F4) / Obsidian (#111215) / Indigo (#434CE6) │
│  • Shared Components: DocumentPageThumbnail, TactilePinPad, SignaturePadDialog│
└───────────────┬──────────────────────────────────────────────┬───────────────┘
                ▼                                              ▼
┌───────────────────────────────────────┐      ┌───────────────────────────────┐
│      DECLUTTERED WORKSPACE SHELL      │      │   HARMONIZED READER & WIZARD  │
│ • HomeScreen: Editorial Hero w/ live  │      │ • UniversalReaderScreen:      │
│   page-1 cover + SwipeToDismiss rows  │      │   Unified Floating Pill Dock  │
│ • ToolsScreen: Category filter pills  │      │   + Real Bitmap Thumbnails    │
│   + Fixed-height 2-col Bento grid     │      │   + Geometric/Highlight Canvas│
│ • FilesScreen: Single-row Filter/Sort │      │ • UnifiedToolWizardScreen:    │
│   toolbar + Cover Grid + PinPad Vault │      │   Visual Page Thumbnail Grid  │
│ • SettingsScreen: Segmented Storage   │      │   Picker (Step 2) + Inline    │
│   Bar + Freehand Signature Pad Modal  │      │   Output Preview (Step 3)     │
└───────────────────────────────────────┘      └───────────────────────────────┘
```

### Interactive Component & State Mapping

1. **`DESIGN_AUDIT.md`**: Updated and structured around the 5 core screens (`Home`, `Tools`, `Files`, `Reader`, `Settings`) plus Onboarding and Wizard, detailing the intended minimal/premium look, layout overloads, design mismatches, and how whitespace and typography resolve each issue.
2. **`DocumentPageThumbnail` Composable**: Renders any `File` and `pageIndex` via `MultiFormatDocumentEngine.renderPageBitmap()` into an aspect-ratio paper card with optional selection badge and rotation indicator.
3. **`TactilePinPad` & `SignaturePadDialog` Composables**:
   - `TactilePinPad`: 4-dot indicator + 3x4 circular numeric keypad + Biometric trigger for App Lock and Private Vault.
   - `SignaturePadDialog`: Interactive freehand `Canvas` drawing pad + typed script preview, saving serialized stroke points to `SignatureEntity`.
4. **Floating Dock Scrim & Tab Animation**: Compact floating navigation bar with a soft bottom gradient scrim and smooth pill indicator.
