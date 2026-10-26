# CleanDash

English | [简体中文](README.zh-CN.md)

A clean, bloat-free Android launcher for aftermarket car head units: the Topway TS10/TS18, FYT and Microntek units
sold on AliExpress.

Inspired by [cleanyourtv](https://github.com/Gathin23/cleanyourtv).

## Screenshots

**Glass Cockpit**

![Glass Cockpit theme: clock and now playing on the left, tile grid with a wide Maps tile on the right](docs/screenshots/glass.png)

**Instrument**

![Instrument theme: large condensed clock, amber media panel, row of eight keys](docs/screenshots/instrument.png)

**Driver Rail (light)**

![Driver Rail theme: white icon rail on the left, big clock, now playing card and a blue CarPlay card](docs/screenshots/rail.png)

These three are from the Android emulator at the TS18's 1280×720, with a demo track playing and stand-in apps so
every tile has something to open. This one is from a real Topway TS18 (Android 10), with the head unit's own status
bar on top and nothing playing:

![Driver Rail theme running on a real TS18 head unit](docs/screenshots/ts18-head-unit.png)

## What it does

One screen: a large clock and date, what's playing, and big tiles for **Radio, Music, Bluetooth, Maps, CarPlay,
Settings, EQ** and **All apps**. The tiles open the apps already on your head unit. CleanDash does not replace them.

Three looks, switched by holding the clock → *Theme*:

- **Glass Cockpit:** dark, with the clock and now playing on the left and a tile grid with a wide Maps tile.
- **Instrument:** black and amber, a big condensed clock, an LCD-style media panel and a row of eight keys.
- **Driver Rail (light):** a day theme with an icon rail on the left, a big clock, now playing and a CarPlay card.

Everything else works the same in every theme:

- Each tile finds its app on its own. It tries the head unit's built-in screens first (Topway `com.tw.*`, FYT
  `com.syu.*`, Microntek), then any app whose name matches ("ZLink", "CarPlay", "FM Radio"…), then Android's
  standard handler.
- **Hold a tile** to choose a different app for it. *Automatic* goes back to the built-in search.
- A tile whose app isn't installed is dimmed. Tap it or hold it to choose one.
- **Now playing** shows the track, artist and cover from whichever app is playing, with play/pause, previous and
  next. Tapping it opens that app. It needs notification access, which Android requires to read other apps' media:
  tap the card and allow CleanDash, or run
  `adb shell cmd notification allow_listener io.github.donnyyu.cleandash/io.github.donnyyu.cleandash.MediaListener`.
  CleanDash doesn't read the notifications themselves.
- **Hold the clock** for the theme, Home screen settings, or to reset every tile to Automatic.
- **All apps** lists everything installed. Hold an app there to open its App info.
- No internet permission, no tracking, no AndroidX, no Kotlin. Plain Java with the platform's own views.

## Install

1. Build: `./gradlew assembleRelease` (JDK 17 and the Android SDK, platform 34). The APK is
   `app/build/outputs/apk/release/app-release.apk`.
2. Copy the APK to a USB stick and open it with the head unit's file manager, or use
   `adb install app-release.apk` over USB debugging.
3. Open **CleanDash** from the app list and try it as a normal app first.
4. When you're happy, tap **Set as Home** and pick CleanDash.

CleanDash only becomes a Home screen after step 4: its Home entry ships switched off.

## Developer options and USB debugging (Topway TS18)

TS18 firmware locks both behind passwords:

- **Developer options:** `7890` followed by the current hour in 24-hour format, from the head unit's clock. At
  10:xx it is `789010`; at 15:xx it is `789015`.
- **USB debugging:** `john@tw-desktop`
- **Factory settings** (in the car settings app): `123456`

The USB port on many of these units only works as a host, so a USB cable to a computer may not give you ADB. Use ADB
over Wi-Fi instead, switched on from the head unit itself:

1. Car settings → Factory settings (`123456`) → Engineer Mode → Debug&Log → ADB shell.
2. Type `setprop service.adb.tcp.port 5555` and press Start.
3. In Developer options, turn USB debugging off and on again, so ADB restarts on port 5555.
4. From the computer, on the same Wi-Fi: `adb connect <head unit IP>:5555`, then accept the prompt on the screen.

The setting goes back to off after a reboot. To keep ADB over Wi-Fi on after reboots, run
`adb shell setprop persist.adb.tcp.port 5555` once while connected; `adb shell setprop persist.adb.tcp.port ""` undoes
it. Port 5555 has no encryption, so anyone on the same Wi-Fi can try to connect.

Engineer Mode can also be opened by dialling `*#*#83781#*#*`. Sources: [StarMusa88's Topway ADB guide](https://github.com/StarMusa88/TopWay-Android-Headunit-ADB-Guide) and
[electronics101clt's UIS7862 notes](https://github.com/electronics101clt/uis7862a-1h10-headunit).

## Going back

Android Settings → Apps → Default apps → Home app → pick the stock launcher. You can also hold CleanDash's clock and
choose *Home screen settings*. CleanDash never disables or uninstalls anything, so the stock launcher is always there.

## Status

Early. Runs on a Topway TS18 (Unisoc SC9863A, Android 10, 1280×720) and the Android emulator. The CleanDash screens
are in English for now.

## License

[MIT](LICENSE). Third-party parts are listed in [NOTICE.md](NOTICE.md).
