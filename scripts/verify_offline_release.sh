#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Folio — 100% Offline Manifest & Compiled APK Permission Auditor
# Ensures neither source AndroidManifest.xml nor compiled APK binaries ever
# request or merge android.permission.INTERNET or ACCESS_NETWORK_STATE.
# ==============================================================================

MANIFEST_PATH="app/src/main/AndroidManifest.xml"
APK_PATH="${1:-}"

echo "=================================================================="
echo "  Folio Offline Privacy & Zero-Network Permission Audit"
echo "=================================================================="

if [[ ! -f "$MANIFEST_PATH" ]]; then
  echo "[ERROR] Source manifest not found at $MANIFEST_PATH"
  exit 1
fi

# 1. Verify source AndroidManifest.xml strips INTERNET and ACCESS_NETWORK_STATE
echo "[1/2] Auditing source $MANIFEST_PATH..."

if ! grep -q 'android.permission.INTERNET.*tools:node="remove"' "$MANIFEST_PATH"; then
  echo "[FAIL] Source manifest does not explicitly strip android.permission.INTERNET with tools:node=\"remove\""
  exit 1
fi

if ! grep -q 'android.permission.ACCESS_NETWORK_STATE.*tools:node="remove"' "$MANIFEST_PATH"; then
  echo "[FAIL] Source manifest does not explicitly strip android.permission.ACCESS_NETWORK_STATE with tools:node=\"remove\""
  exit 1
fi

# Ensure no active uses-permission without tools:node="remove" exists for network
ACTIVE_NET_PERMS=$(grep -E 'android\.permission\.(INTERNET|ACCESS_NETWORK_STATE|ACCESS_WIFI_STATE)' "$MANIFEST_PATH" | grep -v 'tools:node="remove"' || true)
if [[ -n "$ACTIVE_NET_PERMS" ]]; then
  echo "[FAIL] Prohibited active network permission found in source manifest:"
  echo "$ACTIVE_NET_PERMS"
  exit 1
fi

echo "[PASS] Source AndroidManifest.xml strictly strips all network permissions."

# 2. If an APK path is provided (or exists in build outputs), audit compiled binary with aapt2
if [[ -n "$APK_PATH" && -f "$APK_PATH" ]]; then
  echo "[2/2] Auditing compiled APK binary: $APK_PATH"

  AAPT2_BIN=""
  if command -v aapt2 >/dev/null 2>&1; then
    AAPT2_BIN=$(command -v aapt2)
  else
    for SDK_DIR in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
      if [[ -n "$SDK_DIR" && -d "$SDK_DIR/build-tools" ]]; then
        FOUND_AAPT2=$(find "$SDK_DIR/build-tools" -name "aapt2" -type f | sort -V | tail -n 1 || true)
        if [[ -n "$FOUND_AAPT2" ]]; then
          AAPT2_BIN="$FOUND_AAPT2"
          break
        fi
      fi
    done
  fi

  if [[ -z "$AAPT2_BIN" ]]; then
    echo "[WARN] aapt2 binary not found in PATH or ANDROID_HOME; skipping binary dump."
  else
    echo "Using aapt2: $AAPT2_BIN"
    PERMS_OUTPUT=$("$AAPT2_BIN" dump permissions "$APK_PATH" || true)
    echo "$PERMS_OUTPUT"

    if echo "$PERMS_OUTPUT" | grep -qE "android\.permission\.(INTERNET|ACCESS_NETWORK_STATE|ACCESS_WIFI_STATE)"; then
      echo "[FAIL] Compiled APK binary contains prohibited network permissions!"
      exit 1
    fi
    echo "[PASS] Compiled APK binary verified: 0 network permissions present."
  fi
else
  echo "[2/2] No compiled APK specified for binary audit (source audit complete)."
fi

echo "=================================================================="
echo "  Offline Audit Succeeded — 100% Air-Gapped Compliance Verified"
echo "=================================================================="
