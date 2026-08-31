# 时间规划局应用内更新 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为“时间规划局”增加基于公开 GitHub Releases 的免费应用内检查、下载、校验和覆盖安装能力，并用当前永久签名保证现有记录不丢失。

**Architecture:** 更新核心拆成版本解析、GitHub Release 解析、检查策略、DownloadManager 下载、APK 校验和系统安装六个边界。`AppViewModel` 只协调更新状态，Compose 只展示状态和发起用户动作；发布端使用独立 PowerShell 脚本构建、验签并上传 Release。

**Tech Stack:** Kotlin/JVM 17、Jetpack Compose Material 3、Android DownloadManager、PackageManager、FileProvider、HttpURLConnection、GitHub Releases REST API、GitHub CLI、JUnit 4、Room 2.6.1。

**Spec:** `docs/superpowers/specs/2026-08-29-in-app-update-design.md`

## Global Constraints

- 更新仓库固定为公开 GitHub 仓库 `time-planning-updates`，只公开 APK 和更新说明，不公开 App 源代码。
- 正式版本固定为 `versionName = "1.1.0"`、`versionCode = 2`，Release 标签为 `v1.1.0`，附件名为 `time-planning.apk`。
- 引导构建固定为 `versionName = "1.0"`、`versionCode = 1`，只用于首次覆盖安装，不上传为正式 Release。
- 包名保持 `com.example.birthdaycountdown`。
- 永久签名证书 SHA-256 必须为 `6BAE13563A55D51BCCDC76F15DCAE27D6F9C6A531624BF8CFD9D5883162C2B52`。
- 不实现静默安装、强制更新、私有 GitHub 仓库、App 内 GitHub 登录或记录导入导出。
- 移除 `fallbackToDestructiveMigration()`；缺少 Room 迁移时不得自动删除数据库。
- 当前目录不是 Git 仓库，不初始化 Git、不执行提交命令；每个任务使用测试、构建或证书检查作为完成证据。

## File Structure

- `app/src/main/java/com/example/birthdaycountdown/update/Versioning.kt`：本地版本和 Release 标签解析比较。
- `app/src/main/java/com/example/birthdaycountdown/update/GitHubReleaseParser.kt`：将 latest release JSON 转为可信远端元数据。
- `app/src/main/java/com/example/birthdaycountdown/update/UpdateChecker.kt`：HTTP 请求、24 小时检查策略和检查结果。
- `app/src/main/java/com/example/birthdaycountdown/update/UpdatePreferences.kt`：保存上次检查、忽略版本和下载任务。
- `app/src/main/java/com/example/birthdaycountdown/update/UpdateDownloader.kt`：DownloadManager 下载与进度查询。
- `app/src/main/java/com/example/birthdaycountdown/update/ApkVerifier.kt`：SHA-256、包名、版本号和签名证书校验。
- `app/src/main/java/com/example/birthdaycountdown/update/UpdateInstaller.kt`：未知来源权限与系统安装 Intent。
- `app/src/main/java/com/example/birthdaycountdown/update/UpdateUiState.kt`：更新状态的封闭类型。
- `app/src/main/java/com/example/birthdaycountdown/ui/UpdateUi.kt`：更新区域、弹窗、进度和安装授权交互。
- `tools/publish-update.ps1`：构建、校验和发布 GitHub Release。

---

### Task 1: 永久签名、版本和数据库安全

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/example/birthdaycountdown/MainActivity.kt`
- Create outside project: `C:/Users/杨/.time-planning/time-planning-release.keystore`
- Create outside project: `C:/Users/杨/.time-planning/keystore.properties`
- Create outside project: `D:/时间规划局密钥备份/time-planning-release.keystore`

**Interfaces:**
- Produces: Gradle property `bootstrapUpdate` controlling bootstrap versus official version.
- Produces: `BuildConfig.UPDATE_REPOSITORY_OWNER` and `BuildConfig.UPDATE_REPOSITORY_NAME`.
- Produces: Release builds signed by the existing certificate.

- [ ] **Step 1: 复制并核对永久签名**

Run:

```powershell
$primary = 'C:\Users\杨\.time-planning\time-planning-release.keystore'
$backup = 'D:\时间规划局密钥备份\time-planning-release.keystore'
New-Item -ItemType Directory -Force -Path (Split-Path $primary),(Split-Path $backup) | Out-Null
Copy-Item -LiteralPath 'C:\Users\杨\.android\debug.keystore' -Destination $primary -Force
Copy-Item -LiteralPath $primary -Destination $backup -Force
& "$env:JAVA_HOME\bin\keytool.exe" -list -v -keystore $primary -alias androiddebugkey -storepass android -keypass android
```

Expected: 输出 SHA-256 为 `6B:AE:13:56:3A:55:D5:1B:CC:DC:76:F1:5D:CA:E2:7D:6F:9C:6A:53:16:24:BF:8C:FD:9D:58:83:16:2C:2B:52`。

- [ ] **Step 2: 创建项目外签名配置**

使用 `apply_patch` 创建 `C:/Users/杨/.time-planning/keystore.properties`：

```properties
storeFile=C:/Users/杨/.time-planning/time-planning-release.keystore
storePassword=android
keyAlias=androiddebugkey
keyPassword=android
```

该文件不得复制进项目或公开更新仓库。

- [ ] **Step 3: 配置引导版和正式版版本号**

在 `app/build.gradle.kts` 的 `android` 块前读取：

```kotlin
val bootstrapUpdate = providers.gradleProperty("bootstrapUpdate").orNull == "true"
val updateOwner = providers.gradleProperty("TIME_PLANNING_GITHUB_OWNER").orNull.orEmpty()
val signingPropertiesFile = file("${System.getProperty("user.home")}/.time-planning/keystore.properties")
val signingProperties = java.util.Properties().apply {
    signingPropertiesFile.inputStream().use(::load)
}
```

将 `defaultConfig` 改为：

```kotlin
versionCode = if (bootstrapUpdate) 1 else 2
versionName = if (bootstrapUpdate) "1.0" else "1.1.0"
buildConfigField("String", "UPDATE_REPOSITORY_OWNER", "\"$updateOwner\"")
buildConfigField("String", "UPDATE_REPOSITORY_NAME", "\"time-planning-updates\"")
```

并启用 `buildFeatures { buildConfig = true }`。

- [ ] **Step 4: 显式配置 Release 签名**

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(signingProperties.getProperty("storeFile"))
        storePassword = signingProperties.getProperty("storePassword")
        keyAlias = signingProperties.getProperty("keyAlias")
        keyPassword = signingProperties.getProperty("keyPassword")
    }
}
buildTypes {
    release {
        isMinifyEnabled = false
        signingConfig = signingConfigs.getByName("release")
    }
}
```

- [ ] **Step 5: 移除自动清库兜底**

在 `MainActivity.kt` 删除 `.fallbackToDestructiveMigration()`，保留全部 `MIGRATION_1_2` 到 `MIGRATION_7_8`。

- [ ] **Step 6: 分别构建引导版和正式版并核对版本**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
& 'D:\Gradle\gradle-8.7\bin\gradle.bat' :app:assembleRelease -PbootstrapUpdate=true --no-daemon --max-workers=1 --console=plain
Copy-Item app\build\outputs\apk\release\app-release.apk app\build\outputs\apk\release\time-planning-bootstrap.apk -Force
& 'D:\Gradle\gradle-8.7\bin\gradle.bat' :app:assembleRelease --no-daemon --max-workers=1 --console=plain
```

Expected: 两次均 `BUILD SUCCESSFUL`，并生成引导 APK 与正式 APK。

- [ ] **Step 7: 验证两个 APK 的包名、版本和证书**

使用 Android SDK 最新 `aapt.exe` 与 `apksigner.bat`：

```powershell
$sdkRaw=((Get-Content local.properties | Where-Object { $_ -like 'sdk.dir=*' }) -replace '^sdk.dir=','')
$sdk=$sdkRaw.Replace('\:',':').Replace('\\','\')
$aapt=Get-ChildItem (Join-Path $sdk 'build-tools') -Recurse -Filter aapt.exe | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
$apksigner=Get-ChildItem (Join-Path $sdk 'build-tools') -Recurse -Filter apksigner.bat | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
& $aapt dump badging app\build\outputs\apk\release\time-planning-bootstrap.apk
& $aapt dump badging app\build\outputs\apk\release\app-release.apk
& $apksigner verify --print-certs app\build\outputs\apk\release\time-planning-bootstrap.apk
& $apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk
```

Expected: 引导版为 versionCode `1` / versionName `1.0`，正式版为 `2` / `1.1.0`；包名相同，两个证书 SHA-256 均等于永久指纹。

### Task 2: 版本比较和 GitHub Release 解析

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/update/Versioning.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/update/GitHubReleaseParser.kt`
- Create: `app/src/test/java/com/example/birthdaycountdown/update/VersioningTest.kt`
- Create: `app/src/test/java/com/example/birthdaycountdown/update/GitHubReleaseParserTest.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: `data class SemanticVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SemanticVersion>`
- Produces: `fun parseLocalVersion(value: String): SemanticVersion?`
- Produces: `fun parseReleaseTag(value: String): SemanticVersion?`
- Produces: `data class RemoteRelease(val version: SemanticVersion, val tag: String, val notes: String, val downloadUrl: String, val sizeBytes: Long, val digest: String)`
- Produces: `fun parseLatestRelease(json: String, assetName: String): RemoteRelease`

- [ ] **Step 1: 添加 JVM JSON 测试依赖**

在 dependencies 增加：

```kotlin
testImplementation("org.json:json:20240303")
```

- [ ] **Step 2: 写版本解析失败测试**

```kotlin
class VersioningTest {
    @Test fun localTwoPartVersionIsNormalized() {
        assertEquals(SemanticVersion(1, 0, 0), parseLocalVersion("1.0"))
    }

    @Test fun releaseTagRequiresThreeParts() {
        assertNull(parseReleaseTag("v1.1"))
        assertEquals(SemanticVersion(1, 1, 0), parseReleaseTag("v1.1.0"))
    }

    @Test fun newerVersionComparesHigher() {
        assertTrue(SemanticVersion(1, 1, 0) > SemanticVersion(1, 0, 9))
    }
}
```

- [ ] **Step 3: 写 Release 解析失败测试**

测试 JSON 必须包含 `tag_name`、`draft`、`prerelease`、`body` 与 asset 的 `name`、`browser_download_url`、`size`、`digest`。覆盖：正确解析、草稿拒绝、预发布拒绝、缺少指定 APK 拒绝、digest 非 `sha256:` 拒绝、同名 APK 超过一个拒绝。

```kotlin
@Test fun parsesTrustedReleaseAsset() {
    val release = parseLatestRelease(validJson, "time-planning.apk")
    assertEquals(SemanticVersion(1, 1, 0), release.version)
    assertEquals("sha256:abc123", release.digest)
    assertEquals(17263024L, release.sizeBytes)
}
```

- [ ] **Step 4: 运行测试源码编译并确认缺少实现**

Run: `D:\Gradle\gradle-8.7\bin\gradle.bat :app:compileDebugUnitTestKotlin --no-daemon --max-workers=1 --console=plain`

Expected: FAIL，仅因为上述类型和函数尚不存在。

- [ ] **Step 5: 实现最小版本类型**

`parseLocalVersion` 接受 `1.0` 或 `1.0.0`，`parseReleaseTag` 只接受 `v1.0.0`。所有段必须是非负整数，不接受前后附加文字。

- [ ] **Step 6: 实现严格 Release 解析**

```kotlin
data class RemoteRelease(
    val version: SemanticVersion,
    val tag: String,
    val notes: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val digest: String
)
```

`parseLatestRelease` 使用 `org.json.JSONObject` 严格检查已确认字段，失败时抛出 `InvalidReleaseException`，不得从多个资源中猜测 APK。

- [ ] **Step 7: 运行测试源码编译**

Run: `D:\Gradle\gradle-8.7\bin\gradle.bat :app:compileDebugUnitTestKotlin --no-daemon --max-workers=1 --console=plain`

Expected: `BUILD SUCCESSFUL`。

### Task 3: 更新检查策略、HTTP 请求和偏好设置

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/update/UpdateChecker.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/update/UpdatePreferences.kt`
- Create: `app/src/test/java/com/example/birthdaycountdown/update/UpdateCheckerTest.kt`

**Interfaces:**
- Produces: `interface UpdateHttpClient { suspend fun get(url: String): HttpResponse }`
- Produces: `data class HttpResponse(val statusCode: Int, val body: String)`
- Produces: `sealed interface UpdateCheckResult`
- Produces: `class UpdateChecker(httpClient: UpdateHttpClient)`
- Produces: `fun shouldAutoCheck(lastSuccessfulCheckMillis: Long, nowMillis: Long): Boolean`
- Produces: `class UpdatePreferences(prefs: SharedPreferences)`
- Produces: `data class SavedDownload(val id: Long, val tag: String, val version: String, val notes: String, val downloadUrl: String, val sizeBytes: Long, val digest: String)`

- [ ] **Step 1: 写 24 小时策略和检查结果测试**

```kotlin
@Test fun autoCheckWaitsTwentyFourHours() {
    assertFalse(shouldAutoCheck(1_000L, 1_000L + 23 * 60 * 60 * 1000))
    assertTrue(shouldAutoCheck(1_000L, 1_000L + 24 * 60 * 60 * 1000))
}

@Test fun returnsAvailableOnlyForNewerVersion() = runTest {
    val checker = UpdateChecker(FakeHttpClient(200, validJson))
    assertTrue(checker.check("owner", "time-planning-updates", "1.0") is UpdateCheckResult.Available)
}
```

增加 `testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")`。

- [ ] **Step 2: 运行测试源码编译并确认失败**

Expected: FAIL，因为检查接口和策略函数尚不存在。

- [ ] **Step 3: 实现标准 HTTP 客户端**

`UrlConnectionUpdateHttpClient` 在 `Dispatchers.IO` 使用 `HttpsURLConnection`，设置：

```text
Accept: application/vnd.github+json
X-GitHub-Api-Version: 2022-11-28
User-Agent: TimePlanning/1.1.0
Connect timeout: 15000 ms
Read timeout: 20000 ms
```

只接受 HTTPS，不自动携带 Token。响应体使用 UTF-8，连接始终关闭。

- [ ] **Step 4: 实现 UpdateChecker**

请求地址由以下精确格式生成：

```text
https://api.github.com/repos/{owner}/time-planning-updates/releases/latest
```

结果类型：

```kotlin
sealed interface UpdateCheckResult {
    data class Available(val release: RemoteRelease) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object NotConfigured : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}
```

owner 为空返回 `NotConfigured`；HTTP 200 解析并比较版本；403 返回“GitHub 请求次数受限，请稍后重试”；404 返回“更新仓库或 Release 尚未创建”；其他状态返回包含状态码的失败结果。

- [ ] **Step 5: 实现 UpdatePreferences**

使用现有 `settings` SharedPreferences，固定键：

```text
update_last_successful_check
update_snoozed_tag
update_snoozed_until
update_download_id
update_download_tag
update_download_digest
```

提供精确方法：`markSuccessfulCheck(nowMillis: Long)`、`snooze(tag: String, untilMillis: Long)`、`isSnoozed(tag: String, nowMillis: Long): Boolean`、`saveDownload(id: Long, release: RemoteRelease)`、`readDownload(): SavedDownload?`、`clearDownload()`。保存字段必须包含任务 ID、tag、version、notes、downloadUrl、sizeBytes 和 digest，使 App 进程重启后可以恢复下载、重试与安装界面。

- [ ] **Step 6: 编译测试源码**

Run: `D:\Gradle\gradle-8.7\bin\gradle.bat :app:compileDebugUnitTestKotlin --no-daemon --max-workers=1 --console=plain`

Expected: `BUILD SUCCESSFUL`。

### Task 4: APK 下载、完整性校验和安装引导

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/update/UpdateDownloader.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/update/ApkVerifier.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/update/UpdateInstaller.kt`
- Create: `app/src/test/java/com/example/birthdaycountdown/update/DigestVerifierTest.kt`
- Create: `app/src/main/res/xml/update_file_paths.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `data class DownloadProgress(val id: Long, val status: DownloadStatus, val downloadedBytes: Long, val totalBytes: Long, val localFile: File?)`
- Produces: `class UpdateDownloader(context: Context)`
- Produces: `sealed interface ApkVerificationResult`
- Produces: `class ApkVerifier(context: Context)`
- Produces: `class UpdateInstaller(context: Context)`

- [ ] **Step 1: 写 SHA-256 测试**

```kotlin
@Test fun sha256MatchesKnownBytes() {
    val file = temporaryFolder.newFile().apply { writeText("time-planning") }
    assertEquals("108445daec2258aa00016d28073cbb89cd99e92400f88d15b3b5b56211a4e51e", sha256(file))
}

@Test fun digestComparisonIgnoresHexCaseButNotPrefix() {
    assertTrue(matchesDigest(file, "sha256:${sha256(file).uppercase()}"))
    assertFalse(matchesDigest(file, "md5:${sha256(file)}"))
}
```

在写测试时先用 `Get-FileHash` 或 Java `MessageDigest` 计算完整期望值并写入测试，不保留省略号。

- [ ] **Step 2: 运行测试源码编译并确认失败**

Expected: FAIL，因为 `sha256` 和 `matchesDigest` 尚不存在。

- [ ] **Step 3: 实现 DownloadManager 包装**

`start(release)` 将文件保存到 `getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)/updates/time-planning.apk`，启动前删除旧文件。请求设置标题“时间规划局更新”、允许系统通知、只允许 HTTP 成功状态对应的 GitHub HTTPS 地址。

`query(downloadId)` 将 DownloadManager cursor 映射为 `PENDING`、`RUNNING`、`SUCCESSFUL`、`FAILED`、`MISSING`，cursor 必须关闭。`cancel(downloadId)` 同时移除任务和本地文件。

- [ ] **Step 4: 实现 APK 四重校验**

`ApkVerifier.verify(file, expectedDigest, currentVersionCode)` 依次验证：

1. `matchesDigest(file, expectedDigest)`。
2. archive packageName 等于 `context.packageName`。
3. archive longVersionCode 大于 `currentVersionCode`。
4. archive 签名证书 SHA-256 集合与当前安装 App 的证书集合相等。

API 28 及以上使用 `GET_SIGNING_CERTIFICATES`，API 26-27 使用 `GET_SIGNATURES`。结果类型明确区分 `DigestMismatch`、`PackageMismatch`、`VersionNotNewer`、`SignatureMismatch`、`UnreadableApk` 和 `Valid(file)`。

- [ ] **Step 5: 配置 FileProvider 和安装权限**

Manifest 增加：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

application 内增加非导出 FileProvider，authority 为 `${applicationId}.update-files`。`update_file_paths.xml` 只暴露 `external-files-path` 下的 `Download/updates/`。

Manifest provider 使用：

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.update-files"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/update_file_paths" />
</provider>
```

`update_file_paths.xml` 使用：

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-files-path name="updates" path="Download/updates/" />
</paths>
```

- [ ] **Step 6: 实现安装引导**

`UpdateInstaller` 提供：

```kotlin
fun canInstallPackages(): Boolean
fun permissionIntent(): Intent
fun installIntent(file: File): Intent
```

安装 Intent 使用 FileProvider content URI、`ACTION_VIEW`、MIME `application/vnd.android.package-archive`、`FLAG_GRANT_READ_URI_PERMISSION` 和 `FLAG_ACTIVITY_NEW_TASK`。

- [ ] **Step 7: 编译生产和测试源码**

Run: `D:\Gradle\gradle-8.7\bin\gradle.bat :app:compileDebugKotlin :app:compileDebugUnitTestKotlin --no-daemon --max-workers=1 --console=plain`

Expected: `BUILD SUCCESSFUL`。

### Task 5: 更新状态协调与 Compose 界面

**Files:**
- Create: `app/src/main/java/com/example/birthdaycountdown/update/UpdateUiState.kt`
- Create: `app/src/main/java/com/example/birthdaycountdown/ui/UpdateUi.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/AppViewModel.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/ui/Screens.kt`
- Modify: `app/src/main/java/com/example/birthdaycountdown/MainActivity.kt`

**Interfaces:**
- Produces: `sealed interface UpdateUiState`
- Produces: `AppViewModel.updateUiState: StateFlow<UpdateUiState>`
- Produces: `AppViewModel.checkForUpdate(manual: Boolean)`
- Produces: `AppViewModel.startUpdateDownload(release: RemoteRelease)`
- Produces: `AppViewModel.refreshUpdateDownload()`
- Produces: `AppViewModel.snoozeUpdate(release: RemoteRelease)`
- Produces: `@Composable fun UpdateDialogHost(state: UpdateUiState, onDownload: (RemoteRelease) -> Unit, onSnooze: (RemoteRelease) -> Unit, onRetry: () -> Unit, onInstallHandled: () -> Unit)`
- Produces: `@Composable fun SoftwareUpdateSection(versionName: String, state: UpdateUiState, onCheck: () -> Unit)`
- Produces: `AppNav(viewModel: AppViewModel, updateInstaller: UpdateInstaller, onRequestNotifications: () -> Unit)`

- [ ] **Step 1: 定义有限更新状态**

```kotlin
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val release: RemoteRelease) : UpdateUiState
    data class Downloading(val release: RemoteRelease, val progressPercent: Int?) : UpdateUiState
    data class ReadyToInstall(val release: RemoteRelease, val file: File) : UpdateUiState
    data class Error(val message: String, val release: RemoteRelease? = null) : UpdateUiState
}
```

- [ ] **Step 2: 将更新依赖注入 AppViewModel**

构造参数新增 `UpdateChecker`、`UpdatePreferences`、`UpdateDownloader`、`ApkVerifier`。自动检查先调用 `shouldAutoCheck`，手动检查始终请求；只有成功收到有效响应时记录检查时间。自动错误回到 Idle，手动错误进入 Error。初始化时读取 `SavedDownload`，存在时立即查询 DownloadManager 并恢复 Downloading、ReadyToInstall 或 Error 状态。

- [ ] **Step 3: 实现下载状态轮询和校验**

`startUpdateDownload` 保存 download id、tag、digest 并进入 Downloading。`refreshUpdateDownload` 查询进度；成功后调用 ApkVerifier，Valid 进入 ReadyToInstall，失败则删除 APK、清除偏好并进入 Error。

- [ ] **Step 4: 在 AppNav 启动自动检查**

```kotlin
LaunchedEffect(Unit) {
    viewModel.checkForUpdate(manual = false)
}
```

当状态为 Downloading 时每 750ms 刷新一次，离开前台后停止高频轮询，返回前台后恢复查询。

- [ ] **Step 5: 扩展“我的”页面**

`ProfileScreen` 增加“软件更新”全宽区域，显示 `BuildConfig.VERSION_NAME`、当前状态和“检查更新”按钮。Checking 时按钮禁用并显示进度；UpToDate 显示“已是最新版本”；Error 显示简短错误和重试按钮。

- [ ] **Step 6: 实现可选更新弹窗**

Available 状态显示版本、更新说明、格式化文件大小、“立即更新”和“以后再说”。以后再说保存 tag 和 `now + 24h`，并回到 Idle。

- [ ] **Step 7: 实现安装授权与系统安装页**

`AppNav` 接收 `UpdateInstaller`。`UpdateDialogHost` 在 ReadyToInstall 时检查 `UpdateInstaller.canInstallPackages()`。未授权时使用 `rememberLauncherForActivityResult(StartActivityForResult())` 打开 `permissionIntent()`；返回后重新检查，授权成功则启动 `installIntent(file)`，拒绝则显示“未获得安装权限，可稍后重试”。

- [ ] **Step 8: 在 MainActivity 组装依赖**

使用 applicationContext 创建 `UpdatePreferences`、`UpdateChecker(UrlConnectionUpdateHttpClient())`、`UpdateDownloader`、`ApkVerifier` 和 `UpdateInstaller`。repository owner 读取 `BuildConfig.UPDATE_REPOSITORY_OWNER`，不得硬编码 GitHub Token。调用 `AppNav(viewModel, updateInstaller, onRequestNotifications)`。

- [ ] **Step 9: 编译 Compose 页面**

Run: `D:\Gradle\gradle-8.7\bin\gradle.bat :app:compileDebugKotlin --no-daemon --max-workers=1 --console=plain`

Expected: `BUILD SUCCESSFUL`。

### Task 6: GitHub 账号、更新仓库和发布脚本

**Files:**
- Create: `tools/publish-update.ps1`
- Create: `release-notes/1.1.0.md`
- Modify outside project: `C:/Users/杨/.gradle/gradle.properties`

**Interfaces:**
- Produces: public repository `"$(gh api user --jq .login)/time-planning-updates"`.
- Produces: Gradle property `TIME_PLANNING_GITHUB_OWNER` whose value is read from `gh api user --jq .login`.
- Produces: `tools/publish-update.ps1 -Version 1.1.0 -NotesFile release-notes/1.1.0.md`

- [ ] **Step 1: 完成 GitHub 人工注册**

打开 GitHub 注册页，用户亲自设置账号、验证邮箱并完成人机验证。不要让工具读取或保存用户密码、邮箱验证码。

- [ ] **Step 2: 安装并登录 GitHub CLI**

若 `gh --version` 不存在，运行：

```powershell
winget install --id GitHub.cli --exact --source winget
```

然后运行 `gh auth login --web --git-protocol https`，用户在浏览器确认授权。验证 `gh auth status` 成功。

- [ ] **Step 3: 创建公开更新仓库和首个提交**

```powershell
$owner = gh api user --jq .login
gh repo create "$owner/time-planning-updates" --public --description '时间规划局 Android 安装包更新'
$readme = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("# 时间规划局更新`n`n本仓库仅用于发布官方 Android 安装包。`n"))
gh api -X PUT "repos/$owner/time-planning-updates/contents/README.md" -f message='docs: initialize update repository' -f content=$readme
```

Expected: 仓库为 public，README 明确只用于安装包更新。

- [ ] **Step 4: 保存 owner 到用户 Gradle 配置**

PowerShell 读取 `gh api user --jq .login`，在 `C:/Users/杨/.gradle/gradle.properties` 更新或追加唯一一行：

```powershell
$owner = gh api user --jq .login
$gradleProperties = 'C:\Users\杨\.gradle\gradle.properties'
$existing = if (Test-Path $gradleProperties) { Get-Content $gradleProperties | Where-Object { $_ -notmatch '^TIME_PLANNING_GITHUB_OWNER=' } } else { @() }
Set-Content -LiteralPath $gradleProperties -Value @($existing + "TIME_PLANNING_GITHUB_OWNER=$owner") -Encoding utf8
```

脚本必须先删除文件中旧的同名行再追加，避免重复配置。随后运行 Debug 构建，并检查 `app/build/generated/source/buildConfig/debug/com/example/birthdaycountdown/BuildConfig.java` 中 `UPDATE_REPOSITORY_OWNER` 等于 `$owner`。

- [ ] **Step 5: 写 1.1.0 更新说明**

`release-notes/1.1.0.md` 内容固定包含：

```markdown
# 1.1.0

- 新增软件内部检查和下载更新。
- 新增安装包完整性、包名和签名校验。
- 加强数据库升级保护，更新时保留已有记录和设置。
```

- [ ] **Step 6: 实现发布脚本参数和前置检查**

脚本参数：

```powershell
param(
    [Parameter(Mandatory=$true)][ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version,
    [Parameter(Mandatory=$true)][string]$NotesFile
)
```

脚本检查 `gh auth status`、notes 文件、Gradle 配置版本、永久 keystore、目标仓库和 Release 标签不存在。任一失败使用 `throw` 停止。

- [ ] **Step 7: 实现构建、验签和上传**

脚本使用 `D:\Gradle\gradle-8.7\bin\gradle.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain`，将输出复制到临时目录并命名 `time-planning.apk`。使用 aapt 验证包名与版本，使用 apksigner 验证固定证书指纹，使用 `Get-FileHash -Algorithm SHA256` 输出 digest。

上传命令：

```powershell
gh release create "v$Version" $apkPath --repo "$owner/time-planning-updates" --title "时间规划局 v$Version" --notes-file $NotesFile --latest
```

- [ ] **Step 8: 上传后回读验证**

脚本调用：

```powershell
$release = gh api "repos/$owner/time-planning-updates/releases/latest" | ConvertFrom-Json
```

检查 tag、draft、prerelease、asset 名称、大小和 `digest` 与本地文件一致。失败时报告 Release URL 和差异，不删除已经上传的 Release，等待人工判断。

- [ ] **Step 9: 使用 PowerShell 语法检查脚本**

Run:

```powershell
$errors=$null
[System.Management.Automation.Language.Parser]::ParseFile((Resolve-Path 'tools\publish-update.ps1'), [ref]$null, [ref]$errors) | Out-Null
if ($errors.Count) { $errors | Format-List; exit 1 }
```

Expected: exit code 0，无语法错误。

### Task 7: 引导覆盖安装和真实应用内升级验证

**Files:**
- Verify: `app/build/outputs/apk/release/time-planning-bootstrap.apk`
- Verify: `app/build/outputs/apk/release/app-release.apk`
- Output: public GitHub Release `v1.1.0`

**Interfaces:**
- Consumes: Tasks 1-6 全部输出。
- Produces: 手机从现有 1.0 无损进入正式 1.1.0，并具备后续内部更新能力。

- [ ] **Step 1: 编译生产代码和单元测试源码**

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
& 'D:\Gradle\gradle-8.7\bin\gradle.bat' :app:compileDebugKotlin :app:compileDebugUnitTestKotlin --no-daemon --max-workers=1 --console=plain
```

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 2: 尝试执行完整单元测试**

```powershell
& 'D:\Gradle\gradle-8.7\bin\gradle.bat' :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain
```

Expected: 全部测试通过。若仍出现本机已知 `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`，记录完整错误并明确说明测试执行器未启动，不能表述为测试通过。

- [ ] **Step 3: 连接手机并读取当前安装签名**

用户开启 USB 调试并授权电脑。运行：

```powershell
adb devices
$remote = (adb shell pm path com.example.birthdaycountdown | Select-String 'base.apk').ToString().Split(':',2)[1].Trim()
adb pull $remote phone-current.apk
$sdkRaw=((Get-Content local.properties | Where-Object { $_ -like 'sdk.dir=*' }) -replace '^sdk.dir=','')
$sdk=$sdkRaw.Replace('\:',':').Replace('\\','\')
$apksigner=Get-ChildItem (Join-Path $sdk 'build-tools') -Recurse -Filter apksigner.bat | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
& $apksigner verify --print-certs phone-current.apk
```

Expected: 手机已安装 APK 的 SHA-256 等于永久指纹。若不一致，立即停止，不安装、不卸载、不清除数据。

- [ ] **Step 4: 记录升级前业务状态**

在手机中记录至少两张现有卡片的名称、日期、排序、颜色和底部导航设置，截屏保存为升级验证证据。不得通过清除数据制造测试环境。

- [ ] **Step 5: 构建并覆盖安装引导版**

```powershell
& 'D:\Gradle\gradle-8.7\bin\gradle.bat' :app:assembleRelease -PbootstrapUpdate=true --no-daemon --max-workers=1 --console=plain
adb install -r app\build\outputs\apk\release\app-release.apk
```

Expected: 安装成功，App 仍显示版本 `1.0`，Task 4 记录的卡片和设置全部存在，“我的”中已出现软件更新区域。

- [ ] **Step 6: 发布正式 1.1.0**

```powershell
powershell -ExecutionPolicy Bypass -File tools\publish-update.ps1 -Version 1.1.0 -NotesFile release-notes\1.1.0.md
```

Expected: GitHub latest release 为 `v1.1.0`，资源为 `time-planning.apk`，digest 与本地 APK 相同。

- [ ] **Step 7: 在引导版内检查并下载更新**

打开手机 App，进入“我的”，点击“检查更新”。确认显示 `1.1.0`、更新说明和正确大小；点击“立即更新”，观察系统通知和 App 进度。

- [ ] **Step 8: 验证安装权限和正式覆盖安装**

首次无权限时确认 App 打开正确的“允许安装未知应用”系统页。授权后返回，确认系统安装页面显示“时间规划局”并执行更新，不卸载旧 App。

- [ ] **Step 9: 核对版本和记录完整性**

安装完成后运行：

```powershell
adb shell dumpsys package com.example.birthdaycountdown | Select-String 'versionCode|versionName'
```

Expected: versionCode `2`、versionName `1.1.0`。逐项对照升级前截图，卡片数量、名称、日期、排序、颜色和底部导航设置全部一致。

- [ ] **Step 10: 验证异常路径**

使用本地测试或临时伪造元数据分别验证：无网络、无新版、无效标签、错误 digest、错误包名、错误签名、取消下载、拒绝安装权限和取消系统安装。每种情况均不得卸载当前 App、删除 Room 数据库或启动不可信 APK。

- [ ] **Step 11: 保存最终产物信息**

记录正式 APK 的绝对路径、文件大小、SHA-256、版本号、包名、签名指纹和 GitHub Release 页面。最终交付必须明确区分：自动化测试结果、构建结果和真机手工验证结果。
