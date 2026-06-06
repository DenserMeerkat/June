# June

<p align="center">
  <img src="assets/icon.png" alt="App Icon" width="128"/>
</p>

<p align="center">
  <strong>An open-source alternative to Pixel Journal</strong><br>
  Built with Jetpack Compose and Material Design 3
</p>

<p align="center">
    <a href="https://github.com/DenserMeerkat/June/releases/latest">
        <img src="https://img.shields.io/github/v/release/DenserMeerkat/June?include_prereleases&logo=github&style=for-the-badge&color=red&label=Latest%20Release" alt="Release">
    </a>
    <a href="https://github.com/DenserMeerkat/June/releases">
        <img src="https://img.shields.io/github/downloads/DenserMeerkat/June/total?logo=github&style=for-the-badge" alt="Total Downloads">
    </a>
    <a href="https://github.com/DenserMeerkat/June/releases">
        <img src="https://img.shields.io/github/license/DenserMeerkat/June?style=for-the-badge&color=blue" alt="License">
    </a>
</p>

<p align="center">
  <img src="assets/screenshots/home1.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/editor1.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/editor3.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/editor4.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/timeline1.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/timeline4.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/settings1.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/sync1.png" width="23%" style="border-radius:12px; margin: 1px;">
</p>

## Core Features

June is designed to be more than just text—it's a multimedia capsule of your life.

### Capture Every Detail

- **Multimedia Capsules:** Go beyond words by attaching **photos**, **videos**, and **precise locations** to any entry.
- **Smart Organization:** Intelligently categorize your entries using three distinct tag groups: **Spaces**, **People**, and **Topics**.
- **Soundtrack Support:** Paste a link from any major streaming platform (Spotify, Apple Music, etc.), and June automatically fetches the cover art and song details.
- **Mood Tracking:** Tag entries with emojis to log your emotional journey and personal growth over time.
- **Rich Text Editing:** Style your entries with full support for bold, italics, underline, highlight, and **tag autocompletion**.

### Relive Your History

- **Unified Timeline:** Navigate your past through a Month View calendar. See your **media, songs, and locations** all in one place within a seamless flow.
- **Visual Habits:** Keep your momentum going with calendar **streaks** and writing indicators that visualize your consistency.
- **Journal Reminders:** Personalized notification manager with flexible timing and frequency to keep your journaling habit on track.
- **Smart Search & Filtering:** Instantly locate memories by searching through content and dates, or use the advanced filter menu to combine multiple tags (such as `@John` and `#Travel`) to retrieve highly specific entries.

### Secure & Styled

- **Privacy Vault:** Secure your entries with Biometric Unlock or a custom PIN. Includes screen capture and recents menu protection.
- **Expressive Theming:** Enjoy a personalized look with **Dynamic Wallpaper Colors (Material You)** and custom **Font Selection**.
- **Total Ownership:** 100% offline architecture with full Backup & Restore capabilities—your data never leaves your device unless you choose to sync it.
- **Network Toggle:** Full control over your connectivity. Turn off all external network access if you prefer not to fetch song metadata or use online map services.
- **Cloud Sync:** Keep your journal in sync across devices using **WebDAV**. Maintain 100% privacy by using your own Nextcloud, ownCloud, or any WebDAV provider.

## Tech Stack

June is built with modern Android development practices, leveraging **Jetpack Compose** and **Kotlin**.

### Architecture & Core

- **Language:** [Kotlin](https://kotlinlang.org/) (100%)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles.
- **Dependency Injection:** [Koin](https://insert-koin.io/)
- **Navigation:** [Jetpack Navigation Compose](https://developer.android.com/guide/navigation/navigation-compose)
- **Asynchronous:** Coroutines & Flows

### Data & Networking

- **Local Database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite abstraction)
- **Preferences:** [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Networking:** [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)

### UI & Media

- **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
- **Video/Audio:** [Media3 (ExoPlayer)](https://developer.android.com/media/media3)
- **Maps:** Vector-based rendering with custom style providers support (Carto, MapTiler, Mapbox, Stadia, OSM) via [MapLibre](https://maplibre.org/)
- **Theming:** [MaterialKolor](https://github.com/jordond/MaterialKolor) (Dynamic Material You colors)

## Building Locally

To set up June on your local machine, follow these steps:

### 1. Prerequisites

- **Android Studio:** Latest stable version recommended.
- **JDK 17:** The project is configured to use Java 17 toolchain.

### 2. Clone the Repository

```bash
git clone https://github.com/DenserMeerkat/June.git
cd June
```

### 3. Build & Run

1. Open the project in Android Studio and let Gradle sync.
2. Select the debug build variant (default).
3. Click Run.

## Security & Verifiability

### Signing Certificate
SHA-256 fingerprint for the signing certificate used for GitHub releases:

```
E8:21:01:FC:C2:20:98:61:AF:DF:81:1C:03:12:F6:2A:A5:BA:8B:E3:10:E1:D2:74:C6:91:CE:6E:B5:D1:B7:BB
```

### Artifact Attestations
Official APK releases are cryptographically signed to prove they were built unmodified directly from our source code.

* **Via Browser:** Download APKs from [Releases](https://github.com/DenserMeerkat/June/releases) and verify the signed build logs on the [Attestations Dashboard](https://github.com/DenserMeerkat/June/attestations).
* **Via GitHub CLI (Developers):**
  ```bash
  gh attestation verify name-of-apk.apk --repo DenserMeerkat/June
  ```
