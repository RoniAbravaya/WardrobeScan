# WardrobeScan — How to Run the App

Complete the steps in `SETUP.md` before following this guide.

---

## Option A — Run on a Physical Android Phone (Easiest)

### 1. Enable USB Debugging on your phone
1. Go to **Settings → About Phone**
2. Tap **Build Number** 7 times until "Developer mode enabled" appears
3. Go to **Settings → Developer Options**
4. Turn on **USB Debugging**

### 2. Connect the phone and install the app

Plug your phone into the computer via USB, then run:

```bash
./gradlew installDebug
```

The app (`WardrobeScan`) will appear on your phone. Open it.

---

## Option B — Run on an Emulator (No physical device needed)

### 1. Create a virtual device in Android Studio
1. Open Android Studio
2. Go to **Device Manager** (right panel or **View → Tool Windows → Device Manager**)
3. Click **+** → **Create Virtual Device**
4. Choose **Pixel 6** (or any phone with a camera)
5. Select **API 35** system image (download it if prompted)
6. Click **Finish**

### 2. Start the emulator and install the app

Start the emulator from Device Manager, then run:

```bash
./gradlew installDebug
```

---

## Option C — Install the APK directly (fastest, no build needed)

If the app has already been built, just install the APK:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

> `adb` is in your Android SDK → `platform-tools` folder. Make sure it's on your PATH, or use the full path.

---

## First Launch

1. Open **WardrobeScan** on the device
2. Sign up with **email + password** or **Google sign-in**
3. Complete the short onboarding (2 screens)
4. Grant **Camera** and **Location** permissions when asked
5. You're in — tap the scan button to photograph a clothing item

---

## Rebuild After Code Changes

Any time you change Kotlin/XML source files, rebuild and reinstall:

```bash
./gradlew installDebug
```

That's it — Gradle handles everything.

---

## Useful Commands

| What | Command |
|------|---------|
| Build APK only (no install) | `./gradlew assembleDebug` |
| Build + install on connected device | `./gradlew installDebug` |
| Run unit tests | `./gradlew test` |
| Clean build cache | `./gradlew clean` |
| See connected devices | `adb devices` |
| View app logs live | `adb logcat -s WardrobeScan` |
| Uninstall from device | `adb uninstall com.wardrobescan.app` |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `adb: no devices` | Reconnect USB, check USB debugging is on, try a different cable |
| App crashes on launch | Run `adb logcat` and look for `FATAL EXCEPTION` — share the stack trace |
| Camera black screen on emulator | Emulator camera support is limited — use a physical device for camera features |
| Google sign-in fails | Make sure **Google** provider is enabled in Firebase Auth Console |
| Weather not loading | Check your `WEATHER_API_KEY` in `local.properties` and rebuild |
| `installDebug` says "device unauthorized" | Tap "Allow" on the phone screen when the USB debugging dialog appears |
