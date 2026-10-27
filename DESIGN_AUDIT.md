# Folio — Editorial Design Audit & Flagship UI/UX Specification

This document provides the complete architectural design audit for **Folio** across its 5 primary screens (**Home**, **Tools**, **Files**, **Reader**, and **Settings**) plus **Onboarding** and the **3-Step Tool Wizard**. For each screen, it details:
1. **Intended Minimal / Premium Look**
2. **Identified Layout Overloads & How They Were Resolved**
3. **Identified Design Mismatches & How They Were Harmonized**
4. **How Whitespace, Editorial Typography (`Playfair Display` + `Inter`), and the Warm Alabaster / Obsidian / Royal Indigo Palette Elevate the Experience**

---

## 1. Foundation: Premium Colour Schema & Editorial Typography

### Colour Schema (Warm Alabaster, Deep Obsidian & Muted Royal Indigo)
| Token | Light Mode (Alabaster Paper) | Dark Mode (Obsidian Charcoal) | Role & Design Rationale |
| :--- | :--- | :--- | :--- |
| `background` | `#FAF8F4` | `#111215` | Warm archival paper in light mode; true obsidian charcoal (never harsh `#000000`) in dark mode. |
| `surface` | `#F3F0EA` | `#181A1F` | Subtle tonal card elevation without drop shadows or heavy box borders. |
| `surfaceVariant` | `#EAE6DD` | `#20232A` | Secondary containers, active filter pills, and Bento tile wells. |
| `onBackground` / `onSurface` | `#141518` | `#F4F2ED` | High-contrast ink for effortless reading and scannability. |
| `onSurfaceVariant` | `#636670` | `#9BA0AE` | Calm slate-stone metadata, timestamps, and subtitles. |
| `outlineVariant` | `#E2DDD3` | `#292D36` | 1dp architectural hairline dividers separating borderless rows. |
| `primary` (Single Accent) | `#434CE6` | `#8B95F6` | Muted Royal Indigo—used with restraint for primary actions, active states, and progress rings. |
| `error` (Destructive) | `#DC2626` | `#F87171` | Reserved strictly for permanent deletion, redaction warnings, and error states. |

### Typography System (`Playfair Display` + `Inter`)
- **Display & Editorial Mastheads (`Playfair Display`)**: Bundled locally (`res/font/playfair_display.ttf`). Used for screen mastheads (`Home`, `Toolkit`, `Archive`, `Settings`), onboarding headlines, and the `"Continue Reading"` showcase title to evoke the authority and warmth of a printed monograph.
- **UI Controls, Body & Tabular Metrics (`Inter`)**: Bundled locally (`res/font/inter.ttf`). Used for all interactive controls, Bento tiles, file rows, and numeric badges with generous line heights (`1.5x`) and crisp letter spacing.

---

## 2. Screen-by-Screen Audit & Resolution

### Screen 1: Home (`HomeScreen`)

#### Intended Minimal / Premium Look
A serene, magazine-inspired reading desk. Top masthead set in **Playfair Display**, a clean search pill, a prominent **"Continue Reading" Editorial Showcase Card** featuring a live rendered page-1 paper cover thumbnail on the left and reading progress on the right, a 4-action Quick Studio dock (`Scan`, `Merge`, `Compress`, `Sign`), and borderless file rows separated by 1dp hairline dividers (`#E2DDD3`).

#### Layout Overloads Identified & Resolved
- **Overload 1 — Always-Visible Search History Chips**:
  - *Previous Problem*: Recent search chips were rendered permanently below the search input even when the user wasn't searching, pushing the hero card down and adding clutter.
  - *Resolution*: Search history chips now appear **only when the search bar is actively focused or being typed into**.
- **Overload 2 — Heavy Boxed File Rows with Redundant Action Buttons**:
  - *Previous Problem*: Every file row was wrapped in a bordered card (`1.dp` border + container fill) and displayed both a Star button and a Trash button on the right edge.
  - *Resolution*: Replaced boxed cards with **borderless editorial rows separated by 1dp hairline dividers**, added **gesture-first `SwipeToDismissBox`** (swipe right to toggle Favourite, swipe left to move to Trash with Snackbar Undo), and kept only a subtle favourite indicator and row tap action.

#### Design Mismatches Identified & Resolved
- **Mismatch — 4-Letter Text Squares Instead of Paper Thumbnails**:
  - *Previous Problem*: Document rows and the "Continue Reading" card showed a `44.dp` square with text (`PDF`, `EPUB`) instead of looking like actual documents.
  - *Resolution*: Introduced `DocumentPageThumbnail`, which asynchronously renders page 1 of the document into a vertical paper-proportioned miniature cover (`44x58.dp` in rows, `72x96.dp` in the hero card) with a crisp paper border.

---

### Screen 2: Tools Grid (`ToolsScreen`)

#### Intended Minimal / Premium Look
A precision Swiss instrument grid. Pinned recent tools sit in a compact horizontal strip of monochrome-and-indigo pills, followed by a single **Category Filter Bar** (`All`, `Organize`, `Optimize`, `Convert To`, `Convert From`, `Annotate`, `Enhance`, `Security`) and a uniform 2-column Bento grid where every tile has identical geometry.

#### Layout Overloads Identified & Resolved
- **Overload 1 — 31 Verbose Cards Creating Endless Vertical Scroll**:
  - *Previous Problem*: Rendering all 31 tools with 2-line descriptions across 8 stacked categories created a wall of text.
  - *Resolution*: Added **Category Filter Pills** for instant single-category focus and redesigned each tool tile into a **uniform `76.dp`-tall Bento card** displaying a thin-line icon in a tonal well + a crisp 1-line title + a concise 1-line muted subtitle.
- **Overload 2 — Heavy Indigo Blocks in Pinned Tools**:
  - *Previous Problem*: Pinned recent tools used saturated `primaryContainer` blocks that overpowered the top of the screen.
  - *Resolution*: Styled pinned tools as refined warm-stone pills with a subtle indigo icon and hairline border.

#### Design Mismatches Identified & Resolved
- **Mismatch — Orphaned Odd-Count Cards Stretching to 100% Width**:
  - *Previous Problem*: Using `Modifier.weight(1f)` inside `FlowRow(maxItemsInEachRow = 2)` caused the last card in categories with 3 or 5 tools (`Optimize & OCR`, `Convert to PDF`, `Security & Privacy`) to stretch across the entire width of the screen.
  - *Resolution*: Paired tools into explicit 2-column rows (`chunked(2)`) and inserted `Spacer(Modifier.weight(1f))` whenever a row has 1 item—guaranteeing every Bento tile is strictly 50% width.

---

### Screen 3: Files — Library, Grid/List, Private Vault & Trash (`FilesScreen`)

#### Intended Minimal / Premium Look
An archival document library. A top 3-segment switcher (`Library`, `Private Vault`, `Trash`), a **single consolidated Filter & Sort Toolbar** (`Folder` dropdown pill, `Tag` dropdown pill, `Sort` dropdown pill, and `Grid/List` toggle), and either hairline-divided list rows or a true vertical-cover Grid view.

#### Layout Overloads Identified & Resolved
- **Overload — 4 Stacked Rows of `FilterChip`s (~190dp of Clutter)**:
  - *Previous Problem*: Stacking Section chips, Folder chips, Tag chips, and Sort chips in 4 separate horizontal rows pushed the file list nearly halfway down the screen.
  - *Resolution*: Consolidated Folders, Tags, Sort order, and Grid/List toggle into **one compact architectural toolbar row** using clean dropdown menus and active filter badges—reclaiming over `120.dp` of vertical breathing room.

#### Design Mismatches Identified & Resolved
- **Mismatch 1 — Grid View Missing Thumbnails & Actions**:
  - *Previous Problem*: Switching to Grid View showed text-only boxes and hid the `⋮` action menu.
  - *Resolution*: Grid View now renders a **vertical `DocumentPageThumbnail` cover preview** at the top of each card alongside the title, metadata, and `⋮` action menu.
- **Mismatch 2 — Raw Text Input for Private Vault PIN**:
  - *Previous Problem*: Unlocking the Private Vault used a standard keyboard `OutlinedTextField`.
  - *Resolution*: Replaced with `TactilePinPad`—a minimalist 4-dot PIN indicator and circular numeric keypad with haptic feedback and one-tap Biometric authentication.

---

### Screen 4: Universal Reader & Toolbars (`UniversalReaderScreen`)

#### Intended Minimal / Premium Look
Full-bleed reading immersion where chrome floats lightly over the document canvas with unified pill geometry (`28.dp` radius), consistent `1.dp` hairline borders, and zero visual jump when switching between Reading mode and Annotation/Editing mode.

#### Layout Overloads Identified & Resolved
- **Overload — Multi-Tier Bottom Dock Consuming ~40% of Screen Height**:
  - *Previous Problem*: Stacking the full-width page slider, the 15-chip Annotation studio bar, and the BottomAppBar simultaneously crowded the document viewport.
  - *Resolution*: Replaced the full-width stacked bars with a **Unified Mode-Switching Floating Dock** (`AnimatedContent`) that cleanly transitions between the **Reading Floating Pill** (with an integrated compact page scrubber pill) and the **Editing/Annotation Floating Pill** without stacking them on top of each other.

#### Design Mismatches Identified & Resolved
- **Mismatch 1 — Reading vs. Editing Toolbar Inconsistency**:
  - *Previous Problem*: The reading bar was a flat `BottomAppBar` while the annotation bar was a boxy rectangular strip with `FilterChip`s.
  - *Resolution*: Both toolbars now share identical floating pill geometry, elevation, and icon button rhythm.
- **Mismatch 2 — Thumbnails Sheet Showing Book Icons Instead of Page Previews**:
  - *Previous Problem*: Opening the Navigation Sheet's `Thumbnails` tab displayed generic `MenuBook` icons instead of actual document pages.
  - *Resolution*: The Thumbnails grid now renders **real `DocumentPageThumbnail` bitmaps** for every page in the document with an active indigo ring around the current page.
- **Mismatch 3 — Annotation Canvas Drawing Identical Paths for All Tools**:
  - *Previous Problem*: Selecting `Highlight`, `Rectangle`, `Circle`, or `Arrow` drew the same thin freehand line.
  - *Resolution*: The interactive overlay canvas now renders **translucent wide marker bands** for `Highlight`, **crisp geometric rectangles/circles** for `Rectangle`/`Circle`, **directional vectors** for `Arrow`/`Line`, and smooth ink curves for `Pen`.

---

### Screen 5: Settings (`SettingsScreen`) & 3-Step Tool Wizard (`UnifiedToolWizardScreen`)

#### Intended Minimal / Premium Look
- **Settings**: Editorial section headers in **Playfair Display**, clean grouped cards with 1dp hairline row dividers, a **Segmented Visual Storage Bar** illustrating proportional space used by Library, Outputs, Vault, and Cache, a modal **Change PIN Dialog**, and an interactive **Freehand Signature Drawing Pad Modal** (`SignaturePadDialog`).
- **3-Step Tool Wizard**: A streamlined `Pick → Configure → Result` studio where Step 2 displays a **Visual Page Thumbnail Grid Picker** (tap real page thumbnails to toggle selection) and Step 3 renders an **Inline Output Page Preview** alongside the before/after savings metrics.
