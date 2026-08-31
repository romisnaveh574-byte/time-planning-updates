# Time Planning Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a coherent, system-style release of Time Planning Bureau with reliable record management, backup, update progress, and accessible visual polish.

**Architecture:** Keep Room as the record source of truth and SharedPreferences for display/navigation preferences. Add pure codecs and transfer helpers for testable backup and update progress behavior; Compose screens own only navigation and transient UI state.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room, Kotlin coroutines, Android Storage Access Framework, GitHub Releases.

**Spec:** `docs/superpowers/specs/2026-08-27-time-planning-navigation-design.md`

## Global Constraints

- Preserve the current application id and release signing key so an update retains user data.
- Never use Room destructive migration.
- Backup import merges records and does not delete existing records.
- Release asset name remains `time-planning.apk`.

---

### Task 1: Stabilize editing and record management

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/EditScreen.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`

- [ ] Keep saving permanently reachable with a validated fixed bottom action.
- [ ] Add an undoable delete state, and retain pinned status when records are edited.
- [ ] Give long-press sorting an explicit visual state and save only on drag completion.
- [ ] Compile `:app:compileDebugKotlin`.

### Task 2: Build category-based settings

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/SettingsScreens.kt`

- [ ] Replace segmented settings with category rows.
- [ ] Separate display/format, bottom navigation, data backup, update/about, and appearance controls.
- [ ] Compile `:app:compileDebugKotlin`.

### Task 3: Add backup codec and import/export UI

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/data/BackupCodec.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/data/CountdownDao.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/data/CountdownRepository.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/SettingsScreens.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/data/BackupCodecTest.kt`

- [ ] Write a failing codec round-trip test for a lunar, pinned record.
- [ ] Implement versioned JSON encode/decode with strict required-field validation.
- [ ] Export through `ACTION_CREATE_DOCUMENT`; import through `ACTION_OPEN_DOCUMENT` and merge records with new ids.
- [ ] Compile production and test Kotlin sources.

### Task 4: Expose update download progress

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/update/DownloadProgress.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/update/UpdateDownloader.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/SettingsScreens.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/update/DownloadProgressTest.kt`

- [ ] Write a failing progress-copy test that observes final received bytes and completion.
- [ ] Implement a reusable stream copy helper and use it from the downloader.
- [ ] Render checking, downloading, verifying, failed, and install states with an actual percentage when content length is present.
- [ ] Compile production and test Kotlin sources.

### Task 5: Apply restrained system visual polish

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/SettingsScreens.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/UiComponents.kt`
- Modify: app theme files where present

- [ ] Add a translucent surface treatment to navigation, top bars, dialogs, and category rows while retaining opaque/gradient record cards.
- [ ] Respect system dark mode and Material typography scaling; do not hard-code body text colors that break contrast.
- [ ] Compile release and inspect resulting APK metadata.

### Task 6: Publish one verified release

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] Increment the version once after all changes are complete.
- [ ] Assemble a signed release, copy it to the required asset name, verify certificate/version, then create a GitHub Release.
- [ ] Verify GitHub’s latest-release asset metadata before reporting the release.
