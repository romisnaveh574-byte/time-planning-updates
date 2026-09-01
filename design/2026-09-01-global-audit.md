# 2026-09-01 Global Audit

Scope: static review of the current worktree. No connected Android device or emulator was used, so touch behavior, dark-mode contrast, notification delivery, and background execution still need device verification.

## P1 - Correctness and reliability

### 1. A successful async image request can be submitted a second time

Evidence: `OpenAiCompatibleClient.kt:144-155` posts to `/images/generations/async`, but treats every successful response without `task_id` as unsupported and posts again to `/images/generations`. `OpenAiCompatibleClient.kt:175-187` has the same behavior for `/images/edits/async`.

Impact: a relay that completes the async endpoint synchronously can receive two generation requests, yielding duplicate images and potentially two charges.

Recommendation: distinguish an unsupported async endpoint (HTTP error) from a completed image-shaped response. Return the completed response directly, and add response-shape regression tests for both normal generation and edit/reference-image generation.

### 2. Interrupted AI jobs can stay permanently in an active state

Evidence: both services return `START_REDELIVER_INTENT` (`AiImageGenerationService.kt:65`, `AiChatService.kt:58`) but only resume a database item whose status is exactly `PENDING` (`AiImageGenerationService.kt:40-41`, `AiChatService.kt:38-39`). During a request the image job is persisted as `SUBMITTING`, `QUEUED`, `PROCESSING`, or `SAVING` (`OpenAiCompatibleClient.kt:145-170`, `AiImageGenerationService.kt:49-51`).

Impact: after process/service termination during background work, the redelivered intent exits without recovery. The history card can remain at “正在生成”/“AI 正在思考” without a completion or retry path.

Recommendation: use durable worker/task recovery, or at minimum recover active states on startup and offer a clear retry/failure route. For upstream async tasks, persist task ID and resume polling rather than submit again.

### 3. Birthday and anniversary notifications are one-shot

Evidence: `ReminderScheduler.kt:14-25` schedules only one alarm. `ReminderReceiver.kt:12-22` displays a notification and never loads the record or schedules its next occurrence. The manifest also has no boot/time/time-zone receiver.

Impact: yearly reminders cease after their first delivered notification; device restart/time-zone changes can also drop scheduled reminder reliability.

Recommendation: after delivery, asynchronously load the record and schedule the next occurrence. Add restart/time-zone rescheduling if reminders are a core feature, and test birthday, anniversary, leap-day, and exact-alarm-permission fallback paths.

## P2 - User-facing gaps

### 4. AI failures discard the actual cause

Evidence: `AiImageGenerationService.kt:57-60` catches all errors and records only `FAILED`; `AiChatService.kt:50-53` does the same. The UI only renders generic messages such as “生成失败” or “回复失败” (`AiScreens.kt:195-197`). The message schema has no field for an error reason (`AiHistory.kt:29-43`).

Impact: users cannot distinguish a bad key/model, unsupported resolution, request timeout, relay rejection, or expired image URL. Support and retry decisions become guesswork.

Recommendation: persist a concise, sanitized error message, show it beside the failed record, and retain one-tap retry only for safe/recoverable image tasks.

### 5. Continuing a multimodal chat silently drops earlier image context

Evidence: historical chat turns are rebuilt as `ChatTurn(it.role, it.text)` (`AiChatService.kt:40-42`). Only the currently submitted user's image is converted to a data URL (`AiChatService.kt:47`).

Impact: a conversation that depends on an earlier uploaded image can get an answer as though the image had never been supplied, without warning.

Recommendation: persist/rebuild image parts for the selected chat-history format, or explicitly state before continuation that only the latest attachment is sent.

### 6. Backup wording implies a broader scope than the actual data

Evidence: export constructs `AppBackup` from countdown and watchlist data only (`AppViewModel.kt:99-100`), while the settings UI says the backup contains “当前全部记录” and lists record content/settings (`SettingsScreens.kt:161-163`). AI conversation, generated images, and reference images are excluded.

Impact: a user can reasonably expect AI history to survive a backup/restore and discover the loss only after migration.

Recommendation: either include optional AI-history/image backup with a privacy/size warning, or state clearly that AI chat and image records are not included.

### 7. Update UI uses browser links, but obsolete in-app APK install capability remains

Evidence: visible update action opens `ACTION_VIEW` to the GitHub Release page (`SettingsScreens.kt:200-238`). However, `REQUEST_INSTALL_PACKAGES` and FileProvider are still declared (`AndroidManifest.xml:5,36-43`) and `update/UpdateDownloader.kt`, `UpdateInstaller.kt`, and `ApkVerifier.kt` remain with no current call site outside the old downloader chain.

Impact: unnecessary package-install permission/trust surface and ongoing maintenance for a flow the product no longer exposes.

Recommendation: after a final call-site check, remove the unused downloader/installer/verifier, provider XML, FileProvider declaration, and install-packages permission.

### 8. AI home always tells configured users to configure a relay

Evidence: the prompt “请先在设置中配置 AI 中转站。” is rendered unconditionally (`AiScreens.kt:116-120`), regardless of whether chat/image endpoint settings are complete.

Impact: successful configuration still looks incomplete and reduces confidence in the feature.

Recommendation: derive this hint from each endpoint's actual configuration state; show “去设置” only when the selected feature lacks a base URL, key, or model.

## P3 - UI and interaction risks requiring device confirmation

### 9. Dark-mode color semantics are incomplete

Evidence: `AppTheme.kt:29-36` specifies only a subset of the dark color scheme, unlike the explicit light scheme (`AppTheme.kt:13-26`). The redesign uses white cards and bright purple-pink accent surfaces throughout.

Impact: Material defaults may make secondary text, outlines, chips, and selected navigation states inconsistent or insufficiently distinct in dark mode.

Recommendation: set explicit dark `onBackground`, `onSurface`, `onSurfaceVariant`, `outlineVariant`, and container colors, then verify screenshots and contrast on a physical device.

### 10. Dense controls need touch-target and narrow-screen validation

Evidence: bottom-navigation settings place six icon buttons in one row (`SettingsScreens.kt:291-297`), while the app supports four configurable bottom-nav entries (`Screens.kt:192-225`). Image options and watchlist controls also use dense chips and icon actions (`AiScreens.kt:254-265`, `WatchlistScreens.kt:148-178`).

Impact: on narrow devices, controls may fall below practical touch-target size or labels may crowd, especially after users rename navigation entries up to eight characters.

Recommendation: test 320dp/360dp/411dp widths with large system font, TalkBack focus, and both label/icon visibility combinations. Use wrapping grids or a scrollable selector where a row cannot preserve 44-48dp targets.

### 11. Reordering remains discoverability-sensitive

Evidence: watchlist sorting starts only through long-press drag gestures (`WatchlistScreens.kt:158-178`).

Impact: the gesture is hidden; users who do not discover long press will conclude sorting is unavailable, and filtered-list sorting needs real-device testing to ensure it meets expectations.

Recommendation: add a brief one-time hint or explicit reorder mode, then verify long press/drag with one category and filtered category lists on touch hardware.

## Test coverage gaps

Current tests cover small pure helpers and release-link construction, but there are no regression tests for async image response shapes, active-task recovery, reminder recurrence, AI error persistence, backup scope, or navigation/touch behavior. These should accompany P1/P2 fixes.
