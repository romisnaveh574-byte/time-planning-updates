# Purple Pink App Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the confirmed purple-pink visual system to every Compose screen while preserving all existing time-planning, watchlist, AI, backup, and update behavior.

**Architecture:** Keep navigation, ViewModel calls, repositories, and request services unchanged. Centralize the new cold-white surface, purple-to-pink gradient, compact card, and selected-navigation styling in shared UI primitives; individual screens then replace only their visual layout and compose those primitives around existing callbacks and state.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android ViewModel, Room, Kotlin coroutines, JUnit.

**Spec:** `docs/superpowers/specs/2026-09-01-purple-pink-app-redesign.md`

## Global Constraints

- Preserve all existing navigation destinations, input fields, database schema, AI request protocol, background services, backups, update links, and long-press reordering behavior.
- Use a cold-white page surface; reserve the purple-blue to bright-pink gradient for primary cards, primary actions, and selected states.
- Regular cards use white surfaces, light borders or shadows, and a maximum 8dp corner radius; do not nest decorative cards.
- Keep destructive actions Material error red, and retain existing Material semantics for warnings and errors.
- Do not add dependencies, data migrations, or new business entry points.
- Final acceptance requires `:app:testDebugUnitTest`, `:app:assembleDebug`, and `git diff --check` to succeed.

---

### Task 1: Define the Shared Purple-Pink Visual System

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppTheme.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/GlassStyle.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/UiComponents.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/GlassStyleTest.kt`

**Interfaces:**
- Consumes: existing `TimePlanningTheme`, `GlassBackdrop`, `GlassPanel`, and Material 3 component APIs.
- Produces: shared background, `PurplePinkBrush`, compact white surface, icon entry card, section header, status label, and selected navigation appearance used by Tasks 2-5.

- [ ] **Step 1: Update the visual token test for the non-glass surface contract.**

```kotlin
assertEquals(8.dp, GlassStyle.surfaceCornerRadius)
assertEquals(1f, GlassStyle.panelAlpha)
assertTrue(GlassStyle.primaryGradient.isNotEmpty())
```

- [ ] **Step 2: Run the focused UI unit test and confirm the unimplemented token assertion fails.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.GlassStyleTest --no-daemon --max-workers=1"`

Expected: FAIL until the new visual tokens are present.

- [ ] **Step 3: Replace glass backdrop/panel defaults with the cold-white page and compact white-surface system, and add reusable gradient and icon-entry Compose helpers.**

```kotlin
internal val PurplePinkBrush = Brush.linearGradient(listOf(PurplePinkStart, PurplePinkEnd))
internal val AppSurfaceShape = RoundedCornerShape(8.dp)
internal object GlassStyle { val surfaceCornerRadius = 8.dp }

@Composable
internal fun GradientActionCard(title: String, onClick: () -> Unit) { Surface(onClick = onClick) { Text(title) } }
```

- [ ] **Step 4: Run the focused UI test and compile the app.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.GlassStyleTest :app:compileDebugKotlin --no-daemon --max-workers=1"`

Expected: PASS.

- [ ] **Step 5: Commit the shared visual system.**

```bash
git add app/src/main/java/com/example/birthdaycountdown/ui/AppTheme.kt app/src/main/java/com/example/birthdaycountdown/ui/GlassStyle.kt app/src/main/java/com/example/birthdaycountdown/ui/UiComponents.kt app/src/test/java/com/example/birthdaycountdown/ui/GlassStyleTest.kt
git commit -m "feat: add purple pink visual system"
```

### Task 2: Restyle Primary Navigation, Home, Add, and Profile

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/AddFlowRulesTest.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/BottomNavSettingsTest.kt`

**Interfaces:**
- Consumes: Task 1 shared visual primitives and existing `AppNav`, `AddChoice`, `watchlistSummary`, and bottom-navigation settings.
- Produces: a cold-white home screen with gradient overview, compact record entries, three visual add cards, profile grouping, and circular selected navigation without changing routes.

- [ ] **Step 1: Keep behavioral expectations explicit for add choices, watchlist summary, and configured bottom navigation.**

```kotlin
assertEquals(RecordType.BIRTHDAY, AddChoice.BIRTHDAY.recordType)
assertEquals("正在追 5 部", watchlistSummary(5))
assertNull(AddChoice.WATCHLIST.recordType)
```

- [ ] **Step 2: Run add-flow and bottom-navigation tests before visual changes.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AddFlowRulesTest --tests com.example.birthdaycountdown.ui.BottomNavSettingsTest --no-daemon --max-workers=1"`

Expected: PASS, establishing the behavior to preserve.

- [ ] **Step 3: Recompose `HomeScreen`, `AddChoiceScreen`, `ProfileScreen`, and `MainBottomBar` around the shared primitives while retaining their existing callbacks and state.**

```kotlin
GradientActionCard(title = "添加生日", icon = Icons.Outlined.Cake, onClick = { onSelected(AddChoice.BIRTHDAY) })
GradientActionCard(title = "添加纪念日", icon = Icons.Outlined.FavoriteBorder, onClick = { onSelected(AddChoice.ANNIVERSARY) })
GradientActionCard(title = "添加追剧记录", icon = Icons.Outlined.LiveTv, onClick = { onSelected(AddChoice.WATCHLIST) })
```

- [ ] **Step 4: Run behavior tests and debug compilation after the primary-screen change.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AddFlowRulesTest --tests com.example.birthdaycountdown.ui.BottomNavSettingsTest :app:compileDebugKotlin --no-daemon --max-workers=1"`

Expected: PASS.

- [ ] **Step 5: Commit the main navigation restyle.**

```bash
git add app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt app/src/test/java/com/example/birthdaycountdown/ui/AddFlowRulesTest.kt app/src/test/java/com/example/birthdaycountdown/ui/BottomNavSettingsTest.kt
git commit -m "feat: restyle home navigation and add flow"
```

### Task 3: Restyle Record Editing and Watchlist Workflows

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/EditScreen.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/WatchlistScreens.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/EditValidationTest.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/ReorderVisibleItemsTest.kt`

**Interfaces:**
- Consumes: Task 1 surface and action components, existing edit callbacks, and watchlist drag/reorder state.
- Produces: compact grouped forms and watchlist cards that retain save, delete, category, progress, and long-press drag behavior.

- [ ] **Step 1: Preserve edit validation and visible-item reordering expectations.**

```kotlin
assertTrue(isValidSecondInput("0"))
assertEquals(listOf(1L, 2L, 5L, 4L, 3L), moved.map { it.id })
```

- [ ] **Step 2: Run edit and reorder tests before layout changes.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.EditValidationTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest --no-daemon --max-workers=1"`

Expected: PASS.

- [ ] **Step 3: Apply white grouped form sections, a single full-width gradient save action, compact watchlist summary/filter controls, and white record/category cards without altering gesture modifiers.**

```kotlin
Modifier
    .graphicsLayer { alpha = if (dragging) 0.72f else 1f }
    .shadow(if (dragging) 10.dp else 0.dp, AppSurfaceShape)
    .pointerInput(record.id) { detectDragGesturesAfterLongPress(onDrag = onDrag) }
```

- [ ] **Step 4: Run the focused tests and debug compilation.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.EditValidationTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest :app:compileDebugKotlin --no-daemon --max-workers=1"`

Expected: PASS.

- [ ] **Step 5: Commit the form and watchlist restyle.**

```bash
git add app/src/main/java/com/example/birthdaycountdown/ui/EditScreen.kt app/src/main/java/com/example/birthdaycountdown/ui/WatchlistScreens.kt app/src/test/java/com/example/birthdaycountdown/ui/EditValidationTest.kt app/src/test/java/com/example/birthdaycountdown/ui/ReorderVisibleItemsTest.kt
git commit -m "feat: restyle editor and watchlist screens"
```

### Task 4: Restyle AI Entry, Conversation, and Image Generation Screens

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AiScreens.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ai/AiLogicTest.kt`

**Interfaces:**
- Consumes: Task 1 shared primary cards and existing chat/image task states, model configuration, history repository, and retry callbacks.
- Produces: two high-contrast AI entry cards, clearly differentiated message/task states, compact generation parameter groups, and unchanged AI requests.

- [ ] **Step 1: Run the existing AI behavior test suite before restyling.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ai.AiLogicTest --no-daemon --max-workers=1"`

Expected: PASS, covering existing request and response rules.

- [ ] **Step 2: Replace AI screen glass surfaces with shared cards and state labels while retaining all chat/image launch, persistence, saving, and retry callbacks.**

```kotlin
StatusLabel(status = when (task.status) {
    "QUEUED" -> "排队中"
    "RUNNING" -> "生成中"
    "FAILED" -> "生成失败"
    else -> "已完成"
})
```

- [ ] **Step 3: Group ratio, resolution, quality, and reference-image controls in the existing image flow without changing their values or request mapping.**

```kotlin
SectionLabel("图片参数")
SegmentedOptions(labels = ratios, selectedIndex = selectedRatio, onSelected = onRatioSelected)
```

- [ ] **Step 4: Run AI behavior tests and debug compilation.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ai.AiLogicTest :app:compileDebugKotlin --no-daemon --max-workers=1"`

Expected: PASS.

- [ ] **Step 5: Commit the AI visual restyle.**

```bash
git add app/src/main/java/com/example/birthdaycountdown/ui/AiScreens.kt app/src/test/java/com/example/birthdaycountdown/ai/AiLogicTest.kt
git commit -m "feat: restyle ai screens"
```

### Task 5: Restyle Settings and Validate the Whole App

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/SettingsScreens.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/BottomNavSettingsTest.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/update/ReleaseLinksTest.kt`

**Interfaces:**
- Consumes: Task 1 shared components, existing settings navigation, app preferences, backup actions, and release-page intent.
- Produces: grouped settings root and compatible secondary settings surfaces without changing data or update behavior.

- [ ] **Step 1: Run navigation and release-link behavior tests before layout changes.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.BottomNavSettingsTest --tests com.example.birthdaycountdown.update.ReleaseLinksTest --no-daemon --max-workers=1"`

Expected: PASS.

- [ ] **Step 2: Recompose the root settings page into the confirmed `偏好设置` and `数据与应用` groups and apply shared form styling to each secondary page.**

```kotlin
SettingsSection(title = "偏好设置", summary = "显示、导航与 AI 配置") {
    SettingsCategoryRow("显示设置", "日期、单位与卡片显示", Icons.Outlined.Palette, onDisplay)
}
SettingsSection(title = "数据与应用", summary = "备份、更新与版本信息") {
    SettingsCategoryRow("检查更新", "在浏览器打开最新版本", Icons.Outlined.SystemUpdate, onApplication)
}
```

- [ ] **Step 3: Keep the release page as an external browser link and run focused settings/update tests plus debug compilation.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.BottomNavSettingsTest --tests com.example.birthdaycountdown.update.ReleaseLinksTest :app:compileDebugKotlin --no-daemon --max-workers=1"`

Expected: PASS.

- [ ] **Step 4: Run the full automated verification and inspect the final diff.**

Run: `cmd /c "set GRADLE_USER_HOME=D:\\GradleUserHome-TimePlanning&& T: && cd \\ && gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=1"`

Expected: BUILD SUCCESSFUL.

Run: `git diff --check; git status --short`

Expected: no whitespace errors; only the intended visual-system files and plan are modified.

- [ ] **Step 5: Commit the settings restyle and validation-ready change set.**

```bash
git add app/src/main/java/com/example/birthdaycountdown/ui/SettingsScreens.kt app/src/test/java/com/example/birthdaycountdown/ui/BottomNavSettingsTest.kt app/src/test/java/com/example/birthdaycountdown/update/ReleaseLinksTest.kt docs/superpowers/plans/2026-09-01-purple-pink-app-redesign.md
git commit -m "feat: restyle settings with purple pink theme"
```

## Self-Review

- Spec coverage: Task 1 implements the global theme and common visual language. Task 2 covers home, add, profile, and bottom navigation. Task 3 covers record editing, watchlist list, categories, and reorder visuals. Task 4 covers AI home, chat, image generation, parameter groups, status, and retry visual states. Task 5 covers the root settings groups, secondary settings pages, release link behavior, and full verification.
- Placeholder scan: no `TODO`, `TBD`, `implement later`, or `fill in details` markers remain. Code samples use existing tests where possible; identifiers introduced by the plan are defined in the same task.
- Type consistency: all reusable visual helpers are internal Compose functions or constants under the existing `ui` package; no data, navigation, repository, or service interface is changed.
