# Clean Efficiency App Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current glass and purple-pink Compose shell with the approved clean-efficiency Material 3 interface while preserving all existing time, watchlist, AI, reminder, backup, and update behavior.

**Architecture:** Keep Room, repositories, ViewModels, services, and data contracts unchanged. Introduce a small UI design system and a Navigation Compose root shell with four stable top-level destinations; migrate each existing screen onto those shared primitives, then remove the legacy glass layer after every caller has moved.

**Tech Stack:** Kotlin 1.9.24, Android Gradle Plugin 8.5.2, Jetpack Compose BOM 2024.09.03, Material 3, Navigation Compose 2.8.2, Room 2.6.1, Kotlin coroutines, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-09-02-clean-efficiency-app-shell-design.md`

## Global Constraints

- Preserve the Room schema, DAO contracts, repositories, ViewModels, reminder scheduling, AI request protocol, AI history, backup format, update repository, and existing business fields.
- Top-level destinations are exactly `时间`, `追剧`, `AI`, and `我的`; `添加` is a command launched from the relevant page, not a top-level destination.
- Use deep teal for primary focus, coral only for add and key time states, neutral cool-gray page backgrounds, neutral surfaces, and Material error red for destructive actions.
- Use 8dp corners for cards, list items, grouped form surfaces, and modal content; do not introduce large gradients, glass blur, nested cards, decorative color blobs, or purple-pink dominant surfaces.
- Keep page horizontal padding at 16dp and touch targets at least 48dp even when the visible icon container is smaller.
- Keep phone navigation at the bottom and switch to a navigation rail for widths at or above 600dp without changing destinations or business state.
- Do not add account, cloud sync, multi-module restructuring, dependency injection, database migrations, new AI abilities, or new business entry points.
- Use existing Material icons; do not add a second icon library.
- Every task ends with focused tests, debug compilation, and a commit containing only that task.
- Final acceptance requires `:app:testDebugUnitTest`, `:app:assembleDebug`, `git diff --check`, and real-device or emulator screenshot inspection of the main light and dark states.

## File Structure

- Create `app/src/main/java/com/example/birthdaycountdown/ui/AppDesignSystem.kt`: visual tokens and reusable surfaces, top bars, actions, rows, labels, and state views.
- Create `app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt`: route constants, top-level destination metadata, `RootAppScaffold`, and the Navigation Compose `AppNav`.
- Create `app/src/main/java/com/example/birthdaycountdown/ui/HomeScreens.kt`: home, record-type choice, profile, overview, quick entries, and time-record rows moved out of the current oversized `Screens.kt`.
- Modify `app/src/main/java/com/example/birthdaycountdown/ui/AppTheme.kt`: approved light/dark color schemes, shapes, and typography.
- Modify `app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt`: navigation preference compatibility only; no repository or business changes.
- Modify `app/src/main/java/com/example/birthdaycountdown/ui/EditScreen.kt`: two-step record form and stable save/delete actions.
- Modify `app/src/main/java/com/example/birthdaycountdown/ui/WatchlistScreens.kt`: top-level watchlist layout, route-driven creation, reorder feedback, categories, and editor styling.
- Modify `app/src/main/java/com/example/birthdaycountdown/ui/AiScreens.kt`: route-driven AI home/chat/image screens and shared status styling.
- Modify `app/src/main/java/com/example/birthdaycountdown/ui/SettingsScreens.kt`: settings groups, summaries, secondary pages, backup, update, and navigation customization.
- Modify `app/src/main/java/com/example/birthdaycountdown/ui/UiComponents.kt`: retain domain-specific inputs and selectors; remove helpers superseded by `AppDesignSystem.kt` only after callers migrate.
- Delete `app/src/main/java/com/example/birthdaycountdown/ui/GlassStyle.kt` and its old test after no references remain.
- Replace `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt` with focused files; delete it after `AppNavigation.kt` and `HomeScreens.kt` own all of its behavior.

---

### Task 1: Establish the Clean-Efficiency Design System

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/ui/AppDesignSystem.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppTheme.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/AppDesignSystemTest.kt`

**Interfaces:**
- Consumes: Material 3 `ColorScheme`, `Shapes`, `Typography`, `Surface`, `TopAppBar`, and the existing `TimePlanningTheme` entry point.
- Produces: `AppUiTokens`, `AppPage`, `AppTopBar`, `PrimaryActionButton`, `AppListItem`, `SectionHeader`, `StatusLabel`, `EmptyState`, `LoadingState`, and `ErrorState` for Tasks 3-8.

- [ ] **Step 1: Write the failing token contract test.**

```kotlin
package com.example.birthdaycountdown.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDesignSystemTest {
    @Test
    fun cleanEfficiencyTokensUseCompactStableDimensions() {
        assertEquals(8.dp, AppUiTokens.surfaceCornerRadius)
        assertEquals(16.dp, AppUiTokens.pageHorizontalPadding)
        assertEquals(48.dp, AppUiTokens.minimumTouchTarget)
    }
}
```

- [ ] **Step 2: Run the focused test and verify it fails because `AppUiTokens` does not exist.**

Run:

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AppDesignSystemTest --no-daemon --max-workers=1
```

Expected: FAIL with an unresolved reference to `AppUiTokens`.

- [ ] **Step 3: Implement the visual tokens and approved light/dark themes.**

```kotlin
internal object AppUiTokens {
    val surfaceCornerRadius = 8.dp
    val pageHorizontalPadding = 16.dp
    val minimumTouchTarget = 48.dp
    val contentSpacing = 12.dp
    val sectionSpacing = 20.dp
}

internal enum class StatusTone { INFO, SUCCESS, WARNING, ERROR }

internal data class StatusColors(val container: Color, val content: Color)

@Composable
internal fun statusColors(tone: StatusTone): StatusColors = when (tone) {
    StatusTone.INFO -> StatusColors(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    StatusTone.SUCCESS -> StatusColors(Color(0xFFD6F2E1), Color(0xFF123A24))
    StatusTone.WARNING -> StatusColors(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
    StatusTone.ERROR -> StatusColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0C6670),
    onPrimary = Color.White,
    secondary = Color(0xFFEF7B61),
    onSecondary = Color(0xFF21120F),
    background = Color(0xFFF5F7F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFE7ECEE),
    outlineVariant = Color(0xFFDDE3E5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF85D7DF),
    onPrimary = Color(0xFF07363B),
    secondary = Color(0xFFFFA18D),
    onSecondary = Color(0xFF471006),
    background = Color(0xFF151719),
    surface = Color(0xFF22272A),
    surfaceVariant = Color(0xFF2D3437),
    outlineVariant = Color(0xFF3E484C)
)
```

- [ ] **Step 4: Add the shared Compose primitives without embedding business state.**

```kotlin
@Composable
internal fun AppPage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) = Box(
    modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
    content = content
)

@Composable
internal fun StatusLabel(text: String, tone: StatusTone) {
    val colors = statusColors(tone)
    Surface(color = colors.container, contentColor = colors.content, shape = RoundedCornerShape(8.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}
```

- [ ] **Step 5: Run the token test and compile the app.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AppDesignSystemTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Expected: PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the shared design system.**

```powershell
git add app/src/main/java/com/example/birthdaycountdown/ui/AppDesignSystem.kt app/src/main/java/com/example/birthdaycountdown/ui/AppTheme.kt app/src/test/java/com/example/birthdaycountdown/ui/AppDesignSystemTest.kt
git commit -m "feat: add clean efficiency design system"
```

### Task 2: Define Navigation Contracts and Preserve Navigation Preferences

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/AppNavigationTest.kt`
- Modify: `app/src/test/java/com/example/birthdaycountdown/ui/BottomNavSettingsTest.kt`

**Interfaces:**
- Consumes: existing `BottomNavSettings`, `BottomNavItemSettings`, `BottomNavIconId`, and SharedPreferences keys `nav_time_*`, `nav_add_*`, `nav_profile_*`, and `nav_ai_*`.
- Produces: `AppRoute`, `TopLevelDestination`, `TOP_LEVEL_DESTINATIONS`, `recordEditRoute`, `aiChatRoute`, `aiImageRoute`, and `normalizeNavigationSettings`; Task 3 builds the root shell against these exact names.

- [ ] **Step 1: Write failing tests for the four destinations, record routes, and the legacy add-slot conversion.**

```kotlin
@Test
fun topLevelDestinationsUseTheApprovedOrder() {
    assertEquals(
        listOf(TopLevelDestination.TIME, TopLevelDestination.WATCHLIST, TopLevelDestination.AI, TopLevelDestination.PROFILE),
        TOP_LEVEL_DESTINATIONS
    )
}

@Test
fun recordEditRoutesCarryExistingIdsAndNewRecordTypes() {
    assertEquals("record/edit/42", recordEditRoute(recordId = 42L))
    assertEquals("record/new/BIRTHDAY", recordEditRoute(recordType = RecordType.BIRTHDAY))
}

@Test
fun legacyDefaultAddSlotBecomesWatchlistWithoutChangingPreferenceKeys() {
    val legacy = BottomNavSettings(add = BottomNavItemSettings("添加时间", BottomNavIconId.CALENDAR_PLUS, showIcon = true, showLabel = false))
    val normalized = normalizeNavigationSettings(legacy)
    assertEquals("追剧", normalized.add.label)
    assertEquals(BottomNavIconId.MOVIE, normalized.add.icon)
    assertEquals(false, normalized.add.showLabel)
}

@Test
fun defaultSecondNavigationItemRepresentsWatchlist() {
    assertEquals("追剧", DEFAULT_BOTTOM_NAV_SETTINGS.add.label)
    assertEquals(BottomNavIconId.MOVIE, DEFAULT_BOTTOM_NAV_SETTINGS.add.icon)
}
```

- [ ] **Step 2: Run both navigation test classes and verify the new contracts fail.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AppNavigationTest --tests com.example.birthdaycountdown.ui.BottomNavSettingsTest --no-daemon --max-workers=1
```

Expected: FAIL because the new navigation types and `MOVIE` icon do not exist.

- [ ] **Step 3: Add Navigation Compose and define routes as plain strings compatible with Kotlin 1.9.24.**

```kotlin
implementation("androidx.navigation:navigation-compose:2.8.2")
```

```kotlin
internal object AppRoute {
    const val TIME = "time"
    const val WATCHLIST = "watchlist"
    const val WATCHLIST_ADD = "watchlist/add"
    const val AI = "ai"
    const val AI_CHAT = "ai/chat?conversationId={conversationId}"
    const val AI_IMAGE = "ai/image?conversationId={conversationId}"
    const val PROFILE = "profile"
    const val ADD_CHOICE = "record/add"
    const val RECORD_EDIT = "record/edit/{recordId}"
    const val RECORD_NEW = "record/new/{recordType}"
    const val SETTINGS = "settings"
    const val SETTINGS_DISPLAY = "settings/display"
    const val SETTINGS_NAVIGATION = "settings/navigation"
    const val SETTINGS_BACKUP = "settings/backup"
    const val SETTINGS_APPLICATION = "settings/application"
    const val SETTINGS_AI = "settings/ai"
}

internal enum class TopLevelDestination(val route: String) {
    TIME(AppRoute.TIME), WATCHLIST(AppRoute.WATCHLIST), AI(AppRoute.AI), PROFILE(AppRoute.PROFILE)
}

internal val TOP_LEVEL_DESTINATIONS = TopLevelDestination.entries

internal fun recordEditRoute(recordId: Long? = null, recordType: RecordType? = null): String = when {
    recordId != null -> "record/edit/$recordId"
    recordType != null -> "record/new/${recordType.name}"
    else -> error("recordId or recordType is required")
}

internal fun aiChatRoute(conversationId: Long? = null) = "ai/chat?conversationId=${conversationId ?: -1L}"
internal fun aiImageRoute(conversationId: Long? = null) = "ai/image?conversationId=${conversationId ?: -1L}"
```

- [ ] **Step 4: Preserve stored keys and normalize only the former default add item.**

```kotlin
enum class BottomNavIconId { CLOCK, CALENDAR_PLUS, MOVIE, USER, HEART, STAR, SETTINGS }

internal fun normalizeNavigationSettings(value: BottomNavSettings): BottomNavSettings {
    return if (value.add.label == "添加时间" && value.add.icon == BottomNavIconId.CALENDAR_PLUS) {
        value.copy(add = value.add.copy(label = "追剧", icon = BottomNavIconId.MOVIE))
    } else value
}

fun readBottomNavSettings() = normalizeNavigationSettings(
    BottomNavSettings(
        time = readNavItem("time", DEFAULT_BOTTOM_NAV_SETTINGS.time),
        add = readNavItem("add", DEFAULT_BOTTOM_NAV_SETTINGS.add),
        profile = readNavItem("profile", DEFAULT_BOTTOM_NAV_SETTINGS.profile),
        ai = readNavItem("ai", DEFAULT_BOTTOM_NAV_SETTINGS.ai)
    )
)
```

Update `DEFAULT_BOTTOM_NAV_SETTINGS` so its existing `add` slot defaults to `BottomNavItemSettings("追剧", BottomNavIconId.MOVIE)`. Update `navIcon` in the same task so `MOVIE` maps to `Icons.Outlined.Movie`; keep `CALENDAR_PLUS` readable for existing custom preferences.

- [ ] **Step 5: Run navigation tests and debug compilation.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AppNavigationTest --tests com.example.birthdaycountdown.ui.BottomNavSettingsTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Expected: PASS.

- [ ] **Step 6: Commit route contracts and preference compatibility.**

```powershell
git add app/build.gradle.kts app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt app/src/test/java/com/example/birthdaycountdown/ui/AppNavigationTest.kt app/src/test/java/com/example/birthdaycountdown/ui/BottomNavSettingsTest.kt
git commit -m "feat: define app navigation contracts"
```

### Task 3: Build the Root Navigation Shell

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/AppNavigationTest.kt`

**Interfaces:**
- Consumes: Task 1 `AppPage`, Task 2 route contracts, existing `AppViewModel`, `WatchlistViewModel`, `AiHistoryRepository`, and screen callbacks.
- Produces: `AppNav`, `RootAppScaffold`, and `AppNavigationItems`; `MainActivity` continues calling `AppNav(viewModel, watchlistViewModel, aiHistoryRepository, onRequestNotifications)`.

- [ ] **Step 1: Extend route tests for top-level selection and shell visibility.**

```kotlin
@Test
fun nestedRoutesKeepTheirOwningTopLevelDestination() {
    assertEquals(TopLevelDestination.TIME, topLevelDestinationFor("record/edit/42"))
    assertEquals(TopLevelDestination.WATCHLIST, topLevelDestinationFor(AppRoute.WATCHLIST_ADD))
    assertEquals(TopLevelDestination.AI, topLevelDestinationFor("ai/chat?conversationId=9"))
    assertEquals(TopLevelDestination.PROFILE, topLevelDestinationFor(AppRoute.SETTINGS))
}

@Test
fun secondaryRoutesHideTopLevelNavigation() {
    assertEquals(false, shouldShowTopLevelNavigation(AppRoute.ADD_CHOICE))
    assertEquals(true, shouldShowTopLevelNavigation(AppRoute.TIME))
}
```

- [ ] **Step 2: Run `AppNavigationTest` and verify the new mapping assertions fail.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AppNavigationTest --no-daemon --max-workers=1
```

Expected: FAIL until the pure route mapping functions exist.

- [ ] **Step 3: Implement `RootAppScaffold` with a bottom bar below 600dp and a rail at or above 600dp.**

```kotlin
@Composable
internal fun RootAppScaffold(
    currentDestination: TopLevelDestination?,
    settings: BottomNavSettings,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = { AppBottomNavigation(currentDestination, settings, onDestinationSelected) },
                content = content
            )
        } else {
            Row(Modifier.fillMaxSize()) {
                AppNavigationRail(currentDestination, settings, onDestinationSelected)
                Box(Modifier.weight(1f)) { content(PaddingValues()) }
            }
        }
    }
}
```

Map stored navigation settings to the approved visible order explicitly:

```kotlin
internal fun AppNavigationItems(settings: BottomNavSettings) = listOf(
    TopLevelDestination.TIME to settings.time,
    TopLevelDestination.WATCHLIST to settings.add,
    TopLevelDestination.AI to settings.ai,
    TopLevelDestination.PROFILE to settings.profile
)
```

- [ ] **Step 4: Replace manual enum navigation with `NavHost`, `popBackStack`, and top-level state restoration.**

```kotlin
navController.navigate(destination.route) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

Register every route listed in `AppRoute`. Existing screens may retain their current layout during this task, but all forward and back callbacks must go through `NavController` before the task is committed.

For record routes, collect `viewModel.records`, find the existing record by `recordId`, parse `recordType` with `RecordType.valueOf`, and pass the resulting value to `EditScreen`. For AI routes, register a nullable `Long` `conversationId` argument with default `-1L` and translate `-1L` to `null` before calling the screen.

- [ ] **Step 5: Run route tests, all existing UI rule tests, and debug compilation.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AppNavigationTest --tests com.example.birthdaycountdown.ui.AddFlowRulesTest --tests com.example.birthdaycountdown.ui.BottomNavSettingsTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Expected: PASS.

- [ ] **Step 6: Commit the root shell.**

```powershell
git add app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt app/src/test/java/com/example/birthdaycountdown/ui/AppNavigationTest.kt
git commit -m "feat: add navigation compose app shell"
```

### Task 4: Rebuild Home, Add Choice, and Profile

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/ui/HomeScreens.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/AddFlowRulesTest.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/ReorderVisibleItemsTest.kt`

**Interfaces:**
- Consumes: Task 1 design primitives, Task 3 navigation callbacks, existing countdown snapshots, formatting settings, drag reorder helper, and watchlist counts.
- Produces: `HomeScreen`, `AddChoiceScreen`, `ProfileScreen`, `CountdownRecordItem`, and `watchlistSummary`; Task 9 deletes `Screens.kt` once no declarations remain there.

- [ ] **Step 1: Add a failing test for the new add-choice labels and keep existing type mapping assertions.**

```kotlin
@Test
fun addChoicesUseShortTypeLabels() {
    assertEquals(listOf("生日", "纪念日", "追剧记录"), AddChoice.entries.map(AddChoice::label))
}
```

- [ ] **Step 2: Run add-flow and reorder tests.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AddFlowRulesTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest --no-daemon --max-workers=1
```

Expected: the new label test FAILS and existing behavior tests PASS.

- [ ] **Step 3: Move home-related declarations into `HomeScreens.kt` and implement the approved information hierarchy.**

```kotlin
internal enum class AddChoice(val label: String, val recordType: RecordType?) {
    BIRTHDAY("生日", RecordType.BIRTHDAY),
    ANNIVERSARY("纪念日", RecordType.ANNIVERSARY),
    WATCHLIST("追剧记录", null)
}

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    watchlistViewModel: WatchlistViewModel,
    onEdit: (CountdownEntity) -> Unit,
    onAdd: () -> Unit,
    onWatchlist: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "时间", onSearch = { searchVisible = !searchVisible }) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "添加记录") } }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            item { OverviewPanel(nearestRecord, now, format) }
            item { QuickEntryRow(birthdayCount, anniversaryCount, watchRecords.size, onWatchlist) }
            item { SectionHeader("接下来", visibleRecords.size) }
            items(visibleRecords, key = CountdownEntity::id) { record -> CountdownRecordItem(record, onEdit = { onEdit(record) }) }
        }
    }
}
```

Add these focused helpers in the same file so their inputs are explicit and testable through the parent screen:

```kotlin
@Composable
private fun OverviewPanel(record: CountdownEntity?, now: Instant, format: DateFormatPreference)

@Composable
private fun QuickEntryRow(
    birthdayCount: Int,
    anniversaryCount: Int,
    watchlistCount: Int,
    onWatchlist: () -> Unit
)
```

- [ ] **Step 4: Implement the type-choice page and profile summary with no nested cards.**

`AddChoiceScreen` must invoke birthday and anniversary record routes and the watchlist-add route. `ProfileScreen` must show existing record/watch counts and a single settings entry; it must not duplicate settings controls.

- [ ] **Step 5: Run focused tests and debug compilation.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AddFlowRulesTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Expected: PASS.

- [ ] **Step 6: Commit the home flow.**

```powershell
git add app/src/main/java/com/example/birthdaycountdown/ui/HomeScreens.kt app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt app/src/test/java/com/example/birthdaycountdown/ui/AddFlowRulesTest.kt app/src/test/java/com/example/birthdaycountdown/ui/ReorderVisibleItemsTest.kt
git commit -m "feat: rebuild home and add flow"
```

### Task 5: Convert Record Editing to a Two-Step Form

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/EditScreen.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/EditValidationTest.kt`

**Interfaces:**
- Consumes: existing `CountdownEntity` construction, `isValidSecondInput`, lunar conversion, reminder permission callback, dirty-state confirmation, and Task 1 form components.
- Produces: `EditorStep`, `editorValidationMessage`, and an `EditScreen` signature that adds `onDelete: ((CountdownEntity) -> Unit)? = null` while preserving every existing parameter and save output.

- [ ] **Step 1: Write failing pure tests for step order and field-level validation messages.**

```kotlin
@Test
fun editorUsesBasicThenDisplayAndReminderSteps() {
    assertEquals(listOf(EditorStep.BASIC, EditorStep.DISPLAY_AND_REMINDER), EditorStep.entries)
}

@Test
fun blankNameProducesAFieldLevelMessage() {
    assertEquals("请输入名称", editorValidationMessage(name = "", secondText = "0", lunarValid = true, countdownMask = 4, showsDate = true))
}
```

- [ ] **Step 2: Run `EditValidationTest` and verify the new tests fail.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.EditValidationTest --no-daemon --max-workers=1
```

Expected: FAIL because `EditorStep` and `editorValidationMessage` do not exist.

- [ ] **Step 3: Replace expandable editor sections with two stable steps.**

```kotlin
internal enum class EditorStep(val label: String) {
    BASIC("基础信息"), DISPLAY_AND_REMINDER("显示与提醒")
}

SegmentedOptions(
    labels = EditorStep.entries.map(EditorStep::label),
    selectedIndex = EditorStep.entries.indexOf(editorStep),
    onSelected = { editorStep = EditorStep.entries[it] }
)

when (editorStep) {
    EditorStep.BASIC -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; dirty = true },
            label = { Text("名称") },
            isError = name.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )
        SegmentedOptions(
            labels = RecordType.entries.map { if (it == RecordType.BIRTHDAY) "生日" else "纪念日" },
            selectedIndex = RecordType.entries.indexOf(type),
            onSelected = { type = RecordType.entries[it]; dirty = true }
        )
        SegmentedOptions(
            labels = listOf("阳历", "阴历"),
            selectedIndex = if (calendarType == CalendarType.SOLAR) 0 else 1,
            onSelected = { calendarType = if (it == 0) CalendarType.SOLAR else CalendarType.LUNAR; dirty = true }
        )
    }
    EditorStep.DISPLAY_AND_REMINDER -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch("显示阳历", showSolarDate) { showSolarDate = it; dirty = true }
        SettingsSwitch("显示阴历", showLunarDate) { showLunarDate = it; dirty = true }
        UnitMaskChips(countdownMask) { countdownMask = it; dirty = true }
        SettingsSwitch("开启提醒", reminder) { reminder = it; dirty = true }
    }
}
```

Keep all current fields, color and gradient options, reminder behavior, lunar fields, `dirty` updates, and `CountdownEntity` assignments. Change layout and grouping only.

Extend the screen signature so the navigation layer can supply deletion without moving repository logic into the form:

```kotlin
fun EditScreen(
    existing: CountdownEntity?,
    viewModel: AppViewModel,
    onRequestNotifications: () -> Unit,
    showBack: Boolean = true,
    initialType: RecordType = RecordType.ANNIVERSARY,
    onBack: (() -> Unit)? = null,
    onDelete: ((CountdownEntity) -> Unit)? = null,
    onDone: () -> Unit
)
```

`AppNavigation.kt` passes `onDelete = { viewModel.delete(it); navController.popBackStack() }` only for an existing record.

- [ ] **Step 4: Keep save visible, place validation beside the relevant field, and keep deletion Material error red.**

The bottom action area must use `PrimaryActionButton("保存", enabled = validationMessage == null)` and account for IME/system insets. Existing records must expose delete as a separate error-colored command with confirmation; new records must not show delete.

- [ ] **Step 5: Run edit tests and debug compilation.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.EditValidationTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Expected: PASS.

- [ ] **Step 6: Commit the editor redesign.**

```powershell
git add app/src/main/java/com/example/birthdaycountdown/ui/EditScreen.kt app/src/test/java/com/example/birthdaycountdown/ui/EditValidationTest.kt
git commit -m "feat: simplify record editor flow"
```

### Task 6: Promote Watchlist to a Top-Level Workflow

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/WatchlistScreens.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/ReorderVisibleItemsTest.kt`

**Interfaces:**
- Consumes: existing watchlist categories, records, episode adjustment, save/delete, reorder operations, Task 1 status and empty-state components, and Task 3 routes.
- Produces: `WatchlistScreen(onManageCategories, onAdd, onCreationFinished)`, route-driven creation, and visible reorder feedback.

- [ ] **Step 1: Add a failing reorder-state label test.**

```kotlin
@Test
fun reorderStateExplainsTheActiveGesture() {
    assertEquals("正在排序，松开后保存顺序", reorderStatusLabel(dragging = true))
    assertEquals(null, reorderStatusLabel(dragging = false))
}
```

- [ ] **Step 2: Run the reorder test and verify the new assertion fails.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest --no-daemon --max-workers=1
```

Expected: FAIL because `reorderStatusLabel` does not exist.

- [ ] **Step 3: Remove the back arrow from the top-level watchlist route and expose route callbacks.**

```kotlin
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onManageCategories: () -> Unit,
    onAdd: () -> Unit,
    startCreating: Boolean = false,
    onCreationFinished: () -> Unit = {}
)
```

The `watchlist` destination calls `onAdd = { navigate(AppRoute.WATCHLIST_ADD) }`. The `watchlist/add` destination passes `startCreating = true` and pops to `watchlist` after save or dismissal.

- [ ] **Step 4: Apply the approved list hierarchy and explicit reorder feedback without changing gesture thresholds or reorder calls.**

Keep `detectDragGesturesAfterLongPress`, the 72px movement threshold, `moveVisibleItem`, and `viewModel.reorderRecords`. While dragging, render `StatusLabel(reorderStatusLabel(true), StatusTone.INFO)`, reduce the dragged row opacity, and add elevation without resizing the row.

- [ ] **Step 5: Restyle category filters, record rows, editor, empty state, and deletion dialog.**

Use segmented or compact filter controls, a single non-nested row surface per record, standard icon buttons for episode decrement/increment, and Material error red for deletion. Preserve category requirements and all existing callbacks.

- [ ] **Step 6: Run reorder tests and debug compilation.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Expected: PASS.

- [ ] **Step 7: Commit the watchlist workflow.**

```powershell
git add app/src/main/java/com/example/birthdaycountdown/ui/WatchlistScreens.kt app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt app/src/test/java/com/example/birthdaycountdown/ui/ReorderVisibleItemsTest.kt
git commit -m "feat: promote watchlist navigation"
```

### Task 7: Move AI Screens onto the Shared Navigation and State Language

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ai/AiModels.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AiScreens.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ai/AiLogicTest.kt`

**Interfaces:**
- Consumes: existing AI services, history repository, image file handling, request parameters, status helpers, Task 1 shared components, and Task 3 routes.
- Produces: callback-driven `AiHomeScreen`, route-visible `AiChatScreen`, and route-visible `AiImageScreen`; removes the local integer `page` navigation state.

- [ ] **Step 1: Extend the existing status-label test to cover terminal states used by the new `StatusLabel`.**

```kotlin
@Test
fun labelsTerminalImageGenerationStages() {
    assertEquals("已完成", imageGenerationStatusLabel("DONE"))
    assertEquals("生成失败", imageGenerationStatusLabel("FAILED"))
}
```

- [ ] **Step 2: Run `AiLogicTest` and verify the new terminal-label expectations fail.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ai.AiLogicTest --no-daemon --max-workers=1
```

Expected: FAIL until terminal labels are added without changing active-state detection.

- [ ] **Step 3: Replace local page switching with explicit navigation callbacks.**

```kotlin
@Composable
fun AiHomeScreen(
    historyRepository: AiHistoryRepository,
    onChat: (Long?) -> Unit,
    onImage: (Long?) -> Unit,
    onSettings: () -> Unit
)
```

History rows call `onChat(conversation.id)` or `onImage(conversation.id)` according to `conversation.mode`. The `NavHost` parses optional `conversationId` query arguments and passes them to `AiChatScreen` or `AiImageScreen`.

- [ ] **Step 4: Apply the shared page, top-bar, history-row, status, loading, failure, and retry components.**

Do not alter endpoint normalization, payload construction, model selection, image size mapping, reference image processing, persistence, save behavior, or retry callbacks.

- [ ] **Step 5: Group image controls by reference image, ratio, resolution, and quality using existing values.**

Each choice remains mapped to the same request field. Use `SegmentedOptions` only when every option fits; otherwise use a standard exposed menu or selectable row rather than a horizontally clipped chip row.

- [ ] **Step 6: Run AI tests and debug compilation.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ai.AiLogicTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Expected: PASS.

- [ ] **Step 7: Commit the AI navigation and visual update.**

```powershell
git add app/src/main/java/com/example/birthdaycountdown/ai/AiModels.kt app/src/main/java/com/example/birthdaycountdown/ui/AiScreens.kt app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt app/src/test/java/com/example/birthdaycountdown/ai/AiLogicTest.kt
git commit -m "feat: unify ai navigation and states"
```

### Task 8: Rebuild Settings and Navigation Customization

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/SettingsScreens.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/BottomNavSettingsTest.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/update/ReleaseLinksTest.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/data/BackupCodecTest.kt`

**Interfaces:**
- Consumes: existing preferences, backup launchers, update checker, release URL, Task 1 settings rows and form controls, and Task 3 routes.
- Produces: grouped settings root, restyled secondary pages, four-destination customization, and unchanged backup/update behavior.

- [ ] **Step 1: Run navigation, update-link, and backup tests before editing settings UI.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.BottomNavSettingsTest --tests com.example.birthdaycountdown.update.ReleaseLinksTest --tests com.example.birthdaycountdown.data.BackupCodecTest --no-daemon --max-workers=1
```

Expected: PASS, establishing the preference, update, and backup behavior that the layout change must preserve.

- [ ] **Step 2: Rebuild the root settings groups and summaries.**

```kotlin
SectionHeader("偏好设置")
SettingsRow("显示与格式", displaySummary, Icons.Outlined.Tune, onDisplaySettings)
SettingsRow("底部导航", "时间、追剧、AI、我的", Icons.Outlined.Navigation, onNavigationSettings)

SectionHeader("数据与应用")
SettingsRow("数据与备份", "导出或合并恢复本地记录", Icons.Outlined.Folder, onDataBackup)
SettingsRow("AI 设置", "分别配置对话和生图", Icons.Outlined.AutoAwesome, onAiSettings)
SettingsRow("应用更新", "当前 ${BuildConfig.VERSION_NAME}", Icons.Outlined.Info, onApplicationSettings)
```

- [ ] **Step 3: Restyle secondary settings pages without changing stored values or launchers.**

Keep all current date, calendar, time-unit, typography, icon/label visibility, backup, import, update, retry, and browser-opening behavior. The second navigation editor must be labeled `追剧`; internal SharedPreferences keys remain `nav_add_*` for compatibility.

- [ ] **Step 4: Run focused tests and debug compilation.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.BottomNavSettingsTest --tests com.example.birthdaycountdown.update.ReleaseLinksTest --tests com.example.birthdaycountdown.data.BackupCodecTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Expected: PASS.

- [ ] **Step 5: Commit settings and compatibility updates.**

```powershell
git add app/src/main/java/com/example/birthdaycountdown/ui/SettingsScreens.kt app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt app/src/test/java/com/example/birthdaycountdown/ui/BottomNavSettingsTest.kt app/src/test/java/com/example/birthdaycountdown/update/ReleaseLinksTest.kt app/src/test/java/com/example/birthdaycountdown/data/BackupCodecTest.kt
git commit -m "feat: rebuild settings surfaces"
```

### Task 9: Remove the Legacy Shell and Verify the Whole App

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/UiComponents.kt`
- Delete: `app/src/main/java/com/example/birthdaycountdown/ui/GlassStyle.kt`
- Delete: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`
- Delete: `app/src/test/java/com/example/birthdaycountdown/ui/GlassStyleTest.kt`
- Modify: any UI file only where required to remove a remaining legacy import or call found by the scans below.

**Interfaces:**
- Consumes: all Tasks 1-8 outputs.
- Produces: one clean-efficiency UI system with no glass or purple-pink implementation path and a verified Debug APK.

- [ ] **Step 1: Prove every legacy caller has migrated before deleting files.**

Run:

```powershell
rg -n "GlassBackdrop|GlassPanel|glassTopAppBarColors|PurplePink|purple|pink|gradient.*primary" app/src/main/java/com/example/birthdaycountdown/ui -g '*.kt'
```

Expected: no active UI call sites. Color and gradient fields that belong to user-configurable countdown cards may remain; app-shell glass and purple-pink identifiers may not.

- [ ] **Step 2: Delete the empty legacy shell files and remove only imports/helpers made unused by this redesign.**

Delete `GlassStyle.kt`, `GlassStyleTest.kt`, and `Screens.kt` only after `rg` confirms their declarations now exist in the focused files. Do not remove unrelated pre-existing code.

- [ ] **Step 3: Run the full unit suite and build the Debug APK.**

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=1
```

Expected: `BUILD SUCCESSFUL` with all tests passing and `app/build/outputs/apk/debug/app-debug.apk` present.

- [ ] **Step 4: Install and launch on an available emulator or connected Android device.**

```powershell
adb devices
adb install -r '.\app\build\outputs\apk\debug\app-debug.apk'
adb shell am force-stop com.example.birthdaycountdown
adb shell am start -n com.example.birthdaycountdown/.MainActivity
```

Expected: one device in the `device` state and the app opens without a crash. If no device is available, record that screenshot verification remains unverified rather than claiming completion.

- [ ] **Step 5: Inspect required light-mode states and capture evidence.**

Navigate through the running app and inspect: home with records, home empty state when available without deleting user data, add choice, new record validation, existing record edit, watchlist, AI home, settings root, display settings, backup, and application update. Confirm no text clipping, overlapping controls, nested cards, hidden save action, or content under the bottom bar.

Capture at minimum home, add choice, watchlist, AI home, and settings root:

```powershell
New-Item -ItemType Directory -Force '.\app\build\outputs\ui-review' | Out-Null
cmd /c "adb exec-out screencap -p > app\build\outputs\ui-review\current-screen.png"
```

Rename each inspected capture immediately to `home-light.png`, `add-light.png`, `watchlist-light.png`, `ai-light.png`, or `settings-light.png` before taking the next capture.

- [ ] **Step 6: Repeat key inspection in dark mode and at a wide width.**

Use the device or emulator system controls to enable dark mode, then inspect home, editor, watchlist, AI, and settings. On an emulator or resizable device, use a width at or above 600dp and confirm the navigation rail replaces the bottom bar while routes and selected state remain unchanged.

- [ ] **Step 7: Check source cleanliness and the final diff.**

```powershell
rg -n "GlassBackdrop|GlassPanel|glassTopAppBarColors|PurplePink" app/src/main/java/com/example/birthdaycountdown/ui -g '*.kt'
git diff --check
git status --short
```

Expected: no legacy shell references, no whitespace errors, and only intended UI, test, build dependency, spec, and plan changes.

- [ ] **Step 8: Commit cleanup and verified delivery.**

```powershell
git add app/src/main/java/com/example/birthdaycountdown/ui app/src/test/java/com/example/birthdaycountdown/ui
git commit -m "chore: remove legacy app shell"
```

## Self-Review

- Spec coverage: Tasks 1-3 implement the design system, Navigation Compose graph, four top-level destinations, system back behavior, state restoration, and phone/wide navigation. Tasks 4-8 cover home, add, profile, editor, watchlist, AI, settings, backup, update, loading, failure, deletion, and reorder feedback. Task 9 removes the old visual system and performs automated, build, runtime, responsive, light, and dark verification.
- Data compatibility: no Room entity, DAO, repository, AI protocol, backup schema, update repository, or reminder API changes are planned. Existing `nav_add_*` preference keys remain in place and only the former default value is normalized to the watchlist meaning.
- Scope control: no new features, account system, cloud sync, dependency injection, module split, or unrelated refactor is included. The only file split is the existing 480-line `Screens.kt`, which is directly required to separate root navigation from page layout.
- Placeholder scan: no unresolved implementation markers or undefined neighboring interfaces remain. Every produced type or function used by another task is named in the producing task.
- Type consistency: `AppRoute`, `TopLevelDestination`, `TOP_LEVEL_DESTINATIONS`, `AppNav`, `RootAppScaffold`, `AppUiTokens`, `AddChoice`, `EditorStep`, and route callback signatures are defined once and referenced consistently across tasks.
