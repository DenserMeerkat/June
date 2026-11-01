# June

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" alt="App Icon" width="128"/>
</p>

<p align="center">
  <strong>An open-source alternative to Pixel Journal</strong><br>
  Built with Jetpack Compose and Material Design 3
</p>

<p align="center">
    <a href="https://github.com/DenserMeerkat/June/releases/latest">
        <img src="https://img.shields.io/github/v/release/DenserMeerkat/June?include_prereleases&logo=github&style=for-the-badge&color=red&label=Latest" alt="Release">
    </a>
    <a href="https://github.com/DenserMeerkat/June/releases">
        <img src="https://img.shields.io/github/downloads/DenserMeerkat/June/total?logo=github&style=for-the-badge" alt="Total Downloads">
    </a>
    <a href="https://github.com/DenserMeerkat/June/releases">
        <img src="https://img.shields.io/github/license/DenserMeerkat/June?style=for-the-badge&color=blue" alt="License">
    </a>
    <a href="https://hosted.weblate.org/engage/june/">
        <img src="https://img.shields.io/weblate/progress/june?logo=weblate&style=for-the-badge&color=2ecba9" alt="Translation status">
    </a>
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.denser.june.play">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="70" alt="Get it on Google Play">
  </a>
  <a href="https://github.com/DenserMeerkat/June/releases/latest">
    <img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" height="70" alt="Get it on GitHub">
  </a>
</p>

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="32%" style="border-radius:12px; margin: 1px;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="32%" style="border-radius:12px; margin: 1px;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="32%" style="border-radius:12px; margin: 1px;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="32%" style="border-radius:12px; margin: 1px;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="32%" style="border-radius:12px; margin: 1px;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="32%" style="border-radius:12px; margin: 1px;">
</p>

## Core Features

June is more than just text; it is a multimedia capsule of your life.

### Capture Every Detail

- **Multimedia Capsules:** Go beyond words by attaching **photos**, **videos**, and **precise locations** to any entry. Includes a full-screen media viewer with pinch-to-zoom, pan, EXIF metadata inspection, and built-in photo/video compression.
- **Smart Organization:** Intelligently categorize your entries using three distinct tag groups: **Spaces**, **People** (`@`), and **Topics** (`#`).
- **Soundtracks & Song Library:** Paste links from major streaming platforms (Spotify, Apple Music, etc.) to fetch album art and metadata, or import **local audio files** with built-in audio trimming and waveform scrubbing.
- **Mood Tracking:** Tag entries with emojis using the built-in emoji picker with search to log your emotional journey over time.
- **Rich Text & Markdown:** Style your entries with bold, italics, underline, highlight, blockquotes, checklists, and tag autocompletion. Full support for opening, importing, and exporting **Markdown** files.
- **Per-Journal Styling:** Customize fonts, theme accents, and card appearance on individual entries, or extract dynamic color palettes directly from attached media.

### Relive Your History

- **Unified Timeline:** Navigate your past through a Month View calendar. See your **media, songs, and locations** all in one place within a seamless flow.
- **Visual Habits:** Keep your momentum going with calendar **streaks** and writing indicators that visualize your consistency.
- **Journal Reminders:** Personalized notification manager with flexible timing and frequency to keep your journaling habit on track.
- **Recycle Bin:** Soft-delete entries with one-tap restore and permanent disposal options.
- **Smart Search & Filtering:** Instantly locate memories by searching through content and dates, or use advanced multi-tag filtering to retrieve specific entries.

### Secure & Styled

- **Private Vault & Advanced Lock:** Keep your most sensitive entries hidden in an isolated Vault. Protect the entire app or the Vault individually using **Biometrics, PIN, Password, or Pattern**, complete with security recovery questions, screenshot prevention, and recents switcher obfuscation.
- **Variable Fonts & Typography Engine:** Fine-tune bundled variable fonts (Google Sans Flex, Roboto Flex, Recursive, Shantell Sans) with real-time sliders for weight, optical size, and slant.
- **Flexible Map Providers & Theming:** Vector-based map rendering powered by **MapLibre** with out-of-the-box support for **CARTO** and **OpenMap** (free, no API key required), as well as **Stadia Maps** and **MapTiler** (custom API keys supported). Features Light, Dark, and Follow App modes, plus **Dynamic Map Accents** that dynamically tint vector map layers to match your Material You theme.
- **Expressive Theming:** Personalize your experience with **Dynamic Wallpaper Colors (Material You)**, custom palette selection, and monochrome mode.
- **Total Ownership:** 100% offline architecture with full Backup & Restore capabilities. Your data never leaves your device unless you choose to sync it.
- **Network Toggle:** Full control over your connectivity. Turn off all external network access if you prefer not to fetch song metadata or use online map services.
- **Cloud Sync:** Keep your journal in sync across devices using **WebDAV** (Nextcloud, ownCloud, etc.) or **Google Drive** (Google Play build).

## Tech Stack

June is built with modern Android development practices, leveraging **Jetpack Compose** and **Kotlin**.

### Architecture & Core

- **Language:** [Kotlin](https://kotlinlang.org/) (100%)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles.
- **Dependency Injection:** [Koin](https://insert-koin.io/)
- **Navigation:** [Navigation 3](https://developer.android.com/guide/navigation/navigation-3)
- **Asynchronous:** Coroutines & Flows

### Data & Networking

- **Local Database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite abstraction)
- **Preferences:** [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Networking:** [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)

### UI & Media

- **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
- **Video/Audio:** [Media3 (ExoPlayer)](https://developer.android.com/media/media3)
- **Maps:** Vector-based map rendering with multiple style providers (CARTO, OpenMap, Stadia Maps, MapTiler) and dynamic theme colorization via [MapLibre](https://maplibre.org/)
- **Theming:** [MaterialKolor](https://github.com/jordond/MaterialKolor) (Dynamic Material You colors)

## Building Locally

To build June locally:
1. Clone the repository: `git clone https://github.com/DenserMeerkat/June.git`
2. Open the project in **Android Studio** (configured with **JDK 17**).
3. Select the `debug` build variant and run it on your device or emulator.

## Translations

June is localized by community contributors through [Hosted Weblate](https://hosted.weblate.org/engage/june/).

<p align="center">
  <a href="https://hosted.weblate.org/engage/june/">
    <img src="https://hosted.weblate.org/widgets/june/-/app/multi-auto.svg" alt="Translation status graph" />
  </a>
</p>

### Help Translate June
You can help translate June into your language or refine existing translations directly from your browser.

<p align="center">
  <a href="https://hosted.weblate.org/engage/june/">
    <img src="https://hosted.weblate.org/widget/june/287x66-white.png" alt="Contribute on Weblate" />
  </a>
</p>


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

[Privacy Policy](https://densermeerkat.github.io/June/PRIVACY)