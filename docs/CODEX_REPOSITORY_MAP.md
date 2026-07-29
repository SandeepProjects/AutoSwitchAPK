# AutoSwitch Repository Map

Audit timestamp: 2026-07-29 (Asia/Nicosia)

## Repository identity

- Root: `C:\Users\bagos\OneDrive\Desktop\AutoSwitchAPK`
- Git branch: `main`
- HEAD: `5b236ed Fix settings deep-link intent fallback for Vivo/iQOO devices, status bar padding, and updated app icon`
- Gradle project name: `AutoSwitchAPK`
- Included Gradle modules: `:app` only
- Native application module: `app`
- Native application ID: `com.autoswitch.apk`
- Expo Android package if built separately: `com.autoswitch.app`

The working tree was already substantially modified before this audit. The audit did not alter application source or build configuration.

## Concise topology

```text
AutoSwitchAPK/
├── settings.gradle.kts              # Includes only :app
├── build.gradle.kts                 # Root Android plugins
├── gradle/
│   ├── libs.versions.toml           # Android dependency/plugin catalog
│   └── wrapper/                     # Gradle 8.4 wrapper
├── app/
│   ├── build.gradle.kts             # Native Android application module
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/autoswitch/apk/
│   │   │   ├── MainActivity.kt
│   │   │   ├── model/SimInfo.kt
│   │   │   ├── receiver/BootReceiver.kt
│   │   │   ├── service/WifiMonitorService.kt
│   │   │   ├── ui/{HomeFragment,SettingsFragment}.kt
│   │   │   └── utils/{NetworkMonitor,PreferencesManager,SimSwitchManager,UpdateManager}.kt
│   │   └── res/                     # Native layouts, drawables, menu, values, XML
│   ├── _layout.tsx                  # Expo Router source, not a Gradle source set
│   └── (tabs)/                      # Expo Router screens, not a Gradle source set
├── context/                         # Expo React contexts
├── constants/, hooks/, assets/      # Expo support code/assets
├── package.json                     # Expo/React Native toolchain
├── app.json                         # Expo app configuration
└── AutoSwitch.apk                   # Copy of current native release artifact
```

Generated directories (`.gradle`, `app/build`, `node_modules`, `.expo`) are omitted.

## Active native build

`settings.gradle.kts` includes only `:app`. The Android `main` source set reported by Gradle is:

```text
Kotlin/Java: app/src/main/kotlin, app/src/main/java
Manifest:    app/src/main/AndroidManifest.xml
Resources:   app/src/main/res
Assets:      app/src/main/assets
```

There are no custom source sets, product flavors, or additional Gradle modules. The TypeScript route files directly under `app/` do not match any Android Gradle source set.

Native build configuration:

| Property | Value |
|---|---|
| Android Gradle Plugin | 8.3.2 |
| Kotlin Android plugin | 1.9.23 |
| Gradle wrapper | 8.4 |
| Java/Kotlin target | 17 |
| compileSdk | 34 |
| minSdk | 26 |
| targetSdk | 34 |
| versionCode | 1 |
| versionName | 1.0.0 |
| Debug signing | Android debug key |
| Release signing | Android debug key |
| Release minification | Disabled |
| Resource shrinking | Disabled/not configured |
| View binding | Enabled |

The release build is therefore a testing build, not a production-signed release.

## APK source-of-truth proof

The root APK and Gradle release APK are byte-for-byte identical:

| Path | Bytes | SHA-256 |
|---|---:|---|
| `AutoSwitch.apk` | 4,926,668 | `12B6C364E8A0723EDB7C6F9C9E1FCF1BC980A52EB041BE7DA2F1AE26DD01912C` |
| `app/build/outputs/apk/release/app-release.apk` | 4,926,668 | `12B6C364E8A0723EDB7C6F9C9E1FCF1BC980A52EB041BE7DA2F1AE26DD01912C` |

`aapt` parsed the APK as `com.autoswitch.apk` and `dexdump` found the native classes, including:

```text
com/autoswitch/apk/MainActivity
com/autoswitch/apk/ui/HomeFragment
com/autoswitch/apk/ui/SettingsFragment
com/autoswitch/apk/utils/NetworkMonitor
com/autoswitch/apk/utils/SimSwitchManager
com/autoswitch/apk/service/WifiMonitorService
com/autoswitch/apk/receiver/BootReceiver
```

The DEX also contains current native implementation strings and commands such as `Internet Access: Unavailable`, `Automation Active`, `setDefaultDataSubId`, and `cmd phone data set-data-subscription`.

No `index.android.bundle`, Expo bundle, React Native classes, or Expo assets were found in the APK. Consequently:

- `app/(tabs)/index.tsx`: **inactive for the native APK**
- `app/(tabs)/settings.tsx`: **inactive for the native APK**
- `context/NetworkContext.tsx`: **inactive for the native APK**

The native Kotlin layer is the application layer compiled into `AutoSwitch.apk`.

## Expo/React Native layer

The repository also declares Expo SDK 54, Expo Router 6, React Native 0.76.7, and React 18.3.1. Expo's SDK 54 compatibility table targets React Native 0.81 and React 19.1; the declared React Native/React versions correspond to the older SDK 52 generation. This layer is therefore version-inconsistent and must not be represented as a verified secondary build.

Further ambiguity:

- Native and Expo builds use different package IDs.
- The same `app/` directory is both the Gradle module root and Expo Router route root.
- Gradle ignores route TypeScript; Expo tooling may still watch native module content.
- There is no evidence that EAS/Expo prebuild integrates the existing native module.

The Expo layer should be retained but left untouched during the native repair. Its ownership and intended build target require a separate decision.

## Native dependencies

| Direct dependency | Current role | Audit finding |
|---|---|---|
| `androidx.core:core-ktx:1.12.0` | Core Android/Kotlin helpers, notifications, FileProvider | Used; outdated |
| `androidx.appcompat:appcompat:1.6.1` | `AppCompatActivity`, themes, fragments transitively | Used; outdated |
| `com.google.android.material:material:1.11.0` | Material views/theme/navigation/cards/buttons | Used; outdated |
| `androidx.constraintlayout:constraintlayout:2.1.4` | ConstraintLayout widgets | No native layout uses ConstraintLayout; direct dependency appears unnecessary |
| `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0` | `lifecycleScope`, `repeatOnLifecycle` | Used; outdated |
| `androidx.lifecycle:lifecycle-service:2.7.0` | Lifecycle-aware service classes | `WifiMonitorService` extends plain `Service`; direct dependency appears unused |

Coroutines are used directly but arrive transitively through Lifecycle. A direct coroutines dependency should be declared if coroutine use remains.

Lint reported newer stable versions for all direct Android libraries and AGP. Upgrades should be isolated from the functional repair because current AGP/Kotlin-to-latest upgrades cross major compatibility boundaries. Material Components Views and ConstraintLayout are now in maintenance mode, but a Compose rewrite is not justified for this repair.

No telephony, root, shell, update-network, analytics, or third-party behavior libraries are present. Root behavior is implemented directly with `Runtime.exec`.

## Tests

There are no files under `app/src/test` or `app/src/androidTest`. Gradle reports `testReleaseUnitTest NO-SOURCE`.

