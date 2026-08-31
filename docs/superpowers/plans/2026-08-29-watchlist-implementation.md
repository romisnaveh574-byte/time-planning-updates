# 追剧记录 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在“我的”页接入可分类、可排序、可快速增减集数的本地追剧记录功能。

**Architecture:** 保持倒计时表不变，新增分类和追剧记录两张 Room 表，并由独立的 `WatchlistRepository` 与 `WatchlistViewModel` 管理。界面从“我的”页进入独立追剧列表和分类管理页；备份编码扩展为可兼容旧版 JSON 的聚合格式。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、Room 2.6.1、Kotlin Coroutines、org.json、JUnit 4。

**Spec:** `docs/superpowers/specs/2026-08-29-watchlist-design.md`

## Global Constraints

- 数据库从版本 9 升至 10，原 `countdown_records` 不得改表或丢失数据。
- 默认分类固定为：电视剧、电影、动漫、短剧。
- 当前集数为非负整数；记录仅保存当前集数，不增加总集数或在线同步。
- 删除含记录的分类必须在同一数据库事务内先转移记录；最后一个分类不可删除。
- 备份必须继续读取现有 `formatVersion: 1` 的倒计时备份。
- 不发布 GitHub Release 或上传安装包。
- 本工程不是 Git 仓库，每个任务以测试、编译或 APK 构建作为检查点。

---

### Task 1: 追剧数据库与仓储

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/data/WatchCategoryEntity.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/data/WatchRecordEntity.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/data/WatchlistDao.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/data/WatchlistRepository.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/data/AppDatabase.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/data/WatchlistRepositoryTest.kt`

**Interfaces:**
- Produces `WatchCategoryEntity(id: Long, name: String, sortOrder: Int)` and `WatchRecordEntity(id: Long, title: String, categoryId: Long, currentEpisode: Int, sortOrder: Int)`.
- Produces `WatchlistRepository.categories`, `records`, `saveRecord`, `setEpisode`, `reorderRecords`, `saveCategory`, `deleteCategoryAndMoveRecords`.

- [x] **Step 1: Write failing entity validation tests**

```kotlin
@Test fun recordRejectsBlankTitle() {
    assertFailsWith<IllegalArgumentException> {
        WatchRecordEntity(title = " ", categoryId = 1, currentEpisode = 0)
    }
}

@Test fun recordRejectsNegativeEpisode() {
    assertFailsWith<IllegalArgumentException> {
        WatchRecordEntity(title = "海贼王", categoryId = 1, currentEpisode = -1)
    }
}
```

- [x] **Step 2: Run the focused test to verify failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.data.WatchlistRepositoryTest`

Expected: compilation failure because the watch entities do not yet exist.

- [x] **Step 3: Add entities, DAO, repository, and Room 9-to-10 migration**

```kotlin
@Entity(tableName = "watch_categories")
data class WatchCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = Int.MAX_VALUE
) { init { require(name.trim().isNotEmpty()) } }

@Entity(
    tableName = "watch_records",
    foreignKeys = [ForeignKey(
        entity = WatchCategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.NO_ACTION
    )],
    indices = [Index("categoryId")]
)
data class WatchRecordEntity(/* fields above */)
```

The `MIGRATION_9_10` must create both tables, create the category index, and insert the four default categories with deterministic sort orders. `deleteCategoryAndMoveRecords` must run in `database.withTransaction { ... }`, reject deleting the final category, and update records before deleting the source category.

- [x] **Step 4: Run focused tests and compile**

Run: `./gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.data.WatchlistRepositoryTest`

Expected: entity validation tests pass when the local Gradle worker can start. Then run `./gradlew.bat :app:compileDebugKotlin`; Kotlin compilation must pass even if the existing worker-process limitation prevents unit-test execution.

- [x] **Step 5: Checkpoint**

Verify that `MainActivity` will add `AppDatabase.MIGRATION_9_10` before any Room open, and do not create a commit because this workspace has no Git repository.

### Task 2: 追剧备份兼容

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/data/BackupCodec.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/data/CountdownRepository.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/data/BackupCodecTest.kt`

**Interfaces:**
- Consumes the entities and repository created in Task 1.
- Produces `AppBackup(countdownRecords, watchCategories, watchRecords)` and `BackupCodec.encode(AppBackup)` / `BackupCodec.decode(content): AppBackup`.

- [x] **Step 1: Write failing compatibility tests**

```kotlin
@Test fun oldCountdownOnlyBackupDecodesWithEmptyWatchData() {
    val backup = BackupCodec.decode("""{\"formatVersion\":1,\"records\":[]}""")
    assertTrue(backup.watchCategories.isEmpty())
    assertTrue(backup.watchRecords.isEmpty())
}

@Test fun watchBackupRoundTrips() {
    val input = AppBackup(emptyList(), listOf(WatchCategoryEntity(7, "动漫", 0)), listOf(WatchRecordEntity(8, "葬送的芙莉莲", 7, 12, 0)))
    assertEquals(input, BackupCodec.decode(BackupCodec.encode(input)))
}
```

- [x] **Step 2: Run the focused test to verify failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.data.BackupCodecTest`

Expected: test compilation fails because `AppBackup` and the new codec overload do not exist.

- [x] **Step 3: Extend backup codec and import flow minimally**

```kotlin
data class AppBackup(
    val countdownRecords: List<CountdownEntity>,
    val watchCategories: List<WatchCategoryEntity>,
    val watchRecords: List<WatchRecordEntity>
)
```

Keep the `formatVersion` at 1. `decode` must use optional `watchCategories` and `watchRecords` arrays, defaulting both to empty lists when absent. During import, insert categories first, map exported category IDs to inserted local IDs, then insert records with mapped category IDs. Existing countdown import remains additive.

- [x] **Step 4: Run backup tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.data.BackupCodecTest`

Expected: all codec cases pass when the Gradle worker starts; otherwise `:app:compileDebugUnitTestKotlin` must pass.

- [x] **Step 5: Checkpoint**

Verify exported data includes all three arrays and decoding an existing countdown-only backup produces no watch data.

### Task 3: Watchlist state and Compose screens

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/ui/WatchlistViewModel.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/ui/WatchlistScreens.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/domain/WatchlistRules.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/MainActivity.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/domain/WatchlistRulesTest.kt`

**Interfaces:**
- Consumes `WatchlistRepository` from Task 1.
- Produces `adjustedEpisode(currentEpisode: Int, delta: Int): Int`, `WatchlistScreen`, `WatchRecordEditor`, `CategoryManagerScreen`, and `WatchlistViewModel` state flows.

- [x] **Step 1: Write failing episode adjustment test**

```kotlin
@Test fun decrementClampsAtZero() {
    assertEquals(0, adjustedEpisode(currentEpisode = 0, delta = -1))
    assertEquals(6, adjustedEpisode(currentEpisode = 5, delta = 1))
}
```

- [x] **Step 2: Run the focused test to verify failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.domain.WatchlistRulesTest`

Expected: test compilation fails because `adjustedEpisode` does not exist.

- [x] **Step 3: Implement the minimal state and screens**

```kotlin
fun adjustedEpisode(currentEpisode: Int, delta: Int): Int =
    (currentEpisode + delta).coerceAtLeast(0)

fun adjustEpisode(record: WatchRecordEntity, delta: Int) = viewModelScope.launch {
    repository.setEpisode(record, adjustedEpisode(record.currentEpisode, delta))
}
```

`WatchlistScreen` uses a top app bar, an “全部” filter chip plus category chips, a category manager icon, an empty-state add button, and cards with `IconButton` decrement/increment actions. `WatchRecordEditor` validates a nonblank trimmed title and selected category. `CategoryManagerScreen` validates unique trimmed names, disables deletion of the final category, and when deleting a nonempty category requires selecting a different target category before confirming.

- [x] **Step 4: Wire the screens into current navigation**

Add a `watchlistPage` state to `AppNav`. Add a “追剧记录” row in `ProfileScreen`. Include this state in `BackHandler`, returning from the list or category manager to the prior watchlist/profile page. Construct `WatchlistViewModel` from `MainActivity` with `db.watchlistDao()` and pass it to `AppNav`.

- [x] **Step 5: Run focused tests and compile**

Run: `./gradlew.bat :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`

Expected: both Kotlin compilation tasks pass.

- [x] **Step 6: Checkpoint**

Manually inspect code paths to confirm a tap on card plus/minus is not intercepted by the card edit click, and system Back returns to “我的”.

### Task 4: Full verification and regression checks

**Files:**
- Modify: `docs/superpowers/plans/2026-08-29-watchlist-implementation.md` to mark completed tasks.

**Interfaces:**
- Consumes the completed persistence, backup, and UI work.
- Produces a debug APK suitable for local installation without publication.

- [x] **Step 1: Run unit tests**

Run: `./gradlew.bat :app:testDebugUnitTest`

Expected: tests pass. If the known local `GradleWorkerMain` process error occurs, retain its exact output and continue with compile/build verification rather than claiming tests passed.

Result on 2026-08-29: the command was run, but Gradle Test Executor could not start because `worker.org.gradle.process.internal.worker.GradleWorkerMain` was not found. Test source compilation passed separately; no unit-test pass claim is made.

- [x] **Step 2: Build the debug APK**

Run: `./gradlew.bat :app:assembleDebug`

Expected: `BUILD SUCCESSFUL` and an APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [x] **Step 3: Verify no release activity**

Do not execute any `gh release`, GitHub upload, or release build publication command.

- [x] **Step 4: Mark completed tasks**

Update each checked step in this plan only after its corresponding command or inspection is complete. Do not commit because the workspace has no Git repository.
