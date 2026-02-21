# 👔 WardrobeScan

AI-powered wardrobe management app that scans, categorizes, and organizes your clothing items using your phone's camera.

---

## ✨ Features

- **📸 Smart Clothing Scan** — Photograph clothing items and let ML Kit + MediaPipe automatically identify and tag them
- **🏷️ AI Tag Refinement** — Firebase Cloud Functions refine clothing tags for better accuracy
- **🌦️ Weather-Based Suggestions** — Get outfit recommendations based on your local weather (OpenWeather API)
- **🔐 Authentication** — Sign in with Email/Password or Google account
- **☁️ Cloud Sync** — All wardrobe data synced securely via Firebase Firestore & Storage
- **📍 Location Awareness** — Uses GPS for weather-based outfit suggestions

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **DI** | Hilt (Dagger) |
| **Camera** | CameraX |
| **ML** | ML Kit Image Labeling, MediaPipe Vision |
| **Backend** | Firebase (Auth, Firestore, Storage, Cloud Functions, Crashlytics) |
| **Networking** | Retrofit + OkHttp |
| **Image Loading** | Coil |
| **Weather** | OpenWeather API |
| **Async** | Kotlin Coroutines + Flow |

## 📋 Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Latest stable |
| Java JDK | 17 or 21 |
| Node.js | 22.x |
| Firebase CLI | Latest |
| Git | Any |

## 🚀 Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/RoniAbravaya/WardrobeScan.git
   cd WardrobeScan
   ```

2. **Follow the setup guide**
   See [SETUP.md](SETUP.md) for complete step-by-step instructions.

3. **Run the app**
   See [RUN.md](RUN.md) for emulator and device instructions.

## 📁 Project Structure

```
WardrobeScan/
├── app/
│   └── src/main/java/com/wardrobescan/app/
│       ├── data/          # Data layer (repositories, data sources)
│       ├── di/            # Hilt dependency injection modules
│       ├── domain/        # Domain layer (use cases, models)
│       ├── ml/            # ML Kit & MediaPipe integration
│       ├── ui/            # Jetpack Compose UI (screens, components, theme)
│       └── util/          # Utility classes and extensions
├── functions/             # Firebase Cloud Functions (Node.js)
├── firestore.rules        # Firestore security rules
├── storage.rules          # Firebase Storage security rules
└── firebase.json          # Firebase project configuration
```

## 🔧 Build Commands

| Action | Command |
|--------|---------|
| Build APK | `.\gradlew assembleDebug` |
| Build + Install | `.\gradlew installDebug` |
| Clean | `.\gradlew clean` |

## 📄 License

This project is for educational and personal use.
