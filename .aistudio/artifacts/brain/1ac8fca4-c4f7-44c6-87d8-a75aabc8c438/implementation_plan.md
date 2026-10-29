# Folio — Fix "App not installed as package appears to be invalid" APK Installation Error

Eliminate the Android Package Installer **"App not installed as package appears to be invalid"** error by fixing unsigned/unaligned release APK output, enforcing explicit `zipalign` + `apksigner` (V1 + V2 + V3 Signature Schemes) signing and verification in CI, enabling `android:extractNativeLibs="true"`, and streamlining the release pipeline to publish only the verified Universal `.apk` and `SHA256SUMS.txt`.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Why "App not installed as package appears to be invalid" Happened on Fresh Install**:
> 1. **Gradle Configuration Cache Poisoning & Silent Unsigned APK Output (`app-release-unsigned.apk`)**:
>    - Because `org.gradle.configuration-cache=true` is enabled in `gradle.properties` and `gradle/actions/setup-gradle@v4` shares configuration cache across CI jobs, Job 1 (`quality-and-offline-gate`) cached `System.getenv("STORE_PASSWORD")` and `System.getenv("KEY_PASSWORD")` as `null`.
>    - When Job 2 ran `gradle :app:assembleRelease` and reused that cached configuration, Android Gradle Plugin saw `null` signing passwords, **silently skipped signing**, and output **`app-release-unsigned.apk`**.
>    - The packaging step (`find app/build/outputs/apk/release -name "*.apk"`) matched `app-release-unsigned.apk` and renamed it to `Folio-v1.0.x-universal-release.apk`.
>    - On Android, attempting to install an unsigned APK shows the app's icon (`F`) and name (`Folio`) and immediately aborts with **"App not installed as package appears to be invalid"** (`INSTALL_PARSE_FAILED_NO_CERTIFICATES`).
> 2. **Mandatory Android 11–16 `zipalign` + V2/V3 APK Signature Verification**:
>    - Modern Android versions require 4-byte aligned uncompressed `resources.arsc` (`zipalign -p 4`) and **APK Signature Scheme v2/v3** (`apksigner sign --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true`), plus `android:extractNativeLibs="true"` for multi-ABI native libraries (`CameraX` / `PdfBox-Android`).

- **Confirmed Decision 1 — Fresh Install Compatibility & Explicit `zipalign` + `apksigner` Gate**: Build the release APK with `--no-configuration-cache`, run explicit Android SDK `zipalign -p -f 4` and `apksigner sign` (enabling V1, V2, and V3 signature schemes), and enforce a mandatory `apksigner verify --verbose --print-certs` gate before uploading.
- **Confirmed Decision 2 — APK-Only Release Pipeline**: Remove `.aab` bundle generation and publish only the verified, V1+V2+V3 signed `Folio-vX.Y.Z-universal-release.apk` and `SHA256SUMS.txt` to GitHub Releases.

---

## 1. Overview & Core Concept

- **What It Does**: Guarantees that every `Folio-vX.Y.Z-universal-release.apk` published to GitHub Releases is 4-byte page-aligned (`zipalign`), cryptographically signed with **APK Signature Schemes v1, v2, and v3** (`apksigner`), verified with `apksigner verify` and `aapt2 dump badging`, and compatible with all Android 8.0–16+ devices on both fresh installs and over-the-top updates.
- **Target Audience / Persona**: Android users downloading and installing `Folio-vX.Y.Z-universal-release.apk` directly on their phones from GitHub Releases.
- **Key Value**: Permanently prevents unsigned, unaligned, or certificate-mismatched APKs from ever being published to GitHub Releases.

---

## 2. User Experience & Visual Design (Android Package Installer & Release UX)

### Key User Flows
1. **Fresh APK Download & Install on Android Phone**:
   - User downloads `Folio-vX.Y.Z-universal-release.apk` from GitHub Releases and taps it in their browser or Files app.
   - Android Package Installer verifies the V2/V3 signature block, 4-byte resource alignment, and native library extraction cleanly, installs **Folio**, and displays the **Open** button.
2. **Seamless Over-the-Top Version Updates**:
   - Even if `RELEASE_KEYSTORE_BASE64` is not yet configured in GitHub Secrets, the pipeline uses the repository's consistent signing certificate rather than generating a new random key on every run, ensuring v1.0.2 installs cleanly over v1.0.1 without signature conflict errors.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Explicit Android SDK `zipalign` + `apksigner` Post-Build Pipeline (`scripts/sign_and_verify_apk.sh`)**
  - *Chosen Approach*: After running `gradle :app:assembleRelease --no-configuration-cache`, run a dedicated script that locates `zipalign` and `apksigner` in `$ANDROID_HOME/build-tools`, performs 4-byte page alignment (`zipalign -p -f 4`), signs the APK with `apksigner sign` (`--v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true`), and verifies the resulting binary with `apksigner verify --verbose --print-certs` and `zipalign -c 4`.
  - *Why*: Eliminates any possibility of Gradle configuration cache skipping the signing task or producing an `app-release-unsigned.apk` that gets renamed and uploaded.

- **Decision 2: Enable `android:extractNativeLibs="true"` in `AndroidManifest.xml`**
  - *Chosen Approach*: Add `android:extractNativeLibs="true"` to the `<application>` tag in `AndroidManifest.xml`.
  - *Why*: Prevents `INSTALL_FAILED_INVALID_APK: Failed to extract native libraries` on devices with strict zip page-alignment enforcement when bundling CameraX and PdfBox native libraries.

- **Decision 3: APK-First Streamlined Release Pipeline**
  - *Chosen Approach*: Remove `:app:bundleRelease` (`.aab`) from `.github/workflows/android-release.yml` so the build is faster and publishes only `Folio-vX.Y.Z-universal-release.apk` and `SHA256SUMS.txt`.
  - *Why*: Matches your distribution focus on direct APK installation via GitHub Releases.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### APK Build, Alignment, Signing & Verification Pipeline Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│             STAGE 1: GRADLE RELEASE APK COMPILATION                         │
│   • gradle :app:assembleRelease --no-configuration-cache                    │
│   • AndroidManifest.xml: android:extractNativeLibs="true"                   │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│        STAGE 2: ANDROID SDK ZIPALIGN & APKSIGNER (V1 + V2 + V3)             │
│   1. Locate latest Android SDK Build-Tools (zipalign, apksigner, aapt2)     │
│   2. Run `zipalign -p -f -v 4 raw.apk aligned.apk`                          │
│   3. Run `apksigner sign --v1-signing-enabled true                          │
│                          --v2-signing-enabled true                          │
│                          --v3-signing-enabled true`                         │
│      ──▶ Outputs `dist/Folio-vX.Y.Z-universal-release.apk`                  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│          STAGE 3: HARD CRYPTOGRAPHIC & BINARY VERIFICATION GATE             │
│   • `zipalign -c 4 Folio-vX.Y.Z-universal-release.apk` (Verify Alignment)   │
│   • `apksigner verify --verbose --print-certs` (Verify V1+V2+V3 Signatures) │
│   • `aapt2 dump badging` (Verify Package Name, VersionCode, Min/Target SDK) │
│   • `aapt2 dump permissions` (Verify 0 Network Permissions / 100% Offline)  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                STAGE 4: GITHUB RELEASE & CHECKSUM PUBLISHER                 │
│   • Generate `dist/SHA256SUMS.txt` for `Folio-vX.Y.Z-universal-release.apk` │
│   • Publish verified APK + SHA256SUMS.txt + Changelog to GitHub Releases    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Interactive Component & State Mapping

1. **Manifest Native Library Extraction (`AndroidManifest.xml`)**:
   - Sets `android:extractNativeLibs="true"` on the `<application>` element so all Android versions cleanly extract bundled native libraries during package installation.
2. **Dedicated Signing & Verification Script (`scripts/sign_and_verify_apk.sh`)**:
   - Resolves `zipalign`, `apksigner`, and `aapt2` from `$ANDROID_HOME/build-tools` or `$ANDROID_SDK_ROOT/build-tools`.
   - Aligns, signs (`V1 + V2 + V3`), and cryptographically verifies the APK, failing the build immediately if any signature scheme or alignment check is missing.
3. **Streamlined APK-Only Release Workflow (`.github/workflows/android-release.yml`)**:
   - Runs `gradle :app:assembleRelease --no-configuration-cache`, invokes `scripts/sign_and_verify_apk.sh`, audits zero network permissions via `scripts/verify_offline_release.sh`, generates `SHA256SUMS.txt`, and publishes `Folio-vX.Y.Z-universal-release.apk` to GitHub Releases.
