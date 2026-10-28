# Folio — Automated GitHub CI/CD & Android Release Pipeline Guide

This document explains how **Folio** automatically tests, audits, signs, and publishes new Android versions (`APK`, `AAB`, and `SHA256SUMS.txt`) to **GitHub Releases** whenever you push a semantic version tag (`v*.*.*`) or trigger a manual release from the GitHub Actions tab.

---

## 1. Pipeline Overview

Folio includes two automated GitHub Actions workflows in `.github/workflows/`:

| Workflow | File | Trigger | Responsibilities |
| :--- | :--- | :--- | :--- |
| **Android CI & Offline Privacy Gate** | `.github/workflows/android-ci.yml` | Every `push` and `pull_request` to `main` / `master` | 1. Audits `AndroidManifest.xml` for 100% offline compliance<br>2. Runs Unit & Robolectric test suite (`testDebugUnitTest`)<br>3. Runs Android Lint (`lintDebug`)<br>4. Builds Debug APK & audits merged binary permissions via `aapt2` |
| **Android Automated Release Pipeline** | `.github/workflows/android-release.yml` | Pushing a Git tag matching `v*.*.*` (e.g., `v1.2.0`) **or** manual `workflow_dispatch` | 1. Runs the full Quality & Offline Privacy Gate<br>2. Computes `VERSION_NAME` & monotonic `VERSION_CODE` (`major*10000 + minor*100 + patch`)<br>3. Signs release artifacts using `RELEASE_KEYSTORE_BASE64`<br>4. Builds `Folio-vX.Y.Z-universal-release.apk` & `Folio-vX.Y.Z-release.aab`<br>5. Verifies the compiled release APK binary contains zero network permissions (`aapt2 dump permissions`)<br>6. Generates `SHA256SUMS.txt` and categorized release notes<br>7. Publishes directly to **GitHub Releases** |

---

## 2. How Semantic Versioning Works Automatically

In `app/build.gradle.kts`, Folio reads `VERSION_NAME` and `VERSION_CODE` dynamically from environment variables injected by the release pipeline (while defaulting to `1.0.0` / `1` for local builds):

```kotlin
versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
versionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.0"
```

When you push a tag like `v1.2.3` (or enter `1.2.3` in the manual workflow dispatch dialog), the pipeline automatically calculates:
- **`VERSION_NAME`** = `"1.2.3"`
- **`VERSION_CODE`** = `10203` (`1 * 10000 + 2 * 100 + 3`)

This guarantees that every new semantic release has a strictly higher integer `versionCode` so Android users can seamlessly install updates over their existing Folio installation without uninstalling.

---

## 3. One-Time Setup: Configuring Production Signing Secrets on GitHub

Folio's `release` signing configuration in `app/build.gradle.kts` uses key alias **`upload`**. To sign all GitHub Releases with your permanent production key so users can upgrade across versions seamlessly:

### Step A — Generate Your Upload Keystore (Run Locally Once)

```bash
keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Folio Release, OU=Mobile, O=Folio, C=US"
```

### Step B — Base64-Encode the Keystore

```bash
base64 -w 0 my-upload-key.jks > keystore.b64
```
*(On macOS, use `base64 -i my-upload-key.jks | tr -d '\n' > keystore.b64`)*

### Step C — Add Secrets in GitHub Repository Settings

Navigate to your GitHub repository → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**, and add these 3 secrets:

| Secret Name | Value |
| :--- | :--- |
| `RELEASE_KEYSTORE_BASE64` | Contents of `keystore.b64` |
| `RELEASE_STORE_PASSWORD` | Your keystore password |
| `RELEASE_KEY_PASSWORD` | Your key password for alias `upload` |

> **Zero-Config Fallback for Forks**: If you trigger a release before configuring these secrets, the workflow will automatically generate an ephemeral release keystore for alias `upload` and emit a warning annotation so your test release pipeline still succeeds out-of-the-box.

---

## 4. How to Trigger a New Android Release

### Option A — Push a Git Tag (Recommended)

```bash
git tag v1.1.0
git push origin v1.1.0
```

As soon as the tag is pushed, GitHub Actions runs **Android Automated Release Pipeline** and publishes **Folio v1.1.0 (Build 10100)** to your repository's **Releases** page.

### Option B — Manual One-Click Release in GitHub UI

1. Open your repository on GitHub and click the **Actions** tab.
2. Select **Android Automated Release Pipeline** in the left sidebar.
3. Click **Run workflow**, enter the semantic version (e.g. `1.1.0`), optionally toggle **Mark release as a pre-release**, and click **Run workflow**.

---

## 5. Obtainium & Direct APK Auto-Update Support for Android Users

Because the pipeline publishes standardized assets named `Folio-vX.Y.Z-universal-release.apk` alongside `SHA256SUMS.txt` on GitHub Releases:
- Android users who use **Obtainium** can add your GitHub repository URL (`https://github.com/<owner>/<repo>`) directly to Obtainium to receive automatic background update notifications whenever a new tag is released.
- Privacy-conscious users can verify the cryptographic integrity of any downloaded APK or AAB using:
  ```bash
  sha256sum -c SHA256SUMS.txt
  ```
