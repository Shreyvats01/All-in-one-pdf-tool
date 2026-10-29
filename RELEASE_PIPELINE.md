# Folio — Automated GitHub CI/CD & Verified Universal APK Release Pipeline

**Folio** is configured for **100% automatic GitHub Releases** focused on a **verified, directly installable Universal Android APK**. Every time you push or merge changes to `main` (or `master`), push a `v*.*.*` tag, or click **Run workflow**, GitHub Actions automatically tests, audits, increments the version number, creates the Git tag, 4-byte aligns (`zipalign`), signs (`apksigner` V1 + V2 + V3), cryptographically verifies the APK, and publishes it to **GitHub Releases**.

---

## 1. How Zero-Touch Automatic APK Releases Work

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
   - Computes a strictly increasing Android `versionCode` (`MAJOR * 10000 + MINOR * 100 + PATCH + GITHUB_RUN_NUMBER`) so Android users can always install updates directly over their existing app.
3. **Stage 3 — `zipalign` + `apksigner` (V1+V2+V3) Signing, Verification & Publication**:
   - Compiles the release APK with `--no-configuration-cache` (`gradle :app:assembleRelease --no-configuration-cache`) so cached configuration from previous jobs can never strip signing credentials.
   - Runs `scripts/sign_and_verify_apk.sh` to enforce:
     1. **4-byte Page Alignment**: `zipalign -p -f 4`
     2. **Full Multi-Scheme Signing**: `apksigner sign --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true`
     3. **Hard Verification Gate**: `zipalign -c 4`, `apksigner verify --verbose --print-certs` (asserting V1, V2, and V3 signatures are present), and `aapt2 dump badging`.
   - Audits the signed APK binary with `aapt2 dump permissions` to guarantee zero network permissions exist in the binary.
   - Generates `SHA256SUMS.txt` and `RELEASE_NOTES.md`, creates the Git tag (`vX.Y.Z`), and publishes `Folio-vX.Y.Z-universal-release.apk` and `SHA256SUMS.txt` to **GitHub Releases**.

---

## 2. Optional: Configuring a Custom Production Signing Keystore

By default, if no custom secrets are configured yet, the pipeline clones the repository's deterministic certificate into alias `upload` so all your GitHub Releases share a consistent signature and install/update cleanly on any Android device out-of-the-box.

To sign releases with your own custom production `.jks` keystore:

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
