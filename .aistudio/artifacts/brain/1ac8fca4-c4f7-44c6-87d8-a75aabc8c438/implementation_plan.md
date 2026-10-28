# Folio — Automated GitHub CI/CD & Android Release Pipeline

An automated, zero-touch GitHub Actions release pipeline and continuous integration suite that validates Folio's 100% offline guarantee, runs unit/Robolectric and Android Lint checks, builds signed release APK and AAB artifacts with dynamic version codes, generates SHA-256 integrity checksums and structured release notes, and publishes everything directly to GitHub Releases.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and release pipeline decisions reflect your confirmed selections and respect all AI Studio Android signing constraints.

- **Confirmed Decision 1 — Release Trigger & Versioning**: Triggered automatically when pushing a semantic version tag (`v*.*.*`, such as `v1.2.0`) or on demand via `workflow_dispatch` with a custom version input. In addition, a continuous integration workflow runs on every pull request and push to `main` to catch regressions early.
- **Confirmed Decision 2 — Published Release Artifacts**: Publishes a signed Universal Release `.apk`, a signed Play Store `.aab` (Android App Bundle), and a `SHA256SUMS.txt` cryptographic verification manifest directly to **GitHub Releases**.
- **Confirmed Decision 3 — Full Quality & Offline Security Gate**: Every release must pass four automated gates before packaging:
  1. **100% Offline Manifest & Merged Permission Audit**: Verifies that neither `android.permission.INTERNET` nor `android.permission.ACCESS_NETWORK_STATE` exists in the source manifest or the final merged APK manifest (`aapt2 dump permissions`).
  2. **Unit & Robolectric Test Suite**: Runs `testDebugUnitTest` across all 31 PDF tools, universal format parsers, and UI flows.
  3. **Android Lint & Static Analysis**: Runs `lintRelease` / `lintDebug` to catch API compatibility and resource issues.
  4. **Automated Changelog Generation**: Compiles categorized commit history since the previous Git tag into human-readable release notes with installation and SHA-256 verification instructions.

> [!NOTE]
> **Preserving AI Studio Signing Configs**: In accordance with platform constraints, the existing `signingConfigs` (`release` and `debugConfig`) in the Gradle build configuration remain untouched and backwards-compatible. The GitHub Actions workflow injects `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` via environment variables decoded from GitHub Actions Secrets (with an automatic fallback to a generated ephemeral release keystore if secrets are not yet configured in a fork), while dynamic `VERSION_NAME` and `VERSION_CODE` environment variables automatically populate release metadata.

---

## 1. Overview & Core Concept

- **What It Does**: Provides a complete CI/CD automation system for Folio consisting of:
  1. **Continuous Integration Workflow (`Android CI`)**: Runs on every push and pull request to `main` to verify compilation, unit/Robolectric tests, Android Lint, and strict offline permission compliance.
  2. **Automated Release Pipeline (`Android Release`)**: Runs on `v*.*.*` tags or manual dispatch to compute deterministic `versionCode` and `versionName` values, execute the full verification gate, build and sign the production `.apk` and `.aab`, verify the compiled binary contains zero network permissions, generate `SHA256SUMS.txt`, and publish a formatted GitHub Release.
  3. **In-App Release & Offline Integrity Card**: Enhances the **Settings → About & Privacy** section inside Folio so users can view the active semantic version, build channel, signing/integrity status, and 100% offline verification badge directly on their device.
- **Target Audience / Persona**: Android end-users downloading signed APKs from GitHub Releases (or F-Droid / Obtainium compatible feeds) who care about verifiable offline privacy and cryptographic checksums, as well as maintainers releasing updates with a single `git tag v1.1.0 && git push --tags`.
- **Key Value**: Eliminates manual APK building, signing, checksum calculation, and release note drafting while mathematically guaranteeing that no third-party dependency ever sneaks `INTERNET` permissions into a production release.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Maintainer Tag Release Flow**:
   - Maintainer pushes a tag (`git tag v1.2.0 && git push origin v1.2.0`) or clicks **Run workflow** in the GitHub Actions tab and enters `1.2.0`.
   - GitHub Actions spins up the pipeline, validates the 100% offline manifest rule, runs the Robolectric test suite and Lint, compiles and signs `Folio-v1.2.0-universal-release.apk` and `Folio-v1.2.0-release.aab`, verifies the built APK with `aapt2`, generates `SHA256SUMS.txt`, and publishes a complete GitHub Release with categorized release notes.
2. **Android User Download & Obtainium / GitHub Release Flow**:
   - Android users visit the GitHub Releases page (or point an updater like Obtainium to the repository).
   - Users see the structured release changelog, download `Folio-v1.2.0-universal-release.apk`, and optionally verify its SHA-256 fingerprint against `SHA256SUMS.txt`.
3. **In-App Release & Offline Verification Flow (Settings → About)**:
   - Inside Folio's **Settings** screen, the **About & Offline Integrity** card displays the current `versionName` (`BuildConfig.VERSION_NAME`), `versionCode` (`BuildConfig.VERSION_CODE`), package identifier, and an interactive **Verify Runtime Offline Guarantee** inspector that confirms zero network permissions are granted to the installed APK at runtime.

### Visual Identity & Theme (In-App Release & Integrity Inspector)
- **Aesthetic Direction**: Warm Editorial Paper & Obsidian Ink aesthetic matching Folio's redesigned UI.
- **Color Palette & Mood**:
  - Light Surface: Warm Alabaster (`#FAF8F4`) & Soft Parchment (`#F3F0EA`) with 1dp Stone Hairlines (`#E4E0D8`).
  - Dark Surface: True Charcoal (`#111215`) & Elevated Slate (`#181A1F`) with 1dp Hairlines (`#262932`).
  - Accent: Muted Royal Indigo (`#434CE6` Light / `#8B95F6` Dark).
- **Typography & Hierarchy**:
  - Section Title in bundled **Playfair Display** (`titleMedium` / `headlineMedium`).
  - Version numbers, SHA-256 digests, and permission audit badges in bundled **Inter** with tabular numerals (`tnum`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Environment-Driven Dynamic Versioning with Local Fallback**
  - *Chosen Approach*: Read `VERSION_NAME` and `VERSION_CODE` from environment variables in the Gradle build configuration, defaulting seamlessly to `"1.0.0"` and `1` for local AI Studio builds. In CI, tags like `v1.2.3` automatically compute `VERSION_NAME = "1.2.3"` and a monotonic integer `VERSION_CODE = 10203` (`major * 10000 + minor * 100 + patch`).
  - *Why*: Guarantees that Android OS package upgrades (`versionCode` strictly increasing) always succeed without requiring manual commits to bump numbers before tagging.
  - *Alternatives Considered*: Committing version bumps back to `main` from CI—passed over because it creates noisy bot commits and can conflict with branch protection rules.

- **Decision 2: Two-Stage Offline Permission Audit (Source XML + Compiled Binary)**
  - *Chosen Approach*: Audit both the source `AndroidManifest.xml` and the final compiled `release.apk` using Android SDK's `aapt2 dump permissions`. If `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE` appears anywhere in the compiled APK, the pipeline fails immediately and blocks release publication.
  - *Why*: Transitive dependencies can merge unexpected permissions during manifest merger. Inspecting the final compiled APK binary with `aapt2` provides cryptographic certainty that the released app is 100% offline.

- **Decision 3: Compatible Signing Pipeline with Secret Injection**
  - *Chosen Approach*: Preserve the exact `signingConfigs` block in the Gradle configuration (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` with key alias `upload`). In GitHub Actions, decode `RELEASE_KEYSTORE_BASE64` into the workspace and pass the credentials via environment variables. If a developer forks the repo and triggers a test release before configuring secrets, the workflow gracefully generates an ephemeral keystore for the `upload` alias and notes it in the release summary so the pipeline never crashes out-of-the-box.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Pipeline Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        GITHUB REPOSITORY EVENTS                             │
│   • Push Tag: v*.*.* (e.g., v1.2.0)                                         │
│   • Manual Dispatch: workflow_dispatch (custom version input)               │
│   • Push / PR to main (Continuous Integration Gate)                         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 STAGE 1: QUALITY & OFFLINE SECURITY GATE                    │
│  ┌──────────────────────────┐  ┌──────────────────────┐  ┌───────────────┐  │
│  │ Source Manifest Audit    │  │ Unit & Robolectric   │  │ Android Lint  │  │
│  │ Verify tools:node=remove │─▶│ :app:testDebugUnitTest│─▶│ :app:lintDebug│  │
│  │ for INTERNET permissions │  │ (31 PDF Tools + UI)  │  │ Static Checks │  │
│  └──────────────────────────┘  └──────────────────────┘  └───────────────┘  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│             STAGE 2: SEMANTIC VERSIONING & SIGNED RELEASE BUILD             │
│  • Parse Tag vX.Y.Z ──▶ VERSION_NAME="X.Y.Z", VERSION_CODE=(X*10000+Y*100+Z)│
│  • Decode RELEASE_KEYSTORE_BASE64 ──▶ KEYSTORE_PATH, STORE_PASSWORD         │
│  • Execute: gradle :app:assembleRelease :app:bundleRelease                  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│           STAGE 3: BINARY PERMISSION AUDIT & ARTIFACT PACKAGING             │
│  • Run `aapt2 dump permissions Folio-vX.Y.Z-universal-release.apk`          │
│  • Fail build if INTERNET or ACCESS_NETWORK_STATE is present in binary      │
│  • Rename outputs:                                                          │
│      - Folio-vX.Y.Z-universal-release.apk                                   │
│      - Folio-vX.Y.Z-release.aab                                             │
│      - SHA256SUMS.txt (sha256sum of APK & AAB)                              │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                STAGE 4: GITHUB RELEASE & CHANGELOG PUBLISHER                │
│  • Generate categorized Markdown changelog from Git history since last tag  │
│  • Attach signed APK, AAB, and SHA256SUMS.txt via softprops/action-gh-release│
└─────────────────────────────────────────────────────────────────────────────┘
```

### Interactive Component & State Mapping

1. **Gradle Dynamic Version & BuildConfig Bridge**:
   - Reads `System.getenv("VERSION_NAME")` and `System.getenv("VERSION_CODE")?.toIntOrNull()` inside `defaultConfig`, falling back to `"1.0.0"` and `1`.
   - Exposes `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE` to the Android app without modifying any signingConfig rules.
2. **Offline Manifest & APK Verification Script**:
   - Reusable shell verification script executed by both GitHub Actions workflows and available locally to verify:
     - `AndroidManifest.xml` strips `INTERNET` and `ACCESS_NETWORK_STATE` via `tools:node="remove"`.
     - Compiled APK contains zero network permissions when inspected via `aapt2`.
3. **In-App Release & Runtime Offline Inspector (Settings Screen)**:
   - In the **Settings** screen's **About Folio** card, displays:
     - Semantic Version & Version Code (`v1.0.0 (Build 1)`).
     - Package ID (`com.aistudio.folio.docpdf`).
     - Interactive **"Verify Offline APK Integrity"** button that queries Android's `PackageManager.getPackageInfo(packageName, GET_PERMISSIONS)` at runtime and displays a verified badge confirming `0 Network Permissions Granted` alongside release pipeline documentation.
4. **Release Documentation (`RELEASE_PIPELINE.md`)**:
   - Provides step-by-step instructions for generating a signing keystore, encoding it as Base64 for GitHub Secrets (`RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`), and triggering automated releases via Git tags or manual dispatch.
