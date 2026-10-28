#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Folio — JUnit XML Test Summary & Failure Stack Trace Reporter
# Prints a Markdown test results table to stdout and $GITHUB_STEP_SUMMARY
# ==============================================================================

RESULTS_DIR="app/build/test-results/testDebugUnitTest"
SUMMARY_OUT="${GITHUB_STEP_SUMMARY:-/dev/null}"

echo "### Unit & Robolectric Test Suite Summary" | tee -a "$SUMMARY_OUT"
echo "" | tee -a "$SUMMARY_OUT"

if [[ ! -d "$RESULTS_DIR" ]]; then
  echo "No JUnit XML test results found in \`$RESULTS_DIR\`." | tee -a "$SUMMARY_OUT"
  exit 0
fi

echo "| Test Suite | Total | Failures | Errors | Skipped | Time (s) |" | tee -a "$SUMMARY_OUT"
echo "| :--- | :---: | :---: | :---: | :---: | :---: |" | tee -a "$SUMMARY_OUT"

shopt -s nullglob
for xml in "$RESULTS_DIR"/TEST-*.xml; do
  SUITE=$(grep -oP '<testsuite name="\K[^"]+' "$xml" | head -n 1 || basename "$xml")
  TESTS=$(grep -oP 'testsuite[^>]*tests="\K[^"]+' "$xml" | head -n 1 || echo "0")
  FAILURES=$(grep -oP 'testsuite[^>]*failures="\K[^"]+' "$xml" | head -n 1 || echo "0")
  ERRORS=$(grep -oP 'testsuite[^>]*errors="\K[^"]+' "$xml" | head -n 1 || echo "0")
  SKIPPED=$(grep -oP 'testsuite[^>]*skipped="\K[^"]+' "$xml" | head -n 1 || echo "0")
  TIME=$(grep -oP 'testsuite[^>]*time="\K[^"]+' "$xml" | head -n 1 || echo "0.0")

  echo "| \`$SUITE\` | $TESTS | $FAILURES | $ERRORS | $SKIPPED | ${TIME}s |" | tee -a "$SUMMARY_OUT"

  if [[ "$FAILURES" != "0" || "$ERRORS" != "0" ]]; then
    echo "" | tee -a "$SUMMARY_OUT"
    echo "#### Failure Details in \`$SUITE\`" | tee -a "$SUMMARY_OUT"
    echo '```text' | tee -a "$SUMMARY_OUT"
    cat "$xml" | tee -a "$SUMMARY_OUT"
    echo '```' | tee -a "$SUMMARY_OUT"
  fi
done
