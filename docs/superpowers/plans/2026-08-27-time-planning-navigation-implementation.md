# 时间规划局界面与个性化改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有倒计时 App 改造成“时间 / 添加时间 / 我的”三入口的“时间规划局”，并实现导航个性化、卡片拖动排序、折叠编辑器、独立 RGB/CMYK 输入和自适应图标。

**Architecture:** 保留当前 Kotlin、Jetpack Compose、Room 和 SharedPreferences 架构。Room 只新增 `sortOrder` 并迁移到版本 8；底部导航外观继续由 `AppPreferences` 持久化；现有 `Screens.kt` 承担页面组合，独立的颜色换算纯函数放入 domain 以便单元测试。

**Tech Stack:** Kotlin 2/JVM 17、Jetpack Compose Material 3、Room 2.6.1、SharedPreferences、JUnit 4、Android Gradle Plugin。

**Spec:** `docs/superpowers/specs/2026-08-27-time-planning-navigation-design.md`

## Global Constraints

- 保留现有生日、纪念日、阳历、农历、提醒和倒计时算法。
- 三个底部入口的位置和功能固定，只允许修改展示文字、图标和显隐状态。
- 同一导航入口的图标和文字不能同时隐藏。
- Room 必须从版本 7 无损迁移到版本 8。
- 本轮不增加云同步、账号、桌面小组件或新的提醒类型。
- 工作目录不是 Git 仓库，不初始化 Git，也不执行提交命令；每个任务用测试或构建结果作为检查点。

---

### Task 1: 卡片顺序模型与 Room 迁移

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/data/CountdownEntity.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/data/CountdownDao.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/data/CountdownRepository.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/data/AppDatabase.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/MainActivity.kt`

**Interfaces:**
- Produces: `CountdownEntity.sortOrder: Int`
- Produces: `CountdownEntity.titleTextColor: Int`
- Produces: `CountdownEntity.solarTextColor: Int`
- Produces: `CountdownEntity.lunarTextColor: Int`
- Produces: `CountdownEntity.countdownTextColor: Int`
- Produces: `CountdownDao.updateSortOrders(records: List<CountdownEntity>)`
- Produces: `CountdownRepository.reorder(records: List<CountdownEntity>)`
- Produces: `AppDatabase.MIGRATION_7_8`

- [ ] **Step 1: 为实体增加稳定排序字段**

在 `CountdownEntity` 末尾增加：

```kotlin
val titleTextColor: Int = 0xFF29232D.toInt(),
val solarTextColor: Int = 0xFF29232D.toInt(),
val lunarTextColor: Int = 0xFF29232D.toInt(),
val countdownTextColor: Int = 0xFF29232D.toInt(),
val sortOrder: Int = Int.MAX_VALUE
```

保留 `cardTextColor` 读取旧数据，不再用它覆盖四个独立文字对象。

- [ ] **Step 2: 修改查询和批量更新接口**

将查询改为：

```kotlin
@Query("SELECT * FROM countdown_records ORDER BY sortOrder ASC, id ASC")
fun observeAll(): Flow<List<CountdownEntity>>

@Update
suspend fun updateAll(records: List<CountdownEntity>)
```

- [ ] **Step 3: 让新增记录追加到末尾**

在 DAO 增加：

```kotlin
@Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM countdown_records")
suspend fun nextSortOrder(): Int
```

在 repository 的 `save()` 中，仅当 `id == 0L` 时将 `sortOrder` 设为 `dao.nextSortOrder()`；编辑记录保留原顺序。

- [ ] **Step 4: 实现持久化重排**

```kotlin
suspend fun reorder(records: List<CountdownEntity>) {
    dao.updateAll(records.mapIndexed { index, record -> record.copy(sortOrder = index) })
}
```

- [ ] **Step 5: 增加 7 到 8 迁移**

数据库版本改为 8，并增加：

```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE countdown_records ADD COLUMN titleTextColor INTEGER NOT NULL DEFAULT -14081235")
        db.execSQL("ALTER TABLE countdown_records ADD COLUMN solarTextColor INTEGER NOT NULL DEFAULT -14081235")
        db.execSQL("ALTER TABLE countdown_records ADD COLUMN lunarTextColor INTEGER NOT NULL DEFAULT -14081235")
        db.execSQL("ALTER TABLE countdown_records ADD COLUMN countdownTextColor INTEGER NOT NULL DEFAULT -14081235")
        db.execSQL("ALTER TABLE countdown_records ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 2147483647")
        db.execSQL("UPDATE countdown_records SET titleTextColor = cardTextColor, solarTextColor = cardTextColor, lunarTextColor = cardTextColor, countdownTextColor = cardTextColor")
        db.execSQL("UPDATE countdown_records SET sortOrder = id")
    }
}
```

在 `MainActivity` 的 `addMigrations` 末尾加入 `AppDatabase.MIGRATION_7_8`。

- [ ] **Step 6: 编译检查数据库接口**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
& 'D:\Gradle\gradle-8.7\bin\gradle.bat' :app:compileDebugKotlin --no-daemon --max-workers=1 --console=plain
```

Expected: `BUILD SUCCESSFUL`，Room 生成代码不报告字段或 DAO 错误。

### Task 2: 底部导航设置模型与持久化

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt`
- Test: `app/src/test/java/com/example/birthdaycountdown/ui/BottomNavSettingsTest.kt`

**Interfaces:**
- Produces: `enum class BottomNavIconId`
- Produces: `data class BottomNavItemSettings`
- Produces: `data class BottomNavSettings`
- Produces: `AppViewModel.bottomNavSettings: StateFlow<BottomNavSettings>`
- Produces: `AppViewModel.setBottomNavSettings(value: BottomNavSettings)`
- Produces: `AppViewModel.resetBottomNavSettings()`

- [ ] **Step 1: 写导航可见性规则测试**

```kotlin
class BottomNavSettingsTest {
    @Test fun iconCannotBeHiddenWhenLabelIsAlreadyHidden() {
        val item = BottomNavItemSettings("时间", BottomNavIconId.CLOCK, showIcon = true, showLabel = false)
        assertEquals(item, item.withIconVisibility(false))
    }

    @Test fun labelCannotBeHiddenWhenIconIsAlreadyHidden() {
        val item = BottomNavItemSettings("时间", BottomNavIconId.CLOCK, showIcon = false, showLabel = true)
        assertEquals(item, item.withLabelVisibility(false))
    }
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `gradle :app:compileDebugUnitTestKotlin --no-daemon --max-workers=1 --console=plain`

Expected: FAIL，因为导航设置类型尚不存在。

- [ ] **Step 3: 实现最小设置模型**

```kotlin
enum class BottomNavIconId { CLOCK, CALENDAR_PLUS, USER, HEART, STAR, SETTINGS }

data class BottomNavItemSettings(
    val label: String,
    val icon: BottomNavIconId,
    val showIcon: Boolean = true,
    val showLabel: Boolean = true
) {
    fun withIconVisibility(visible: Boolean) =
        if (!visible && !showLabel) this else copy(showIcon = visible)

    fun withLabelVisibility(visible: Boolean) =
        if (!visible && !showIcon) this else copy(showLabel = visible)
}
```

`BottomNavSettings` 固定包含 `time`、`add`、`profile` 三项，并提供 `DEFAULT_BOTTOM_NAV_SETTINGS`。

- [ ] **Step 4: 持久化全部导航字段**

在 `AppPreferences` 中按 `nav_time_label`、`nav_time_icon`、`nav_time_show_icon`、`nav_time_show_label` 命名保存三项配置。读取枚举时使用 `runCatching`，非法旧值回退到默认图标。

- [ ] **Step 5: 暴露 ViewModel 状态和重置操作**

使用 `MutableStateFlow(preferences.readBottomNavSettings())`，保存时同步更新 StateFlow 和 SharedPreferences；重置时写入默认值。

- [ ] **Step 6: 编译单元测试源码**

Run: `gradle :app:compileDebugUnitTestKotlin --no-daemon --max-workers=1 --console=plain`

Expected: Kotlin 测试源码编译成功；若本机仍出现既有 `GradleWorkerMain` 错误，记录为环境限制，不把它表述为测试通过。

### Task 3: 三入口主导航与“我的”页面

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`

**Interfaces:**
- Consumes: `AppViewModel.bottomNavSettings`
- Produces: `enum class MainTab { TIME, ADD, PROFILE }`
- Produces: `MainBottomBar(...)`
- Produces: `ProfileScreen(...)`
- Produces: `BottomNavSettingsScreen(...)`

- [ ] **Step 1: 将 AppNav 状态分为主标签和覆盖页**

使用 `MainTab` 保存底部选中项，用可空的 `editingRecord` 表示已有卡片编辑，用布尔状态控制设置详情。底部栏仅在三个主页面显示；编辑已有记录和设置详情隐藏底部栏。

- [ ] **Step 2: 实现固定功能的底部栏**

`MainBottomBar` 始终按 TIME、ADD、PROFILE 顺序渲染。文字取设置值，图标由 `BottomNavIconId` 映射到 Material Icons Extended 的线性图标；`showIcon` 或 `showLabel` 控制对应 slot 是否输出。

- [ ] **Step 3: 接入三个独立页面**

- TIME 调用卡片列表页面。
- ADD 调用 `EditScreen(existing = null)`，保存后切换到 TIME。
- PROFILE 显示“设置”入口，并由入口打开现有全局设置详情。

移除首页 `FloatingActionButton` 和顶部设置按钮，顶部标题改为“时间规划局”。

- [ ] **Step 4: 实现底部导航设置 UI**

每个入口提供：文字输入框、图标选择菜单、显示图标开关、显示文字开关。触发非法双隐藏时不改变状态。页面底部提供“恢复默认”。

- [ ] **Step 5: 编译检查路由与 Compose 类型**

Run: `gradle :app:compileDebugKotlin --no-daemon --max-workers=1 --console=plain`

Expected: `BUILD SUCCESSFUL`，无 Compose slot、图标或状态类型错误。

### Task 4: 修正新建语义并实现卡片拖动排序

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`

**Interfaces:**
- Consumes: `CountdownRepository.reorder(records)`
- Produces: `AppViewModel.reorder(records: List<CountdownEntity>)`

- [ ] **Step 1: 确保新增页面不复用旧记录**

进入 ADD 标签时始终传递 `existing = null`。只有从卡片点击进入时才设置 `editingRecord`；保存回调清空编辑状态，避免下一次添加携带旧 ID。

- [ ] **Step 2: 在 ViewModel 暴露重排操作**

```kotlin
fun reorder(records: List<CountdownEntity>) = viewModelScope.launch {
    repository.reorder(records)
}
```

- [ ] **Step 3: 使用 Compose 指针输入实现长按拖动**

列表维护当前显示顺序副本；卡片容器使用 `detectDragGesturesAfterLongPress`，根据累计 Y 位移跨越相邻卡片中心时交换元素。删除 `IconButton` 放在独立的点击消费区域，避免删除触发拖动。

- [ ] **Step 4: 松手后保存顺序**

`onDragEnd` 调用 `viewModel.reorder(localRecords)`；`onDragCancel` 恢复数据库当前顺序。拖动项使用 `graphicsLayer` 增加轻微阴影和透明度，布局尺寸不变化。

- [ ] **Step 5: 手工状态检查**

验证：新增两条记录后数量增加为两条；编辑第一条不会改变数量；拖动后离开并返回 TIME 页面，顺序保持。

### Task 5: 折叠编辑页和对象选择式控制器

**Files:**
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`

**Interfaces:**
- Produces: `ExpandableEditorSection(title, expanded, onExpandedChange, content)`
- Produces: `enum class DisplayTarget { SOLAR, LUNAR, COUNTDOWN }`
- Produces: `enum class StyleTarget { BACKGROUND, TITLE, SOLAR, LUNAR, COUNTDOWN }`

- [ ] **Step 1: 建立五个折叠分组**

将现有编辑控件原样移动到基础信息、显示内容、提醒设置、卡片颜色、渐变样式五组；初始只展开基础信息。组标题使用紧凑行和展开/收起图标，不嵌套卡片。

- [ ] **Step 2: 合并显示单位控制器**

顶部使用阳历、阴历、剩余时间三个分段按钮。下方只渲染当前 `DisplayTarget` 对应的 `solarDisplayMask`、`lunarDisplayMask` 或 `countdownDisplayMask`，继续使用已有 `UnitMaskChips`。

- [ ] **Step 3: 合并颜色和渐变对象选择**

颜色与渐变分组分别使用一个 `StyleTarget` 状态。选择目标后只显示该对象的一套编辑器或渐变列表；已选色板和渐变继续显示对号。

- [ ] **Step 4: 保证完整滚动和保存可达**

整个内容使用单一 `verticalScroll`，保存按钮放在内容末尾，并加入至少 96dp 底部空间。开启提醒后展开的控件不能覆盖保存按钮。

- [ ] **Step 5: 编译检查编辑状态映射**

Run: `gradle :app:compileDebugKotlin --no-daemon --max-workers=1 --console=plain`

Expected: `BUILD SUCCESSFUL`，三个显示 mask 和五个样式字段均映射到正确对象。

### Task 6: RGB/CMYK 独立输入和双向换算

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/domain/ColorConversion.kt`
- Create: `app/src/test/java/com/example/birthdaycountdown/domain/ColorConversionTest.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`

**Interfaces:**
- Produces: `data class RgbColor(val red: Int, val green: Int, val blue: Int)`
- Produces: `data class CmykColor(val cyan: Int, val magenta: Int, val yellow: Int, val key: Int)`
- Produces: `fun rgbToCmyk(rgb: RgbColor): CmykColor`
- Produces: `fun cmykToRgb(cmyk: CmykColor): RgbColor`

- [ ] **Step 1: 写颜色换算测试**

```kotlin
@Test fun blackRoundTrips() {
    assertEquals(CmykColor(0, 0, 0, 100), rgbToCmyk(RgbColor(0, 0, 0)))
    assertEquals(RgbColor(0, 0, 0), cmykToRgb(CmykColor(0, 0, 0, 100)))
}

@Test fun redRoundTrips() {
    assertEquals(CmykColor(0, 100, 100, 0), rgbToCmyk(RgbColor(255, 0, 0)))
    assertEquals(RgbColor(255, 0, 0), cmykToRgb(CmykColor(0, 100, 100, 0)))
}
```

- [ ] **Step 2: 运行测试源码编译并确认缺少实现**

Run: `gradle :app:compileDebugUnitTestKotlin --no-daemon --max-workers=1 --console=plain`

Expected: FAIL，因为颜色类型和函数尚不存在。

- [ ] **Step 3: 实现标准换算公式**

RGB 输入先校正到 0..255，CMYK 输入先校正到 0..100；计算使用 Double，输出使用 `roundToInt()`。纯黑单独返回 `0,0,0,100`，避免除零。

- [ ] **Step 4: 用七个独立输入框替换逗号输入**

RGB 行包含 R/G/B 三个等宽数字框，CMYK 行包含 C/M/Y/K 四个等宽数字框。每个框维护可编辑字符串；获得焦点时允许临时空值，失焦或键盘确认时校正范围并同步另一颜色空间。

- [ ] **Step 5: 接回五个样式对象和实时预览**

确认当前 `StyleTarget` 后，将 RGB 值转换为 ARGB Int 并写入对应颜色字段：背景使用 `cardBackgroundColor`，标题使用 `titleTextColor`，阳历使用 `solarTextColor`，阴历使用 `lunarTextColor`，剩余时间使用 `countdownTextColor`。纯色渐变状态读取这些字段；非纯色状态继续读取对应渐变 ID。

- [ ] **Step 6: 运行颜色测试**

Run: `gradle :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain`

Expected: 颜色换算测试通过；如果 Gradle worker 环境错误阻止执行，则至少要求 `compileDebugUnitTestKotlin` 成功并记录限制。

### Task 7: 应用名称与自适应图标

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create: `app/src/main/res/drawable/ic_launcher_background.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.png`
- Create: density-specific legacy launcher PNG files under `app/src/main/res/mipmap-*`
- Source asset: `C:/Users/杨/Desktop/7a022042-8101-4cee-9c89-726399327033.png`

**Interfaces:**
- Produces: Android launcher resources referenced as `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`

- [ ] **Step 1: 更新名称资源和 Manifest**

将 `app_name` 设为“时间规划局”，Manifest 的 `android:label` 改为 `@string/app_name`，并增加 `android:icon` 与 `android:roundIcon`。

- [ ] **Step 2: 处理用户提供的怀表图**

从原图裁取透明前景，适当放大表盘但完整保留顶部表冠。将主体限制在 Android 自适应图标安全区内，背景使用与原图协调的纯色，不改变怀表主体设计。

- [ ] **Step 3: 生成自适应和旧版图标资源**

前景提供高分辨率 PNG，自适应 XML 分别引用背景与前景；为 API 26 以下兼容资源生成 mdpi、hdpi、xhdpi、xxhdpi、xxxhdpi launcher PNG。

- [ ] **Step 4: 构建资源检查**

Run: `gradle :app:processDebugResources --no-daemon --max-workers=1 --console=plain`

Expected: `BUILD SUCCESSFUL`，Manifest 中所有图标和名称资源可解析。

### Task 8: 全量验证与 APK 产物

**Files:**
- Verify: `app/src/main/java/com/example/birthdaycountdown/**`
- Verify: `app/src/test/java/com/example/birthdaycountdown/**`
- Output: `app/build/outputs/apk/debug/app-debug.apk`

**Interfaces:**
- Consumes: Tasks 1-7 的全部实现
- Produces: 可安装 Debug APK

- [ ] **Step 1: 编译生产 Kotlin**

Run: `gradle :app:compileDebugKotlin --no-daemon --max-workers=1 --console=plain`

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 2: 编译单元测试 Kotlin**

Run: `gradle :app:compileDebugUnitTestKotlin --no-daemon --max-workers=1 --console=plain`

Expected: 测试源码编译成功。

- [ ] **Step 3: 尝试执行单元测试**

Run: `gradle :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain`

Expected: 测试通过；若仍出现已知 `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`，保留完整错误并明确说明测试未执行完成。

- [ ] **Step 4: 构建 Debug APK**

Run: `gradle :app:assembleDebug --no-daemon --max-workers=1 --console=plain`

Expected: `BUILD SUCCESSFUL`，APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **Step 5: 核对最终关键行为**

检查三入口切换、新增不覆盖、编辑不新增、卡片拖动排序、重启持久化、双隐藏限制、折叠滚动、RGB/CMYK 输入、色板对号、应用名称和图标。无法在本机模拟器验证的交互必须在交付说明中明确列为待真机验证，不以编译成功代替 UI 行为验证。
