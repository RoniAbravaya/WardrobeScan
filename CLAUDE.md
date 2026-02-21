# CLAUDE.md — WardrobeScan

This file is the authoritative guide for AI assistants working on this codebase.
Read it fully before making any changes.

---

## Project Overview

**WardrobeScan** is an Android app (Kotlin + Jetpack Compose) that lets users photograph clothing items, automatically identify and categorize them using on-device ML, and receive weather-aware outfit suggestions.

**Firebase project ID:** `wardrobescan-app`
**App ID (package):** `com.wardrobescan.app`
**Min SDK:** 26 (Android 8.0) | **Target/Compile SDK:** 35 | **JVM:** 17

---

## Repository Structure

```
WardrobeScan/
├── app/                                      # Android application module
│   ├── build.gradle.kts                      # App-level Gradle config (deps, build config)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml           # Permissions, single Activity declaration
│       │   ├── assets/
│       │   │   └── selfie_multiclass_256x256.tflite  # MediaPipe segmentation model (bundled)
│       │   └── java/com/wardrobescan/app/
│       │       ├── WardrobeScanApp.kt         # @HiltAndroidApp Application class
│       │       ├── MainActivity.kt            # Single activity; hosts NavGraph
│       │       ├── data/
│       │       │   ├── model/                 # Pure Kotlin data models
│       │       │   │   ├── AnalysisResult.kt
│       │       │   │   ├── ClothingCategory.kt  # enum: TOP, BOTTOM, OUTERWEAR, DRESS, SHOES, ACCESSORY
│       │       │   │   ├── ClothingItem.kt    # Firestore document model (main wardrobe entity)
│       │       │   │   ├── DominantColor.kt
│       │       │   │   ├── Outfit.kt          # + Occasion enum (CASUAL, WORK, GOING_OUT)
│       │       │   │   ├── UserProfile.kt
│       │       │   │   └── WeatherData.kt     # + computed properties (isRainy, isCold, isHot…)
│       │       │   ├── remote/
│       │       │   │   ├── WeatherApiService.kt  # Retrofit interface for OpenWeather API
│       │       │   │   └── dto/
│       │       │   │       └── WeatherResponse.kt
│       │       │   └── repository/
│       │       │       ├── AuthRepository.kt     # Firebase Auth (email, Google sign-in)
│       │       │       ├── WardrobeRepository.kt # Firestore CRUD + real-time Flow for items
│       │       │       ├── OutfitRepository.kt   # Firestore CRUD + real-time Flow for outfits
│       │       │       ├── StorageRepository.kt  # Firebase Storage (original + cutout uploads)
│       │       │       └── WeatherRepository.kt  # OpenWeather API via Retrofit
│       │       ├── di/
│       │       │   └── AppModule.kt           # Hilt @Module: provides all singletons
│       │       ├── domain/
│       │       │   └── OutfitSuggestionEngine.kt  # Pure rule-based outfit suggestions (no Android deps)
│       │       ├── ml/
│       │       │   ├── ClothingAnalyzer.kt    # Orchestrates full 4-step analysis pipeline
│       │       │   ├── ImageSegmenter.kt      # MediaPipe: segments clothing from background
│       │       │   ├── ImageLabeler.kt        # ML Kit: labels the segmented image
│       │       │   ├── LabelCategoryMapper.kt # Maps ML Kit label strings → ClothingCategory
│       │       │   └── ColorExtractor.kt      # Extracts dominant colors via Palette API
│       │       ├── ui/
│       │       │   ├── navigation/
│       │       │   │   └── NavGraph.kt        # All Compose routes declared here (Routes object)
│       │       │   ├── screen/                # One file per screen (Composable functions)
│       │       │   │   ├── AuthScreen.kt
│       │       │   │   ├── HomeScreen.kt
│       │       │   │   ├── ItemDetailScreen.kt
│       │       │   │   ├── OnboardingScreen.kt
│       │       │   │   ├── OutfitScreen.kt
│       │       │   │   ├── ScanScreen.kt
│       │       │   │   ├── SettingsScreen.kt
│       │       │   │   └── WardrobeScreen.kt
│       │       │   ├── theme/
│       │       │   │   ├── Color.kt
│       │       │   │   ├── Theme.kt
│       │       │   │   └── Type.kt
│       │       │   └── viewmodel/             # @HiltViewModel — one per screen
│       │       │       ├── AuthViewModel.kt
│       │       │       ├── HomeViewModel.kt
│       │       │       ├── ItemDetailViewModel.kt
│       │       │       ├── OutfitViewModel.kt
│       │       │       ├── ScanViewModel.kt
│       │       │       ├── SettingsViewModel.kt
│       │       │       └── WardrobeViewModel.kt
│       │       └── util/
│       │           └── Analytics.kt           # Thin wrapper over Firebase Analytics
│       └── test/
│           └── java/com/wardrobescan/app/
│               ├── domain/
│               │   └── OutfitSuggestionEngineTest.kt
│               └── ml/
│                   ├── ColorExtractorTest.kt
│                   └── LabelCategoryMapperTest.kt
├── functions/                                 # Firebase Cloud Functions (Node.js 22)
│   ├── index.js                               # refineClothingTags callable function
│   └── package.json
├── gradle/
│   ├── libs.versions.toml                     # Version catalog — all dependency versions here
│   └── wrapper/
├── build.gradle.kts                           # Root Gradle config (plugins only)
├── settings.gradle.kts
├── gradle.properties
├── firestore.rules                            # Users can only read/write their own data
├── storage.rules                              # Same ownership rule for Storage
├── firebase.json
├── .firebaserc                                # Default Firebase project: wardrobescan-app
├── SETUP.md                                   # One-time project setup guide
└── RUN.md                                     # How to build and run on emulator/device
```

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.1.0 |
| UI | Jetpack Compose (BOM 2024.12.01) + Material 3 |
| Architecture | MVVM + Repository |
| DI | Hilt 2.54 (KSP) |
| Navigation | Compose Navigation 2.8.5 |
| Camera | CameraX 1.4.1 |
| On-device ML | MediaPipe Tasks Vision 0.10.20 + ML Kit Image Labeling 17.0.9 |
| Color extraction | AndroidX Palette |
| Backend | Firebase (Auth, Firestore, Storage, Functions, Analytics, Crashlytics) |
| Weather API | OpenWeather API via Retrofit 2.11.0 + OkHttp 4.12.0 |
| Image loading | Coil 2.7.0 |
| Local storage | DataStore Preferences 1.1.2 |
| Location | Google Play Services Location 21.3.0 |
| Permissions | Accompanist Permissions 0.36.0 |
| Testing | JUnit 4, MockK 1.13.13, Turbine 1.2.0, kotlinx-coroutines-test |
| Cloud Functions | Node.js 22, firebase-functions v7, firebase-admin v13 |

---

## Architecture

### MVVM + Unidirectional Data Flow

```
Screen (Composable)
  ↓ user events (lambda callbacks)
ViewModel (@HiltViewModel)
  ↓ calls repository / ML layer
Repository / ClothingAnalyzer
  ↓ returns Result<T> or Flow<T>
ViewModel updates StateFlow<UiState>
  ↑ observed by Screen via collectAsStateWithLifecycle()
```

**Rules:**
- Screens are stateless — they only render `UiState` and call ViewModel methods.
- ViewModels hold a single `MutableStateFlow<XxxUiState>` exposed as `StateFlow`.
- Repositories return `Result<T>` for one-shot operations and `Flow<T>` for real-time streams.
- Firestore real-time listeners are exposed as `callbackFlow { ... awaitClose { } }`.
- Never call Firebase or ML Kit directly from a screen or ViewModel — go through repositories and the ML layer.

### Dependency Injection

All singletons are provided in `AppModule` (`di/AppModule.kt`):
- `FirebaseAuth`, `FirebaseFirestore` (with offline persistence enabled), `FirebaseStorage`, `FirebaseFunctions`, `FirebaseAnalytics`
- `OkHttpClient` (with BASIC logging interceptor)
- `WeatherApiService` (Retrofit, base URL `https://api.openweathermap.org/`)

Repositories and ML classes use `@Singleton` + `@Inject constructor`.
ViewModels use `@HiltViewModel` + `@Inject constructor`.

### Navigation

All routes are defined in `Routes` object in `NavGraph.kt`:

| Route constant | Path | Screen |
|---|---|---|
| `Routes.ONBOARDING` | `onboarding` | `OnboardingScreen` |
| `Routes.AUTH` | `auth` | `AuthScreen` |
| `Routes.HOME` | `home` | `HomeScreen` |
| `Routes.SCAN` | `scan` | `ScanScreen` |
| `Routes.WARDROBE` | `wardrobe` | `WardrobeScreen` |
| `Routes.ITEM_DETAIL` | `item/{itemId}` | `ItemDetailScreen` |
| `Routes.OUTFITS` | `outfits` | `OutfitScreen` |
| `Routes.SETTINGS` | `settings` | `SettingsScreen` |

Start destination is determined at runtime based on onboarding state and auth state.

---

## ML Pipeline

`ClothingAnalyzer.analyze(bitmap)` orchestrates four sequential steps:

1. **Segmentation** (`ImageSegmenter`) — MediaPipe `selfie_multiclass_256x256.tflite` model (bundled in `assets/`). Produces a cutout `Bitmap` with transparent background (category 0 = background, everything else = keep).
2. **Labeling** (`ImageLabeler`) — ML Kit Image Labeling on the cutout.
3. **Category mapping** (`LabelCategoryMapper`) — Maps label strings to `ClothingCategory` enum. Case-insensitive, whitespace-trimmed. Returns the first matching label and its `ClothingCategory`.
4. **Color extraction** (`ColorExtractor`) — Extracts dominant colors from the cutout using AndroidX Palette.

**Confidence threshold:** `ClothingAnalyzer.CONFIDENCE_THRESHOLD = 0.6f`
If confidence is below this, `ScanUiState.needsManualCategory = true` and the UI prompts the user to confirm or change the detected category.

The `ImageSegmenter` is lazily initialized and must be closed (called from `ScanViewModel.onCleared()`).

---

## Data Models

### `ClothingItem` (Firestore document)

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | `@DocumentId` — auto-assigned |
| `userId` | `String` | Firebase UID of owner |
| `category` | `String` | Lowercase name of `ClothingCategory` |
| `subcategory` | `String` | ML Kit label that matched (e.g., "jacket") |
| `labels` | `List<String>` | All ML Kit labels from the scan |
| `colors` | `List<DominantColor>` | Dominant colors extracted by Palette |
| `imageUrl` | `String` | Firebase Storage URL (original photo) |
| `cutoutUrl` | `String` | Firebase Storage URL (transparent cutout PNG) |
| `season` | `String` | "all" / "spring" / "summer" / "autumn" / "winter" |
| `warmthScore` | `Int` | 1 (very light) – 5 (very warm) |
| `waterproof` | `Boolean` | |
| `breathable` | `Boolean` | |
| `userNotes` | `String` | Free-text notes from user |
| `confidence` | `Float` | ML confidence (0–1) |
| `createdAt` | `Timestamp?` | `@ServerTimestamp` |
| `updatedAt` | `Timestamp?` | `@ServerTimestamp` |

`categoryEnum: ClothingCategory?` is a computed `@get:Exclude` property — not stored in Firestore.

### `ClothingCategory` enum

Values: `TOP`, `BOTTOM`, `OUTERWEAR`, `DRESS`, `SHOES`, `ACCESSORY`.
Use `ClothingCategory.fromString(value)` for deserialization.
Store as `category.name.lowercase()` when writing to Firestore.

### `WeatherData`

Computed boolean properties (used by `OutfitSuggestionEngine`):
- `isRainy`: condition in `["rain", "drizzle", "thunderstorm"]`
- `isSnowy`: condition == `"snow"`
- `isCold`: temperature < 10°C
- `isHot`: temperature > 28°C
- `isWindy`: windSpeed > 10 m/s

### `Outfit`

References clothing items by `itemIds: List<String>` (Firestore document IDs). Stores `occasion`, `rating` (0 = unrated, 1–5), `saved`, and `weatherSummary`.

---

## Firestore Schema

```
users/{userId}/
  items/{itemId}       ← ClothingItem documents
  outfits/{outfitId}   ← Outfit documents
```

**Security rules** (`firestore.rules`): Authenticated users can only read/write their own `users/{userId}/**` subtree.
**Storage rules** (`storage.rules`): Same ownership constraint on `users/{userId}/**`.

Firestore is configured with **offline persistence enabled** and unlimited cache size (see `AppModule.provideFirestore()`).

---

## Outfit Suggestion Engine

`OutfitSuggestionEngine.suggest(items, weather, occasion)` is a pure Kotlin class (no Android dependencies, fully unit-testable).

**Rules:**
- Returns at most 3 outfit suggestions.
- **Strategy 1:** Top + Bottom combinations (with optional outerwear if cold/rainy, shoes, accessories).
- **Strategy 2:** Dress combinations (only if weather is not cold and strategy 1 produced fewer than 3).
- **Weather filters:**
  - Cold (<10°C): items must have `warmthScore >= 4`. Falls back to all items if too strict.
  - Hot (>28°C): items must have `warmthScore <= 2` and `breathable = true`. Falls back if too strict.
  - Rainy: prefer `waterproof` shoes and `waterproof` outerwear.
- **Accessory selection:** umbrella/rain hat for rain; scarf/gloves/beanie for cold; sunglasses/hat for hot.

---

## Firebase Cloud Functions

**Location:** `functions/index.js` (Node.js 22)

| Function | Trigger | Purpose |
|---|---|---|
| `refineClothingTags` | HTTPS callable | Server-side tag refinement (MVP placeholder for Vertex AI) |

`refineClothingTags` verifies auth, verifies item ownership, and currently marks the item as `refined: true`. The comment in the function shows the intended Vertex AI integration pattern to replace the placeholder.

To deploy:
```bash
cd functions && npm install
firebase deploy --only functions --force
```

---

## Build & Development Workflow

### Prerequisites

- Android Studio (latest stable)
- JDK 17 or 21
- Node.js 22.x (for Cloud Functions)
- Firebase CLI (`npm install -g firebase-tools`)

### One-time Setup

See `SETUP.md` for the full setup sequence. Key steps:

1. Create `local.properties` in the project root:
   ```
   sdk.dir=/path/to/Android/Sdk
   WEATHER_API_KEY=your_openweather_api_key
   ```
2. Place `app/google-services.json` (download from Firebase Console or via `firebase apps:sdkconfig`).
3. Install functions dependencies: `cd functions && npm install`
4. Deploy Firebase rules and functions (see SETUP.md steps 6–8).

### Common Gradle Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build + install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Clean build cache
./gradlew clean
```

On Windows, use `gradlew.bat` or `.\gradlew` in PowerShell.

### Configuration

`WEATHER_API_KEY` is read from `local.properties` and injected as `BuildConfig.WEATHER_API_KEY`. It is **never** committed to source control (`.gitignore` covers `local.properties` and `google-services.json`).

### Dependency Management

All dependency versions are managed in `gradle/libs.versions.toml` (Version Catalog). When adding a dependency:
1. Add the version to `[versions]`
2. Add the library reference to `[libraries]`
3. Reference it in `app/build.gradle.kts` as `libs.xxx`

---

## Testing

Unit tests live in `app/src/test/`. Current coverage:
- `LabelCategoryMapperTest` — verifies label-to-category mapping (case-insensitive, whitespace handling, unknown labels)
- `ColorExtractorTest` — verifies color extraction behavior
- `OutfitSuggestionEngineTest` — comprehensive tests for all weather scenarios and outfit strategies

**Test conventions:**
- Use backtick test names: `` fun `rainy weather prefers waterproof outerwear`() ``
- Use MockK for mocking (`io.mockk:mockk`)
- Use Turbine for `Flow` testing (`app.cash.turbine:turbine`)
- Use `kotlinx-coroutines-test` for coroutine testing

**Tests that can be unit-tested** (no Android framework): `LabelCategoryMapper`, `ColorExtractor`, `OutfitSuggestionEngine`.
Everything touching Android, Firebase, or CameraX requires instrumented (Espresso/UI) tests, which are not yet written.

Run tests:
```bash
./gradlew test
```

---

## Key Conventions

### Kotlin / Compose

- **UiState pattern:** Every ViewModel exposes a single `data class XxxUiState` as a `StateFlow`. All state mutations create a new copy via `.copy(...)`.
- **Error handling:** ViewModels store `error: String?` in their UiState. Clear the error after it is displayed.
- **`Result<T>`:** All repository suspend functions return `Result<T>`. Use `.getOrThrow()` inside `try/catch` in ViewModels, or `.fold(onSuccess, onFailure)`.
- **Coroutines:** Use `viewModelScope.launch` in ViewModels. Repositories use `Dispatchers.Default` for CPU-bound work (ML), `Dispatchers.IO` is handled by Firebase/Retrofit internally.
- **Navigation:** Pass navigation callbacks (lambdas) down to screens — never give screens access to `NavController` directly.
- **`collectAsStateWithLifecycle()`:** Always use this (not `collectAsState()`) to respect lifecycle.

### Firestore

- Collection path for items: `users/{uid}/items/{itemId}`.
- Collection path for outfits: `users/{uid}/outfits/{outfitId}`.
- Models use default no-arg values so Firestore can deserialize them (`@DocumentId` on `id`).
- Timestamp fields use `@ServerTimestamp` — pass `null` and Firestore fills them in.
- Store category as `category.name.lowercase()` (e.g., `"top"`, `"outerwear"`).

### Firebase Storage

Images are stored under `users/{uid}/originals/` and `users/{uid}/cutouts/` (see `StorageRepository`).

### Analytics

Use the `Analytics` class (injected via Hilt) — never call `FirebaseAnalytics` directly. Current events: `scan_started`, `scan_success`, `scan_manual_fix`, `outfit_shown`, `outfit_saved`, `outfit_liked`.

### Adding a New Screen

1. Add a route constant to `Routes` in `NavGraph.kt`.
2. Create `ui/screen/NewScreen.kt` with a `@Composable fun NewScreen(...)`.
3. Create `ui/viewmodel/NewViewModel.kt` with `@HiltViewModel`.
4. Add the `composable(Routes.NEW_SCREEN)` block to `NavGraph`.
5. Wire navigation callbacks in `NavGraph`.

### Adding a New Repository

1. Create `data/repository/NewRepository.kt` with `@Singleton` + `@Inject constructor`.
2. Add any new Firebase/network clients it needs to `AppModule`.
3. Hilt will automatically discover it — no `@Provides` needed for the repository itself (only for its constructor parameters if they're not already provided).

---

## Permissions

Declared in `AndroidManifest.xml`:
- `CAMERA` — required (camera feature declared as `required="true"`)
- `INTERNET`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_NETWORK_STATE`

Runtime permission requests are handled with Accompanist Permissions (`com.google.accompanist:accompanist-permissions`).

---

## Known Limitations / Planned Work

- **`refineClothingTags` Cloud Function** is a placeholder — replace the body with actual Vertex AI / Cloud Vision API calls.
- **Google Sign-In** in `AuthScreen` has a placeholder comment — the Activity result flow for the Google Sign-In intent is not yet wired.
- **Instrumented tests** (UI / integration) are not written yet.
- The segmentation model (`selfie_multiclass_256x256.tflite`) is optimized for person/selfie segmentation — clothing-only segmentation accuracy varies.
- Camera preview is limited on the Android emulator; use a physical device for camera features.
