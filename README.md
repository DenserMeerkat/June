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
        <img src="https://img.shields.io/github/v/release/DenserMeerkat/June?include_prereleases&logo=github&style=for-the-badge&label=Latest%20Release" alt="Latest Release">
    </a>
    <a href="https://github.com/DenserMeerkat/June/releases">
        <img src="https://img.shields.io/github/downloads/DenserMeerkat/June/total?logo=github&style=for-the-badge" alt="Total Downloads">
    </a>
</p>

<p align="center">
  <img src="assets/screenshots/home1.png" alt="Screenshot 1" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/editor1.png" alt="Screenshot 2" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/search2.png" alt="Screenshot 3" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/settings1.png" alt="Screenshot 4" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/editor2.png" alt="Screenshot 5" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/editor3.png" alt="Screenshot 6" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/editor4.png" alt="Screenshot 7" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/editor5.png" alt="Screenshot 8" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/timeline1.png" alt="Screenshot 9" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/timeline3.png" alt="Screenshot 10" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/timeline5.png" alt="Screenshot 11" width="200" style="border-radius:24px;"/>
  <img src="assets/screenshots/timeline8.png" alt="Screenshot 12" width="200" style="border-radius:24px;"/>
</p>

## Core Features

June is designed to be more than just text, it's a multimedia capsule of your life.

### Capture Every Detail

- **Multimedia Capsules:** Go beyond words by attaching **photos**, **songs** and **maps** to any entry.
- **Soundtrack Support:** Paste a link from Spotify or Apple Music, and June automatically fetches the cover art and details.
- **Mood Tracking:** Tag entries with emojis to log your emotional journey over time.

### Relive Your History

- **Fluid Timeline:** Navigate your past via a smooth month-strip or browse the dedicated **Media Gallery**.
- **Visual Habits:** Keep your momentum going with calendar streaks and writing indicators.
- **Smart Search:** Instantly locate memories by content, date, tags, or attached media.

### Secure & Styled

- **Biometric Vault:** Protect your private thoughts with Fingerprint or Face Unlock.
- **Expressive Theming:** Dynamic Wallpaper Colors or Custom Themes for a personalized look.
- **Total Ownership:** 100% offline architecture with full Backup & Restore capabilities.

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
- **Maps:** [OSMDroid](https://github.com/osmdroid/osmdroid) (OpenStreetMap)
- **Theming:** [MaterialKolor](https://github.com/jordond/MaterialKolor) (Dynamic Material You colors)
