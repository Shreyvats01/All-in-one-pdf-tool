# Folio — Automated GitHub CI/CD & Zero-Touch Android Release Pipeline

**Folio** is configured for **100% automatic GitHub Releases**. Every time you push or merge changes to `main` (or `master`), push a `v*.*.*` tag, or click **Run workflow**, GitHub Actions automatically tests, audits, increments the version number, creates the Git tag, signs the APK and AAB, and publishes a new release to **GitHub Releases**.

---

## 1. How Zero-Touch Automatic Releases Work

When you push changes from AI Studio or Git to your `main` or `master` branch:

1. **Stage 1 — Quality, Robolectric & 100% Offline Gate**:
   - Audits `app/src/main/AndroidManifest.xml` to ensure `INTERNET` and `ACCESS_NETWORK_STATE` are stripped.
   - Runs all unit and Robolectric tests (`gradle :app:testDebugUnitTest`) on **Temurin JDK 21** and publishes a JUnit Markdown summary table to the GitHub Actions run page.
   - Runs Android Lint (`gradle :app:lintDebug`).
2. **Stage 2 — Automatic Semantic Versioning & Tagging**:
   - Inspects your existing Git tags (`v*.*.*`) and commit message:
     - Commit with `BREAKING CHANGE` or `!:` → automatically bumps **Major** version (`v2.0.0`).
     - Commit starting with `feat:` → automatically bumps **Minor** version (`v1.1.0`).
     - Any other commit → automatically bumps **Patch** version (`v1.0.1`, `v1.0.2`, etc.).
   - Computes a strictly increasing Android `versionCode` (`MAJOR * 10000 + MINOR * 100 + PATCH + GITHUB_RUN_NUMBER`) so Android users can always install the update directly over their existing app.
3. **Stage 3 — Signed Build, Binary Permission Audit & Release Publication**:
   - Builds `Folio-vX.Y.Z-universal-release.apk` and `Folio-vX.Y.Z-release.aab`.
   - Audits the compiled release APK binary with `aapt2 dump permissions` to guarantee zero network permissions exist in the binary.
   - Generates `SHA256SUMS.txt` and categorized release notes (`RELEASE_NOTES.md`).
   - Automatically creates the Git tag (`vX.Y.Z`) and publishes the release to your repository's **GitHub Releases** page.

---

## 2. Optional: Configuring Your Production Signing Keystore

By default, if no secrets are configured yet, the pipeline automatically generates a signing keystore so your releases build and publish out-of-the-box.

To sign all releases with a permanent production key (so Android users can update across versions without uninstalling):

1. **Generate a keystore locally**:
   ```bash
   keytool -genkeypair -v \
     -keystore my-upload-key.jks \
     -alias upload \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000 \
     -dname "CN=Folio Release, OU=Mobile, O=Folio, C=US"
   ```
2. **Base64-encode the keystore**:
   ```bash
   base64 -w 0 my-upload-key.jks > keystore.b64
   ```
3. **Add 3 Repository Secrets in GitHub** (**Settings** → **Secrets and variables** → **Actions**):
   - `RELEASE_KEYSTORE_BASE64`: Contents of `keystore.b64`
   - `RELEASE_STORE_PASSWORD`: Your keystore password
   - `RELEASE_KEY_PASSWORD`: Your key password for alias `upload`
