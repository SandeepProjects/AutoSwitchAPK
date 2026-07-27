# AutoSwitchAPK 📱⚡

An intelligent Android application that monitors Wi-Fi connectivity in real-time and **automatically switches mobile data to a user-selected SIM card (SIM 1 / SIM 2)** whenever the Wi-Fi network goes off or disconnects.

---

## 🌟 Key Features

- 📶 **Real-Time Wi-Fi Monitoring**: Uses Android `ConnectivityManager.NetworkCallback` to instantly detect Wi-Fi connection loss (`onLost`).
- 🎛️ **Dual SIM Selection**: Detects all active SIM cards (`SubscriptionManager`) and lets you choose whether to default to **SIM 1** or **SIM 2** when Wi-Fi drops.
- 🔄 **Automated Data SIM Switch**:
  - Direct System API & Reflection (`SubscriptionManager.setDefaultDataSubId`).
  - Native Root / Custom ROM Shell command fallback (`cmd phone data set-data-subscription <subId>`).
  - Seamless system settings prompt fallback if unrooted.
- 🚀 **Foreground Service & Auto-Start**: Runs in the background with a persistent status notification and automatically restarts on device boot (`BOOT_COMPLETED`).
- 📊 **Live Activity Logs**: In-app terminal style event logger displaying Wi-Fi status transitions and SIM switch triggers.

---

## 📸 Screenshots & Architecture

```
                       ┌─────────────────────────┐
                       │   Wi-Fi Connection      │
                       └────────────┬────────────┘
                                    │ (Disconnect / Lost)
                                    ▼
                       ┌─────────────────────────┐
                       │   WifiMonitorService    │
                       │   (Foreground Service)  │
                       └────────────┬────────────┘
                                    │
                                    ▼
                       ┌─────────────────────────┐
                       │    SimSwitchManager     │
                       └──────┬───────────┬──────┘
                              │           │
           (Root / System API)│           │(Fallback Prompt)
                              ▼           ▼
                   ┌──────────────┐   ┌──────────────┐
                   │ Active Data  │   │ Wireless &   │
                   │ SIM Switched │   │ SIM Settings │
                   └──────────────┘   └──────────────┘
```

---

## 🛠️ Requirements & Permissions

### Permissions Required
- `ACCESS_NETWORK_STATE` & `CHANGE_NETWORK_STATE`: For detecting Wi-Fi state changes.
- `READ_PHONE_STATE`: To detect installed SIM card subscription IDs and carrier names.
- `FOREGROUND_SERVICE`: Keeps the network monitor alive in the background.
- `RECEIVE_BOOT_COMPLETED`: Automatically resumes monitoring after reboot.
- `POST_NOTIFICATIONS` (Android 13+): Displays status notification while monitoring.

---

## 💻 Tech Stack & Build System

- **Language**: Kotlin 1.9
- **UI Framework**: Android Material 3 Design
- **Minimum SDK**: API 26 (Android 8.0)
- **Target SDK**: API 34 (Android 14)
- **Build Tool**: Gradle (Kotlin DSL)

---

## 🔨 How to Build

1. Clone this repository:
   ```bash
   git clone https://github.com/SandeepProjects/AutoSwitchAPK.git
   cd AutoSwitchAPK
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and build the project (`Build > Make Project` or `./gradlew assembleDebug`).
4. Install the APK on your dual-SIM Android device (`./gradlew installDebug` or via ADB).

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more details.
