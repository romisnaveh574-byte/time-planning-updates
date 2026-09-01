# Watch Status Management and Image Regeneration Design

## Goal

Make watch statuses configurable from the existing category manager and add a completed-image regeneration action that preserves the original result.

## Scope

- Add a "内容分类" and "观看状态" switch inside the watchlist category manager.
- Allow watch statuses to be created, renamed, deleted, and reordered.
- Keep the system "正在追" status non-deletable because it drives the home summary and the default status for new watch records.
- Require a destination status when deleting a status that still owns records.
- Preserve existing watch records, imports, exports, home summaries, filters, and status tones.
- Add "再次生成" to completed AI image records.
- Regeneration creates a new task with the original prompt, size, quality, reference image, and current image endpoint configuration while preserving the old result.

## Data Model

### Watch Statuses

Add `WatchStatusEntity` backed by a `watch_statuses` table:

- `id: String`: stable identifier. Built-in IDs keep the existing enum names such as `WATCHING` and `COMPLETED`; custom IDs use UUID values.
- `name: String`: user-visible label.
- `systemType: String?`: identifies built-in semantics. `WATCHING` is used by the home summary and as the default status. Other built-ins retain their visual tones.
- `sortOrder: Int`: user-defined display order.

`WatchRecordEntity` stores the status ID in its existing `status` TEXT column. The Kotlin property becomes a string status ID rather than a hard-coded enum, avoiding a destructive table rebuild.

### Database Migration

Add Room migration `16 -> 17`:

1. Create `watch_statuses`.
2. Insert the five current statuses in their current order.
3. Leave every existing `watch_records.status` value unchanged because those values match the built-in status IDs.

No existing watch record is deleted or rewritten.

## Repository Rules

- Ensure the built-in statuses exist after startup and import.
- Reject blank or duplicate status names.
- New custom statuses are appended to the end.
- `WATCHING` may be renamed and reordered but cannot be deleted.
- Deleting an unused status removes it directly.
- Deleting a used status requires a different destination status and moves all affected records in the same database transaction.
- Status sorting updates only `watch_statuses.sortOrder`.
- Record sorting remains scoped to the currently visible status and content category.

## User Interface

### Category Manager

The existing top-right "分类管理" page gains two tabs:

- "内容分类": retains the current content-category management behavior.
- "观看状态": lists statuses in display order with record counts and controls for reorder, rename, and delete.

The status page has an add action. The delete button is disabled for the system `WATCHING` status. Deleting a used status opens a destination-status selector before confirmation.

### Watchlist and Editor

- Status filters use the database status order and names.
- Creating a record from a selected status preserves that status.
- Editing a record uses the current status list.
- If a status name changes, every linked record immediately shows the new name.
- The home summary continues to count records linked to the system `WATCHING` status even if its visible name changes.

## Backup Compatibility

Increase the backup format to version 2 and include `watchStatuses`.

- Version 2 exports status definitions and status IDs used by records.
- Version 1 remains readable. Its enum status values map to the matching built-in status IDs.
- Import creates or matches statuses before importing records.
- Duplicate status names are matched case-insensitively.
- Records whose status cannot be resolved fall back to the system `WATCHING` status.

## AI Image Regeneration

Completed image cards gain a "再次生成" button.

On activation:

1. Verify that no image task is currently active in that conversation.
2. Verify the saved reference image still exists when the original task used one.
3. Insert a new pending image message in the same image conversation using the original prompt, requested size, quality, and reference-image path.
4. Start `AiImageGenerationService` for the new message.
5. Keep the original message and generated image unchanged.

The image endpoint configuration is read by the service when the new task starts, so regeneration uses the currently selected image relay configuration.

If the reference image is missing, no task is created and the UI displays "原参考图已不存在，请重新上传参考图".

Failed-task retry keeps its current behavior. Completed-task regeneration always creates a new message so successful history is never overwritten.

## Testing

- Room migration creates all built-in statuses and preserves existing record status IDs.
- Repository tests cover add, duplicate-name rejection, protected-status deletion, record migration on delete, and reorder.
- Backup tests cover version 2 round trips and version 1 compatibility.
- Watchlist rule tests cover system status lookup, renamed `WATCHING`, and custom statuses.
- AI history tests verify completed regeneration copies all generation inputs into a new pending record and leaves the original untouched.
- UI compilation and Release build verify both management tabs and the new completed-image action.
- Run all JVM tests through the ASCII `T:\` workspace path, then run `assembleRelease`.

## Files Expected to Change

- `data/WatchRecordEntity.kt`
- `data/WatchStatusEntity.kt` (new)
- `data/WatchlistDao.kt`
- `data/WatchlistRepository.kt`
- `data/AppDatabase.kt`
- `data/BackupCodec.kt`
- `data/AiHistory.kt`
- `ui/WatchlistViewModel.kt`
- `ui/WatchlistScreens.kt`
- `ui/Screens.kt`
- `ui/AiScreens.kt`
- Focused unit and migration tests under `app/src/test` and `app/src/androidTest`

## Out of Scope

- Custom colors or icons for user-created watch statuses.
- Deleting the system `WATCHING` status.
- Editing the prompt or generation settings inside the completed-card regeneration action.
- Changing AI chat retry behavior.
