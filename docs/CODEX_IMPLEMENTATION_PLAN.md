# AutoSwitch Approved-Work Implementation Plan

Status: awaiting explicit approval. No implementation work is authorized yet.

Guiding decision: repair only the native Android layer compiled into `com.autoswitch.apk`. Retain but do not modify the inactive Expo/React Native layer.

## Phase 1 — Preserve state and reproduce current bugs

- **Objective:** Preserve the pre-change evidence and reproduce failures before repair.
- **Files to create:** `docs/CODEX_ADB_EVIDENCE.md`, initial `docs/CODEX_TEST_MATRIX.md`, optional focused log captures under `docs/evidence/`.
- **Files to modify:** None in application source.
- **Files to remove/deprecate:** None.
- **Exact behavior changed:** None; install the preserved APK and measure existing behavior.
- **Dependencies:** Connected authorized Android device; known Wi-Fi/mobile test conditions.
- **Risks:** Network toggles affect the user's connectivity; device privacy in logs.
- **Complexity:** Medium — reproduction depends on OEM/device/network state.
- **Test method:** ADB install/launch, filtered logcat, T-01 through T-05 baseline.
- **Completion criteria:** Each reported failure is reproduced or explicitly marked NOT REPRODUCED/BLOCKED with timestamps and APK hash.
- **Rollback method:** Reinstall preserved APK from the timestamped archive if needed.

## Phase 2 — Establish one authoritative network-state model

- **Objective:** Represent phase, transport, capability, validation, metering, transition, timestamp, and error separately.
- **Files to create:** `app/src/main/java/com/autoswitch/apk/model/NetworkState.kt`, pure reducer/mapper tests under `app/src/test/java/com/autoswitch/apk/network/`.
- **Files to modify:** `app/src/main/java/com/autoswitch/apk/utils/NetworkMonitor.kt`, `gradle/libs.versions.toml`, `app/build.gradle.kts`.
- **Files to remove/deprecate:** Nested `NetworkMonitor.ConnectionType` and incomplete nested `NetworkState`.
- **Exact behavior changed:** Initial state becomes `INITIALIZING`; validated and internet-capable are independent; VPN/Ethernet/other transports no longer become false offline.
- **Dependencies:** Explicit coroutine and JUnit dependencies if retained by the design.
- **Risks:** State mapping errors across API 26–34+.
- **Complexity:** High — this is the core correctness boundary.
- **Test method:** Table-driven reducer/mapper unit tests, API guards, lint.
- **Completion criteria:** All required visible states can be derived without telephony permission; unit tests pass.
- **Rollback method:** Revert only the model/monitor commit or restore files from audit archive.

## Phase 3 — Consolidate duplicate callbacks

- **Objective:** Make one application-scoped default-network callback the sole native source of network truth.
- **Files to create:** `app/src/main/java/com/autoswitch/apk/AutoSwitchApplication.kt`.
- **Files to modify:** `NetworkMonitor.kt`, `AndroidManifest.xml`, `HomeFragment.kt`, `WifiMonitorService.kt`.
- **Files to remove/deprecate:** Public consumer-controlled `startMonitoring()/stopMonitoring()` ownership.
- **Exact behavior changed:** Register once per process with `registerDefaultNetworkCallback`; serialize callbacks on one handler; coalesce handover reconciliation; expose read-only `StateFlow`.
- **Dependencies:** Phase 2 model.
- **Risks:** Callback edge cases during rapid default-network replacement.
- **Complexity:** High — process/lifecycle concurrency must be exact.
- **Test method:** Transition unit tests, structured logs, T-03/T-04/T-06/T-08/T-18.
- **Completion criteria:** One callback registration per process, no stuck `CHECKING`, no duplicate transitions.
- **Rollback method:** Revert application owner and monitor wiring together.

## Phase 4 — Correct lifecycle and service behavior

- **Objective:** Make foreground collection and optional background guided monitoring deterministic.
- **Files to create:** Optional service-state helper/test files.
- **Files to modify:** `MainActivity.kt`, `HomeFragment.kt`, `SettingsFragment.kt`, `WifiMonitorService.kt`, `BootReceiver.kt`, `PreferencesManager.kt`, `AndroidManifest.xml`.
- **Files to remove/deprecate:** Unconditional shared monitor stop calls; broad ignored service exceptions; automatic background settings launch.
- **Exact behavior changed:** Background monitoring defaults off; explicit enable starts one FGS collector; disable stops it; service validates preference on start; foreground failure stops safely; force-stop/reboot limits are disclosed.
- **Dependencies:** Phase 3 process-scoped monitor.
- **Risks:** OEM FGS and boot restrictions; notification denial.
- **Complexity:** High — modern Android background rules and OEM behavior vary.
- **Test method:** T-08/T-09/T-10/T-16 and, with user consent, T-17.
- **Completion criteria:** UI enabled/running states agree; no duplicate collectors; scope always cancels; no background activity launch.
- **Rollback method:** Disable background toggle and revert service/receiver changes as one unit.

## Phase 5 — Add honest SIM diagnostics and permission handling

- **Objective:** Separate active subscriptions, slots, default-data SIM, active cellular subscription, mobile-data state, permission state, and switching capability.
- **Files to create:** `model/SimDiagnostics.kt`, `utils/SimDiagnosticsProvider.kt`, unit tests.
- **Files to modify:** `model/SimInfo.kt`, `SimSwitchManager.kt`, `MainActivity.kt`, `HomeFragment.kt`, `SettingsFragment.kt`, `PreferencesManager.kt`, `AndroidManifest.xml`.
- **Files to remove/deprecate:** ICC ID storage, root detection/execution, hidden-API reflection, raw subscription-ID logs, fake SIM 1/SIM 2 choices.
- **Exact behavior changed:** Standard devices report guided manual only; no automatic switch attempt; phone permission is requested contextually with rationale; denial leaves network monitoring functional.
- **Dependencies:** Actual device for OEM subscription/settings validation.
- **Risks:** Subscription APIs/OEM labels differ; permission can be permanently denied.
- **Complexity:** High — telephony privacy and OEM variance.
- **Test method:** Unit capability tests and T-12/T-13/T-14/T-15.
- **Completion criteria:** No `su`, `cmd phone`, or hidden setter in DEX; current/default/preferred SIM are never conflated.
- **Rollback method:** Revert telephony model/provider/UI wiring; core network monitor remains independent.

## Phase 6 — Remove misleading statuses

- **Objective:** Ensure every visible/notification claim maps to measured state or explicit preference.
- **Files to create:** UI state mapper and tests if not created earlier.
- **Files to modify:** `HomeFragment.kt`, `SettingsFragment.kt`, `WifiMonitorService.kt`, `strings.xml`.
- **Files to remove/deprecate:** “Active” shorthand, “auto-switched” messages, false current-SIM labels, hard-coded update claim.
- **Exact behavior changed:** UI distinguishes automation enabled, background monitor running, switching support, preferred SIM, default-data SIM, and active transport.
- **Dependencies:** Phases 2–5.
- **Risks:** Copy can become too technical.
- **Complexity:** Medium — behavior is mostly mapping but must remain accurate.
- **Test method:** Snapshot/state mapper tests plus T-01–T-16.
- **Completion criteria:** No unsupported success claim in any state or notification.
- **Rollback method:** Revert mapper/resources without reverting lower-level models.

## Phase 7 — Redesign Home UI

- **Objective:** Deliver a calm, professional, accessible status-first Home screen.
- **Files to create:** Only narrowly needed shape/icon/color-selector resources.
- **Files to modify:** `fragment_home.xml`, `HomeFragment.kt`, `strings.xml`, `colors.xml`, `themes.xml`, relevant drawables.
- **Files to remove/deprecate:** Pulse-ring animation/resources, fake update banner, repetitive status rows.
- **Exact behavior changed:** One primary connection card; secondary SIM/capability and automation cards; contextual action; visible checking/error/permission states.
- **Dependencies:** Stable UI state mapper.
- **Risks:** Small-screen and large-font clipping.
- **Complexity:** Medium — controlled native Views redesign, not a framework rewrite.
- **Test method:** Layout previews plus T-01/T-02/T-03/T-04/T-05/T-07/T-19.
- **Completion criteria:** Hierarchy and wording meet audit acceptance; 200% font remains usable.
- **Rollback method:** Restore previous layout/resources and mapper binding.

## Phase 8 — Refine Settings UI

- **Objective:** Make settings correspond to real capabilities and system shortcuts.
- **Files to create:** Optional selectors/icons only.
- **Files to modify:** `fragment_settings.xml`, `SettingsFragment.kt`, `strings.xml`, menu/theme resources.
- **Files to remove/deprecate:** Nonexistent SIM slot options, “switch” wording where only alerts/guidance exist, unrelated shortcuts.
- **Exact behavior changed:** Theme selector is clear; background guided-monitor toggle explains notification/force-stop limits; SIM choices reflect active subscriptions; settings shortcut has fallbacks.
- **Dependencies:** SIM diagnostics and service policy.
- **Risks:** OEM settings intents may differ.
- **Complexity:** Medium.
- **Test method:** T-10/T-11/T-12/T-13/T-14/T-16/T-19.
- **Completion criteria:** Every control has an observable effect and honest description.
- **Rollback method:** Restore previous layout/listeners while retaining safe lower layers.

## Phase 9 — Add diagnostics/logging required for testing

- **Objective:** Produce bounded, privacy-safe transition evidence.
- **Files to create:** `utils/DiagnosticLogger.kt` and tests if a helper is justified.
- **Files to modify:** `NetworkMonitor.kt`, `WifiMonitorService.kt`, `BootReceiver.kt`, `PreferencesManager.kt`, Settings diagnostics UI.
- **Files to remove/deprecate:** Raw subscription IDs and unstructured/sensitive logs.
- **Exact behavior changed:** Logs show previous/event/next/timestamp/reason with redacted telephony values; in-app history is bounded and clearable.
- **Dependencies:** Final state enums.
- **Risks:** Excess logging or personal data exposure.
- **Complexity:** Low — bounded structured logging with a narrow schema.
- **Test method:** Unit redaction/bounds tests and filtered logcat inspection.
- **Completion criteria:** Required transitions are provable without identifiers or unbounded growth.
- **Rollback method:** Disable diagnostic sink and revert helper calls.

## Phase 10 — Build testing APK

- **Objective:** Produce a reproducible debug-signed release test artifact.
- **Files to create:** Build evidence in `docs/CODEX_CHANGELOG.md` and test matrix.
- **Files to modify:** No source unless a build defect is diagnosed and separately recorded.
- **Files to remove/deprecate:** None.
- **Exact behavior changed:** None; packaging only.
- **Dependencies:** Unit tests and lint pass; reviewed diff.
- **Risks:** Debug signing can conflict with another installed signature.
- **Complexity:** Low.
- **Test method:** `assembleRelease`, `aapt`, `apksigner`, SHA-256 source/copy equality.
- **Completion criteria:** Build exit 0; metadata correct; labeled testing/debug-signed.
- **Rollback method:** Retain previous APK/hash and restore it from archive.

## Phase 11 — Install through ADB

- **Objective:** Install and launch the exact testing APK on the connected device.
- **Files to create:** ADB evidence/log snippets.
- **Files to modify:** Test documents only.
- **Files to remove/deprecate:** None.
- **Exact behavior changed:** Device installation only.
- **Dependencies:** Authorized device and compatible signature/package.
- **Risks:** Signature mismatch may require user-approved uninstall and data loss; do not uninstall silently.
- **Complexity:** Low unless signature conflict occurs.
- **Test method:** `adb install -r`, resolved launcher activity, focused process/logcat.
- **Completion criteria:** ADB returns `Success`; installed package hash/version correspond to artifact.
- **Rollback method:** Reinstall preserved APK if signatures match; otherwise ask before uninstalling.

## Phase 12 — Execute real-device matrix

- **Objective:** Run T-01 through T-20 with PASS/FAIL/BLOCKED/NOT APPLICABLE evidence.
- **Files to create:** Screenshots/log captures as privacy-safe evidence.
- **Files to modify:** `CODEX_TEST_MATRIX.md`, `CODEX_ADB_EVIDENCE.md`.
- **Files to remove/deprecate:** None.
- **Exact behavior changed:** None; verification.
- **Dependencies:** User cooperation for network/permission/airplane actions; reboot requires explicit agreement.
- **Risks:** Connectivity interruption and OEM variability.
- **Complexity:** High — broad state matrix and timing measurement.
- **Test method:** Mandatory matrix format with device, hash, steps, observed result, logs, timestamp.
- **Completion criteria:** All applicable critical tests executed; no pending item marked pass.
- **Rollback method:** Restore device network settings and stop background monitoring.

## Phase 13 — Fix observed failures

- **Objective:** Repair only failures demonstrated by Phase 12 evidence.
- **Files to create:** Focused regression tests.
- **Files to modify:** Exact implicated native files, documented per failure.
- **Files to remove/deprecate:** Any newly proven unsafe behavior.
- **Exact behavior changed:** Limited to observed defects; no unrelated rewrite.
- **Dependencies:** Reproducible failure and root-cause analysis.
- **Risks:** Fix can regress another transport/lifecycle path.
- **Complexity:** Medium to High depending on evidence.
- **Test method:** New failing test first where feasible, focused device reproduction, full critical regression.
- **Completion criteria:** Root cause documented; focused and regression tests pass.
- **Rollback method:** Revert each focused fix independently.

## Phase 14 — Rebuild and re-test

- **Objective:** Prove fixes on a new immutable APK hash.
- **Files to create:** Updated build/evidence entries.
- **Files to modify:** Test/changelog documents only unless another failure is found.
- **Files to remove/deprecate:** Superseded testing artifact from the deliverable path, while retaining its hash in records.
- **Exact behavior changed:** None beyond Phase 13 fixes.
- **Dependencies:** Phase 13 complete.
- **Risks:** Testing the wrong APK hash.
- **Complexity:** Medium.
- **Test method:** Rebuild, metadata/signature/hash verification, reinstall, critical matrix rerun.
- **Completion criteria:** Every result references the final candidate hash.
- **Rollback method:** Restore prior known artifact by recorded hash.

## Phase 15 — Produce final approved testing APK

- **Objective:** Deliver one traceable testing artifact and final report.
- **Files to create:** `docs/CODEX_FINAL_REPORT.md`, `docs/CODEX_KNOWN_LIMITATIONS.md`.
- **Files to modify:** `AutoSwitch.apk`, final test/changelog/ADB documents.
- **Files to remove/deprecate:** None; old hashes remain documented.
- **Exact behavior changed:** None; final packaging/copy.
- **Dependencies:** Critical tests pass or remaining blocks are explicitly accepted.
- **Risks:** Overstating readiness.
- **Complexity:** Low.
- **Test method:** Hash equality between release output and root copy; final metadata/signature inspection.
- **Completion criteria:** Report says `READY FOR USER TESTING`, `BLOCKED`, or `NOT READY` based only on evidence.
- **Rollback method:** Restore previous root APK from archive.

## Phase 16 — Prepare production signing only after explicit approval

- **Objective:** Design production signing/release handling after functionality and design approval.
- **Files to create:** Secure local signing configuration instructions; no key committed.
- **Files to modify:** Build signing configuration only after separate approval.
- **Files to remove/deprecate:** Debug signing from production release variant.
- **Exact behavior changed:** Production artifact becomes release-signed and versioned.
- **Dependencies:** Explicit user approval, secure credential location, completed critical tests.
- **Risks:** Key loss/exposure, package update incompatibility, Play policy/target SDK requirements.
- **Complexity:** High — signing identity is permanent operational infrastructure.
- **Test method:** `apksigner verify --print-certs`, upgrade install test, store policy checks.
- **Completion criteria:** Secrets remain outside Git; signed upgrade path is proven; release status is accurately stated.
- **Rollback method:** Stop before distribution; never replace or delete the signing key casually.

## Exact initial change set proposed after approval

Create:

```text
app/src/main/java/com/autoswitch/apk/AutoSwitchApplication.kt
app/src/main/java/com/autoswitch/apk/model/NetworkState.kt
app/src/main/java/com/autoswitch/apk/model/SimDiagnostics.kt
app/src/main/java/com/autoswitch/apk/utils/SimDiagnosticsProvider.kt
app/src/test/java/com/autoswitch/apk/network/NetworkStateReducerTest.kt
app/src/test/java/com/autoswitch/apk/ui/NetworkUiStateMapperTest.kt
app/src/test/java/com/autoswitch/apk/sim/SimCapabilityTest.kt
docs/CODEX_CHANGELOG.md
docs/CODEX_TEST_MATRIX.md
docs/CODEX_ADB_EVIDENCE.md
docs/CODEX_KNOWN_LIMITATIONS.md
```

Modify:

```text
gradle/libs.versions.toml
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/autoswitch/apk/MainActivity.kt
app/src/main/java/com/autoswitch/apk/model/SimInfo.kt
app/src/main/java/com/autoswitch/apk/receiver/BootReceiver.kt
app/src/main/java/com/autoswitch/apk/service/WifiMonitorService.kt
app/src/main/java/com/autoswitch/apk/ui/HomeFragment.kt
app/src/main/java/com/autoswitch/apk/ui/SettingsFragment.kt
app/src/main/java/com/autoswitch/apk/utils/NetworkMonitor.kt
app/src/main/java/com/autoswitch/apk/utils/PreferencesManager.kt
app/src/main/java/com/autoswitch/apk/utils/SimSwitchManager.kt
app/src/main/res/layout/activity_main.xml
app/src/main/res/layout/fragment_home.xml
app/src/main/res/layout/fragment_settings.xml
app/src/main/res/menu/bottom_nav_menu.xml
app/src/main/res/values/colors.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/main/res/values-night/colors.xml
```

Remove or deprecate after replacement is compiled and reviewed:

```text
app/src/main/java/com/autoswitch/apk/utils/UpdateManager.kt
app/src/main/res/xml/file_paths.xml
root/reflection switching methods in SimSwitchManager.kt
pulse/update-only drawable resources proven unused by lint
unused direct ConstraintLayout and lifecycle-service dependencies
FileProvider and REQUEST_INSTALL_PACKAGES manifest declarations
```

Explicitly out of scope for the native repair:

```text
app/(tabs)/index.tsx
app/(tabs)/settings.tsx
context/NetworkContext.tsx
all other Expo/React Native files
production signing keys
root/Shizuku/ADB-assisted switching
```

## Approval gate

Implementation begins only after the user sends:

```text
APPROVE PHASE 1
```

