# Privacy Policy for June

**Effective Date:** June 7, 2026

"June" is committed to protecting your privacy. This application is designed with a 100% offline-first architecture, giving you total data ownership.

### 1. Data Collection and Usage
* **Personal Data & Journal Entries:** All journal entries, text, mood tracking logs, and tags are stored locally on your device's secured database. We do not collect, store, or transmit your journal data to any external servers.
* **Media & Files:** Photos, videos, and audio clips attached to your entries remain entirely on your local device storage.
* **Precise Location:** June requests access to your device's location to allow you to stamp locations onto your journal entries. This data is handled entirely locally and is never shared with third parties or other users.

### 2. Third-Party Services & Optional Network Connections
* **In-App Purchases & Billing (Google Play only):** If you choose to purchase June Plus on the Google Play version, purchases and transaction receipts are processed securely by Google Play Billing and verified through RevenueCat. RevenueCat collects an anonymous, randomly generated App User ID and purchase history tokens to unlock and restore your purchased features across devices. Neither June nor RevenueCat ever collect, access, or store your payment details (such as credit card numbers), real identity, or journal data.
* **Map Providers:** The app requests vector map tiles to render map views and location markers. June supports CARTO and OpenMap out of the box without requiring API keys. If you configure optional API keys for Stadia Maps or MapTiler, map tile requests are sent directly from your device to those respective providers. Map rendering is processed locally on-device via MapLibre; your journal entries, notes, and personal location history are never shared with or logged by map tile providers.
* **Soundtrack Metadata:** When you link a streaming platform URL, the app fetches public music cover art metadata via open APIs.
* **Cloud Sync (WebDAV):** If you choose to enable Cloud Sync, your data is transferred using WebDAV directly to your own designated private cloud provider (e.g., Nextcloud). We do not host or have access to this sync pipeline.
* **Cloud Sync (Google Drive - Google Play only):** If you choose to enable Google Drive sync, the app connects directly to your Google Account. Your data is synced privately to a secure, sandboxed app-data folder on your Google Drive. June cannot read, write, or view any other files in your Google Drive. Your account name, email, and avatar are processed strictly on-device to show your link status. We do not host or have access to this sync pipeline.
* **Typography (Google Fonts):** The app supports custom font choices. If dynamic font selection is enabled, font family assets are retrieved dynamically through Google's downloadable fonts service.

### 3. Security & Device Permissions
* **App Lock & Private Vault:** June offers optional locking mechanisms to secure the entire app or an isolated Private Vault. You can lock entries using standard Android device Biometrics or a custom PIN, Password, or Pattern. All custom lock credentials and security recovery questions are salted, hashed, and stored strictly on-device. They are never sent to external servers.
* **Screen Capture & Recents Protection:** When enabled, June marks the window as secure (FLAG_SECURE) to prevent unauthorized screenshots and hide app previews from the Android recents task switcher.
* **Notifications & Reminders:** June requests permission to post notifications and schedule exact alarms (including rescheduling them when your device restarts) to deliver journal writing reminders at your preferred times. This process runs entirely on-device and does not transmit any timing data or logs to external servers.

### 4. Changes to This Policy
We may update our Privacy Policy from time to time. Any changes will be updated directly within this public document.

### 5. Contact Us
If you have any questions about this privacy policy, you can contact the developer via our GitHub repository open issues.