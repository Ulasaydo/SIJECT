# SIJECT

SIJECT is an Android application for real-time sign language recognition and sentence translation. The app opens the camera, extracts MediaPipe landmarks from the signer, feeds a 30-frame landmark sequence into a TensorFlow Lite model, smooths the prediction output, translates accepted words into a sentence through a backend API, and reads the translated sentence aloud with Android Text-to-Speech.

The current repository is an MVP-focused Android project. It is ready for team development, device testing, model validation, backend integration, and performance tuning.

## Current Status

MVP completion is roughly 90%+. The main Android pipeline is implemented and builds successfully.

Implemented:

- CameraX preview and frame analyzer.
- Front/back camera selection from settings.
- MediaPipe Tasks landmark extraction.
- Landmark overlay for debug inspection.
- TensorFlow Lite model loading and tensor shape validation.
- 30-frame circular buffer for sequence inference.
- Top-3 prediction display with confidence.
- Smoothing before accepting words.
- Retrofit backend integration for translation and health check.
- Offline fallback when backend is unavailable.
- Automatic and manual Text-to-Speech playback.
- Copy translated sentence action.
- Local word history screen with RecyclerView.
- Unit tests and basic instrumented tests.
- Documentation under `docs/`.

Recently improved:

- Landmark overlay orientation was corrected for camera rotation.
- Analysis resolution and MediaPipe GPU delegate were tuned for low-end Android devices.
- TFLite inference is throttled to keep the camera test flow usable.

Known limitations:

- The physical-device FPS observed on Samsung A21s is around 6-9 FPS, not the long-term target of 25+ FPS.
- Pose and face landmarkers are currently disabled in fast mode; hand landmarks remain active and missing pose/face values are zero-filled.
- Model accuracy cannot be considered validated until the training landmark order, labels, and real device behavior are confirmed with the model owner.
- Backend URL must be configured per developer when testing on a physical phone.

## Architecture

The project follows a lightweight MVVM and layered Android structure.

```text
app/src/main/java/com/signlanguage/translator/
├── data/
│   ├── model/          Request, response, prediction, translation, history models
│   └── repository/     Local storage, remote API, app settings, repository facade
├── domain/
│   ├── services/       CameraX, MediaPipe, TFLite, TTS, API, frame buffer, processors
│   └── usecases/       Frame processing, smoothing, word translation
├── ui/
│   ├── activities/     Main activity and permission helpers
│   ├── fragments/      Translation, history, settings screens
│   ├── viewmodels/     UI state and screen logic
│   └── views/          Landmark overlay, confidence bar, word display
└── utils/              Constants, logging, tensors, permissions, performance helpers
```

Main runtime flow:

```text
CameraX ImageProxy
  -> MediaPipeService
  -> LandmarkProcessor
  -> FrameBufferManager
  -> TFLiteService
  -> PredictionProcessor
  -> SmoothedPredictionUseCase
  -> TranslateWordsUseCase
  -> TranslationFragment UI
  -> TextToSpeechService
```

## Model And Landmark Contract

Bundled assets live under:

```text
app/src/main/assets/
```

Required files:

- `sign_language_model.tflite`
- `labels.txt`
- `landmark_indices.json`
- `hand_landmarker.task`
- `pose_landmarker.task`
- `face_landmarker.task`

Current model contract:

```text
Input  = [1, 30, 1629]
Output = [1, 250]
```

The input means:

```text
30 frames x 543 landmarks x 3 coordinates = 30 x 1629
```

Expected landmark order:

```text
pose_33, face_468, left_hand_21, right_hand_21
```

Important validation work still needed:

- Confirm `landmark_indices.json` exactly matches the training-time landmark order.
- Confirm `labels.txt` order matches model output indices.
- Resolve why the model outputs 250 classes while the current `labels.txt` contains 20 human-readable labels.
- Test real gestures against the model owner’s expected labels and confidence ranges.

## Backend API

Default backend URL:

```text
http://10.0.2.2:8000/
```

This works for Android Emulator only. On a physical Android device, each developer must create or update local `local.properties`:

```properties
BACKEND_URL=http://<backend-machine-lan-ip>:8000/
```

Example:

```properties
BACKEND_URL=http://192.168.1.20:8000/
```

Expected backend endpoints:

- `GET /api/health`
- `POST /api/translate`

If backend is offline, the app falls back to joining accepted words locally.

## Development Setup

Requirements:

- Android Studio with JDK bundled.
- Android SDK installed.
- Android device or emulator for camera/instrumented testing.
- Git.

Clone:

```bash
git clone https://github.com/Ulasaydo/SIJECT.git
cd SIJECT
```

Open the project in Android Studio. Android Studio should create `local.properties` automatically with the local SDK path.

Build debug APK:

```bash
./gradlew :app:assembleDebug
```

If the terminal cannot find Java on macOS, use Android Studio's bundled JDK:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

Run unit tests:

```bash
./gradlew testDebugUnitTest
```

Build Android test APK:

```bash
./gradlew :app:assembleDebugAndroidTest
```

Run instrumented tests on a connected device or emulator:

```bash
./gradlew connectedDebugAndroidTest
```

Install and launch manually:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.signlanguage.translator/.ui.activities.MainActivity
```

## Manual QA Checklist

Use this checklist before merging camera, model, backend, or UI changes:

- Camera permission is requested.
- Camera preview opens and is not black.
- Front/back camera setting works.
- Landmark overlay follows hand orientation correctly.
- Overlay does not draw sideways or mirrored incorrectly.
- FPS counter updates.
- App does not crash when no hand is visible.
- Top-3 predictions render with confidence values.
- Translated sentence placeholder is not copied or spoken.
- Manual TTS works when a real sentence exists.
- Copy button writes the current translated sentence to clipboard.
- Refresh translation keeps previous behavior.
- History screen lists accepted words.
- History clear action works.
- Backend offline state is shown clearly.
- Physical device test runs for at least 5 minutes without crash.

## Roadmap

### Sprint 1: Stabilize Real Device Pipeline

- Verify overlay orientation on multiple devices and both cameras.
- Measure FPS and latency on low-end, mid-range, and emulator targets.
- Decide between fast mode and full holistic mode.
- Add a user/developer setting for performance mode if needed.
- Run a 5+ minute smoke test on physical devices.

### Sprint 2: Model Contract Validation

- Validate landmark order with the model owner.
- Confirm coordinate normalization used during training.
- Confirm exact label order and complete 250-class mapping.
- Add a test fixture for known gestures.
- Document accepted confidence thresholds per gesture.

### Sprint 3: Backend Integration

- Confirm production/dev backend URL strategy.
- Validate `/api/health` and `/api/translate` response schemas.
- Add clear backend configuration documentation for emulator and real phones.
- Improve retry/error messaging if backend is offline.
- Add integration tests with mocked API responses.

### Sprint 4: Test Coverage And Quality

- Expand Espresso tests for translation, copy, TTS button states, settings, and history.
- Add more unit tests around frame throttling and prediction smoothing.
- Add benchmark/performance tests for MediaPipe and TFLite paths.
- Add CI once the GitHub repo workflow is agreed.

### Sprint 5: Product Polish

- Improve settings layout for developer diagnostics.
- Add clearer debug information for model confidence and latency.
- Improve history persistence and export if needed.
- Review accessibility labels and Turkish/English copy.
- Prepare release build configuration.

### Sprint 6: Optional Architecture Improvements

- Introduce Hilt dependency injection if the service graph grows.
- Split performance mode configuration into a dedicated settings model.
- Move backend DTO and API tests into a more formal data layer test structure.
- Add typed result/error handling around frame processing.

## Documentation Map

All supporting documentation is under `docs/`.

- `docs/SPEC_VS_REALITY_REPORT.md`: implementation status against the original spec.
- `docs/REMAINING_TASKS.md`: remaining validation and integration checklist.
- `docs/CHANGELOG.md`: project change history.
- `docs/PROJECT_REVIEW_REPORT.md`: review notes.
- `docs/parts/`: staged implementation notes.
- `docs/root_parts/`: preserved older root part documents that differed from `docs/parts/`.

## Team Workflow

Recommended workflow for team members:

1. Pull latest `main`.
2. Create a feature branch.
3. Keep changes scoped.
4. Run unit tests before pushing.
5. Run device smoke tests for camera/model changes.
6. Update docs when changing setup, model contract, backend behavior, or roadmap.

Commands:

```bash
git pull
git checkout -b feature/<short-name>
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug
```

Before opening a pull request, include:

- What changed.
- How it was tested.
- Whether physical device testing was done.
- Any model/backend assumptions.

## Repository Hygiene

These files are intentionally not committed:

- `local.properties`
- `.idea/`
- `.claude/`
- `.gradle/`
- `.DS_Store`
- `build/`
- `app/build/`

Do not commit local SDK paths, personal IDE settings, generated APKs, or machine-specific files.

## Current GitHub Repository

```text
https://github.com/Ulasaydo/SIJECT
```
