# Folio — Fix GitHub Actions `:app:testDebugUnitTest` Failure (`4 tests completed, 1 failed`)

Resolve the `Execution failed for task ':app:testDebugUnitTest' (4 tests completed, 1 failed)` error occurring in Step 2 of the GitHub Actions pipeline by aligning the CI JDK version with Android SDK 36 / Robolectric 4.16 bytecode requirements, adding required JVM module-open and headless flags for Robolectric on Linux runners, and surfacing full stack traces and JUnit summaries directly in GitHub Actions logs.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Root Cause Analysis of `4 tests completed, 1 failed`**:
> - `ExampleUnitTest` contains **3** pure JVM unit tests (`documentFormat_detectsAllSupportedFormats`, `parsePageIndices_handlesRangesAndListsCorrectly`, `allThirtyOnePdfTools_areRegistered`), all **3 of which pass**.
> - `ExampleRobolectricTest` uses `@RunWith(RobolectricTestRunner::class)` with `@Config(sdk = [36])` and `robolectric = "4.16.1"`. Android 16 (API 36) `android-all-instrumented` JARs and Gradle 9.3.1 / AGP 9.1.1 require **JDK 21 bytecode support (class file version 65.0)**.
> - Because the GitHub Actions workflows were configured with **JDK 17** (`java-version: "17"` — class file version 61.0), JUnit 4 failed during runner initialization of `ExampleRobolectricTest` (`UnsupportedClassVersionError` / `initializationError`) and recorded the entire `ExampleRobolectricTest` class as **1 failed test** before executing its methods (`3 passed + 1 failed = 4 tests completed, 1 failed`).

- **Confirmed Decision 1 — Upgrade CI to JDK 21 & Harden Robolectric Runtime**: Upgrade `actions/setup-java@v4` to **Temurin JDK 21** across both CI and Release workflows, and configure `testOptions.unitTests.all` with `--add-opens` JVM arguments, `java.awt.headless=true`, and deterministic Robolectric properties.
- **Confirmed Decision 2 — Full Console Stack Traces & GitHub Step Summary**: Enable `TestExceptionFormat.FULL` (`showExceptions`, `showCauses`, `showStackTraces`, `showStandardStreams`) in Gradle and publish a Markdown JUnit summary table directly to `$GITHUB_STEP_SUMMARY` on every run.

---

## 1. Overview & Core Concept

- **What It Does**: Eliminates the Robolectric runner initialization failure on GitHub Actions `ubuntu-latest` runners so all unit and Robolectric tests (`ExampleUnitTest` + `ExampleRobolectricTest`) execute and pass deterministically in CI, while printing full test execution logs and a Markdown summary table in the GitHub Actions UI.
- **Target Audience / Persona**: Maintainers and contributors running automated CI checks and tag-based releases on GitHub Actions.
- **Key Value**: Ensures zero-friction green builds on GitHub Actions and immediate visibility into test execution results without downloading HTML report archives.

---

## 2. User Experience & Visual Design (CI Console & Step Summary UX)

### Key Developer Flows
1. **Live Console Output in GitHub Actions**:
   - Each test method prints its individual status (`PASSED`, `SKIPPED`, `FAILED`) in real time in the Step 2 console log.
   - If any assertion ever fails in the future, Gradle prints the exact line number, expected vs. actual values, and full causal stack trace right in the workflow log.
2. **GitHub Actions Step Summary Table (`$GITHUB_STEP_SUMMARY`)**:
   - At the end of the test step (`if: always()`), the workflow parses the JUnit XML reports and renders a clean summary table directly on the GitHub Actions workflow run page showing total tests, passed count, failed count, execution time, and any failure details.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Temurin JDK 21 in GitHub Actions + JVM Module Opens in Gradle**
  - *Chosen Approach*: Set `java-version: "21"` in `.github/workflows/android-ci.yml` and `.github/workflows/android-release.yml`, and configure `unitTests.all` in Gradle with `--add-opens=java.base/java.lang=ALL-UNNAMED`, `--add-opens=java.base/java.util=ALL-UNNAMED`, `--add-opens=java.base/java.io=ALL-UNNAMED`, and `-Djava.awt.headless=true`.
  - *Why*: Android SDK 35/36 `android-all` JARs used by Robolectric 4.16 are compiled targeting JDK 21 (class file version 65.0) and use deep reflection on `java.base` internals. Providing JDK 21 + `--add-opens` ensures 100% compatibility on both local and cloud runners.

- **Decision 2: Self-Contained File I/O in Robolectric PDF Tests**
  - *Chosen Approach*: Ensure `ExampleRobolectricTest` initializes PDFBox and uses isolated temporary directories with explicit cleanup and assertions, avoiding any dependency on external system fonts or display servers on headless Linux runners.
  - *Why*: Prevents flaky I/O or graphics state across parallel test workers on `ubuntu-latest`.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### CI Test Execution & Diagnostic Pipeline Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                   GITHUB ACTIONS RUNNER (ubuntu-latest)                     │
│   • actions/setup-java@v4 ──▶ Temurin JDK 21 (Class File v65.0 Support)     │
│   • gradle/actions/setup-gradle@v4 ──▶ Gradle 9.3.1 + AGP 9.1.1             │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                GRADLE UNIT TEST HARNESS (:app:testDebugUnitTest)            │
│   • isIncludeAndroidResources = true                                        │
│   • isReturnDefaultValues = true                                            │
│   • JVM Args: -Xmx2g, -Djava.awt.headless=true, --add-opens=java.base/...   │
│   • TestLogging: FULL stack traces, causes, stdout/stderr in console        │
└──────────────────┬───────────────────────────────────────┬──────────────────┘
                   │                                       │
                   ▼                                       ▼
┌─────────────────────────────────────┐ ┌─────────────────────────────────────┐
│        ExampleUnitTest (3)          │ │     ExampleRobolectricTest (4)      │
│ • Format Detection (18 formats)     │ │ • Offline Manifest & App Name Audit │
│ • Page Range Parser                 │ │ • Non-Destructive PDF Merge/Rotate  │
│ • 31-Tool Registry Verification     │ │ • AES-256-GCM Vault Round-Trip      │
│                                     │ │ • Runtime Release Integrity Audit   │
└──────────────────┬──────────────────┘ └──────────────────┬──────────────────┘
                   │                                       │
                   └───────────────────┬───────────────────┘
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│              GITHUB STEP SUMMARY & ARTIFACT REPORTER (if: always)           │
│   • Parses TEST-*.xml and prints Passed/Failed Markdown Table to Summary    │
│   • Uploads full HTML & XML report bundle if any diagnostic inspection needed│
└─────────────────────────────────────────────────────────────────────────────┘
```

### Interactive Component & State Mapping

1. **Gradle Test Harness Configuration**:
   - Configures `testOptions.unitTests` with `isIncludeAndroidResources = true`, `isReturnDefaultValues = true`, and `all { ... }` block enabling `TestExceptionFormat.FULL` and required JDK 21 `--add-opens` flags.
2. **GitHub Actions Workflows (`android-ci.yml` & `android-release.yml`)**:
   - Upgrades `actions/setup-java@v4` from `java-version: "17"` to `java-version: "21"`.
   - Adds an automated JUnit XML summary step (`if: always()`) that outputs every test suite's result and any failure stack trace directly into `$GITHUB_STEP_SUMMARY` and the workflow console log.
