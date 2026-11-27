# Nox Store 🚀
### Modern Autonomous Android App Store with Background Updates

**Nox Store** (`com.nox.store`) is a Google Play Store-inspired application distribution platform built entirely with **Kotlin** and **Jetpack Compose**. It is engineered to host and distribute apps directly to users, featuring seamless unattended background updates on modern Android devices.

---

## ✨ Features & Capabilities

- **Play Store-Grade UI & UX**:
  - High-impact **Hero Showcase Banner** with dynamic backdrop gradients.
  - Category filtering pill chips (*Communication, Productivity, Personalization, Development, Tools*).
  - Instant live search bar with filter suggestions and profile badge.
  - Multi-tab bottom navigation with live badge counts for available updates.
- **Android 12+ Unattended Background Updates**:
  - Leverages Android's native `PackageInstaller` session API with `USER_ACTION_NOT_REQUIRED`.
  - Once an app is initially installed from Nox Store, future updates are applied **silently in the background without user intervention**.
  - `UPDATE_PACKAGES_WITHOUT_USER_ACTION` support for Android 14+ ownership protection.
- **Background Synchronization Engine**:
  - Powered by Android `WorkManager` (`AutoUpdateWorker`).
  - Periodically checks for updates (customizable interval, default 12h).
  - Network constraints (e.g. Wi-Fi only mode to save mobile data).
  - Posts native notifications for pending updates or completed background installations.
- **Streaming APK Downloader**:
  - Real-time download speed calculation (MB/s).
  - Smooth animated linear and circular progress indicators.
  - Safe caching in app-scoped directory with automatic installation trigger.
- **Dynamic Catalog Publishing**:
  - Easy app publishing via a remote `apps.json` endpoint.
  - Update version codes, changelogs, APK URLs, and release notes without updating the store client.

---

## 🏗️ Architecture & Stack

- **Framework**: Jetpack Compose (BOM 2024.10.01, Material 3)
- **Language**: Kotlin 2.0.21
- **Async & Reactive**: Kotlin Coroutines & StateFlow
- **Background Work**: AndroidX WorkManager 2.10.0
- **Image Loading**: Coil Compose 2.7.0
- **Networking**: OkHttp 4.12.0
- **Target SDK**: Android 15 (API 35) | **Min SDK**: Android 8.0 (API 26)

---

## 📦 How to Publish an App

To publish an app or a new update to Nox Store, simply add or update an entry in `apps.json`:

```json
{
  "id": "my_new_app",
  "packageName": "com.example.app",
  "name": "My App",
  "developer": "Nox Studios",
  "category": "Tools",
  "summary": "Short one-liner summary",
  "description": "Full description of what the app does.",
  "iconUrl": "https://url-to-app-icon.png",
  "bannerUrl": "https://url-to-banner.png",
  "screenshots": ["https://url-to-screenshot1.png"],
  "latestVersionCode": 1,
  "latestVersionName": "1.0.0",
  "downloadUrl": "https://github.com/shahin0964/repo/releases/download/v1.0.0/app-latest.apk",
  "sizeBytes": 15000000,
  "rating": 4.9,
  "reviewsCount": "1.2K",
  "downloadsCount": "5K+",
  "changelog": [
    "Initial release"
  ],
  "minSdk": 26,
  "targetSdk": 35,
  "isFeatured": true
}
```

Whenever `latestVersionCode` is greater than the user's installed version, Nox Store will automatically notify the user or perform a silent background update!
