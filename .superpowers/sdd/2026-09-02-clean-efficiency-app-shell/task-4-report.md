# Task 4 Report

Date: 2026-09-02
Branch: `codex/clean-efficiency-app-shell`

## Files

- `app/src/main/java/com/example/birthdaycountdown/ui/HomeScreens.kt`
- `app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt`
- `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`
- `app/src/test/java/com/example/birthdaycountdown/ui/AddFlowRulesTest.kt`

`app/src/test/java/com/example/birthdaycountdown/ui/ReorderVisibleItemsTest.kt` was part of the focused verification run and remained unchanged.

## RED Evidence

Command:

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AddFlowRulesTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest --no-daemon --max-workers=1
```

Result:

```text
> Task :app:compileDebugUnitTestKotlin FAILED
e: file:///U:/app/src/test/java/com/example/birthdaycountdown/ui/AddFlowRulesTest.kt:11:84 Unresolved reference: label

FAILURE: Build failed with an exception.
BUILD FAILED in 14s
```

Interpretation: the new `addChoicesUseShortTypeLabels` test failed for the expected reason because `AddChoice.label` did not exist yet.

## GREEN Evidence

First implementation compile check:

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AddFlowRulesTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Intermediate failure fixed during implementation:

```text
e: file:///U:/app/src/main/java/com/example/birthdaycountdown/ui/HomeScreens.kt:352:21 'public' function exposes its 'internal' parameter type argument AddChoice
```

Final verification command:

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AddFlowRulesTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Final result:

```text
> Task :app:compileDebugKotlin
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 27s
29 actionable tasks: 7 executed, 22 up-to-date
```

## Commands and Output

Focused diff review:

```powershell
git -C U:/ diff -- app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt app/src/main/java/com/example/birthdaycountdown/ui/HomeScreens.kt app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt app/src/test/java/com/example/birthdaycountdown/ui/AddFlowRulesTest.kt app/src/test/java/com/example/birthdaycountdown/ui/ReorderVisibleItemsTest.kt
git -C U:/ status --short
```

Observed state before commit:

```text
 M app/src/main/java/com/example/birthdaycountdown/ui/AppNavigation.kt
 M app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt
 M app/src/test/java/com/example/birthdaycountdown/ui/AddFlowRulesTest.kt
?? app/src/main/java/com/example/birthdaycountdown/ui/HomeScreens.kt
```

## Self-Review

- `HomeScreens.kt` now owns the home, add-choice, profile, countdown item, and `watchlistSummary` declarations required by this task.
- `Screens.kt` was reduced to navigation glue plus `navIcon`, leaving non-home declarations in place for later tasks.
- The Time screen now uses the approved top bar plus FAB shell, keeps search and drag reorder behavior, and preserves existing countdown formatting and business callbacks.
- The add-choice route still dispatches birthday, anniversary, and watchlist destinations through the existing navigation callbacks.
- The profile screen now shows record summaries and one settings entry without duplicating settings controls or nesting cards.

## Commit

- Implementation commit: `a7ee4f3` (`feat: rebuild home and add flow`)

## Concerns

- The approved single-list home hierarchy removed the old dedicated pinned and 7-day grouped sections. The existing pin callback is still wired in the overflow menu, but pinning is now less visually prominent than before.
- I did not run visual/device QA; verification for this task is limited to the required focused unit tests and Kotlin compilation.

## Fix Round 1

### Scope

- Render the visible add-choice labels from `AddChoice.label` so the UI text matches the approved enum values exactly.
- Make pinning affect the single-list home ordering by showing pinned visible records first with stable relative order, while keeping drag reorder scoped to each pin-state group.

### RED Evidence

Command:

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AddFlowRulesTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest --tests com.example.birthdaycountdown.ui.HomeScreenOrderingTest --no-daemon --max-workers=1
```

Result:

```text
> Task :app:compileDebugUnitTestKotlin FAILED
e: file:///U:/app/src/test/java/com/example/birthdaycountdown/ui/AddFlowRulesTest.kt:16:50 Unresolved reference: addChoiceOptions
e: file:///U:/app/src/test/java/com/example/birthdaycountdown/ui/AddFlowRulesTest.kt:16:73 Unresolved reference: AddChoiceOption
e: file:///U:/app/src/test/java/com/example/birthdaycountdown/ui/HomeScreenOrderingTest.kt:11:23 Unresolved reference: pinnedFirstStableOrder

FAILURE: Build failed with an exception.
BUILD FAILED in 16s
```

Interpretation: the new contract tests failed for the expected missing production helpers.

### GREEN Evidence

Verification command:

```powershell
$env:GRADLE_USER_HOME='D:\GradleUserHome-TimePlanning'
.\gradlew.bat :app:testDebugUnitTest --tests com.example.birthdaycountdown.ui.AddFlowRulesTest --tests com.example.birthdaycountdown.ui.ReorderVisibleItemsTest --tests com.example.birthdaycountdown.ui.HomeScreenOrderingTest :app:compileDebugKotlin --no-daemon --max-workers=1
```

Result:

```text
> Task :app:compileDebugKotlin
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 30s
29 actionable tasks: 10 executed, 19 up-to-date
```

### Notes

- `AddChoiceScreen` now renders `AddChoiceOption.label`, and `addChoiceOptions()` derives each label directly from `AddChoice.label`.
- `HomeScreenOrderingTest` covers the pure pinned-first stable-order helper.
- The home list still uses one section; pinning now has visible effect through ordering plus a compact `置顶` status label on pinned rows.
- Fix commit: `e6ce51a` (`fix: align add labels and pin ordering`)
