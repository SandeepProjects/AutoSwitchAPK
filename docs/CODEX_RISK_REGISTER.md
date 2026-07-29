# AutoSwitch Risk Register

Audit date: 2026-07-29

| ID | Severity | Risk | Evidence | Impact | Proposed mitigation | Verification |
|---|---|---|---|---|---|---|
| R-01 | Critical | Root switching is enabled by default | `PreferencesManager.useRootSwitch` defaults `true` | Unexpected privileged commands | Default privileged behavior off; remove root execution from Phase 1 | Source test; no `su`/`cmd phone` in final DEX |
| R-02 | Critical | Target subscription can be `-1` | `preferredSubId` defaults `-1` and Settings only stores slot | Invalid global/phone settings mutation | Select only discovered active subscriptions; guided mode only | Unit tests and T-13/T-15 |
| R-03 | Critical | Switch success is not verified | Root exit code/reflection return immediately yields `true` | False success and wrong data SIM | Remove current switching; any future mode must re-read default data subscription | T-15 |
| R-04 | High | Active cellular SIM is falsely labeled | Home uses `preferredSimSlot` for cellular label | User may act on wrong SIM/billing assumption | Separate preferred, default-data, and active-cellular fields | Unit UI mapping tests and T-03 |
| R-05 | High | Network can remain in `CHECKING` | `onLost/onAvailable` force checking without guaranteed final query | Stale UI after handover | Default-network callback plus one coalesced timeout/reconciliation | Reducer tests and T-03/T-04 |
| R-06 | High | API 29 call on minSdk 26 | Lint `NewApi` error for `transportInfo` | Crash on Android 8/9 | Remove SSID or guard API and permissions | Lint and API 26 emulator/device |
| R-07 | High | Revocable phone permission is not safely modeled | Lint `MissingPermission`; denial callback ignored | Missing info, inconsistent UI, potential exception | Explicit permission state and guarded telephony access | Lint and T-12/T-13 |
| R-08 | High | Automation UI and service reality disagree | Preference defaults enabled; service is not started on first launch | False “active” claim | Default off; expose enabled/running/support as separate states | T-01/T-16 |
| R-09 | High | Fake update availability | Hard-coded target version 2 always exceeds installed 1 | Misleading banner and unsafe install path | Remove UpdateManager/UI/permission/provider until real signed update design exists | DEX/manifest inspection |
| R-10 | High | Background service claims unsupported automatic SIM switching | Manifest subtype, notification, service behavior | Misrepresentation; possible policy rejection | Guided notification-only wording/behavior | Manifest inspection and T-16 |
| R-11 | Medium | Any consumer can unregister shared callback | Fragment and service call singleton start/stop | Stale state while another collector remains | Process-scoped single owner; no consumer stop API | Lifecycle tests and T-08/T-10/T-16 |
| R-12 | Medium | Callback request observes non-default networks | `registerNetworkCallback(INTERNET)` | Unrelated/stale transitions | `registerDefaultNetworkCallback` | Unit transition logs and T-18 |
| R-13 | Medium | Delayed checks are not coalesced | Every callback posts a new 100 ms runnable | Reordering/churn | Dedicated serialized handler and one pending reconciliation | Transition log inspection |
| R-14 | Medium | Main-thread root check | `isRootAvailable()` called from `onViewCreated()` | UI stall/ANR risk | Remove root detection from normal UI | StrictMode/manual cold start |
| R-15 | Medium | Root process can wait indefinitely | Blocking `waitFor()` without timeout | Hung background service/thread | Remove; future privileged mode requires timeout | Source review |
| R-16 | Medium | Subscription identifiers enter logs/storage | Raw sub ID logging; `SimInfo.iccId` retained | Privacy exposure | Remove ICC ID and redact/omit sub IDs in logs | Logcat review T-13 |
| R-17 | Medium | Service continues after foreground-start failure | Broad catch around `startForeground` | Illegal/unstable background execution | Stop self and surface failure | Service tests/T-16 |
| R-18 | Medium | `START_STICKY` ignores disabled preference on recreation | `onStartCommand` only logs | Service may run contrary to UI intent | Re-check preference/action; use deliberate restart policy | T-16/T-17 |
| R-19 | Medium | FileProvider paths are overly broad | `external-path`, files, cache all path `.` | Excessive grantable file scope | Remove provider with update flow or narrow paths | Merged manifest |
| R-20 | Medium | Cold-start permission request lacks rationale | MainActivity requests phone + notification together | Poor consent; repeated denial confusion | Feature-triggered, separate permission education | T-12/T-13/T-16 |
| R-21 | Medium | No tests | `testReleaseUnitTest NO-SOURCE` | Regressions likely | Add reducer/mapper/capability tests | Gradle test PASS |
| R-22 | Medium | No real-device evidence | Empty `adb devices -l` | Runtime claims unproven | Execute T-01–T-20 after device attachment | ADB evidence |
| R-23 | Medium | Expo dependency set is inconsistent | SDK 54 with RN 0.76/React 18; SDK 54 expects RN 0.81/React 19.1 | Secondary build likely broken | Keep inactive; audit separately before any Expo build | `expo-doctor` in separate scope |
| R-24 | Low | Native and Expo package IDs differ | `.apk` vs `.app` | Two separate installed apps/confusion | Document and decide ownership later | Build metadata |
| R-25 | Low | Direct dependencies appear unused | ConstraintLayout and lifecycle-service | Unnecessary attack/maintenance surface | Remove after confirming compile/tests | Dependency report |
| R-26 | Medium | Light theme is partially hard-coded dark | Activity/nav resource choices | Visual inconsistency and contrast risk | Theme-aware tokens/selectors | T-11/T-19 |
| R-27 | Medium | Large text can overlap label/value rows | RelativeLayout opposing text without constraints | Accessibility failure | Responsive vertical/constraint layouts | T-19 |
| R-28 | Low | Hard-coded/unlocalized strings | 40 `HardcodedText`, 15 `SetTextI18n` warnings | Accessibility/localization debt | Move user text to resources with formatted strings | Lint |
| R-29 | Low | Target/AGP/dependencies are outdated | Lint dependency/target findings | Maintenance/security compatibility debt | Upgrade in isolated post-functional phase | Compatibility matrix and regression suite |

Risk acceptance is not implied by this document. R-01 through R-10 must be resolved before a testing APK is presented as trustworthy.

