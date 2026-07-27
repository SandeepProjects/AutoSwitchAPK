# AutoSwitch — Complete Agent Briefing

> Hand this file to any agent that has access to the Android SDK / EAS build tools.  
> It contains the full context, architecture, code map, design system, and exact next steps to build a release APK.

---

## 1. What This App Does

**AutoSwitch** is a minimal Android utility app (2 screens) that:

1. **Monitors Wi-Fi** in real time using Android's network change events (no polling — battery-friendly)
2. **Alerts the user the moment Wi-Fi drops** with a native Android `Alert` that offers "Open Settings" → deep-links straight to Android wireless settings
3. **Lets the user pick SIM 1 or SIM 2** as their preferred mobile data card
4. **Has a dark / light / system theme toggle** that persists across restarts

There is **no backend, no database, no server**. Everything is local-only, stored in `AsyncStorage`.

---

## 2. Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Framework | **Expo SDK 54** (`expo ~54.0.27`) | Cross-platform, Expo Go preview, EAS build |
| Language | **TypeScript 5.9** | Strict typing throughout |
| Navigation | **Expo Router 6** (file-based) | 2 tabs: Home + Settings |
| Animations | **react-native-reanimated 4** | Pulse rings on the status orb |
| Network | **@react-native-community/netinfo 12** | Event-driven WiFi/cellular detection |
| Storage | **@react-native-async-storage/async-storage 2.2** | Persist settings + theme choice |
| Icons | **@expo/vector-icons** (`MaterialCommunityIcons`) | No emojis anywhere |
| Haptics | **expo-haptics** | Tactile feedback on toggles |
| Safe Area | **react-native-safe-area-context 5** | Android 16 edge-to-edge compatible |
| Fonts | **@expo-google-fonts/inter** | Inter 400/500/600/700 |

> **Note:** `expo-background-fetch`, `expo-notifications`, and `expo-task-manager` are listed in `package.json` from a previous attempt but are **NOT used** — do not import them. They cause crashes in Expo Go.

---

## 3. File Structure

```
artifacts/mobile/
├── app/
│   ├── _layout.tsx            ← Root layout: providers + font loading
│   ├── +not-found.tsx         ← 404 screen (scaffold default)
│   └── (tabs)/
│       ├── _layout.tsx        ← Tab bar: Home + Settings (NativeTabs on iOS 26, Tabs on Android)
│       ├── index.tsx          ← HOME SCREEN — live connection status + orb animation
│       └── settings.tsx       ← SETTINGS SCREEN — theme toggle + auto-switch + tips
├── context/
│   ├── NetworkContext.tsx     ← Monitors WiFi/cellular, fires Alert on drop, stores autoSwitch setting
│   └── ThemeContext.tsx       ← User theme preference (system/light/dark), persisted in AsyncStorage
├── constants/
│   └── colors.ts              ← Full light + dark palette tokens
├── hooks/
│   └── useColors.ts           ← Reads ThemeContext → returns correct palette
├── components/
│   ├── ErrorBoundary.tsx      ← Scaffold default, wraps whole app
│   └── ui/                    ← Scaffold UI primitives (not used by app screens)
├── assets/
│   └── images/
│       └── icon.png           ← Custom app icon (shield + wifi + cyan glow on navy)
└── app.json                   ← Expo config, splash backgroundColor: "#050B18"
```

---

## 4. Provider Tree (app/_layout.tsx)

```
SafeAreaProvider
  └── ErrorBoundary
        └── ThemeProvider          ← must wrap everything (useColors depends on it)
              └── QueryClientProvider
                    └── GestureHandlerRootView
                          └── KeyboardProvider
                                └── NetworkProvider   ← starts NetInfo listener here
                                      └── Stack (expo-router)
                                            └── (tabs)
```

---

## 5. Design System

### Color Tokens (`constants/colors.ts`)

```ts
// DARK theme (default on Android dark mode)
background:       '#050B18'   // deep navy
card:             '#0D1526'
foreground:       '#F0F4FF'
primary:          '#00C8FF'   // electric cyan  — active states, switches, buttons
accent:           '#00E87A'   // electric green — SIM 2 badge, success states
success:          '#00E87A'
destructive:      '#FF3B3B'
warning:          '#FFB020'
mutedForeground:  '#8896A8'
border:           '#1A2B45'

// LIGHT theme
background:       '#F0F4FF'
card:             '#FFFFFF'
primary:          '#0062FF'
accent:           '#00C97A'
success:          '#00C97A'
```

### Typography
- Font family: **Inter** (loaded via `@expo-google-fonts/inter`)
- All weights used: `Inter_400Regular`, `Inter_500Medium`, `Inter_600SemiBold`, `Inter_700Bold`
- Font sizes are **responsive** — computed from `useWindowDimensions().width`, not hardcoded

### Responsive Layout Pattern
Every screen uses this pattern:
```tsx
const { width, height } = useWindowDimensions();
const isTablet = width >= 600;
const hPad = Math.round(width * 0.06);
const orbSize = Math.min(Math.round(width * (isTablet ? 0.28 : 0.42)), 196);
```
This scales correctly across all Android phones, tablets, and foldables.

### Safe Area
Always use `useSafeAreaInsets()` — never hardcode top/bottom padding. Android 16 forces edge-to-edge.
```tsx
const insets = useSafeAreaInsets();
const topPad = Platform.OS === 'web' ? 67 : insets.top;
const botPad = Platform.OS === 'web' ? 34 + 84 : insets.bottom + 84;
```

---

## 6. Screen Details

### Home Screen (`app/(tabs)/index.tsx`)

**Layout (top → bottom):**
1. **Header row** — "AutoSwitch" title + "Network Monitor" subtitle + Active/Paused pill badge
2. **Status orb** — fixed-size container `{ width: orbSize, height: orbSize }` with:
   - Two `PulseRing` components using `StyleSheet.absoluteFillObject` (so scale() expands from dead center)
   - Orb circle with icon inside
   - Color: green (`success`) when WiFi, cyan (`primary`) when cellular, red (`destructive`) when offline
3. **Network name** — large bold text below orb (SSID if WiFi, "Mobile Data · 4G" if cellular)
4. **Status label** — "Connected via Wi-Fi" / "Connected via SIM 1" / "Offline — no internet"
5. **Info card** — two rows: Auto-Switch status + Internet availability
6. **CTA button** — "Enable Mobile Data" — only shown when `!isConnected`

**Pulse ring animation — critical detail:**
```tsx
// Both rings live INSIDE the fixed-size orb container, not as siblings of it.
// StyleSheet.absoluteFillObject overlays them exactly on the orb.
// React Native's default transform-origin is element center → rings expand outward correctly.
<View style={{ width: orbSize, height: orbSize }}>
  <PulseRing ... />   // delay: 0
  <PulseRing ... />   // delay: 600ms
  <View style={styles.orb}>...</View>
</View>
```

---

### Settings Screen (`app/(tabs)/settings.tsx`)

**Sections:**
1. **APPEARANCE** — `ThemeSegment` component: three-button segmented control (Auto / Light / Dark)
2. **AUTOMATION** — `Switch` for Auto-Switch toggle, stored via `NetworkContext.setAutoSwitchEnabled()`
3. **DATA SIM** — Static row showing "SIM 1 · Active" badge (SIM selection is set in NetworkContext)
4. **DEVICE** — "Open Network Settings" → `Linking.openURL('android.settings.WIRELESS_SETTINGS')`
5. **TIPS & PERMISSIONS** — Three `TipCard` components:
   - Battery Optimization → `android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS`
   - Background Data → `android.settings.DATA_USAGE_SETTINGS`
   - Switch to Mobile Data → `android.settings.WIFI_SETTINGS`
6. **ABOUT** — Version 1.0.0 + Platform (shows Android version number)

---

## 7. Context API Reference

### `useNetwork()` — from `context/NetworkContext.tsx`

```ts
{
  netState: NetInfoState | null,      // raw @react-native-community/netinfo state
  isConnected: boolean,               // true if any internet available
  connectionType: string,             // 'wifi' | 'cellular' | 'none' | 'other'
  ssid: string | null,                // WiFi network name (null if not on WiFi)
  cellularGeneration: string | null,  // '4G' | '5G' | '3G' etc. (null if not cellular)
  autoSwitchEnabled: boolean,         // persisted in AsyncStorage key: '@autoswitch_v2_settings'
  setAutoSwitchEnabled: (val: boolean) => void,
  openMobileSettings: () => void,     // deep-links to Android wireless settings
}
```

**Key behavior:** When `connectionType` transitions from `'wifi'` to anything else AND `autoSwitchEnabled === true`, a native `Alert.alert()` fires with "Open Settings" action.

### `useTheme()` — from `context/ThemeContext.tsx`

```ts
{
  preference: 'system' | 'light' | 'dark',   // what user chose, persisted '@autoswitch_theme'
  resolved: 'light' | 'dark',                // actual scheme after applying system default
  setPreference: (p: ThemePreference) => void,
}
```

### `useColors()` — from `hooks/useColors.ts`

Returns the active palette from `constants/colors.ts` based on `useTheme().resolved`. Always use this — never hardcode hex values in components.

---

## 8. AsyncStorage Keys

| Key | Type | Purpose |
|---|---|---|
| `@autoswitch_v2_settings` | `{ autoSwitchEnabled: boolean }` | Auto-switch on/off |
| `@autoswitch_theme` | `'system' \| 'light' \| 'dark'` | Theme preference |

---

## 9. Android-Specific Details

### Deep-link URLs used
```
android.settings.WIRELESS_SETTINGS           → Wi-Fi & mobile data overview
android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS → Battery optimization list
android.settings.DATA_USAGE_SETTINGS         → Background data per-app
android.settings.WIFI_SETTINGS               → Wi-Fi settings (has "switch to mobile" toggle)
```

### app.json config
```json
{
  "expo": {
    "name": "AutoSwitch",
    "slug": "mobile",
    "orientation": "portrait",
    "splash": { "backgroundColor": "#050B18" },
    "android": {},
    "newArchEnabled": true
  }
}
```
> `"android": {}` is intentionally minimal. Add `package`, `versionCode`, and `permissions` here before building the APK.

---

## 10. Building a Release APK

The app is **100% Expo Go compatible** for development. For a standalone APK the agent needs EAS Build.

### Step 1 — Add Android package name to `app.json`
```json
"android": {
  "package": "com.yourname.autoswitch",
  "versionCode": 1,
  "adaptiveIcon": {
    "foregroundImage": "./assets/images/icon.png",
    "backgroundColor": "#050B18"
  },
  "permissions": [
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.ACCESS_WIFI_STATE",
    "android.permission.CHANGE_NETWORK_STATE",
    "android.permission.INTERNET"
  ]
}
```

### Step 2 — Add EAS config (`eas.json` at project root)
```json
{
  "cli": { "version": ">= 14.0.0" },
  "build": {
    "preview": {
      "android": {
        "buildType": "apk"
      }
    },
    "production": {
      "android": {
        "buildType": "app-bundle"
      }
    }
  }
}
```

### Step 3 — Build the APK
```bash
# From artifacts/mobile directory
eas build --platform android --profile preview
```
This uploads the project to Expo's build servers and produces a downloadable `.apk` link (no local Android SDK needed on the build machine).

### Step 4 — Install on device
```bash
adb install autoswitch.apk
# or just download the .apk link on the phone and tap to install
```

---

## 11. What Is NOT Yet Built (potential next steps)

| Feature | Complexity | Notes |
|---|---|---|
| **Biometric / PIN lock for Settings** | Medium | `expo-local-authentication`, gate the Settings tab |
| **Live signal strength bars** | Medium | `@react-native-community/netinfo` cellular signal via `details.strength` |
| **SIM 2 selection** | Low | Currently hardcoded to SIM 1 in Alert message; wire up the SIM preference from context |
| **Custom SIM labels** | Low | Let user rename "SIM 1" → "Jio", "SIM 2" → "Airtel" |
| **Automatic data switch (root/ADB)** | Hard | Requires `MODIFY_PHONE_STATE` — only works on rooted devices or ADB |
| **Accessibility service** | Hard | A proper background foreground service using `expo-task-manager` + full native build |

---

## 12. Known Issues / Gotchas

1. **`expo-notifications` / `expo-background-fetch` / `expo-task-manager`** — these packages are in `package.json` from a previous build attempt but are **not imported anywhere**. If an agent adds imports for them, the app will crash with `Cannot read properties of undefined (reading 'DENIED')`. Leave them unimported until a proper native build.

2. **`@react-native-community/netinfo@12.0.1`** — Expo 54 expects version `11.4.1`. The app works fine at 12.0.1 but Metro shows a warning. Downgrade to `11.4.1` if the build complains.

3. **SSID is null on Android 10+** without `ACCESS_FINE_LOCATION` permission. The app handles this gracefully (shows "Wi-Fi" instead of the SSID name). Granting location permission will reveal the real SSID.

4. **Web preview always shows light theme** — the web Expo preview runs in a browser iframe and always returns `colorScheme: light`. On a real Android device in dark mode, the dark theme works correctly.

5. **Pulse rings** — both `PulseRing` components **must** be inside the fixed-size orb container (`<View style={{ width: orbSize, height: orbSize }}>`) so that `StyleSheet.absoluteFillObject` overlays them correctly and the `scale()` transform expands from the orb center. Moving them outside breaks the animation.

---

## 13. Quick Command Reference

```bash
# Run dev server (from workspace root)
pnpm --filter @workspace/mobile run dev

# Typecheck
pnpm --filter @workspace/mobile run typecheck

# Install a new package
cd artifacts/mobile && pnpm add <package-name>

# Access the running app
# → scan QR code in Expo Go on Android
# → or open the Replit preview pane
```

---

*Last updated: July 2026. Built with Expo SDK 54, React Native 0.81.5, TypeScript 5.9.*
