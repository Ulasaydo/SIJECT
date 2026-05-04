# SIJECT

Android MVP for real-time sign language recognition and sentence translation.

## Current Scope

- CameraX front camera preview and frame analyzer.
- MediaPipe Tasks pipeline for pose, face, and hands in video mode.
- 543-landmark model contract with a `30 x 1629` circular frame buffer.
- TFLite interpreter path for `sign_language_model.tflite` with `[1, 30, 1629]` input and `[1, 250]` output validation.
- Retrofit backend integration for `POST /api/translate` and `GET /api/health`.
- Offline fallback that joins recognized words when backend is unavailable.
- Automatic and manual TextToSpeech playback for translated sentences.
- Translation history backed by local storage and rendered with RecyclerView.

## Required Assets

Place these under `app/src/main/assets/`:

- `sign_language_model.tflite`
- `labels.txt`
- `landmark_indices.json`

`landmark_indices.json` should contain an `indices` array with exactly 543 values in this order:

```text
pose_33, face_468, left_hand_21, right_hand_21
```

`labels.txt` may contain known class labels. Unknown model output indices fall back to `class_<index>` labels in debug output.

## Backend

Default emulator URL: `http://10.0.2.2:8000/`

Expected endpoints:

- `POST /api/translate`
- `GET /api/health`

For a physical device, set `BACKEND_URL` in `local.properties` to the backend machine's LAN IP, for example:

```properties
BACKEND_URL=http://192.168.1.20:8000/
```

## Build

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

## Tests

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebugAndroidTest
```

Run instrumented tests on a connected device or emulator:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDebugAndroidTest
```

## Manual Test Checklist

- Camera opens after permission grant.
- Preview is not black.
- Overlay points are visible in debug mode.
- FPS counter updates.
- Top 3 predictions render with confidence.
- Sentence updates after a recognized word is accepted.
- Manual TTS and copy buttons work only when a real sentence is visible.
- History screen lists recognized words and can clear history.
- Backend offline mode shows joined words.
- App runs for 5+ minutes without crash.
