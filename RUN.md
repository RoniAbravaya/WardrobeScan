# WardrobeScan — How to Run the App

Complete the steps in `SETUP.md` before following this guide.

---

## Run on the Emulator (Windows PowerShell)

### Step 1 — Start the emulator

Open a terminal in VS Code and run:

```powershell
& "C:\Users\ADMIN\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_36.0
```

Wait ~20 seconds until the phone screen appears.

### Step 2 — Install and launch the app

Open a **second terminal** (click the `+` icon in the VS Code terminal panel) and run:

```powershell
.\gradlew installDebug
```

The app will install and open automatically on the emulator.

---

## Run on a Physical Android Phone

### 1. Enable USB Debugging on your phone
1. Go to **Settings → About Phone**
2. Tap **Build Number** 7 times until "Developer mode enabled" appears
3. Go to **Settings → Developer Options**
4. Turn on **USB Debugging**

### 2. Connect the phone and install the app

Plug your phone in via USB, then run:

```powershell
.\gradlew installDebug
```

---

## First Launch

1. Open **WardrobeScan** on the device
2. Sign up with **email + password** or **Google sign-in**
3. Complete the short onboarding
4. Grant **Camera** and **Location** permissions when asked
5. Tap the scan button to photograph a clothing item

---

## Rebuild After Code Changes

```powershell
.\gradlew installDebug
```

---

## Useful Commands (PowerShell)

| What | Command |
|------|---------|
| Build APK only | `.\gradlew assembleDebug` |
| Build + install | `.\gradlew installDebug` |
| Clean build cache | `.\gradlew clean` |
| See connected devices | `& "C:\Users\ADMIN\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices` |
| Uninstall app | `& "C:\Users\ADMIN\AppData\Local\Android\Sdk\platform-tools\adb.exe" uninstall com.wardrobescan.app` |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Unexpected token '-avd'` error | Make sure you have `&` at the start of the emulator command |
| `'&&' is not a valid statement separator` | Run each command separately — do not use `&&` in PowerShell |
| `adb: no devices` | Start the emulator first, wait for it to fully boot, then run installDebug |
| App crashes on launch | Share the error — run `.\gradlew installDebug` again and check the output |
| Camera black screen on emulator | Emulator camera is limited — use a physical device for camera features |
| Google sign-in fails | Make sure **Google** provider is enabled in Firebase Auth Console |
