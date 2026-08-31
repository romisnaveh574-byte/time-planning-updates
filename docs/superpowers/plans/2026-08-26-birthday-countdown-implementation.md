# Birthday Countdown Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an offline-first Android app with editable birthday/anniversary cards, second-level timers, selectable global date format, and yearly local reminders.

**Architecture:** A single-activity Jetpack Compose app uses ViewModels and StateFlow for UI state, Room for local persistence, a pure domain calculator for all date math, and AlarmManager/BroadcastReceiver for yearly notifications. The app uses the device time zone and recalculates from the current clock whenever the foreground screen resumes.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose Material 3, Room, Lifecycle ViewModel/StateFlow, java.time, AndroidX test/JUnit.

---

## File Map

- Create `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`.
- Create `app/build.gradle.kts` with Compose, Room, lifecycle, and test dependencies.
- Create `app/src/main/AndroidManifest.xml` with notification permission and receiver declaration.
- Create `app/src/main/java/com/example/birthdaycountdown/MainActivity.kt` as the Compose entry point and navigation host.
- Create `app/src/main/java/com/example/birthdaycountdown/data/CountdownEntity.kt`, `CountdownDao.kt`, `AppDatabase.kt`, and `CountdownRepository.kt` for local data.
- Create `app/src/main/java/com/example/birthdaycountdown/domain/CountdownCalculator.kt` and `DateFormatter.kt` for deterministic time calculations.
- Create `app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt`, `HomeScreen.kt`, `EditScreen.kt`, and `SettingsScreen.kt` for state and UI.
- Create `app/src/main/java/com/example/birthdaycountdown/notifications/ReminderScheduler.kt` and `ReminderReceiver.kt` for yearly reminders.
- Create `app/src/test/java/com/example/birthdaycountdown/domain/CountdownCalculatorTest.kt` and `DateFormatterTest.kt`.
- Create `app/src/androidTest/java/com/example/birthdaycountdown/ui/HomeScreenTest.kt` for the primary UI flow.

### Task 1: Bootstrap the Android project

**Files:** `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add the Gradle project and Android application module**

  Configure a Kotlin Android application using a stable compile SDK available in the local Android SDK, minSdk 26 (required for `java.time`), Compose compiler, Material 3, Room KTX, lifecycle ViewModel Compose, and AndroidX test libraries.

- [ ] **Step 2: Configure the manifest**

  Declare `POST_NOTIFICATIONS` for Android 13+, `SCHEDULE_EXACT_ALARM` only if required by the selected target SDK, the launcher activity, and the non-exported reminder receiver.

- [ ] **Step 3: Verify the bootstrap**

  Run `./gradlew :app:assembleDebug` from the project root. Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

### Task 2: Implement persistence

**Files:** `data/CountdownEntity.kt`, `data/CountdownDao.kt`, `data/AppDatabase.kt`, `data/CountdownRepository.kt`

- [ ] **Step 1: Write repository tests for insert/update/delete and birthday uniqueness**

  Use an in-memory Room database to assert that a birthday row is replaced when a second birthday is saved, while multiple anniversary rows remain.

- [ ] **Step 2: Run the persistence tests and verify they fail before implementation**

  Run `./gradlew :app:testDebugUnitTest --tests '*Repository*'`. Expected: compilation or missing-implementation failures.

- [ ] **Step 3: Add the entity and DAO**

  Store `id`, `type` (`BIRTHDAY` or `ANNIVERSARY`), `name`, `dateTimeIso`, `reminderEnabled`, and `reminderMinutesBefore`. Expose `Flow<List<CountdownEntity>>`, insert/update, delete, and a query for the birthday row.

- [ ] **Step 4: Add Room database and repository**

  Build a singleton `Room.databaseBuilder`, convert between entity and domain model, and enforce the single-birthday rule in the repository before insert/update.

- [ ] **Step 5: Run the persistence tests**

  Run the same Gradle test command. Expected: PASS.

### Task 3: Implement deterministic date calculations and formatting

**Files:** `domain/CountdownCalculator.kt`, `domain/DateFormatter.kt`, `app/src/test/.../CountdownCalculatorTest.kt`, `DateFormatterTest.kt`

- [ ] **Step 1: Write failing tests for the agreed rules**

  Cover next birthday before/after this year's target, future anniversary countdown, elapsed anniversary plus next anniversary, leap-day birthday mapping to February 28 in non-leap years, second-level differences, and both display formats.

- [ ] **Step 2: Run the domain tests to verify failure**

  Run `./gradlew :app:testDebugUnitTest --tests '*CountdownCalculatorTest' --tests '*DateFormatterTest'`. Expected: FAIL because the calculator and formatter do not exist.

- [ ] **Step 3: Implement pure domain functions**

  Define `CountdownRecord`, `RecordType`, `TimerSnapshot`, and functions that accept an explicit `Clock`/`ZoneId` so tests never depend on wall-clock time. Use `java.time` calendar arithmetic; compute years/months/days with `Period`, then hours/minutes/seconds from the remaining `Duration`.

- [ ] **Step 4: Implement the two global formats**

  Format Chinese as `yyyy 年 M 月 d 日 HH 时 mm 分 ss 秒`; format numeric as `yyyy/MM/dd HH:mm:ss`. Keep zero padding only in the numeric format.

- [ ] **Step 5: Run domain tests**

  Expected: PASS for all calculator and formatter tests.

### Task 4: Build state and Compose screens

**Files:** `MainActivity.kt`, `ui/AppViewModel.kt`, `ui/HomeScreen.kt`, `ui/EditScreen.kt`, `ui/SettingsScreen.kt`

- [ ] **Step 1: Add ViewModel state and one-second refresh**

  Collect the repository flow, expose current `Instant` from a coroutine ticker while the app is started, and expose global `DateFormatPreference` from DataStore or a small Room settings table. Recalculate snapshots from `Instant.now()` instead of incrementing counters.

- [ ] **Step 2: Add the home screen**

  Render a scrollable list/grid of square-corner cards. Each card shows name, formatted date/time, and the required countdown or elapsed values. Add an add button, card click-to-edit, delete confirmation, and empty state.

- [ ] **Step 3: Add the edit screen**

  Provide type selection, name input, date picker, time picker with seconds, reminder toggle, and reminder lead-time selection. Validate non-empty name and valid date/time before saving.

- [ ] **Step 4: Add settings screen**

  Provide one global date-format selector and default reminder controls. Changing the format must update all cards immediately.

- [ ] **Step 5: Wire navigation and permissions**

  Use a simple sealed route state in the single activity. Request notification permission on first reminder enable for API 33+.

- [ ] **Step 6: Run the app and Compose tests**

  Run `./gradlew :app:testDebugUnitTest` and `./gradlew :app:connectedDebugAndroidTest` when an emulator/device is available. Expected: unit tests PASS; instrumentation tests PASS on a connected API 26+ device.

### Task 5: Add yearly local reminders

**Files:** `notifications/ReminderScheduler.kt`, `notifications/ReminderReceiver.kt`, `data/CountdownRepository.kt`, `AndroidManifest.xml`

- [ ] **Step 1: Write scheduler tests for lead time and next-year rescheduling**

  Assert that enabled records schedule the target minus the configured minutes, disabled records cancel their alarm, and a received alarm schedules the same record for the next anniversary.

- [ ] **Step 2: Implement scheduler and receiver**

  Use a stable request code derived from the record id, schedule the next valid annual occurrence, publish a notification with the record name and target date, and reschedule after delivery. Cancel alarms on edit/delete or reminder disable.

- [ ] **Step 3: Handle Android notification permission and exact-alarm fallback**

  Request `POST_NOTIFICATIONS` at runtime. If exact alarms are unavailable, use the nearest permitted trigger and keep the record usable without crashing.

- [ ] **Step 4: Run notification tests and manual verification**

  Run `./gradlew :app:testDebugUnitTest`. On an emulator, create a reminder one minute ahead, background the app, and verify the notification appears and the next yearly alarm remains scheduled.

### Task 6: Final verification and packaging

**Files:** no new production files; update `README.md` with build/run instructions.

- [ ] **Step 1: Run the complete verification suite**

  Run `./gradlew :app:testDebugUnitTest`, `./gradlew :app:lintDebug`, and `./gradlew :app:assembleDebug`. Expected: all tests pass, lint has no errors, and the debug APK is produced.

- [ ] **Step 2: Perform the acceptance checklist**

  Verify add/edit/delete, one birthday limit, multiple anniversaries, both date formats, second-level refresh, future anniversary transition, app restart persistence, reminder lead time, and notification permission behavior.

- [ ] **Step 3: Record the APK path and known limitations**

  Report the generated APK path and any emulator-only limitations. Do not claim notification timing was verified unless a device/emulator test was actually run.

## Execution Note

The source files and tests have been created in this workspace. Build, unit-test, lint, and emulator verification remain pending because this machine currently has no Java, Kotlin, Gradle, Android SDK, or ADB installation.
