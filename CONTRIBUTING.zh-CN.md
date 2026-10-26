# 参与 CleanDash 开发

[English](CONTRIBUTING.md) | 简体中文

感谢你的帮助。欢迎提交问题报告、更多车机的适配、新主题和各种修复。

## 开始之前

- **发现问题，或者你的车机用不了？** 请用表单提交 issue：表单会询问车机型号、芯片、Android 版本和屏幕分辨率，这些是复现问题必需的信息。
- **较大的改动**（新功能、新主题、不同的布局方式）？请先开一个 issue 讨论，避免白费功夫。
- **小修复**（错别字、包名、崩溃）：直接提交 pull request 即可。

## CleanDash 坚持的原则

这些是项目的规则，违反其中任何一条的 pull request 都不会被合并：

- **不申请联网权限，不做统计，不做跟踪。** CleanDash 只打开已经安装的应用。
- **纯 Java，只用系统自带的控件。** 应用里不用 AndroidX、不用 Kotlin、不用第三方库。它要在内存小、速度慢的车机上也流畅运行。
- **绝不停用、隐藏或卸载用户的应用。** 原厂桌面必须一直都在，随时可以换回去。
- **用户主动选择之前，不成为主屏幕。** 主屏幕入口默认关闭，只有点 *Set as Home* 才会打开。
- **保持 APK 小巧。** 字体只保留拉丁字符和固定字重（见 `NOTICE.md`）；能用矢量图的地方不用位图。
- **第三方内容必须使用兼容的许可证**，列在 `NOTICE.md` 中；许可证要求附带全文的，放在 `licenses/` 里。

## 编译

需要 JDK 17 和 Android SDK（platform 34、build-tools 34）。通过 `ANDROID_HOME` 环境变量或 `local.properties` 文件（`sdk.dir=…`，不要提交）告诉 Gradle SDK 的位置。

```sh
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

这条命令会运行单元测试、lint（必须是 0 个错误），并生成 `app/build/outputs/apk/release/app-release.apk`。GitHub 会对每个 pull request 运行同样的命令。

## 测试

- **在模拟器上：** 创建一个 Android 10（API 29）、1280×720、density 160 的虚拟设备，和 TS18 一样。用 `adb shell wm size 1024x600` 检查其他分辨率（之后用 `wm size reset` 恢复）。
- **在车机上：** 参考 [README](README.zh-CN.md) 中"开发者选项和 USB 调试"一节开启无线 ADB，然后运行 `adb install -r app-release.apk`。从应用列表打开 CleanDash 即可，不需要设为主屏幕。
- **任何界面改动：** 至少在 1024×600 和 1280×720 上检查，并在 PR 中附上改动前后的截图。

## 代码在哪里

| 你想要…… | 看这里 |
|---|---|
| 适配其他车机的自带应用 | `Tiles.java`：把包名或 `包名/Activity` 加到对应的图块里，并把包名加到 `AndroidManifest.xml` 的 `<queries>` 中 |
| 修改图块查找应用的方式 | `Tiles.resolve()` 和 `Keywords.java`（在 `KeywordsTest.java` 中加测试） |
| 添加主题 | 一个布局 `res/layout/home_<名称>.xml`，`res/values/themes.xml` 中的 `Theme.CleanDash.<名称>` 样式，`colors.xml` 中的颜色，以及 `HomeTheme.java` 中的一行 |
| 修改正在播放 | `NowPlaying.java`（读取媒体会话）和 `MainActivity.bindMedia()` |

**主屏幕布局的工作方式：** 任何设置了 `android:tag` 为图块 id（`radio`、`music`、`bluetooth`、`maps`、`carplay`、`settings`、`eq`、`apps`）的控件都是一个图块。图块里 tag 为 `icon`、`label` 和 `hint` 的控件会被自动填充。同一个图块可以在布局中出现两次。时钟容器的 id 是 `clock`，主屏幕按钮是 `set_home`，媒体卡片的各部分是 `media_card`、`media_art`、`media_title`、`media_artist`、`media_state`、`media_source`、`media_progress`、`media_prev`、`media_play` 和 `media_next`，媒体相关的都可以省略。复制一个现有的 `home_*.xml` 再修改即可。

如果主题在 1024×600 屏幕上放不下，把尺寸放到 `res/values/dimens.xml`（紧凑）和 `res/values-h640dp/dimens.xml`（TS18 及更大），参考 Night Halo 的做法。

## 代码风格

与周围的代码保持一致：4 空格缩进，注释简短并说明"为什么"，命名直白易读。每个 pull request 只做一件事。提交说明写清改了什么、为什么改。

## 许可证

CleanDash 使用 [MIT 许可证](LICENSE)。提交 pull request 即表示你同意你的贡献也以该许可证发布。
