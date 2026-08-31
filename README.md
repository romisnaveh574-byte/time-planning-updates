# 生日倒计时 Android App

这是一个离线优先的 Android App 原型，使用 Kotlin、Jetpack Compose 和 Room。每张卡片可以单独使用阳历或阴历，阴历支持闰月，并显示另一种历法的换算日期。

## 构建

1. 安装 Android Studio（包含 JDK 和 Android SDK）。
2. 用 Android Studio 打开本目录并等待 Gradle 同步完成。
3. 直接从 Android Studio 运行 `app`，或在 Android Studio 的终端执行 `gradlew :app:assembleDebug`。

最低支持 Android 8.0（API 26）。Android 13 及以上首次开启提醒时需要授予通知权限。
