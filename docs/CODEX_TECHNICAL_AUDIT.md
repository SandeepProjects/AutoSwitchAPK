# AutoSwitch Technical Audit

Audit timestamp: 2026-07-29 (Asia/Nicosia)

Status: **NOT READY**

Scope: read-only source/build/APK inspection plus non-mutating Gradle diagnostics. No application source or build configuration was changed. The only repository additions from this audit are the requested documents under `docs/`.

## Evidence classification

### Confirmed by source inspection

- The repository contains independent native Android and Expo/React Native application layers.
- The native network monitor uses one singleton `ConnectivityManager.NetworkCallback`, a `MutableStateFlow`, delayed active-network queries, and manual start/stop ownership shared by a fragment and service.
- Root command execution is enabled by default in preferences.
- SIM switching reports success from reflection invocation or root process exit code without re-reading the default data subscription.
- The UI displays the configured preferred SIM as though it were the active cellular SIM.
- The foreground service claims automatic switching and can launch system settings from a background fallback.
- The update banner is driven by a hard-coded version 2 claim, not a remote or trustworthy local version source.
- Phone and notification permissions are requested together at cold start without an in-app rationale; denial is not surfaced.
- There are no automated tests.

### Confirmed by build tooling

- Gradle includes only the native `:app` module.
- `testReleaseUnitTest` has no source.
- `lintRelease` fails with 2 errors and 139 warnings.
- The two lint errors are an unguarded API 29 call while minSdk is 26 and missing runtime permission handling for `TelephonyManager.networkType`.
- Major warning groups include 40 hard-coded strings, 38 unused resources, 15 dynamic unlocalized strings, 12 outdated dependency findings, and missing image accessibility text.

### Confirmed by APK inspection

- `AutoSwitch.apk` is a parseable native APK for `com.autoswitch.apk`.
- It contains the audited native classes and switching command strings.
- It contains no React Native/Expo JavaScript bundle.
- It is V2-signed with an Android debug certificate.
- The root APK is identical to `app/build/outputs/apk/release/app-release.apk`.

### Confirmed on physical device

None. `adb devices -l` returned no attached devices.

### Unverified

- Actual callback timing and visible handover latency.
- Whether the current app stays permanently in `CHECKING` on the user's device.
- OEM behavior of SIM settings intents.
- Active subscription count and permission behavior on the target device.
- Foreground-service start/restart behavior on the target OEM build.
- Theme, accessibility, rotation, small-screen, and large-font runtime behavior.
- Whether any root command works on the device.
- Whether the device is rooted.

### Blocked

- All physical-device tests T-01 through T-20: **BLOCKED — no device connected**.
- Bug reproduction using Wi-Fi/mobile transitions: **BLOCKED — no device connected**.
- Installation/launch/logcat evidence: **BLOCKED — no device connected**.

## Preservation evidence

Initial repository state:

```text
Root:   C:\Users\bagos\OneDrive\Desktop\AutoSwitchAPK
Branch: main
HEAD:   5b236ed Fix settings deep-link intent fallback for Vivo/iQOO devices, status bar padding, and updated app icon
```

The initial worktree already contained modified native/Expo files and many untracked native files. No attempt was made to reset, overwrite, stage, or commit them.

Safe archive:

```text
Path:   C:\Users\bagos\OneDrive\Desktop\AutoSwitchAPK_audit_backup_20260729_134423.zip
Bytes:  8,386,552
SHA256: 47850360C255038DFE00A1075D40F9247D1083989D7D87B44705D1E6008E223C
```

Excluded: `.gradle`, `build`, `app/build`, `node_modules`, `.expo`.

## APK metadata

Tooling: Android SDK Build Tools 37.0.0 `aapt` and `apksigner`.

```text
Path:         C:\Users\bagos\OneDrive\Desktop\AutoSwitchAPK\AutoSwitch.apk
Size:         4,926,668 bytes
Modified:     2026-07-29T13:30:10.5011381+03:00
SHA-256:      12B6C364E8A0723EDB7C6F9C9E1FCF1BC980A52EB041BE7DA2F1AE26DD01912C
Package:      com.autoswitch.apk
Version:      1.0.0 (1)
Min SDK:      26
Target SDK:   34
Compile SDK:  34
Launcher:     com.autoswitch.apk.MainActivity
Signature:    V2, Android Debug certificate, RSA 2048
Cert SHA-256: 81EE03EA453673150D8567606F8E2749D92B9C8FF34A377C54830E1792036514
```

Label: `AutoSwitch APK`. This is a **TESTING APK — DEBUG-SIGNED — NOT FOR PLAY STORE DISTRIBUTION**.

## Manifest and permission audit

The generated release merged manifest was inspected at `app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`.

| Permission | Type / API notes | Actual use and denial behavior | Finding |
|---|---|---|---|
| `INTERNET` | Normal/install-time | No network client exists; capability inspection does not need it | Currently unused |
| `ACCESS_NETWORK_STATE` | Normal/install-time | Required for `ConnectivityManager` state/capabilities | Keep |
| `CHANGE_NETWORK_STATE` | Normal/install-time | No API changes network state | Remove |
| `READ_PHONE_STATE` | Dangerous/runtime | Used indirectly for subscription/radio details; cold-start requested; denial ignored in UI | Optional feature only; redesign request/denial flow |
| `READ_BASIC_PHONE_STATE` | Non-dangerous, API 33+ | Used to support `networkType`; duplicated by broader phone permission and unguarded below API 33 | Remove or strictly version-gate; not needed for core monitoring |
| `FOREGROUND_SERVICE` | Normal/install-time, API 28+ | Required by optional monitor service | Keep only if guided background monitoring remains |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Normal/app-op/instant, API 34+ | Used by `specialUse` service | Technically declared; use-case/Play eligibility still needs review |
| `RECEIVE_BOOT_COMPLETED` | Normal/install-time | Starts service after boot/package replacement when preference is enabled | Keep only for explicit background opt-in |
| `POST_NOTIFICATIONS` | Dangerous/runtime on API 33+ | Requested at cold start; denial not explained; an FGS may run with reduced notification visibility | Request only when background monitoring is enabled |
| `REQUEST_INSTALL_PACKAGES` | Special app access/declaration | Supports hard-coded local update feature; unknown-source access is not checked | Remove with misleading update flow |

Not declared and not currently justified: `CHANGE_WIFI_STATE`, `READ_PHONE_NUMBERS`, `MODIFY_PHONE_STATE`, `FOREGROUND_SERVICE_DATA_SYNC`.

`ACCESS_WIFI_STATE` is not declared. The app attempts to derive an SSID from `NetworkCapabilities.transportInfo`; on modern Android this information may be redacted and can involve additional privacy permissions. The repair should avoid showing SSID unless a real, justified permission design is approved.

## Component audit

| Component | Exported | Finding |
|---|---:|---|
| `MainActivity` | Yes | Correctly exported for launcher intent |
| `WifiMonitorService` | No | Uses `specialUse`; description falsely claims automatic SIM switching |
| `BootReceiver` | Yes | Listens only for system boot/package replacement; exposure should be minimized and device-tested |
| `FileProvider` | No | Correctly a file-sharing provider, not an update checker; paths expose broad external/files/cache roots to any URI grant |
| AndroidX Startup provider | No | Added transitively |
| AndroidX ProfileInstallReceiver | Yes with `DUMP` permission | Library component protected by a system signature permission |

There are no deep links. The service notification uses an immutable/update-current `PendingIntent`, which is appropriate. A notification channel is created. There is no code that checks whether the foreground service is already running; repeated `startService` calls reuse the service instance, but lifecycle and collector idempotence are implicit rather than explicit.

## Network-monitoring map

### Active native path

```text
ConnectivityManager.registerNetworkCallback(INTERNET request)
    ↓ callbacks for any matching network, not specifically the default network
NetworkMonitor.triggerStateCheck()
    ↓ fixed 100 ms Handler delay
ConnectivityManager.activeNetwork + getNetworkCapabilities()
    ↓
MutableStateFlow<NetworkState>
    ├── HomeFragment (repeatOnLifecycle STARTED)
    └── WifiMonitorService (service CoroutineScope collector)
```

### Inactive Expo path

`context/NetworkContext.tsx` separately registers `NetInfo.addEventListener`, but this code is absent from the native APK.

### Callback count and lifecycle

- The singleton owns one callback object and `isRegistered` prevents simultaneous duplicate native registration.
- `HomeFragment.onResume()` and `WifiMonitorService.onCreate()` both call `startMonitoring()`.
- `HomeFragment.onPause()` may call `stopMonitoring()` when automation is disabled.
- `WifiMonitorService.onDestroy()` always calls `stopMonitoring()`.
- Ownership is not reference-counted. One consumer can unregister the shared callback while another collector remains active.
- StateFlow collectors are lifecycle-safe in `HomeFragment`; view binding is cleared correctly.
- The service scope uses `SupervisorJob + Dispatchers.Default` and is canceled in `onDestroy()`.
- The application process has no explicit monitor owner. Cold start begins with a default `NONE` state, so an offline flash is possible before the first query.

### Correctness defects

1. `registerNetworkCallback()` receives events for all networks with the internet capability, while the code always queries only `activeNetwork`. A non-default network event can cause unrelated state churn.
2. `onAvailable()` and `onLost()` force `CHECKING`, but no guaranteed second reconciliation is scheduled. If Android does not deliver a later capability callback, the app can remain stuck on `CHECKING`.
3. Every event adds a delayed runnable; pending checks are not coalesced or removed.
4. The model omits `NET_CAPABILITY_INTERNET`, metering, VPN, Ethernet, permission/error state, and transition reason.
5. Unsupported transports are mapped to `NONE`, so a validated VPN or Ethernet network can be shown as offline.
6. `NET_CAPABILITY_VALIDATED` is read correctly, but the UI translates any unvalidated state to red “Internet Access: Unavailable,” including normal handover and connected-but-unvalidated Wi-Fi.
7. `transportInfo` is called without an API 29 guard even though minSdk is 26. Lint marks this as a build-blocking `NewApi` error.
8. `TelephonyManager.networkType` lacks explicit permission handling. Lint marks this as a build-blocking `MissingPermission` error.
9. The field named `carrierName` actually stores “2G/3G/4G/5G,” not a carrier name.
10. `lastChangeTime` changes for every query, even when the semantic state is unchanged.

### Required authoritative model

Proposed immutable native model:

```kotlin
data class NetworkState(
    val phase: NetworkPhase,
    val transport: NetworkTransport,
    val hasInternetCapability: Boolean,
    val isValidated: Boolean,
    val isMetered: Boolean?,
    val cellularSubscriptionId: Int?,
    val updatedAtEpochMillis: Long,
    val reason: NetworkReason?,
    val error: NetworkError?
)

enum class NetworkPhase {
    INITIALIZING, CHECKING, CONNECTED, OFFLINE, ERROR
}

enum class NetworkTransport {
    WIFI, CELLULAR, ETHERNET, VPN, OTHER, NONE
}
```

Phone-permission and SIM diagnostics should be a separate model so denial cannot break basic connectivity:

```kotlin
data class SimDiagnostics(
    val permissionState: PhonePermissionState,
    val activeSubscriptions: List<SimSummary>,
    val defaultDataSubscriptionId: Int?,
    val activeCellularSubscriptionId: Int?,
    val mobileDataEnabled: Boolean?,
    val switchingCapability: SwitchingCapability,
    val updatedAtEpochMillis: Long
)
```

Exact mechanism:

```text
AutoSwitchApplication
→ one process-scoped NetworkMonitor
→ registerDefaultNetworkCallback once on a dedicated serialized HandlerThread
→ read-only StateFlow<NetworkState>
→ HomeFragment collects with repeatOnLifecycle(STARTED)
→ optional WifiMonitorService collects the same flow
```

Monitoring starts eagerly when the app process starts, stays registered for process lifetime, and does not expose consumer-controlled `stopMonitoring()`. This prevents duplicate registration and ownership races. Process death discards the monitor; cold start emits `INITIALIZING`, then reconciles the current default network. `onLost` emits a short `CHECKING` transition and schedules one coalesced reconciliation; `onAvailable`/`onCapabilitiesChanged` cancel the pending handover and publish immediately. A prolonged lack of a default network becomes `OFFLINE`, not permanent `CHECKING`.

## SIM and telephony findings

The current code correctly reads subscription ID, slot index, and default-data subscription when permission allows, but the result is not used by the Home screen.

Critical defects:

- `preferredSubId` defaults to `-1` and is never updated by the Settings SIM radio buttons.
- `useRootSwitch` defaults to `true`.
- A Wi-Fi loss can therefore execute root commands with subscription ID `-1`.
- Root detection runs `Runtime.exec("which su")` synchronously from `HomeFragment.onViewCreated()`, potentially blocking the main thread.
- Root commands have no timeout and trust only process exit code.
- Hidden-API reflection trusts a return without re-reading `getDefaultDataSubscriptionId()`.
- The app logs raw subscription IDs.
- `SimInfo` retains ICC ID even though the UI does not need it.
- Home cellular text uses `preferredSimSlot` as the current connection SIM. This is a false claim.
- “Supported (Root)” is inferred from finding an `su` binary, not from a verified switching capability.
- The settings chooser always offers SIM 1 and SIM 2 even on single-SIM devices or inactive slots.
- Background fallback calls `startActivity()` to open settings, which may be blocked by background activity-launch restrictions.

Initial repair capability policy:

```text
Phone permission denied          → PERMISSION_REQUIRED
No active subscription          → UNAVAILABLE
Standard third-party app         → GUIDED_MANUAL_ONLY
Root/assisted modes              → not implemented in Phase 1
Unexpected diagnostic failure    → UNKNOWN
```

Root and hidden-API switching should be removed from the approved first implementation. Any future privileged mode must be a separately approved, explicit opt-in and must verify the default data subscription after every operation.

## Service and lifecycle findings

- The service exists to observe Wi-Fi loss and attempt SIM switching or open settings.
- It is a foreground service with a low-importance channel and `specialUse` type.
- The service calls `startForeground()` in `onCreate()`, but catches all failure and continues running; failure should stop the service.
- `START_STICKY` can request recreation even though `onStartCommand()` does not re-check whether automation is still enabled.
- The service's local `isWifiConnected` starts `false`, so a cold start on cellular does not generate a Wi-Fi-loss event; that is reasonable, but the state machine is undocumented.
- Repeated starts do not create repeated `onCreate()` collectors in one service instance, but explicit collector job idempotence is missing.
- Disabling the toggle calls `stopService()` and cancels the scope in `onDestroy()`.
- The preference defaults enabled, yet first application launch does not start the service. The UI can say automation is active while no background service is running.
- Boot/package replacement starts the service when the preference is enabled.
- Force-stop prevents normal restart until the user launches/interacts again; the UI/docs do not disclose this.
- OEM battery optimization and Android background restrictions remain unverified.
- Notification denial is not handled as a service-specific state.

Recommended Phase 1 behavior: default background monitoring to off; keep a foreground service only for explicit guided Wi-Fi-loss notifications; never execute root/reflection; never launch settings directly from the background; provide a notification action that the user taps.

## Update flow findings

`UpdateManager` is not a real update checker:

- It hard-codes latest version code 2/name 1.0.1 and release notes.
- Every installed versionCode 1 build reports an update even when no update APK exists.
- It searches for a file named `AutoSwitch.apk` in public Downloads or app external files.
- The UI shows “New Update Available” even when the install file is null.
- It does not verify the candidate package, version, signing certificate, or hash.
- `FileProvider` is only the secure URI-sharing mechanism for the install intent.

This feature is misleading and broadens permission/file exposure. Remove it from the repaired testing build unless a trustworthy update source and signature-validation design is separately approved.

## UI/UX and accessibility findings

- Connection, validation, automation, switching support, and preferred SIM exist in the same visual hierarchy but are not semantically reliable.
- The current native orb is smaller than the reported Expo orb, but still continuously animates and consumes attention without adding information.
- “Automation Active” and row value “Active” can appear while the service is not running and automatic switching is unsupported.
- Cellular status displays the preferred SIM rather than measured current/default data SIM.
- Unvalidated Wi-Fi is styled as internet unavailable without explaining that transport remains connected.
- The “Open SIM Settings” action is always visible instead of being contextual.
- The local update banner makes an unsupported update claim.
- Activity and bottom navigation backgrounds/tints are hard-coded dark, so light theme is internally inconsistent.
- Bottom navigation uses the same tint for selected and unselected items.
- Unicode symbols/emoji and vector icons are mixed, producing inconsistent icon weight and accessibility.
- The status `ImageView` has no content description; lint reports it.
- Most strings are hard-coded in layouts/code; lint reports 40 `HardcodedText` and 15 `SetTextI18n` findings.
- Relative-layout label/value rows can overlap under large fonts or long translations.
- No landscape, small-screen, large-font, contrast, focus-order, or TalkBack test has been performed.

## Diagnostic results

Commands:

```powershell
.\gradlew.bat :app:dependencies --configuration releaseRuntimeClasspath --console=plain
.\gradlew.bat :app:sourceSets --console=plain
.\gradlew.bat :app:testReleaseUnitTest :app:lintRelease --console=plain
```

Results:

```text
Dependency report: PASS
Source-set report: PASS
Unit tests:         NO-SOURCE
Lint:               FAIL — 2 errors, 139 warnings
```

No APK rebuild was performed, so the preserved historical APK was not overwritten.

## Primary risks and disposition

1. **Critical:** root commands enabled by default with target subscription `-1`.
2. **Critical:** unverified root/reflection operation can be presented as successful.
3. **High:** active cellular SIM is falsely derived from user preference.
4. **High:** handover may stay stuck in `CHECKING`.
5. **High:** minSdk 26 app calls API 29 without a guard.
6. **High:** UI can claim automation active while the service is not running.
7. **High:** hard-coded update availability and unverified local APK installation.
8. **Medium:** shared monitor ownership allows one consumer to unregister another's callback.
9. **Medium:** permission denial and notification behavior are not represented honestly.
10. **Medium:** no automated or physical-device evidence exists.

Detailed entries are in `docs/CODEX_RISK_REGISTER.md`.

## Audit conclusion

The native APK source-of-truth is established, but the current APK is not ready for user testing as a trustworthy network/SIM utility. It compiles into an APK, but lint fails and several displayed states are technically false or unsafe. The Expo files do not affect this APK and should not be edited during the native repair.

Implementation must remain gated until the user explicitly sends `APPROVE PHASE 1`.

