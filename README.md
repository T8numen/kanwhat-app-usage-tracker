# 看什么 (KanWhat)

看什么是一个 Android 使用统计应用，用于查看每日与近 7 天的应用使用情况，并支持按包名过滤、系统应用排除、自定义背景图与多语言切换。

## 主要功能
- 今日页圆环统计（含应用图标跟随占比）
- 应用使用排行榜（可切换右侧指标：使用时长 / 启动次数 / 占比）
- 7 天使用时长页面（柱状图 + 应用排行）
- 日期左右切换查看历史天数据
- 排除系统应用与自定义包名排除
- 自定义背景图（主页与 7 天页面）
- 中英文切换

## 隐私说明
- 数据主要来源于 Android `UsageStatsManager`
- 不上传个人使用数据到云端
- 使用统计及配置保存在本地

## 构建
```bash
./gradlew :app:assembleDebug
```

Debug APK 默认输出（带版本号）：
- `app/build/outputs/apk/debug/app-debug-v<versionName>-<versionCode>.apk`

历史 Debug APK 归档目录：
- `apk-history/debug/`

## 发布约定
每次版本更新都执行以下动作：
1. 更新 `app/build.gradle.kts` 中版本号。
2. 更新 `CHANGELOG.md`。
3. 构建 debug APK。
4. 将对应 APK 保留在 `apk-history/debug/`。
5. 推送源码到 GitHub。
6. 创建 GitHub Release，并上传该版本 APK 资产。

## 版本资产
- `app-debug-v1.3.0-4.apk`
- `app-debug-v1.4.0-5.apk`
- `app-debug-v1.5.0-6.apk`
- `app-debug-v1.5.1-7.apk`

> 以上 APK 同步保存在仓库目录 `apk-history/debug/`，并在 GitHub Releases 中提供下载。

## 当前版本
- Version Name: `1.5.1`
- Version Code: `7`

## 开发环境
- Android Studio（Hedgehog+）
- Kotlin + Jetpack Compose
- Room + DataStore + WorkManager

## 许可证
仅供学习与个人使用。

## GitHub Releases
- [v1.3.0](https://github.com/T8numen/kanwhat-app-usage-tracker/releases/tag/v1.3.0)
- [v1.4.0](https://github.com/T8numen/kanwhat-app-usage-tracker/releases/tag/v1.4.0)
- [v1.5.0](https://github.com/T8numen/kanwhat-app-usage-tracker/releases/tag/v1.5.0)
- [v1.5.1](https://github.com/T8numen/kanwhat-app-usage-tracker/releases/tag/v1.5.1)
