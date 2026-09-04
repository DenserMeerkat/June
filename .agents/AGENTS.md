# June Developer Rules: Versioning & Migration Checklist

Any developer or AI agent modifying database schemas, sync manifests, or export/import models in this repository must follow this checklist.

---

## 1. Database Schema Changes
* **File**: [JournalDatabase.kt](core/src/main/java/com/denser/june/core/data/database/journal/JournalDatabase.kt) -> `VERSION`
* **Action**: If you modify any Room entity class (e.g., adding, removing, or changing database fields):
  1. Increment `JournalDatabase.VERSION` by 1.
  2. Implement a new Room `Migration` in [DatabaseMigrations.kt](core/src/main/java/com/denser/june/core/data/database/DatabaseMigrations.kt).
  3. Register the new migration in [DatabaseFactory.kt](core/src/main/java/com/denser/june/core/data/database/DatabaseFactory.kt).

---

## 2. Programmatic Startup Repairs (Data-Repair Migrations)
* **File**: [SyncManager.kt](core/src/main/java/com/denser/june/core/domain/sync/SyncManager.kt) -> `CURRENT_DATA_REPAIR_VERSION`
* **Action**: If you need to fix existing corrupted data (like incorrect paths or fields) programmatically on app launch:
  1. Increment `CURRENT_DATA_REPAIR_VERSION` by 1.
  2. Implement the repair task inside `SyncManager.repairDoubleConcatenatedImages()` (or add a new specialized function).
  3. Ensure it writes the new version to `syncPrefs.setLastCompletedDataRepairVersion(CURRENT_DATA_REPAIR_VERSION)` once it completes successfully.

---

## 3. Sync Manifest & Cloud File Layout Changes
* **File**: [CloudProvider.kt](core/src/main/java/com/denser/june/core/domain/sync/CloudProvider.kt) -> `SyncManifest.CURRENT_SCHEMA_VERSION`
* **Action**: If you modify how journals, media, or sync metadata are structured remotely in the cloud (WebDAV/Google Drive):
  1. Increment `SyncManifest.CURRENT_SCHEMA_VERSION` in `SyncManifest.companion object`.
  2. Update references in `SyncManager.kt` and `ExportImpl.kt` (which consume `SyncManifest.CURRENT_SCHEMA_VERSION`).
  3. Ensure providers and `SyncManager` retain backward-compatible fallback reading logic for older `schemaVersion` configurations to avoid breaking existing users' remote states.
* **Manifest Schema Version History**:
  - `schemaVersion = 1`: Initial release (basic file listing).
  - `schemaVersion = 2`: Added `deletedIds` array for cloud deletion tracking.
  - `schemaVersion = 3` (Current): Added `journalMetadata` (`rev` counter + SHA-256 `contentHash`) and `mediaMetadata` (`size` + SHA-256 file `hash`).

---

## 4. Local Backup (ZIP) Format Changes
* **Files**: [ExportImpl.kt](core/src/main/java/com/denser/june/core/data/backup/ExportImpl.kt) & [RestoreImpl.kt](core/src/main/java/com/denser/june/core/data/backup/RestoreImpl.kt)
* **Action**: If you update the backup ZIP contents or formatting:
  1. If introducing a new format, define a new manifest file marker (like `manifest.json`).
  2. Update `RestoreImpl.kt` to run conditional parsing blocks (e.g. format detection) to guarantee older backup files (like legacy ZIPs containing `journal_data.json` at schema version 3) remain fully restorable.

---

## 5. App Versioning & Release Codes (`versionCode`)
* **File**: [app/build.gradle.kts](app/build.gradle.kts)
* **Rule**: `versionCode` must strictly increase on every release. Never decrement below `100000`. Declare `val appVersionCode = <INT>` and `val appVersionName = "<STR>"` as literal constants on single lines.
* **Format**: `M mm pp b` (Major `M`, 2-digit Minor `mm`, 2-digit Patch `pp`, 1-digit Build `b`)
  * `1.0.0` -> `100000`
  * `1.0.1` -> `100010`
  * `1.0.2` -> `100020`
  * `1.0.10` -> `100100`
  * `1.1.0` -> `101000`
  * `1.10.0` -> `110000`
  * `2.0.0` -> `200000`
