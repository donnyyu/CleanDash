# Contributing to CleanDash

English | [简体中文](CONTRIBUTING.zh-CN.md)

Thanks for helping. Bug reports, support for more head units, new themes and fixes are all welcome.

## Before you start

- **A bug or a head unit that doesn't work?** Open an issue with the form: it asks for the model, chip, Android
  version and screen size, which is what we need to reproduce anything.
- **A big change** (a new feature, a new theme, a different layout system)? Open an issue first to talk it through,
  so your work doesn't go to waste.
- **Small fixes** (typos, a package name, a crash): go straight to a pull request.

## What CleanDash stays

These are the project's rules. A pull request that breaks one won't be merged:

- **No internet permission, no analytics, no tracking.** CleanDash only opens apps that are already installed.
- **Plain Java and the platform's own views.** No AndroidX, no Kotlin, no third-party libraries in the app. It has
  to run well on slow head units with little memory.
- **Never disable, hide or uninstall the user's apps.** The stock launcher must always stay there to go back to.
- **Home stays off until the user asks.** The HOME entry ships disabled; only *Set as Home* turns it on.
- **Keep the APK small.** Fonts are cut down to Latin and fixed weights (see `NOTICE.md`); no bitmaps where a vector
  drawable will do.
- **Third-party content needs a compatible licence**, listed in `NOTICE.md`, with the licence text in `licenses/` when
  it asks for that.

## Building

You need JDK 17 and the Android SDK (platform 34, build-tools 34). Point Gradle at the SDK with `ANDROID_HOME` or a
`local.properties` file (`sdk.dir=…`, not committed).

```sh
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

That runs the unit tests, lint (it must report 0 errors) and builds `app/build/outputs/apk/release/app-release.apk`.
GitHub runs the same command on every pull request.

## Testing

- **On the emulator:** make an Android 10 (API 29) virtual device at 1280×720, density 160, like a TS18. Check other
  sizes with `adb shell wm size 1024x600` (then `wm size reset`).
- **On a head unit:** see "Developer options and USB debugging" in the [README](README.md) for ADB over Wi-Fi, then
  `adb install -r app-release.apk`. Open CleanDash from the app list; you don't need to make it the Home screen.
- **Any visual change:** check it at 1024×600 and 1280×720 at least, and put before/after screenshots in the PR.

## Where things are

| You want to… | Look at |
|---|---|
| Support another head unit's built-in apps | `Tiles.java`: add its package or `package/Activity` names to the right tile, and the package to `<queries>` in `AndroidManifest.xml` |
| Change how a tile finds its app | `Tiles.resolve()` and `Keywords.java` (with a test in `KeywordsTest.java`) |
| Add a theme | A layout `res/layout/home_<name>.xml`, a `Theme.CleanDash.<Name>` style in `res/values/themes.xml`, colours in `colors.xml`, and one line in `HomeTheme.java` |
| Change now playing | `NowPlaying.java` (reading media sessions) and `MainActivity.bindMedia()` |

**How a Home layout works:** each tile is any view with `android:tag` set to a tile id (`radio`, `music`,
`bluetooth`, `maps`, `carplay`, `settings`, `eq`, `apps`). Inside it, the views tagged `icon`, `label` and `hint` are
filled in for you. A layout may show a tile twice. The clock container has the id `clock`, the Home button `set_home`,
and the media card's parts `media_card`, `media_art`, `media_title`, `media_artist`, `media_state`, `media_source`,
`media_progress`, `media_prev`, `media_play` and `media_next`; all of the media ones are optional. Copy an existing
`home_*.xml` and change it.

If a theme doesn't fit a 1024×600 screen, put the sizes in `res/values/dimens.xml` (compact) and
`res/values-h640dp/dimens.xml` (TS18-sized and up), as Night Halo does.

## Code style

Match the code around you: 4-space indent, short comments that say *why*, names that read plainly. Keep pull requests
to one change each. Write commit messages that say what changed and why.

## Licence

CleanDash is under the [MIT License](LICENSE). By sending a pull request you agree that your contribution is released
under it too.
