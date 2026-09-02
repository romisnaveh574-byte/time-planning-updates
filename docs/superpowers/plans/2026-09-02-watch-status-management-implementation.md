# Watch Status Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Make watch statuses configurable from 分类管理 while preserving existing records, filters, backups, and home summaries.

**Architecture:** Replace enum-only status handling with stable string IDs backed by a Room watch_statuses table. Keep built-in IDs and semantics, resolve labels/order from the database, and perform status deletion plus record migration transactionally. Extend the existing category manager with a content/status switch and keep all watchlist screens consuming repository-provided status definitions.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Kotlin coroutines/Flow, existing repository/ViewModel test stack.

**Spec:** docs/superpowers/specs/2026-09-01-watch-status-management-image-regeneration-design.md

## Global Constraints

- Keep built-in status IDs such as WATCHING and existing watch_records.status values unchanged.
- WATCHING may be renamed and reordered but cannot be deleted.
- Deleting a used status requires a different destination status and moves records in one transaction.
- Backup version 1 remains readable; version 2 includes watchStatuses.
- The home summary counts the system WATCHING ID, not its visible label.

---

### Task 1: Add the persisted watch-status model and migration

**Files:** Create data/WatchStatusEntity.kt; modify data/WatchRecordEntity.kt and data/AppDatabase.kt; test AppDatabaseMigrationTest.kt.

**Interfaces:** WatchStatusEntity(id: String, name: String, systemType: String?, sortOrder: Int); SYSTEM_WATCHING_ID = WATCHING; database version 17 with MIGRATION_16_17 creating watch_statuses and seeding five built-ins; WatchRecordEntity.status becomes String.

- [ ] Write a migration test creating a version-16 database with every legacy status and assert five seeded rows plus unchanged record status strings.
- [ ] Run ./gradlew :app:connectedDebugAndroidTest --tests '*AppDatabaseMigrationTest*' and verify failure before implementation.
- [ ] Implement entity, migration, seed data, and Room registration without rewriting watch_records.
- [ ] Re-run the migration test and verify pass.
- [ ] Commit with message feat: persist configurable watch statuses.

### Task 2: Add DAO and repository status-management operations

**Files:** modify data/WatchlistDao.kt and data/WatchlistRepository.kt; test WatchlistRepositoryTest.kt.

**Interfaces:** DAO provides status Flow, insert/update/delete, record-status update, and record count. Repository provides addWatchStatus, renameWatchStatus, reorderWatchStatuses, deleteWatchStatus, and ensureBuiltInStatuses.

- [ ] Add failing tests for blank/duplicate names, rename, reorder, protected WATCHING deletion, unused deletion, used deletion requiring destination, and transactional record migration.
- [ ] Run ./gradlew :app:testDebugUnitTest --tests '*WatchlistRepositoryTest*' and verify failure.
- [ ] Implement trimmed nonblank case-insensitive validation and transactional delete-plus-migrate; reject source=destination and deleting WATCHING.
- [ ] Re-run focused tests and verify pass.
- [ ] Commit with message feat: manage watch status lifecycle.

### Task 3: Update backup format and compatibility

**Files:** modify data/BackupCodec.kt; test BackupCodecTest.kt.

**Interfaces:** Version 2 serializes watchStatuses with id/name/systemType/sortOrder. Version 1 maps legacy enum values to built-ins; unknown IDs fall back to WATCHING.

- [ ] Add failing version-2 round-trip and version-1 compatibility tests.
- [ ] Run ./gradlew :app:testDebugUnitTest --tests '*BackupCodecTest*' and verify failure.
- [ ] Implement encoding/decoding and import statuses before records.
- [ ] Re-run focused tests and verify pass.
- [ ] Commit with message feat: backup configurable watch statuses.

### Task 4: Wire ViewModel, filters, editor, and home summary

**Files:** modify ui/WatchlistViewModel.kt, ui/WatchlistScreens.kt, ui/Screens.kt; test WatchlistViewModelTest.kt.

**Interfaces:** ViewModel exposes ordered statuses, mutation events, and deletion destinations. Filters/editor bind status IDs and labels from Flow. Home summary counts status ID WATCHING.

- [ ] Add failing tests for renamed WATCHING and custom status filters.
- [ ] Run focused tests and verify failure.
- [ ] Replace enum-derived UI state with repository status state while preserving content categories.
- [ ] Re-run focused tests and verify pass.
- [ ] Commit with message feat: bind watchlist UI to configurable statuses.

### Task 5: Build the two-tab category manager and deletion flow

**Files:** modify ui/WatchlistScreens.kt and ui/Screens.kt; test CategoryManagerSemanticsTest.kt.

**Interfaces:** Category manager has 内容分类 and 观看状态 tabs. Status rows show counts and reorder/rename/delete. Used-status deletion selects a destination and confirms; WATCHING delete disabled.

- [ ] Add Compose semantics tests for tabs, protected delete, and destination selector.
- [ ] Run focused UI tests and verify failure.
- [ ] Implement tabs and dialogs using existing components and wording.
- [ ] Run JVM and connected Compose tests and verify pass.
- [ ] Commit with message feat: add watch status category manager.

### Task 6: Full verification

- [ ] Run ./gradlew :app:testDebugUnitTest.
- [ ] Run ./gradlew :app:connectedDebugAndroidTest.
- [ ] Run ./gradlew :app:assembleRelease.
- [ ] Run git diff --check and record outputs before publishing.

