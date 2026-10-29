#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Folio — Android Release APK Alignment, V1+V2+V3 Signing & Cryptographic Gate
# ==============================================================================
# Guarantees that the release APK is:
#   1. 4-byte page-aligned (`zipalign -p -f -v 4`)
#   2. Signed with APK Signature Schemes v1 (JAR), v2 (Full APK), and v3 (Key Rotation)
#   3. Verified via `zipalign -c 4`, `apksigner verify --verbose --print-certs`,
#      and `aapt2 dump badging` before ever being published to GitHub Releases.
# ==============================================================================

RAW_APK_Input="${1:-}"
TARGET_APK="${2:-dist/Folio-universal-release.apk}"
KEYSTORE_PATH_ARG="${3:-${KEYSTORE_PATH:-my-upload-key.jks}}"
KEY_ALIAS_ARG="${4:-upload}"
STORE_PASS_ARG="${5:-${STORE_PASSWORD:-}}"
KEY_PASS_ARG="${6:-${KEY_PASSWORD:-${STORE_PASS_ARG}}}"

if [[ -z "$RAW_APK_Input" || ! -f "$RAW_APK_Input" ]]; then
  echo "[ERROR] Raw APK file not found: '${RAW_APK_Input}'"
  exit 1
fi

if [[ ! -f "$KEYSTORE_PATH_ARG" ]]; then
  echo "[ERROR] Keystore file not found at: '${KEYSTORE_PATH_ARG}'"
  exit 1
fi

if [[ -z "$STORE_PASS_ARG" ]]; then
  echo "[ERROR] Keystore password (STORE_PASSWORD) is empty. Refusing to produce an unsigned APK."
  exit 1
fi

# Locate Android SDK Build-Tools (zipalign, apksigner, aapt2)
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
BUILD_TOOLS_DIR=""
if [[ -d "${SDK_ROOT}/build-tools" ]]; then
  BUILD_TOOLS_DIR=$(find "${SDK_ROOT}/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1 || true)
fi

find_tool() {
  local tool_name="$1"
  if [[ -n "$BUILD_TOOLS_DIR" && -x "${BUILD_TOOLS_DIR}/${tool_name}" ]]; then
    echo "${BUILD_TOOLS_DIR}/${tool_name}"
  elif command -v "$tool_name" >/dev/null 2>&1; then
    command -v "$tool_name"
  else
    echo ""
  fi
}

ZIPALIGN_BIN=$(find_tool "zipalign")
APKSIGNER_BIN=$(find_tool "apksigner")
AAPT2_BIN=$(find_tool "aapt2")

if [[ -z "$ZIPALIGN_BIN" ]]; then
  echo "[ERROR] Could not locate 'zipalign' in Android SDK build-tools (${SDK_ROOT}/build-tools) or PATH."
  exit 1
fi

if [[ -z "$APKSIGNER_BIN" ]]; then
  echo "[ERROR] Could not locate 'apksigner' in Android SDK build-tools (${SDK_ROOT}/build-tools) or PATH."
  exit 1
fi

echo "==> Using zipalign : ${ZIPALIGN_BIN}"
echo "==> Using apksigner: ${APKSIGNER_BIN}"
echo "==> Source APK     : ${RAW_APK_Input}"
echo "==> Target APK     : ${TARGET_APK}"

mkdir -p "$(dirname "$TARGET_APK")"
TMP_ALIGNED_APK="$(mktemp /tmp/folio-aligned-XXXXXX.apk)"
trap 'rm -f "$TMP_ALIGNED_APK" "${TARGET_APK}.idsig"' EXIT

echo "==> [Step 1/4] Running 4-byte page alignment (zipalign -p -f 4)..."
"$ZIPALIGN_BIN" -p -f 4 "$RAW_APK_Input" "$TMP_ALIGNED_APK"

echo "==> [Step 2/4] Signing APK with V1 + V2 + V3 Signature Schemes (apksigner sign)..."
"$APKSIGNER_BIN" sign \
  --ks "$KEYSTORE_PATH_ARG" \
  --ks-key-alias "$KEY_ALIAS_ARG" \
  --ks-pass "pass:${STORE_PASS_ARG}" \
  --key-pass "pass:${KEY_PASS_ARG}" \
  --min-sdk-version 21 \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$TARGET_APK" \
  "$TMP_ALIGNED_APK"

echo "==> [Step 3/4] Verifying 4-byte alignment on signed APK (zipalign -c 4)..."
"$ZIPALIGN_BIN" -c 4 "$TARGET_APK"
echo "[PASS] APK 4-byte alignment verified."

echo "==> [Step 4/4] Verifying V1 + V2 + V3 cryptographic signatures (apksigner verify)..."
VERIFY_OUT=$("$APKSIGNER_BIN" verify --min-sdk-version 21 --verbose --print-certs "$TARGET_APK" 2>&1 | grep -v "^WARNING: META-INF/" || true)
echo "$VERIFY_OUT"

if ! echo "$VERIFY_OUT" | grep -q "Verified using v1 scheme (JAR signing): true"; then
  echo "[FAIL] APK is missing v1 (JAR) signature scheme!"
  exit 1
fi

if ! echo "$VERIFY_OUT" | grep -q "Verified using v2 scheme (APK Signature Scheme v2): true"; then
  echo "[FAIL] APK is missing v2 (APK Signature Scheme v2) signature!"
  exit 1
fi

if ! echo "$VERIFY_OUT" | grep -q "Verified using v3 scheme (APK Signature Scheme v3): true"; then
  echo "[FAIL] APK is missing v3 (APK Signature Scheme v3) signature!"
  exit 1
fi

if [[ -n "$AAPT2_BIN" ]]; then
  echo "==> Inspecting APK badging metadata (aapt2 dump badging)..."
  BADGING_HEAD=$("$AAPT2_BIN" dump badging "$TARGET_APK" | head -n 5 || true)
  echo "$BADGING_HEAD"
  if ! echo "$BADGING_HEAD" | grep -q "package: name='com.aistudio.folio.docpdf'"; then
    echo "[FAIL] APK badging does not contain expected package name 'com.aistudio.folio.docpdf'!"
    exit 1
  fi
fi

APK_SIZE_BYTES=$(wc -c < "$TARGET_APK" | tr -d ' ')
echo "[PASS] Signed Universal Release APK verified (${APK_SIZE_BYTES} bytes): ${TARGET_APK}"
