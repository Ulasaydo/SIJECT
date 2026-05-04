# 🚀 Gemma 4 Destekli İşaret Dili Çeviri Uygulaması
## Mobil Geliştirme (Android) - Teknik Doküman | Part 2
### Bölümler: UI / Kullanıcı Akışı · CameraX Entegrasyonu

---

## 📑 İçindekiler (Bu Bölüm)
4. [UI / Kullanıcı Akışı](#4-ui--kullanıcı-akışı)
5. [CameraX Entegrasyonu](#5-camerax-entegrasyonu)

---

## 4. UI / Kullanıcı Akışı

### 4.1 Ekran Hiyerarşisi

```
MainActivity (Fragment Container)
│
├── TranslationFragment ⭐ (PRIMARY - Camera & Recognition)
│   ├── CameraX PreviewView
│   ├── LandmarkOverlayView (Canvas - skeleton drawing)
│   ├── RecognizedWordsView (Top 3 words with confidence)
│   ├── TranslatedSentenceView (Backend output)
│   └── BottomActionBar (Settings, History buttons)
│
├── HistoryFragment (Word history - optional MVP v1.1)
│   └── RecyclerView (Word list with timestamps)
│
└── SettingsFragment (Configuration)
    ├── Camera selection (front/back)
    ├── Confidence threshold slider (0.5 - 0.9)
    ├── Landmark overlay toggle (debug)
    ├── FPS counter toggle (debug)
    └── Clear history button
```

### 4.2 Main UI Layout (TranslationFragment)

```xml
┌────────────────────────────────────────────────┐
│  [StatusBar - Battery, Time, Permissions]      │
├────────────────────────────────────────────────┤
│                                                │
│           PreviewView (CameraX)                │
│           ↓                                    │
│      [Camera Live Feed - 16:9]                │
│           ↓                                    │
│      [Canvas Overlay - Skeleton]              │
│       (x, y, z keypoints + lines)             │
│           ↓                                    │
│    [FPS Counter - Top Right]                  │
│                                                │
├────────────────────────────────────────────────┤
│ Recognized Words:                             │
│ ┌──────────────────────────────────────────┐  │
│ │ 1. hello       (89%) 🟢                   │  │
│ │ 2. bye         (76%) 🟡                   │  │
│ │ 3. computer    (65%) 🟠                   │  │
│ └──────────────────────────────────────────┘  │
├────────────────────────────────────────────────┤
│ Translated Sentence:                          │
│ ┌──────────────────────────────────────────┐  │
│ │ "Merhaba, hoşça kalın."                 │  │
│ │                                          │  │
│ │ [🔊 TTS Oynat] [📋 Kopyala] [🔄 Yenile]│  │
│ └──────────────────────────────────────────┘  │
├────────────────────────────────────────────────┤
│ [⚙️ Settings] [📜 History] [🐛 Debug]        │
└────────────────────────────────────────────────┘
```

### 4.3 Kullanıcı Akışı (Flow)

```
START
  ↓
[Request CAMERA Permission] → Granted? → No → [Show Error, Exit]
  ↓ Yes
[Initialize CameraX, MediaPipe, TFLite]
  ↓
[Start Camera Stream (30 FPS)]
  ↓
[Loop: For Each Frame]
  ├─→ MediaPipe.process(frame) → 543 landmarks
  ├─→ LandmarkProcessor.select(180 landmarks)
  ├─→ FrameBuffer.add(frame_features)
  ├─→ FrameBuffer.isFull? → No → [Continue]
  │                          ↓ Yes
  ├─→ TFLite.inference() → (1, 20) probabilities
  ├─→ PredictionProcessor.postProcess() → top words
  ├─→ SmoothingFilter.apply(last 3 predictions)
  ├─→ Confidence >= 0.70? → Yes → [Show Word]
  │                          No → [Show "Uncertain"]
  ├─→ UIModel.updatePredictions(word, confidence)
  ├─→ ViewModel.updateUI(RecognizedWords)
  ├─→ Fragment.render(words, confidence%)
  │
  ├─→ [DrawOverlay] → LandmarkOverlayView.draw(landmarks)
  ├─→ [UpdateMetrics] → FPS counter, latency
  │
  ├─→ User Pressed [Translate]?
  │   ├─→ API.translateWords(words) → Sentence
  │   ├─→ ViewModel.updateSentence(sentence)
  │   ├─→ Fragment.displaySentence()
  │   └─→ [Optional] TTS.speak(sentence)
  │
  └─→ Loop back to Frame processing

  [User Presses Back/Exit]
  ├─→ Stop Camera
  ├─→ Release MediaPipe
  ├─→ Close TFLite
  ├─→ Save Settings
  └─→ EXIT
```

### 4.4 State Management (ViewModel)

```kotlin
// TranslationViewModel state
data class TranslationUiState(
    val recognizedWords: List<String> = emptyList(),
    val confidences: List<Float> = emptyList(),
    val translatedSentence: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val fps: Int = 0,
    val inferenceTime: Long = 0L,
    val debugMode: Boolean = false
)

// LiveData observables
val uiState: LiveData<TranslationUiState>
val recognizedWord: LiveData<String>
val confidence: LiveData<Float>
val translatedSentence: LiveData<String>
val frameCaptureTime: LiveData<Long>
```

---

## 5. CameraX Entegrasyonu

### 5.1 CameraX Setup

**Bağımlılıklar:**
```gradle
dependencies {
    implementation 'androidx.camera:camera-camera2:1.3.0'
    implementation 'androidx.camera:camera-lifecycle:1.3.0'
    implementation 'androidx.camera:camera-view:1.3.0'
}
```

### 5.2 Camera Service Implementation

```kotlin
class CameraService(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageAnalyzer: ImageAnalysis
    private var frameCallback: ((ImageProxy) -> Unit)? = null

    fun startCamera(
        previewView: PreviewView,
        onFrameAvailable: (ImageProxy) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Preview use case (show on screen)
            val preview = Preview.Builder()
                .setTargetResolution(Size(640, 480))
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // ImageAnalyzer use case (frame processing)
            imageAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(
                            AspectRatioStrategy.LANDSCAPE_16_9_FALLBACK
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_NV21)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        onFrameAvailable(imageProxy)
                        imageProxy.close()  // ⚠️ MUST close to prevent memory leak
                    }
                }

            // Select front camera (user-facing)
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                LogUtils.d("CameraService", "Camera started successfully")
            } catch (e: Exception) {
                LogUtils.e("CameraService", "Failed to bind camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        cameraProvider.unbindAll()
        LogUtils.d("CameraService", "Camera stopped")
    }
}
```

### 5.3 Frame Capture & Processing Loop

```kotlin
// In TranslationFragment
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    cameraService = CameraService(requireContext(), viewLifecycleOwner)

    cameraService.startCamera(binding.previewView) { imageProxy ->
        // This callback runs on background thread
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val bitmap = imageProxyToBitmap(imageProxy)

                val startTime = System.currentTimeMillis()
                val landmarks = mediapibeService.process(bitmap)
                val mpTime = System.currentTimeMillis() - startTime

                val processedFrame = landmarkProcessor.extractSelectedLandmarks(landmarks)
                val isBufferFull = frameBufferManager.addFrame(processedFrame)

                if (isBufferFull) {
                    val inferenceStart = System.currentTimeMillis()
                    val prediction = tfliteService.inference(
                        frameBufferManager.getBuffer(),
                        confidenceThreshold = 0.70f
                    )
                    val inferenceTime = System.currentTimeMillis() - inferenceStart

                    viewModel.updatePrediction(prediction, mpTime, inferenceTime)
                }

                runOnUiThread {
                    binding.landmarkOverlay.setLandmarks(landmarks)
                }

            } catch (e: Exception) {
                LogUtils.e("FrameProcessing", "Error: ${e.message}")
            }
        }
    }
}
```

### 5.4 Performance Considerations

**⚠️ Critical Points:**

1. **Frame Closure:** `imageProxy.close()` MUST be called to prevent memory leaks
2. **Backpressure Strategy:** `STRATEGY_KEEP_ONLY_LATEST` ensures we don't queue frames
3. **Threading:** Frame processing on background thread, UI updates on main thread
4. **Resolution:** 640x480 @ 30 FPS balanced between quality and performance
5. **Image Format:** NV21 or YUV preferred for faster processing

**FPS Optimization:**
```kotlin
// If FPS drops below 25:
// Option 1: Skip every 2nd frame analysis
if (frameCount % 2 == 0) {
    // Process frame
}

// Option 2: Reduce MediaPipe confidence threshold
mediapibeService.setConfidenceThreshold(0.5f)  // Faster, less accurate

// Option 3: Use lower resolution
// Change from 640x480 to 480x360
```

---

**← Önceki: [part1.md](part1.md) | → Devam: [part3.md](part3.md) — MediaPipe & TFLite Entegrasyonu**
