# Changelog

## 1.5.2 - 2026-03-29
- Home 顶部区域增加状态栏安全间距与额外顶边距，修复标题与状态栏重叠问题。
- 调整圆环中心时长文本字号，减少视觉压迫。
- 调整圆环外侧应用图标轨道半径并缩小图标尺寸，避免图标贴环过近。
- 新增设置项“显示应用包名”：
  - 可开关首页应用列表中的包名显示，
  - 同步作用于 7 天页面应用列表。
- Version bump: `versionName` 1.5.1 -> 1.5.2, `versionCode` 7 -> 8.

## 1.5.1 - 2026-03-29
- Fixed major Chinese text rendering issue caused by wrong resource file encoding (GBK -> UTF-8) in `strings.xml`.
- Cleaned up weekly legend text and ensured all Home/Weekly/Settings labels render correctly under Chinese locale.
- Version bump: `versionName` 1.5.0 -> 1.5.1, `versionCode` 6 -> 7.
## 1.5.0 - 2026-03-29
- Refactored app navigation to remove the bottom tab bar and switch to dedicated routes:
  - Home,
  - 7-day usage page,
  - Settings,
  - About.
- Reworked Home to a near 1:1 layout requested by design reference:
  - reduced center ring size by about one third,
  - ring-side icons now follow each segment's real usage proportion,
  - added five metric items under the ring (fragmentation, usage duration, launches, unlocks, notifications),
  - tapping "Usage duration" opens the 7-day usage page,
  - compact app list with higher density to show more apps per screen,
  - app icons no longer use extra framed background blocks.
- Rebuilt the weekly page into a 7-day usage detail page with chart + app ranking list and bottom date range.
- Added custom background image feature:
  - choose/clear background image in Settings,
  - persisted URI storage,
  - applied to both Home and 7-day pages.
- Improved startup performance path by removing repeated resume-triggered loading and consolidating usage refresh to a single snapshot query flow.
- Improved app label resolution logic with stronger launcher/activity label fallback when package-suffix labels (e.g. `mm`) are encountered.
- Renamed app display name:
  - Chinese: `看什么`,
  - English: `KanWhat`.
- Version bump: `versionName` 1.4.0 -> 1.5.0, `versionCode` 5 -> 6.

## 1.4.0 - 2026-03-29
- Rebuilt the Today screen into a ring-centered layout:
  - top bar shows app name and current version,
  - settings icon on the right opens Settings,
  - app list moved below the center ring,
  - bottom date navigator added with previous/next day switching.
- Added ring-side app icons around the usage ring (icons only, no names or percentages).
- Added per-day data browsing on Home with direct UsageStats query for selected date.
- Added configurable right-side list metric in Settings:
  - Usage time,
  - Launch count,
  - Usage percentage.
- Added launch-count calculation from UsageEvents and usage-percentage calculation per selected day.
- Restored status bar visibility on app launch (removed forced status-bar hiding).
- Version bump: `versionName` 1.3.0 -> 1.4.0, `versionCode` 4 -> 5.

## 1.3.0 - 2026-03-29
- Improved app name/icon resolution by using normalized package IDs plus launcher-app metadata lookup, reducing fallback names like `mm`.
- Added Android 11+ package visibility support (`QUERY_ALL_PACKAGES`) to improve package label and icon resolution reliability.
- Added Home header app branding update: app name shown at top-left with current version below it.
- Added settings for usage filtering:
  - Exclude system apps toggle.
  - Custom excluded package list with batch edit (newline/comma separated) and validation.
- Applied user-defined filters to Today, Weekly, and widget cache refresh pipelines.
- Added debug APK archive behavior so each versioned APK is copied to `apk-history/debug` and older versions are kept.
- Version bump: `versionName` 1.2.0 -> 1.3.0, `versionCode` 3 -> 4.

## 1.2.0 - 2026-03-29
- Fixed app name resolution for UsageStats process package names by normalizing package IDs (for example `com.tencent.mm:tools` -> `com.tencent.mm`) before label lookup.
- Updated the Today app list row to show both the app display name and the full package name.
- Added debug APK versioned filename output: `app-debug-v<versionName>-<versionCode>.apk`.
- Version bump: `versionName` 1.1.0 -> 1.2.0, `versionCode` 2 -> 3.

## 1.1.0 - 2026-03-29
- Fixed Today screen issue where only the current app appeared in usage list by relaxing package filtering.
- Fixed missing status bar on app launch by disabling forced fullscreen and removing `enableEdgeToEdge()`.
- Added release note process and changelog file.
- Version bump: `versionName` 1.0 -> 1.1.0, `versionCode` 1 -> 2.



