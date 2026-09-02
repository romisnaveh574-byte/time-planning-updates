# Task 1 Report: Persisted Watch Statuses

## Delivered

- Added `WatchStatusEntity` with stable `id`, display `name`, optional `systemType`, and `sortOrder`.
- Added the five built-in statuses in the existing enum order and kept `WATCHING` as `SYSTEM_WATCHING_ID`.
- Changed `WatchRecordEntity.status` to persist a stable string ID. A deprecated constructor accepts the legacy `WatchStatus` enum so existing callers continue to compile until the dynamic-status UI work replaces them.
- Bumped Room from version 16 to 17. `MIGRATION_16_17` creates `watch_statuses`, seeds the built-ins with `INSERT OR IGNORE`, and intentionally leaves `watch_records` unchanged so all existing enum-name values remain valid IDs.
- Added an Android instrumentation migration test which creates a complete version-16-shaped database, then opens it through `AppDatabase.create()` (the production migration registration path) and verifies seeded statuses, preserved record values, and the migrated table schema after Room's open-time validation.
- Made minimal compile-preserving string-ID conversions in backup, domain, and UI call sites. These are compatibility adapters only; later tasks own dynamic status selection, labels, and filters.

## Verification

Passed:

```powershell
$env:GRADLE_USER_HOME='C:\gradle-ascii'
.\gradlew.bat :app:testDebugUnitTest --tests '*WatchlistRulesTest*' --tests '*BackupCodecTest*' --no-daemon

.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
```

`adb devices -l` reported no connected devices and the local emulator list is empty. The instrumentation migration test therefore compiles but has not run on a device/emulator. An attempted filtered `connectedDebugAndroidTest` invocation also established that this Gradle task does not support `--tests`; the unfiltered invocation did not produce an instrumented test result because no device was available.

The repository does not export a Room v16 schema asset (`exportSchema = false`), so the test provisions the v16 tables directly and uses the real Room 17 open/upgrade path. A future hardening step can enable schema export and switch to `MigrationTestHelper` once the v16 identity hash/schema artifact is checked in.

## Residual Risk

Room migration behavior has not yet been exercised on an Android runtime. Run `:app:connectedDebugAndroidTest` with a connected device or configured AVD before release.

## Commit

`63c99ef feat: persist configurable watch statuses`

Follow-up fix commit: recorded below after commit.
