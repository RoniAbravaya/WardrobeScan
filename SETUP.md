# WardrobeScan — Project Setup Guide

This file contains step-by-step instructions for setting up the project after cloning.
It is written so an AI code builder (Claude Code, Cursor, Copilot, etc.) can execute every step automatically.

---

## Prerequisites — Install These First (one-time, manual)

Before asking the AI to run the setup steps, make sure the following are installed on your machine:

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Latest stable | https://developer.android.com/studio |
| Java JDK | 17 or 21 | https://adoptium.net |
| Node.js | 22.x | https://nodejs.org |
| Firebase CLI | Latest | `npm install -g firebase-tools` |
| Git | Any | https://git-scm.com |

Android Studio installs the Android SDK automatically. Note the SDK path after installation (usually `C:\Users\<you>\AppData\Local\Android\Sdk` on Windows or `~/Library/Android/sdk` on macOS).

---

## Credentials You Must Obtain Before Setup

You need **two** external credentials. Obtain them before running the AI setup steps:

### 1. OpenWeather API Key (free)
1. Go to https://openweathermap.org and create a free account
2. Go to your profile → **API Keys**
3. Copy the default key (or generate a new one)

### 2. Firebase Access
The Firebase project `wardrobescan-app` already exists.
You need to be added as a member **or** have the `google-services.json` file shared with you.

**Option A — Firebase team member (preferred):**
Ask the project owner to add your Google account at:
https://console.firebase.google.com/project/wardrobescan-app/settings/iam

Then run `firebase login` when prompted during setup.

**Option B — Direct file share:**
Ask the project owner to send you the `google-services.json` file and place it at `app/google-services.json` manually. Skip the Firebase login steps below.

---

## AI Setup Steps — Paste This Block to Your AI Code Builder

> Tell your AI: "Execute every numbered step in this file in order."

---

### Step 1 — Create `local.properties`

Create the file `local.properties` in the project root with the following content.
Replace `YOUR_SDK_PATH` with your actual Android SDK path and `YOUR_WEATHER_KEY` with your OpenWeather API key.

**Windows example:**
```
sdk.dir=C\:/Users/YourName/AppData/Local/Android/Sdk
WEATHER_API_KEY=YOUR_WEATHER_KEY
```

**macOS/Linux example:**
```
sdk.dir=/Users/yourname/Library/Android/sdk
WEATHER_API_KEY=YOUR_WEATHER_KEY
```

---

### Step 2 — Log in to Firebase CLI

Run:
```bash
firebase login
```

This opens a browser. Sign in with the Google account that has access to the `wardrobescan-app` Firebase project.

Verify access by running:
```bash
firebase projects:list
```

You should see `wardrobescan-app` in the list.

---

### Step 3 — Set the active Firebase project

Run:
```bash
firebase use wardrobescan-app
```

---

### Step 4 — Download `google-services.json`

Find the Android app ID by running:
```bash
firebase apps:list --project wardrobescan-app
```

Then download the config file (replace `APP_ID` with the ID from the command above):
```bash
firebase apps:sdkconfig android APP_ID --out app/google-services.json
```

Verify the file exists at `app/google-services.json`.

---

### Step 5 — Install Firebase Functions dependencies

Run:
```bash
cd functions
npm install
cd ..
```

---

### Step 6 — Deploy Firestore security rules

Run:
```bash
firebase deploy --only firestore:rules
```

Expected output: `Deploy complete!`

---

### Step 7 — Deploy Storage security rules

Run:
```bash
firebase deploy --only storage
```

Expected output: `Deploy complete!`

---

### Step 8 — Deploy Cloud Functions

Run:
```bash
firebase deploy --only functions --force
```

Expected output: `Deploy complete!` with `refineClothingTags` listed as deployed.

If you see a Node.js version error, make sure `functions/package.json` has `"node": "22"` under `engines`.

---

### Step 9 — Enable Firebase Authentication providers

This step must be done manually in the Firebase Console (cannot be done via CLI):

1. Go to https://console.firebase.google.com/project/wardrobescan-app/authentication/providers
2. Enable **Email/Password**
3. Enable **Google** (set a support email when prompted)

---

### Step 10 — Build the Android app

Run from the project root:
```bash
./gradlew assembleDebug
```

**Windows users:** use `gradlew.bat assembleDebug` if the above fails.

Expected output: `BUILD SUCCESSFUL`

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

---

### Step 11 — Verify setup is complete

Run:
```bash
./gradlew assembleDebug && echo "BUILD OK"
firebase projects:list | grep wardrobescan-app
```

Both commands should succeed. If `BUILD OK` prints and `wardrobescan-app` appears, the setup is complete.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `google-services.json` not found | Re-run Step 4, verify the file is at `app/google-services.json` |
| `WEATHER_API_KEY` is empty | Check `local.properties` — key must be on its own line with no spaces |
| `sdk.dir` not found | Check Android SDK path in `local.properties` — use forward slashes |
| Firebase functions deploy fails with Node error | Ensure `functions/package.json` has `"node": "22"` |
| Build fails with `compileSdk 35` error | Open Android Studio → SDK Manager → install Android 15 (API 35) |
| `firebase login` says non-interactive | Run `firebase login --interactive` or use `firebase login:ci` for CI environments |
