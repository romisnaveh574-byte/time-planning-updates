# Task 2 Report

## Result

Implemented persisted watch-status DAO and repository lifecycle operations.

## Changes

- Added status Flow/list queries, insert/update/delete, record status update, status usage count, and transactional record migration queries to `WatchlistDao`.
- Added `WatchlistRepository.watchStatuses`, `allWatchStatuses`, `ensureBuiltInStatuses`, `addWatchStatus`, `renameWatchStatus`, `reorderWatchStatuses`, and `deleteWatchStatus`.
- Added trimmed nonblank validation and case-insensitive duplicate-name protection.
- Allowed renaming/reordering `WATCHING`, while protecting it from deletion.
- Deleting a used status now requires a different existing destination and migrates records inside the same Room transaction.
- Reordering requires the complete current status set and writes contiguous sort orders.
- Added focused unit coverage for add/rename, duplicate validation, built-in seeding, reorder, protected deletion, unused deletion, and used-status migration.

## Verification

- `:app:compileDebugKotlin`: PASS.
- `:app:testDebugUnitTest --tests com.example.birthdaycountdown.data.WatchlistRepositoryTest`: BLOCKED in the local Gradle test runner during JUnit discovery with `ClassNotFoundException`; all existing test classes show the same initialization failure, so no test body executed.

## Notes

The repository constructor keeps Room as the production transaction provider and accepts an internal transaction runner seam for JVM tests that use a fake DAO.
