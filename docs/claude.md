# 🚀 Gemma 4 Destekli İşaret Dili Çeviri Uygulaması
## Mobil Geliştirme (Android) - Teknik Doküman
### Hazırlayan: Teknik Analiz | Geliştirici: Mobil Geliştirme Ekibi

---

## 📑 İçindekiler
1. [Projedeki Sorumluluk Alanım](#1-projedeki-sorumluluk-alanım)
2. [Mobil Uygulama Genel Amacı](#2-mobil-uygulama-genel-amacı)
3. [Android Mimari Tasarımı](#3-android-mimari-tasarımı)
4. [UI / Kullanıcı Akışı](#4-ui--kullanıcı-akışı)
5. [CameraX Entegrasyonu](#5-camerax-entegrasyonu)
6. [MediaPipe SDK Entegrasyonu](#6-mediapipe-sdk-entegrasyonu)
7. [TFLite Model Entegrasyonu](#7-tflite-model-entegrasyonu)
8. [Backend API Haberleşmesi](#8-backend-api-haberleşmesi)
9. [Veri Akışı (Detaylı)](#9-veri-akışı-detaylı)
10. [Performans ve Optimizasyon Planı](#10-performans-ve-optimizasyon-planı)
11. [Olası Teknik Sorunlar ve Çözümleri](#11-olası-teknik-sorunlar-ve-çözümleri)
12. [Test Planı](#12-test-planı)
13. [Dosya / Klasör Yapısı Önerisi](#13-dosya--klasör-yapısı-önerisi)
14. [Geliştirme Yol Haritası](#14-geliştirme-yol-haritası)
15. [Hatice'ye Sorulması Gerekenler (TODOs)](#15-hatiçeye-sorulması-gerekenler-todos)

---

## 1. Projedeki Sorumluluk Alanım

### Teknik Sorumluluklar (Mobil Geliştirme Ekibi)

| Görev | Açıklama | Bağımlılık |
|-------|----------|-----------|
| **Android Mimarisi** | MVVM pattern, Repository pattern, Coroutines | - |
| **UI/UX Tasarımı** | XML Layout, ViewBinding, Material Design | - |
| **CameraX Entegrasyonu** | Ön kamera, 30 FPS, live preview | - |
| **MediaPipe SDK** | Holistic (543 landmark), frame işleme | Hatice (landmark seçimi) |
| **TFLite Model** | Inference, input/output handling | Hatice (model, labels.txt) |
| **Landmark İşleme** | 180 landmark seçimi, x/y/z normalization | Hatice (indeks listesi) |
| **30 Frame Buffer** | Circular buffer yönetimi | - |
| **Post-Processing** | argmax, confidence threshold (0.70) | - |
| **Backend API** | Retrofit entegrasyonu, HTTP requests | Rabia (backend) |
| **UI State Management** | ViewModel, LiveData, smoothing (3-frame avg) | - |
| **İskelet Görselleştirmesi** | Canvas overlay, debug mode | - |
| **Performans Tuning** | FPS optimization, memory management | - |
| **Permission Handling** | CAMERA runtime permissions | - |
| **Error Handling** | Network timeouts, offline mode, crashes | - |

---

## 2. Mobil Uygulama Genel Amacı

### Proje Vizyonu
Işaret dili kullanan bireylerin, işaret dili bilmeyen kişilerle **gerçek zamanlı, akıcı ve çift yönlü iletişim** kurabilmesini sağlayan, yapay zeka destekli bir Android uygulaması.

### Mobil Uygulamanın Kapsam Alanı

**Yapması Gerekenler (MVP - Minimum Viable Product):**
- ✅ Ön kamerayı başlat ve canlı görüntüyü ekranda göster
- ✅ MediaPipe Holistic ile 543 landmark çıkar (30 FPS)
- ✅ Son 30 frame'den oluşan sequence'ı TFLite modeline gönder
- ✅ Model çıktısını (1, 20) post-process et ve kelime tahminini yap
- ✅ Tahmin edilen kelimeyi ekranda göster (confidence %'si ile)
- ✅ Kelime listesini Backend API'ye gönder (geldiğinde)
- ✅ Backend'den dönen cümleyi ekranda göster

**Yapmaması Gerekenler (Out of Scope):**
- ❌ Video kayıt / export
- ❌ Bulut depolama
- ❌ Kullanıcı hesapları
- ❌ İzin yönetimi (basit permission handling yeterli)
- ❌ Dil desteği (Türkçe/İngilizce seçimi - başlangıçta Türkçe)
- ❌ Voice input (sadece gesture recognition)

---

## 3. Android Mimari Tasarımı

### 3.1 Genel Mimari Yapısı

```
┌──────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  (Activities, Fragments, ViewModels, LiveData)      │
├──────────────────────────────────────────────────────┤
│                 Business Logic Layer                 │
│  (ViewModels, Repositories, Use Cases)              │
├──────────────────────────────────────────────────────┤
│                    Data Layer                        │
│  (Services: Camera, MediaPipe, TFLite, API)         │
├──────────────────────────────────────────────────────┤
│                 Device APIs & Network                │
│  (Android Camera API, MediaPipe SDK, Network)       │
└──────────────────────────────────────────────────────┘
```

### 3.2 Seçilen Teknolojiler ve Gerekçeleri

| Teknoloji | Seçim | Neden |
|-----------|-------|-------|
| **Programlama Dili** | Kotlin | Modern, safe, concise, Android recommended |
| **UI Framework** | XML Layout + ViewBinding | CameraX entegrasyonu daha stabil, overlay çizimi daha kolay |
| **Mimari Pattern** | MVVM | Screen rotation sırasında state korunur, test edilebilir |
| **DI Framework** | Hilt (Optional, başlangıçta Manual) | Dependency injection, modular code |
| **Async Programming** | Coroutines + Flow | Main thread bloke olmaz, lifecycle-aware |
| **HTTP Client** | Retrofit + OkHttp | Modern, reactive, error handling kolay |
| **Image Processing** | Canvas (2D) | Landmark çizimi, performans iyi |
| **Minimum API** | 26 (Android 8.0+) | CameraX, MediaPipe, TFLite support |
| **Target API** | 34 (Latest) | Güvenlik, performance, yeni features |

### 3.3 Paket Yapısı (Kurulu Mimarı)

```
app/src/main/
│
├── java/com/signlanguage/translator/
│   ├── ui/
│   │   ├── activities/
│   │   │   ├── MainActivity.kt (Entry point, fragment container)
│   │   │   ├── PermissionHelper.kt (Runtime permissions)
│   │   │   └── BaseActivity.kt (Common lifecycle)
│   │   │
│   │   ├── fragments/
│   │   │   ├── TranslationFragment.kt (Main UI - camera, preview)
│   │   │   ├── HistoryFragment.kt (Word history - optional)
│   │   │   └── SettingsFragment.kt (Camera, threshold settings)
│   │   │
│   │   ├── viewmodels/
│   │   │   ├── TranslationViewModel.kt (Main state manager)
│   │   │   ├── HistoryViewModel.kt (History management)
│   │   │   └── SharedViewModel.kt (Fragment communication)
│   │   │
│   │   └── views/
│   │       ├── LandmarkOverlayView.kt (Canvas - skeletal drawing)
│   │       ├── ConfidenceBar.kt (Confidence visualization)
│   │       └── WordDisplayView.kt (Animated word display)
│   │
│   ├── data/
│   │   ├── repository/
│   │   │   ├── SignLanguageRepository.kt (Main repo - orchestrator)
│   │   │   ├── LocalDataRepository.kt (SharedPreferences)
│   │   │   └── RemoteDataRepository.kt (API calls)
│   │   │
│   │   └── model/
│   │       ├── PredictionResult.kt (Data class)
│   │       ├── TranslationResponse.kt (API response)
│   │       └── WordHistory.kt (Local data)
│   │
│   ├── domain/
│   │   ├── services/
│   │   │   ├── CameraService.kt (CameraX wrapper)
│   │   │   ├── MediaPipeService.kt (Landmark extraction)
│   │   │   ├── TFLiteService.kt (Model inference)
│   │   │   ├── LandmarkProcessor.kt (180 landmark selection)
│   │   │   ├── FrameBufferManager.kt (30-frame buffer)
│   │   │   ├── APIService.kt (Retrofit client)
│   │   │   └── TextToSpeechService.kt (TTS - future)
│   │   │
│   │   └── usecases/
│   │       ├── ProcessFrameUseCase.kt (Frame → landmark → prediction)
│   │       ├── TranslateWordsUseCase.kt (Words → sentence via API)
│   │       └── SmoothedPredictionUseCase.kt (3-frame averaging)
│   │
│   └── utils/
│       ├── Constants.kt (Model paths, thresholds, labels)
│       ├── Extensions.kt (Utility functions)
│       ├── TensorUtils.kt (Tensor reshape, operations)
│       ├── PermissionUtils.kt (Permission checking)
│       ├── LogUtils.kt (Logging with tags)
│       └── DebugConfig.kt (Debug flags)
│
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── fragment_translation.xml (Camera + overlay + UI)
│   │   ├── fragment_history.xml
│   │   ├── fragment_settings.xml
│   │   └── item_word.xml (RecyclerView item)
│   │
│   ├── values/
│   │   ├── strings.xml (UI text)
│   │   ├── dimens.xml (Sizes, margins)
│   │   ├── colors.xml (Color palette)
│   │   ├── styles.xml (Themes)
│   │   └── attrs.xml (Custom attributes)
│   │
│   └── menu/
│       └── bottom_nav_menu.xml (Bottom navigation)
│
├── assets/
│   ├── sign_language_model.tflite (1.6-2.5 MB) ⭐ FROM HATICE
│   ├── labels.txt (250 word labels, line by line) ⭐ FROM HATICE
│   └── landmark_indices.json (180 landmark indices) ⭐ TODO: FROM HATICE
│
└── AndroidManifest.xml
    ├── CAMERA permission (required)
    ├── INTERNET permission (backend)
    └── Features: android:uses-feature camera
```

### 3.4 Mimari Katmanlar (Detaylı)

#### **UI Layer (Presentation)**
- **Purpose:** User interaction, state visualization
- **Components:** Activities, Fragments, ViewModels, Views
- **Responsibilities:**
  - Camera preview göster
  - Landmark overlay çiz
  - Recognized words göster (confidence ile)
  - Translated sentence göster
  - User input al (settings, etc.)
  - Lifecycle manage et

#### **Domain Layer (Business Logic)**
- **Purpose:** Core functionality, independence from UI/data details
- **Components:** Use Cases, Services, Repositories
- **Responsibilities:**
  - CameraX frame capture
  - MediaPipe landmark extraction
  - TFLite model inference
  - 30-frame buffer management
  - API communication orchestration
  - Error handling, retry logic

#### **Data Layer (Infrastructure)**
- **Purpose:** Data sources (local, remote, device APIs)
- **Components:** Repository implementations, Data sources
- **Responsibilities:**
  - Network API calls (Retrofit)
  - Local data persistence (SharedPreferences)
  - Device access (Camera, MediaPipe)
  - Sensor data collection

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
                // Unbind all use cases
                cameraProvider.unbindAll()
                
                // Bind preview + analyzer
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
    
    // Start camera with frame callback
    cameraService.startCamera(binding.previewView) { imageProxy ->
        // This callback runs on background thread
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Convert ImageProxy to suitable format for MediaPipe
                val bitmap = imageProxyToBitmap(imageProxy)  // or use YUV directly
                
                // 2. Process frame in MediaPipe
                val startTime = System.currentTimeMillis()
                val landmarks = mediapibeService.process(bitmap)
                val mpTime = System.currentTimeMillis() - startTime
                
                LogUtils.d("Frame", "MediaPipe: ${mpTime}ms")
                
                // 3. Add to frame buffer
                val processedFrame = landmarkProcessor.extractSelectedLandmarks(landmarks)
                val isBufferFull = frameBufferManager.addFrame(processedFrame)
                
                // 4. If buffer is full, run inference
                if (isBufferFull) {
                    val inferenceStart = System.currentTimeMillis()
                    val prediction = tfliteService.inference(
                        frameBufferManager.getBuffer(),
                        confidenceThreshold = 0.70f
                    )
                    val inferenceTime = System.currentTimeMillis() - inferenceStart
                    
                    LogUtils.d("Inference", "TFLite: ${inferenceTime}ms")
                    
                    // 5. Update ViewModel (main thread)
                    viewModel.updatePrediction(prediction, mpTime, inferenceTime)
                }
                
                // 6. Update overlay (landmarks)
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

## 6. MediaPipe SDK Entegrasyonu

### 6.1 MediaPipe Setup

**Bağımlılıklar:**
```gradle
dependencies {
    implementation 'com.google.mediapipe:tasks-vision:0.10.9'
}
```

### 6.2 MediaPipe Service (Landmark Extraction)

```kotlin
class MediaPipeService(private val context: Context) {
    private lateinit var holistic: Holistic
    private var confidenceThreshold: Float = 0.5f
    
    init {
        initializeHolistic()
    }
    
    private fun initializeHolistic() {
        val options = Holistic.HolisticOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("mediapipe_model_path_here")  // TODO: Check correct path
                    .build()
            )
            .setRunningMode(RunningMode.VIDEO)
            // Static image mode: false (for video stream)
            .build()
        
        holistic = Holistic(context, options)
        LogUtils.d("MediaPipe", "Holistic initialized successfully")
    }
    
    fun process(bitmap: Bitmap): List<NormalizedLandmark> {
        return try {
            val timestamp = SystemClock.uptimeMillis()
            
            // Convert bitmap to MPImage
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            // Run MediaPipe Holistic
            val result = holistic.detectAsync(mpImage, timestamp)
            
            // Combine all landmarks: face + pose + hands
            val allLandmarks = mutableListOf<NormalizedLandmark>()
            
            // Face landmarks (468)
            result.faceLandmarks()?.forEach { landmark ->
                allLandmarks.add(landmark)
            }
            
            // Pose landmarks (33)
            result.poseLandmarks()?.forEach { landmark ->
                allLandmarks.add(landmark)
            }
            
            // Left hand (21)
            result.leftHandLandmarks()?.forEach { landmark ->
                allLandmarks.add(landmark)
            }
            
            // Right hand (21)
            result.rightHandLandmarks()?.forEach { landmark ->
                allLandmarks.add(landmark)
            }
            
            // Total: 468 + 33 + 21 + 21 = 543 landmarks
            require(allLandmarks.size == 543) {
                "Expected 543 landmarks, got ${allLandmarks.size}"
            }
            
            LogUtils.d("MediaPipe", "Extracted 543 landmarks successfully")
            allLandmarks
            
        } catch (e: Exception) {
            LogUtils.e("MediaPipe", "Error: ${e.message}")
            emptyList()
        }
    }
    
    fun setConfidenceThreshold(threshold: Float) {
        this.confidenceThreshold = threshold
    }
    
    fun release() {
        holistic.close()
        LogUtils.d("MediaPipe", "Holistic released")
    }
}

// Data class for landmarks
data class NormalizedLandmark(
    val x: Float,      // [0, 1] - normalized X coordinate
    val y: Float,      // [0, 1] - normalized Y coordinate
    val z: Float,      // Depth or relative Z (varies by model)
    val visibility: Float  // Confidence that landmark is visible
)
```

### 6.3 Landmark Visualization (Debug)

```kotlin
class LandmarkOverlayView(context: Context, attrs: AttributeSet? = null) 
    : View(context, attrs) {
    
    private var landmarks: List<NormalizedLandmark> = emptyList()
    private val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 3f
    }
    
    fun setLandmarks(newLandmarks: List<NormalizedLandmark>) {
        this.landmarks = newLandmarks
        postInvalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (landmarks.isEmpty()) return
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Draw landmarks as circles
        landmarks.forEach { landmark ->
            val x = landmark.x * width
            val y = landmark.y * height
            val visibility = landmark.visibility
            
            // Color based on visibility confidence
            paint.color = when {
                visibility > 0.7f -> Color.GREEN
                visibility > 0.5f -> Color.YELLOW
                else -> Color.RED
            }
            
            canvas.drawCircle(x, y, 4f, paint)
        }
        
        // Draw connections (if needed for skeleton visualization)
        drawConnections(canvas, width, height)
    }
    
    private fun drawConnections(canvas: Canvas, width: Float, height: Float) {
        // Pose connections (simplified)
        val poseConnections = listOf(
            Pair(11, 12),  // Shoulders
            Pair(12, 14),  // Right arm
            Pair(14, 16),  // Right forearm
            Pair(11, 13),  // Left arm
            Pair(13, 15),  // Left forearm
            // ... more connections
        )
        
        paint.color = Color.CYAN
        paint.strokeWidth = 2f
        
        poseConnections.forEach { (idx1, idx2) ->
            if (idx1 < landmarks.size && idx2 < landmarks.size) {
                val start = landmarks[idx1]
                val end = landmarks[idx2]
                
                canvas.drawLine(
                    start.x * width, start.y * height,
                    end.x * width, end.y * height,
                    paint
                )
            }
        }
    }
}
```

### 6.4 Potential Issues & Solutions

| Issue | Symptom | Solution |
|-------|---------|----------|
| **Landmarks Out of Order** | Model predictions random | Check landmark extraction order matches training |
| **Missing Landmarks** | Some body parts not detected | Low light, fast movement, camera angle |
| **Visibility = 0** | Invisible landmarks used anyway | Filter by visibility threshold (>0.5) |
| **High Latency** | FPS drops | Run MediaPipe on GPU (if available) |
| **Memory Leak** | App crash after ~2 min | Call `release()` in Fragment.onDestroy |

---

## 7. TFLite Model Entegrasyonu

### 7.1 Model Files Setup

**Files to obtain from Hatice:**
- ✅ `sign_language_model.tflite` (1.6-2.5 MB)
- ✅ `labels.txt` (250 words, line by line)
- 🔴 **TODO:** `landmark_indices.json` (which 180 of 543 landmarks to use)
- 🔴 **TODO:** Normalization information (scale/range of x, y, z)

**Place files in:**
```
app/src/main/assets/
├── sign_language_model.tflite
├── labels.txt
└── landmark_indices.json  (when received)
```

### 7.2 Model Input/Output Specification

```kotlin
/*
 * MODEL SPECIFICATION
 * 
 * Input Shape:  (1, 30, 540)
 *   - 1: Batch size (always 1)
 *   - 30: Number of frames (temporal dimension)
 *   - 540: Features per frame (180 landmarks × 3 coordinates)
 *
 * Input Data Type: Float32
 * Input Range: [0, 1] normalized (CONFIRM with Hatice)
 *
 * Output Shape: (1, 20)
 *   - 1: Batch size
 *   - 20: Number of word classes (softmax probabilities)
 *
 * Output Data Type: Float32
 * Output Values: [0, 1] - normalized probabilities (sums to ~1.0)
 *
 * Latency on Mid-Range Device: ~50-100 ms (estimated)
 * Model Size: ~2 MB (after TFLite conversion)
 */
```

### 7.3 TFLite Service (Inference Engine)

```kotlin
class TFLiteService(private val context: Context) {
    
    private lateinit var interpreter: Interpreter
    private lateinit var labels: List<String>
    private var modelLoaded = false
    
    init {
        loadModel()
        loadLabels()
    }
    
    private fun loadModel() {
        try {
            val modelBuffer = context.assets.open("sign_language_model.tflite")
                .readBytes()
                .let { ByteBuffer.wrap(it).order(ByteOrder.nativeOrder()) }
            
            val options = Interpreter.Options()
            options.setNumThreads(4)  // Use 4 threads for inference
            options.setUseNNAPI(true)  // Use NNAPI acceleration if available
            // options.setUseGpuDelegate(true)  // GPU (if supported)
            
            interpreter = Interpreter(modelBuffer, options)
            modelLoaded = true
            
            // Log input/output details
            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)
            
            LogUtils.d("TFLite", "Model loaded successfully")
            LogUtils.d("TFLite", "Input shape: ${inputTensor.shape().contentToString()}")
            LogUtils.d("TFLite", "Output shape: ${outputTensor.shape().contentToString()}")
            
        } catch (e: Exception) {
            LogUtils.e("TFLite", "Failed to load model: ${e.message}")
            modelLoaded = false
        }
    }
    
    private fun loadLabels() {
        try {
            labels = context.assets.open("labels.txt")
                .bufferedReader()
                .readLines()
                .map { it.trim() }
            
            LogUtils.d("TFLite", "Loaded ${labels.size} labels: $labels")
            
        } catch (e: Exception) {
            LogUtils.e("TFLite", "Failed to load labels: ${e.message}")
            labels = emptyList()
        }
    }
    
    fun inference(
        inputBuffer: FloatArray,
        confidenceThreshold: Float = 0.70f
    ): PredictionResult {
        
        if (!modelLoaded || labels.isEmpty()) {
            return PredictionResult(
                predictedIndex = -1,
                confidence = 0f,
                label = "Model Not Loaded",
                allProbabilities = emptyList()
            )
        }
        
        return try {
            val startTime = System.currentTimeMillis()
            
            // Prepare input: reshape to (1, 30, 540)
            val inputShape = intArrayOf(1, 30, 540)
            val input = Array(1) { Array(30) { FloatArray(540) } }
            
            var idx = 0
            for (t in 0..29) {
                for (f in 0..539) {
                    input[0][t][f] = inputBuffer[idx++]
                }
            }
            
            // Prepare output: (1, 20)
            val output = Array(1) { FloatArray(20) }
            
            // Run inference
            interpreter.run(input, output)
            
            val inferenceTime = System.currentTimeMillis() - startTime
            
            // Post-process: get argmax and confidence
            val predictions = output[0]
            val predictedIndex = predictions.indices.maxByOrNull { predictions[it] } ?: -1
            val confidence = if (predictedIndex >= 0) predictions[predictedIndex] else 0f
            
            val label = when {
                predictedIndex < 0 -> "No Prediction"
                confidence < confidenceThreshold -> "Uncertain (${String.format("%.0f%%", confidence * 100)})"
                predictedIndex >= labels.size -> "Index Out of Range"
                else -> labels[predictedIndex]
            }
            
            LogUtils.d(
                "TFLite",
                "Inference: ${inferenceTime}ms | Prediction: $label (${String.format("%.1f%%", confidence * 100)})"
            )
            
            PredictionResult(
                predictedIndex = predictedIndex,
                confidence = confidence,
                label = label,
                allProbabilities = predictions.toList(),
                inferenceTime = inferenceTime
            )
            
        } catch (e: Exception) {
            LogUtils.e("TFLite", "Inference failed: ${e.message}")
            PredictionResult(
                predictedIndex = -1,
                confidence = 0f,
                label = "Error: ${e.message}",
                allProbabilities = emptyList()
            )
        }
    }
    
    fun close() {
        if (::interpreter.isInitialized) {
            interpreter.close()
            LogUtils.d("TFLite", "Interpreter closed")
        }
    }
}

// Data classes
data class PredictionResult(
    val predictedIndex: Int,
    val confidence: Float,
    val label: String,
    val allProbabilities: List<Float>,
    val inferenceTime: Long = 0L
)
```

### 7.4 Input Preparation (Frame Buffer to Tensor)

```kotlin
class FrameBufferManager(
    private val frameSize: Int = 540,  // 180 landmarks × 3 coordinates
    private val maxFrames: Int = 30,
    private val debugMode: Boolean = false
) {
    
    private val buffer = FloatArray(maxFrames * frameSize)
    private var frameCount = 0
    private var currentIndex = 0
    private val frameTimestamps = mutableListOf<Long>()
    
    /**
     * Add a new frame (540 floats) to the circular buffer.
     * Returns true when buffer is full (ready for inference).
     */
    fun addFrame(frame: FloatArray): Boolean {
        require(frame.size == frameSize) {
            "Frame size mismatch: expected $frameSize, got ${frame.size}"
        }
        
        // Copy frame into buffer at current position
        System.arraycopy(
            frame, 0,                        // source
            buffer, currentIndex * frameSize, // destination offset
            frameSize                         // length
        )
        
        frameTimestamps.add(System.currentTimeMillis())
        if (frameTimestamps.size > maxFrames) {
            frameTimestamps.removeAt(0)
        }
        
        // Move to next position (circular)
        currentIndex = (currentIndex + 1) % maxFrames
        
        // Increment frame count until we reach max
        if (frameCount < maxFrames) {
            frameCount++
        }
        
        if (debugMode && frameCount == maxFrames) {
            val fps = calculateFPS()
            LogUtils.d("FrameBuffer", "Buffer full | FPS: $fps | Frames queued: $frameCount")
        }
        
        return frameCount == maxFrames
    }
    
    /**
     * Get the complete buffer (may be partially filled if < 30 frames yet).
     * If buffer is not full, pads with zeros.
     */
    fun getBuffer(): FloatArray {
        return buffer.copyOf()
    }
    
    /**
     * Get the buffer properly ordered (handles circular buffer offset).
     * Returns frames in correct temporal order: [oldest ... newest].
     */
    fun getBufferOrdered(): FloatArray {
        val result = FloatArray(maxFrames * frameSize)
        
        if (frameCount < maxFrames) {
            // Buffer not yet full: copy from index 0
            System.arraycopy(buffer, 0, result, 0, frameCount * frameSize)
        } else {
            // Buffer is full: copy in circular order
            val oldestIndex = currentIndex
            System.arraycopy(
                buffer, oldestIndex * frameSize,
                result, 0,
                (maxFrames - oldestIndex) * frameSize
            )
            System.arraycopy(
                buffer, 0,
                result, (maxFrames - oldestIndex) * frameSize,
                oldestIndex * frameSize
            )
        }
        
        return result
    }
    
    fun calculateFPS(): Int {
        if (frameTimestamps.size < 2) return 0
        val timeDiff = frameTimestamps.last() - frameTimestamps.first()
        return if (timeDiff > 0) ((frameTimestamps.size - 1) * 1000 / timeDiff).toInt() else 0
    }
    
    fun isFull(): Boolean = frameCount == maxFrames
    
    fun reset() {
        buffer.fill(0f)
        frameCount = 0
        currentIndex = 0
        frameTimestamps.clear()
        LogUtils.d("FrameBuffer", "Buffer reset")
    }
    
    fun getFrameCount(): Int = frameCount
}
```

### 7.5 Output Post-Processing

```kotlin
object PredictionProcessor {
    
    /**
     * Process raw TFLite output (1, 20) into readable prediction.
     * 
     * Steps:
     * 1. Apply softmax (if not already applied by model)
     * 2. Find argmax (predicted class index)
     * 3. Map index to label
     * 4. Apply confidence threshold
     */
    fun processPrediction(
        output: FloatArray,
        labels: List<String>,
        confidenceThreshold: Float = 0.70f,
        debugMode: Boolean = false
    ): PredictionResult {
        
        require(output.size == 250) { "Expected 250 output values, got ${output.size}" }
        require(labels.size == 250) { "Expected 250 labels, got ${labels.size}" }
        
        // Find argmax
        var maxIdx = 0
        var maxValue = output[0]
        
        for (i in output.indices) {
            if (output[i] > maxValue) {
                maxValue = output[i]
                maxIdx = i
            }
        }
        
        val confidence = maxValue
        val label = labels[maxIdx]
        
        val result = PredictionResult(
            predictedIndex = maxIdx,
            confidence = confidence,
            label = if (confidence >= confidenceThreshold) label else "Uncertain",
            allProbabilities = output.toList()
        )
        
        if (debugMode) {
            val top3 = output.withIndex()
                .sortedByDescending { it.value }
                .take(3)
                .map { "${labels[it.index]}: ${String.format("%.1f%%", it.value * 100)}" }
            
            LogUtils.d("Prediction", "Top3: ${top3.joinToString(" | ")}")
        }
        
        return result
    }
}
```

---

## 8. Backend API Haberleşmesi

### 8.1 API Contract (Expected Endpoint)

```kotlin
/*
 * Backend API Endpoint (Rabia tarafından yapılacak)
 * 
 * POST /api/translate (or similar)
 * 
 * Request Body:
 * {
 *   "words": ["hello", "bye", "computer"],
 *   "language": "tr"  // Optional
 * }
 * 
 * Response Body:
 * {
 *   "sentence": "Merhaba, hoşça kalın.",
 *   "confidence": 0.87,
 *   "alternatives": [
 *     "Merhaba, hoşça kalın.",
 *     "Selam, hoşça kalın."
 *   ]
 * }
 * 
 * Status Codes:
 * - 200: Success
 * - 400: Invalid request
 * - 500: Server error
 * - Timeout: 10 seconds
 */
```

### 8.2 Retrofit Setup

```gradle
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
}
```

### 8.3 API Service Definition

```kotlin
// Data classes for API communication
data class TranslateRequest(
    @SerializedName("words")
    val words: List<String>,
    
    @SerializedName("language")
    val language: String = "tr"
)

data class TranslateResponse(
    @SerializedName("sentence")
    val sentence: String,
    
    @SerializedName("confidence")
    val confidence: Float = 0f,
    
    @SerializedName("alternatives")
    val alternatives: List<String> = emptyList()
)

// Retrofit API interface
interface SignLanguageAPI {
    
    @POST("/api/translate")
    suspend fun translateWords(
        @Body request: TranslateRequest
    ): Response<TranslateResponse>
    
    @GET("/api/health")
    suspend fun checkHealth(): Response<String>
}

// Retrofit client singleton
object RetrofitClient {
    
    private const val BASE_URL = "http://10.0.2.2:8000/"  // Android emulator localhost
    // For real device: "http://192.168.1.100:8000/"  // Update IP as needed
    
    val api: SignLanguageAPI by lazy {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SignLanguageAPI::class.java)
    }
}
```

### 8.4 Repository Implementation

```kotlin
class SignLanguageRepository(
    private val api: SignLanguageAPI
) {
    
    /**
     * Translate words to a sentence via backend API.
     * Returns Result<String> with sentence or error message.
     */
    suspend fun translateWords(words: List<String>): Result<String> {
        return try {
            if (words.isEmpty()) {
                return Result.success("")
            }
            
            val request = TranslateRequest(words = words, language = "tr")
            val response = api.translateWords(request)
            
            when {
                response.isSuccessful && response.body() != null -> {
                    val sentence = response.body()!!.sentence
                    LogUtils.d("API", "Translation: $sentence")
                    Result.success(sentence)
                }
                
                response.code() == 400 -> {
                    Result.failure(
                        Exception("Invalid request: ${response.message()}")
                    )
                }
                
                response.code() >= 500 -> {
                    Result.failure(
                        Exception("Server error: ${response.message()}")
                    )
                }
                
                else -> {
                    Result.failure(
                        Exception("Unknown error: ${response.message()}")
                    )
                }
            }
            
        } catch (e: SocketTimeoutException) {
            LogUtils.e("API", "Timeout")
            Result.failure(Exception("Request timeout"))
        } catch (e: ConnectException) {
            LogUtils.e("API", "Connection failed")
            Result.failure(Exception("No internet connection"))
        } catch (e: Exception) {
            LogUtils.e("API", "Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check if backend is reachable (health check).
     */
    suspend fun checkBackendHealth(): Boolean {
        return try {
            val response = api.checkHealth()
            response.isSuccessful
        } catch (e: Exception) {
            LogUtils.e("API", "Health check failed: ${e.message}")
            false
        }
    }
}
```

### 8.5 ViewModel - API Integration

```kotlin
class TranslationViewModel(
    private val repository: SignLanguageRepository
) : ViewModel() {
    
    private val _translatedSentence = MutableLiveData<String>("")
    val translatedSentence: LiveData<String> = _translatedSentence
    
    private val _apiError = MutableLiveData<String?>(null)
    val apiError: LiveData<String?> = _apiError
    
    private val _isLoadingTranslation = MutableLiveData(false)
    val isLoadingTranslation: LiveData<Boolean> = _isLoadingTranslation
    
    fun translateWords(words: List<String>) {
        viewModelScope.launch {
            _isLoadingTranslation.value = true
            _apiError.value = null
            
            val result = repository.translateWords(words)
            
            result.onSuccess { sentence ->
                _translatedSentence.value = sentence
                LogUtils.d("ViewModel", "Translation successful: $sentence")
            }
            
            result.onFailure { exception ->
                _apiError.value = exception.message ?: "Unknown error"
                LogUtils.e("ViewModel", "Translation failed: ${exception.message}")
                
                // Offline fallback: simple word concatenation
                _translatedSentence.value = words.joinToString(" ")
            }
            
            _isLoadingTranslation.value = false
        }
    }
}
```

### 8.6 Fragment Integration

```kotlin
// In TranslationFragment
private fun callBackendAPI(words: List<String>) {
    binding.progressBar.visibility = View.VISIBLE
    
    viewModel.translateWords(words)
    
    viewModel.translatedSentence.observe(viewLifecycleOwner) { sentence ->
        if (sentence.isNotEmpty()) {
            binding.translatedSentenceText.text = sentence
            binding.translatedSentenceCard.visibility = View.VISIBLE
            
            // Optional: TTS speak sentence
            textToSpeechService.speak(sentence)
        }
    }
    
    viewModel.apiError.observe(viewLifecycleOwner) { error ->
        if (error != null) {
            showErrorSnackbar("API Error: $error")
            binding.translatedSentenceText.text = "Error: $error"
        }
    }
    
    viewModel.isLoadingTranslation.observe(viewLifecycleOwner) { isLoading ->
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
```

---

## 9. Veri Akışı (Detaylı)

### 9.1 End-to-End Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        1. FRAME CAPTURE (30 FPS)                        │
│                                                                         │
│  CameraX.ImageAnalyzer                                                 │
│    └─> ImageProxy (YUV NV21 format)                                    │
│        Size: 640x480 (varies with device)                              │
│        Timestamp: System.currentTimeMillis()                           │
│                                                                         │
└────────────────────────────┬────────────────────────────────────────────┘
                             │ (Background Thread - Executors.newSingleThreadExecutor)
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                  2. IMAGE CONVERSION & PREPROCESSING                    │
│                                                                         │
│  ImageProxy → Bitmap (or YUV buffer directly)                          │
│  Size: 640x480 RGB/YUV                                                │
│  Format: Bitmap.Config.ARGB_8888 (or YUV for faster processing)       │
│                                                                         │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                   3. MEDIAPIPE HOLISTIC PROCESSING                      │
│                                                                         │
│  Input: Bitmap (640x480)                                              │
│  Processing:                                                           │
│    ├─ Convert to MPImage                                              │
│    ├─ Run Holistic model                                              │
│    └─ Extract landmarks with confidence scores                        │
│                                                                         │
│  Output: 543 NormalizedLandmarks                                      │
│    ├─ Face: 468 landmarks (x, y, z, visibility)                       │
│    ├─ Pose: 33 landmarks (x, y, z, visibility)                        │
│    ├─ Left Hand: 21 landmarks (x, y, z, visibility)                   │
│    └─ Right Hand: 21 landmarks (x, y, z, visibility)                  │
│                                                                         │
│  Time: ~30-50ms                                                        │
│                                                                         │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│              4. LANDMARK SELECTION & FEATURE EXTRACTION                 │
│                                                                         │
│  LandmarkProcessor.extractSelectedLandmarks(543 landmarks)            │
│                                                                         │
│  TODO (From Hatice):                                                  │
│    Which 180 of 543 landmarks? (indices list needed)                  │
│                                                                         │
│  Assumed Selection:                                                    │
│    ├─ Pose: indices 0-32 (33 points)                                 │
│    ├─ Left Hand: indices 489-509 (21 points) from MediaPipe         │
│    ├─ Right Hand: indices 468-488 (21 points) from MediaPipe        │
│    └─ Face Subset: indices 0-104 (105 points selected)              │
│       [Exact face landmark selection TBD]                            │
│                                                                         │
│  For each landmark:                                                    │
│    output[i] = x                                                      │
│    output[i+1] = y                                                    │
│    output[i+2] = z                                                    │
│                                                                         │
│  Output: FloatArray (540)                                            │
│    = 180 landmarks × 3 coordinates (x, y, z)                        │
│                                                                         │
│  TODO (From Hatice):                                                  │
│    Normalization range? (x, y, z currently [0, 1] assumed)          │
│                                                                         │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    5. CIRCULAR FRAME BUFFER MANAGEMENT                  │
│                                                                         │
│  FrameBufferManager (size=30, feature=540)                           │
│                                                                         │
│  State (First 2 frames):                                              │
│    ┌─────────────────────────────────────────────────┐               │
│    │ Frame 0: [x0, y0, z0, x1, y1, z1, ..., x179, y179, z179]     │
│    │ Frame 1: [x0, y0, z0, x1, y1, z1, ..., x179, y179, z179]     │
│    │ Frame 2: [0, 0, 0, ..., 0]  (not yet filled)                  │
│    │ ...                                                            │
│    │ Frame 29: [0, 0, 0, ..., 0]  (not yet filled)                 │
│    └─────────────────────────────────────────────────┘               │
│                                                                       │
│  Buffer Size: 30 × 540 = 16,200 floats ≈ 64 KB                     │
│                                                                       │
│  When full (30 frames captured):                                     │
│    Total input size: (1, 30, 540) = 16,200 floats = 64 KB          │
│                                                                       │
│  Processing Time: ~33ms × 30 = ~1 second of capture                │
│                                                                       │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
              [Buffer NOT full] │ [Buffer IS full]
                             │         │
                             ↓         ↓
                        [Continue]  ┌────────────────────────────────────┐
                                    │  6. TFLITE MODEL INFERENCE        │
                                    │                                    │
                                    │  Input:  (1, 30, 540) FloatArray │
                                    │  Model:  LSTM (TFLite optimized)  │
                                    │  Output: (1, 20) FloatArray      │
                                    │                                    │
                                    │  Values: [p₀, p₁, ..., p₁₉]     │
                                    │  Where pᵢ = confidence of word i   │
                                    │                                    │
                                    │  Time: ~50-100ms                 │
                                    │                                    │
                                    └────────────────┬───────────────────┘
                                                     │
                                                     ↓
                    ┌────────────────────────────────────────────────────┐
                    │       7. POST-PROCESSING & PREDICTION              │
                    │                                                    │
                    │  Step 1: argmax(output)                          │
                    │    predictedIndex = 2  (for example)            │
                    │    confidence = output[2] = 0.87                │
                    │                                                  │
                    │  Step 2: Label Mapping                          │
                    │    label = labels[2] = "drink"                  │
                    │                                                  │
                    │  Step 3: Confidence Threshold Check             │
                    │    if confidence >= 0.70 → Show "drink"        │
                    │    else → Show "Uncertain"                      │
                    │                                                  │
                    │  Output: PredictionResult(                      │
                    │    predictedIndex: 2,                           │
                    │    confidence: 0.87,                            │
                    │    label: "drink",                              │
                    │    allProbabilities: [0.02, 0.05, 0.87, ...] │
                    │  )                                              │
                    │                                                  │
                    └────────────────┬─────────────────────────────────┘
                                     │
                                     ↓
        ┌────────────────────────────────────────────────────────────┐
        │           8. SMOOTHING FILTER (3-FRAME AVERAGING)          │
        │                                                            │
        │  Keep history of last 3 predictions:                      │
        │    [Prediction(frame 27): "drink" (0.87)]                │
        │    [Prediction(frame 28): "drink" (0.85)]                │
        │    [Prediction(frame 29): "drink" (0.89)]                │
        │                                                            │
        │  Majority Label: "drink" (3/3)                            │
        │  Average Confidence: (0.87 + 0.85 + 0.89) / 3 = 0.87    │
        │                                                            │
        │  if avgConfidence >= 0.70 AND majorityLabel != "Uncertain"│
        │    → Final prediction: "drink"                            │
        │                                                            │
        │  else                                                      │
        │    → Continue collecting frames (no UI update)            │
        │                                                            │
        └────────────────┬───────────────────────────────────────────┘
                         │
                         ↓
    ┌────────────────────────────────────────────────────────────┐
    │        9. UI STATE UPDATE (Main Thread)                   │
    │                                                            │
    │  ViewModel.updatePrediction(PredictionResult)            │
    │  LiveData: recognizedWord.value = "drink"                │
    │  LiveData: confidence.value = 0.87                       │
    │                                                            │
    │  Fragment observes LiveData and updates UI:              │
    │    ├─ Update RecognizedWordsView: "drink (87%)"         │
    │    ├─ Add to word history: ["hello", "bye", "drink"]   │
    │    └─ Redraw LandmarkOverlay (for debug visualization)  │
    │                                                            │
    └────────────────┬───────────────────────────────────────────┘
                     │
                     ↓
    ┌────────────────────────────────────────────────────────────┐
    │  10. OPTIONAL: BACKEND API CALL (When word complete)      │
    │                                                            │
    │  User words collected: ["hello", "bye", "drink"]          │
    │                                                            │
    │  API Request:                                              │
    │  POST /api/translate                                      │
    │  {                                                         │
    │    "words": ["hello", "bye", "drink"],                   │
    │    "language": "tr"                                       │
    │  }                                                         │
    │                                                            │
    │  Network: 10-200ms (depending on latency)                │
    │                                                            │
    │  API Response (200 OK):                                   │
    │  {                                                         │
    │    "sentence": "Merhaba, hoşça kalın.",                 │
    │    "confidence": 0.92,                                    │
    │    "alternatives": [...]                                 │
    │  }                                                         │
    │                                                            │
    │  ViewModel.translatedSentence.value = "Merhaba, hoşça kalın."
    │                                                            │
    │  Fragment updates UI:                                     │
    │    └─ TranslatedSentenceView: "Merhaba, hoşça kalın."   │
    │    └─ Optional TTS: Speak sentence                       │
    │                                                            │
    └────────────────────────────────────────────────────────────┘
```

### 9.2 Data Structure Transformations

```
Frame 0: ImageProxy (YUV 640x480)
         ↓
      [Bitmap]
         ↓
[543 NormalizedLandmarks: {x, y, z, visibility}]
         ↓
[180 Selected Landmarks: {x, y, z}]
         ↓
[FloatArray(540): x₀, y₀, z₀, x₁, y₁, z₁, ..., x₁₇₉, y₁₇₉, z₁₇₉]
         ↓ (accumulated in buffer 30×)
[FloatArray(16200): 30 frames × 540 features]
         ↓
[3D Tensor (1, 30, 540)]
         ↓
[TFLite Inference]
         ↓
[FloatArray(20): p₀, p₁, ..., p₁₉ (logits or probabilities)]
         ↓
[argmax → index, confidence]
         ↓
[Label Mapping: index → "drink"]
         ↓
[Confidence Threshold: 0.70 check]
         ↓
[3-Frame Smoothing Average]
         ↓
[Final Prediction: "drink" (87%)]
         ↓
[API Request: {words: ["drink"]}]
         ↓
[API Response: {sentence: "İçmek istiyorum."}]
         ↓
[UI Display + TTS]
```

---

## 10. Performans ve Optimizasyon Planı

### 10.1 Performance Targets

| Metrik | Target | Acceptable | Critical |
|--------|--------|-----------|----------|
| **FPS** | 30 FPS | 25+ FPS | <20 FPS |
| **MediaPipe Latency** | <40ms | <50ms | >100ms |
| **TFLite Inference** | <80ms | <100ms | >150ms |
| **Total Frame Time** | <130ms | <150ms | >200ms |
| **Memory Usage** | <100MB | <150MB | >300MB |
| **Battery Drain** | <10% / hour | <15% / hour | >30% / hour |
| **Startup Time** | <2s | <3s | >5s |

### 10.2 Memory Management

**Estimated Memory Breakdown:**
```
Component                           Memory    Notes
─────────────────────────────────────────────────────
CameraX PreviewView                ~20 MB    Frame buffers
MediaPipe Model (on GPU)           ~150 MB   Loaded weights
TFLite Model                       ~5 MB     Weights + buffers
FrameBuffer (30×540 float)         ~64 KB    Frame data
Android System + App               ~80 MB    Baseline
─────────────────────────────────────────────────────
TOTAL (approximate)                ~250 MB   Safe for 2GB RAM device
```

**Optimization Strategies:**

1. **Image Compression**
   ```kotlin
   // Reduce frame size for faster processing
   val preview = Preview.Builder()
       .setTargetResolution(Size(480, 360))  // Instead of 640×480
       .build()
   ```

2. **GPU Acceleration**
   ```kotlin
   val options = Interpreter.Options()
   options.setUseGpuDelegate(true)  // If GPU available
   // OR
   options.setUseNNAPI(true)        // Android acceleration
   ```

3. **Model Quantization**
   ```
   Ask Hatice: Is the model INT8 quantized?
   INT8 = 1/4 memory, slightly lower accuracy
   ```

4. **Threading**
   ```kotlin
   // Use background thread for heavy operations
   imageAnalyzer.setAnalyzer(
       Executors.newSingleThreadExecutor(),
       { imageProxy -> /* processing */ }
   )
   ```

5. **Avoid Frame Dropping**
   ```kotlin
   // KEEP_ONLY_LATEST: discard buffered frames, process latest
   imageAnalysis.setBackpressureStrategy(
       ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
   )
   ```

### 10.3 FPS Optimization Strategies

**If FPS drops below 25:**

1. **Skip Alternate Frames**
   ```kotlin
   var frameCounter = 0
   
   imageAnalyzer.setAnalyzer { imageProxy ->
       if (++frameCounter % 2 == 0) {
           // Process every 2nd frame only (15 FPS effective)
           processFrame(imageProxy)
       }
   }
   ```

2. **Reduce Resolution**
   ```kotlin
   // 640×480 → 480×360 (less compute)
   preview.setTargetResolution(Size(480, 360))
   ```

3. **Reduce MediaPipe Confidence**
   ```kotlin
   mediapibeService.setConfidenceThreshold(0.3f)  // Faster
   ```

4. **Batch Processing (if applicable)**
   ```kotlin
   // Process 2-3 frames together instead of 30 individual
   ```

### 10.4 Profiling & Monitoring

```kotlin
// Add timing measurements
class PerformanceMonitor {
    private val timings = mutableMapOf<String, Long>()
    
    fun startTiming(label: String) {
        timings[label] = System.currentTimeMillis()
    }
    
    fun endTiming(label: String) {
        val duration = System.currentTimeMillis() - (timings[label] ?: 0)
        LogUtils.d("Timing", "$label: ${duration}ms")
    }
    
    // Usage:
    // monitor.startTiming("MediaPipe")
    // val landmarks = mediapibeService.process(frame)
    // monitor.endTiming("MediaPipe")  // logs "MediaPipe: 35ms"
}
```

---

## 11. Olası Teknik Sorunlar ve Çözümleri

### 11.1 Landmark & Model Issues

| Sorun | Semptom | Çözüm |
|-------|---------|-------|
| **Landmark Index Mismatch** | Model tahminleri random | Hatice'den kesin indeks listesi al, test et |
| **Normalization Scale Mismatch** | Tahminler garbage | Hatice'den normalization range sor |
| **Model Not Loading** | "Model Not Loaded" hatası | assets/ klasöründe model dosyası olduğunu kontrol et |
| **Label Count Mismatch** | Index out of bounds | labels.txt'te 250 satır olduğunu doğrula |
| **Tensor Shape Mismatch** | "Shape mismatch" error | Input reshape doğru format (1, 30, 540) kontrol et |
| **Confidence Always 0** | Tahminler her zaman 0 | Model output post-processing doğru yapıldığını kontrol et |

### 11.2 Camera & MediaPipe Issues

| Sorun | Semptom | Çözüm |
|-------|---------|-------|
| **Camera Won't Start** | Black screen | CAMERA permission check, onCreate timing |
| **Frame Drops** | Stuttering FPS | setBackpressureStrategy(KEEP_ONLY_LATEST) |
| **MediaPipe Crash** | App crash after 10s | imageProxy.close() çağrıldığını kontrol et |
| **Landmarks Out of Sync** | Skeleton jerky | CameraX frame size MediaPipe input'a uyuyor mu kontrol et |
| **Low Visibility Landmarks** | Bilinmeyen noktalar | Aydınlık/açı kontrolü, kamera temizliği |
| **Memory Leak** | App crash ~2 min | mediapibeService.release() onDestroy'de çağrıl |

### 11.3 TFLite Issues

| Sorun | Semptom | Çözüm |
|-------|---------|-------|
| **Inference Always Returns 0** | Tahminler 0 olur | Model.tflite dosyası corrupted değil mi kontrol et |
| **Very High Latency (>200ms)** | Uygulamaya kayıp | GPU/NNAPI acceleration aç, ya da model boyutu kontrol et |
| **OutOfMemory Exception** | Crash after 1-2 inference | Model quantization (INT8) sor, bellek optimize et |
| **Input Tensor Shape Mismatch** | "Shape must be [1,30,540]" | reshape() doğru parametrelerle çağrıldığını kontrol et |

### 11.4 Network & API Issues

| Sorun | Semptom | Çözüm |
|-------|---------|-------|
| **Connection Refused** | API response null | Backend URL doğru mu? (localhost vs real IP) |
| **Socket Timeout** | 10s after API call | Backend yanıt yok, timeout=10s kontrol et |
| **401 Unauthorized** | API returns 401 | Auth token/headers gerekli mi? (MVP'de yok) |
| **No Internet** | Network error | Offline fallback: word.joinToString(" ") |
| **Malformed JSON** | Gson deserialization error | API response schema TranslateResponse'e uyuyor mu |

### 11.5 UI & State Issues

| Sorun | Semptom | Çözüm |
|-------|---------|-------|
| **Words Stuck on Screen** | Eski kelimeler silinmiyor | recognizedWords LiveData sıfırlanıyor mu? |
| **Overlay Lag** | Iskelet çizimi gecikmeli | Canvas invalidate() efficient, background thread'de draw |
| **Screen Rotation Crash** | Crash on rotate | ViewModel verileri saklanıyor mu? Fragment state |
| **Permission Dialog Loop** | Permission always asked | Runtime permission denied handling kontrol |

---

## 12. Test Planı

### 12.1 Unit Tests (Services)

```kotlin
// TFLiteService unit test example
@RunWith(AndroidTestRunner::class)
class TFLiteServiceTest {
    
    private lateinit var service: TFLiteService
    
    @Before
    fun setup() {
        service = TFLiteService(ApplicationProvider.getApplicationContext())
    }
    
    @Test
    fun testInferenceOutputShape() {
        val input = FloatArray(1 * 30 * 540)  // Random data
        val result = service.inference(input)
        
        assertEquals(result.predictedIndex >= 0, true)
        assertEquals(result.confidence in 0f..1f, true)
        assertEquals(result.allProbabilities.size, 20)
    }
    
    @Test
    fun testLabelMapping() {
        val input = FloatArray(1 * 30 * 540) { 0f }
        val result = service.inference(input)
        
        assertTrue(result.label in listOf(
            "hello", "bye", "drink", "computer", "book"
            // ... all 250 labels
        ))
    }
    
    @After
    fun tearDown() {
        service.close()
    }
}
```

### 12.2 Integration Tests (E2E)

```kotlin
@RunWith(AndroidTestRunner::class)
class TranslationIntegrationTest {
    
    @Test
    fun testFullPipeline() {
        // 1. Capture frame
        val frame = captureTestFrame()
        
        // 2. Process through MediaPipe
        val landmarks = mediapibeService.process(frame)
        assertEquals(landmarks.size, 543)
        
        // 3. Extract landmarks
        val processedFrame = landmarkProcessor.extractSelectedLandmarks(landmarks)
        assertEquals(processedFrame.size, 540)
        
        // 4. Add to buffer
        frameBufferManager.addFrame(processedFrame)
        
        // ... repeat until buffer full
        
        // 5. Run inference
        val prediction = tfliteService.inference(frameBufferManager.getBuffer())
        
        // 6. Verify output
        assertTrue(prediction.confidence >= 0f)
        assertTrue(prediction.label in labels)
    }
}
```

### 12.3 UI Tests (Espresso)

```kotlin
@RunWith(AndroidTestRunner::class)
class TranslationFragmentTest {
    
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun testRecognizedWordsDisplay() {
        // Wait for camera to start
        Thread.sleep(2000)
        
        // Check if recognized words view is visible
        onView(withId(R.id.recognizedWordsView))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testTranslateSentenceDisplay() {
        // Simulate prediction update
        // (Would need to mock ViewModel)
        
        onView(withId(R.id.translatedSentenceText))
            .check(matches(isDisplayed()))
    }
}
```

### 12.4 Performance Tests

```kotlin
// Benchmark landmark extraction speed
@BenchmarkRule
fun benchmarkMediaPipe() {
    val bitmap = createTestBitmap(640, 480)
    
    benchmarkRule.measureRepeated {
        mediapibeService.process(bitmap)
    }
}

// Benchmark TFLite inference
fun benchmarkTFLite() {
    val input = FloatArray(1 * 30 * 540)
    
    benchmarkRule.measureRepeated {
        tfliteService.inference(input)
    }
}
```

### 12.5 Manual Testing Checklist

- [ ] Camera starts on app launch
- [ ] Frame capture works (no black screen)
- [ ] MediaPipe landmarks visible on overlay
- [ ] FPS counter shows ~30 FPS
- [ ] Model inference runs every ~1 second
- [ ] Recognized words appear with confidence
- [ ] Words update when new gesture recognized
- [ ] Confidence threshold 0.70 works (uncertain words filtered)
- [ ] 3-frame smoothing stabilizes predictions
- [ ] API call translates words to sentence
- [ ] Sentence displays correctly
- [ ] App rotates without crash
- [ ] App works 5+ minutes without crash
- [ ] App works on different devices (API 26+)
- [ ] Offline mode works (fallback word joining)
- [ ] Low FPS handled gracefully
- [ ] Memory doesn't leak over time

---

## 13. Dosya / Klasör Yapısı Önerisi

```
sign-language-translator/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/signlanguage/translator/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── activities/
│   │   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   │   ├── PermissionHelper.kt
│   │   │   │   │   │   └── BaseActivity.kt
│   │   │   │   │   │
│   │   │   │   │   ├── fragments/
│   │   │   │   │   │   ├── TranslationFragment.kt
│   │   │   │   │   │   ├── HistoryFragment.kt
│   │   │   │   │   │   └── SettingsFragment.kt
│   │   │   │   │   │
│   │   │   │   │   ├── viewmodels/
│   │   │   │   │   │   ├── TranslationViewModel.kt
│   │   │   │   │   │   ├── HistoryViewModel.kt
│   │   │   │   │   │   └── SharedViewModel.kt
│   │   │   │   │   │
│   │   │   │   │   └── views/
│   │   │   │   │       ├── LandmarkOverlayView.kt
│   │   │   │   │       ├── ConfidenceBar.kt
│   │   │   │   │       └── WordDisplayView.kt
│   │   │   │   │
│   │   │   │   ├── data/
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── SignLanguageRepository.kt
│   │   │   │   │   │   ├── LocalDataRepository.kt
│   │   │   │   │   │   └── RemoteDataRepository.kt
│   │   │   │   │   │
│   │   │   │   │   └── model/
│   │   │   │   │       ├── PredictionResult.kt
│   │   │   │   │       ├── TranslateRequest.kt
│   │   │   │   │       ├── TranslateResponse.kt
│   │   │   │   │       └── WordHistory.kt
│   │   │   │   │
│   │   │   │   ├── domain/
│   │   │   │   │   ├── services/
│   │   │   │   │   │   ├── CameraService.kt
│   │   │   │   │   │   ├── MediaPipeService.kt
│   │   │   │   │   │   ├── TFLiteService.kt
│   │   │   │   │   │   ├── LandmarkProcessor.kt
│   │   │   │   │   │   ├── FrameBufferManager.kt
│   │   │   │   │   │   ├── APIService.kt
│   │   │   │   │   │   └── TextToSpeechService.kt
│   │   │   │   │   │
│   │   │   │   │   └── usecases/
│   │   │   │   │       ├── ProcessFrameUseCase.kt
│   │   │   │   │       ├── TranslateWordsUseCase.kt
│   │   │   │   │       └── SmoothedPredictionUseCase.kt
│   │   │   │   │
│   │   │   │   └── utils/
│   │   │   │       ├── Constants.kt
│   │   │   │       ├── Extensions.kt
│   │   │   │       ├── TensorUtils.kt
│   │   │   │       ├── PermissionUtils.kt
│   │   │   │       ├── LogUtils.kt
│   │   │   │       └── DebugConfig.kt
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── fragment_translation.xml
│   │   │   │   │   ├── fragment_history.xml
│   │   │   │   │   ├── fragment_settings.xml
│   │   │   │   │   └── item_word.xml
│   │   │   │   │
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── dimens.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── styles.xml
│   │   │   │   │   └── attrs.xml
│   │   │   │   │
│   │   │   │   ├── menu/
│   │   │   │   │   └── bottom_nav_menu.xml
│   │   │   │   │
│   │   │   │   └── drawable/
│   │   │   │       ├── ic_camera.xml
│   │   │   │       ├── ic_history.xml
│   │   │   │       └── ic_settings.xml
│   │   │   │
│   │   │   ├── assets/
│   │   │   │   ├── sign_language_model.tflite ⭐ (FROM HATICE)
│   │   │   │   ├── labels.txt ⭐ (FROM HATICE)
│   │   │   │   └── landmark_indices.json (TODO: FROM HATICE)
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── androidTest/
│   │   │   └── java/com/signlanguage/translator/
│   │   │       ├── TFLiteServiceTest.kt
│   │   │       ├── TranslationFragmentTest.kt
│   │   │       └── TranslationIntegrationTest.kt
│   │   │
│   │   └── test/
│   │       └── java/com/signlanguage/translator/
│   │           ├── FrameBufferManagerTest.kt
│   │           └── LandmarkProcessorTest.kt
│   │
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 14. Geliştirme Yol Haritası

### Week 1-2: Temel Altyapı

**Tasks:**
- [ ] Android Studio project setup
- [ ] CameraX integration & permission handling
- [ ] UI layouts (XML) oluştur
- [ ] Navigation setup (Fragments)
- [ ] Dependency injection structure (Hilt)
- [ ] Project modularization

**Deliverables:**
- ✅ Camera preview working
- ✅ Basic UI structure complete
- ✅ Permission flow implemented

---

### Week 2-3: MediaPipe & Landmark Processing

**Tasks (await Hatice - landmark indices):**
- [ ] MediaPipe Holistic integration
- [ ] Landmark extraction implementation
- [ ] LandmarkProcessor (180 landmark selection)
- [ ] LandmarkOverlayView (Canvas drawing)
- [ ] Performance profiling

**Deliverables:**
- ✅ Live landmark detection (543 points)
- ✅ Skeleton visualization on screen
- ✅ FPS counter visible

---

### Week 3-4: TFLite Model Integration

**Tasks (await Hatice - model + labels):**
- [ ] TFLite model loading
- [ ] Model inference pipeline
- [ ] FrameBufferManager (30-frame buffer)
- [ ] Post-processing (argmax, confidence)
- [ ] Smoothing filter (3-frame avg)

**Deliverables:**
- ✅ Model inference working
- ✅ Predicted words on screen
- ✅ Confidence % displayed
- ✅ <100ms inference latency

---

### Week 4-5: Backend API Integration

**Tasks (await Rabia - backend ready):**
- [ ] Retrofit setup
- [ ] API service definition
- [ ] Repository pattern implementation
- [ ] ViewModel API integration
- [ ] Error handling & offline fallback

**Deliverables:**
- ✅ API calls working (mock if backend not ready)
- ✅ Translated sentence displayed
- ✅ Network error handling

---

### Week 5-6: Optimization & Testing

**Tasks:**
- [ ] Performance tuning
- [ ] Memory optimization
- [ ] FPS profiling & optimization
- [ ] Unit tests
- [ ] Integration tests
- [ ] Manual testing on real devices

**Deliverables:**
- ✅ 30 FPS maintained
- ✅ <250 MB memory usage
- ✅ No crashes over 5+ minutes
- ✅ Works on API 26+

---

### Week 6-7: Polish & Documentation

**Tasks:**
- [ ] UI/UX refinements
- [ ] Error messages improvements
- [ ] Settings screen fully functional
- [ ] Code documentation (KDoc)
- [ ] README & setup guide

**Deliverables:**
- ✅ Production-ready app
- ✅ Full documentation
- ✅ Deployment ready

---

**Total Timeline: 7 weeks** for MVP (Camera → Recognition → API → Display)

---

## 15. Hatice'ye Sorulması Gerekenler (TODOs)

### 🔴 **CRITICAL - Modeli Nasıl Eğittin**

1. **180 Landmark Selection**
   - [ ] Modeli eğitirken hangi MediaPipe landmark'larını seçtin?
   - [ ] Indeks listesi var mı? (Örn: [0-33] pose, [468-488] right hand, ...)
   - [ ] Face landmarks dahil mi? Kaç tane face landmark kullandın?
   - [ ] Best practice: `landmark_indices.json` dosyasını share et

   **Format bekleniyor:**
   ```json
   {
     "selected_indices": [0, 1, 2, ..., 179],
     "total_landmarks": 180,
     "breakdown": {
       "pose": [0, 33],
       "face": [33, 138],
       "left_hand": [138, 159],
       "right_hand": [159, 180]
     }
   }
   ```

2. **Coordinate Normalization**
   - [ ] x, y, z değerleri hangi range'de?
     - [0, 1] normalized?
     - Pixel koordinatları (0-640)?
     - Başka bir scale?
   - [ ] MediaPipe çıktılarını direkt mi kullandın, yoksa ön-işlem yaptın mı?

3. **Model Validation & Accuracy**
   - [ ] Test set accuracy'si kaç?
   - [ ] Hangi kelimeler en sık yanlış tahmin ediliyor?
   - [ ] Edge case önerileri var mı?

4. **Label Order Confirmation**
   - [ ] 20 kelimenin sırası:
     ```
     hello, bye, drink, computer, book, yes, no, 
     thank you, please, help, name, what, where, 
     when, why, eat, water, more, finish, good
     ```
   - [ ] Bu sıra modelde hard-coded, değişmedi mi?

5. **Model File Details**
   - [ ] `.tflite` dosyasının tam path ve boyutu: ✅ (1.6-2.5 MB)
   - [ ] Input/Output shapes kesin: ✅ ((1,30,540) / (1,20))
   - [ ] Quantization type: INT8? Float32?
   - [ ] Expected inference time on CPU?

---

### 🟡 **IMPORTANT - Geliştirme Sırasında**

6. **Confidence Threshold Tuning**
   - [ ] 0.70 threshold uygun mu?
   - [ ] Hassas tanınması için önerilen threshold?
   - [ ] False positive rate kabul edilebilir mi?

7. **Real-Time Performance**
   - [ ] Kamera real-time ile test ettiniz mi?
   - [ ] 30 FPS'te model çalışıyor mu?
   - [ ] Gecikme sorunları yaşadınız mı?

8. **Edge Cases & Robustness**
   - [ ] Işık koşulları değiştiğinde model robust mu?
   - [ ] Hızlı el hareketleri mi, yavaş mı?
   - [ ] Bir kişi mi, birden fazla kişi mi?
   - [ ] Kamera açısı kritik mi?

---

### 🟢 **NICE TO HAVE - Gelecek Versiyonlar**

9. **Model Improvements**
   - [ ] Fine-tuning için training kodu share edilebilir mi?
   - [ ] Dropout/batch norm hiper parametreleri?
   - [ ] Model ensemble önerisi var mı?

10. **Debugging Support**
    - [ ] Modelin input preprocessing kodu paylaş
    - [ ] Test data sample (landmark arrays) gönderme imkanı?

---

### 📝 **Response Template for Hatice**

Hatice'den cevaplarını alırken şu şekilde formatla:

```markdown
## Model Details (Hatice'den Gelen)

### 1. Landmark Selection
- Total landmarks: 180
- Indices list: [0-33] pose, [489-509] left hand, [468-488] right hand, [...]
- Face landmarks: [X] Included | [X] Specific subset (indices: ...)

### 2. Normalization
- Coordinate range: [0, 1] normalized (MediaPipe standard)
- Preprocessing applied: None (raw MediaPipe output)

### 3. Model Accuracy
- Test accuracy: 92%
- Most confused pairs: (drink, water), (yes, no)

### 4. Other Details
- Inference time: 45-60ms on Snapdragon 660
- Confidence distribution: [histogram or stats]
```

---

## 📞 İletişim & Support

**Mobil Geliştirme Ekibi:**
- Teknik sorumlu: Mobil Lead
- Yardımcı: Mobil Developer

**Bağımlılık Zinciri:**
1. Hatice → Modeli ve landmark indekslerini hazırla
2. Ulaş → Android altyapısını kur
3. Rabia → Backend API'yi hazırla
4. Tüm ekip → Integration ve testing

---

## 📚 Referanslar & Resources

### Official Documentation
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [MediaPipe Holistic](https://developers.google.com/mediapipe/solutions/vision/holistic_pose_landmarker)
- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [Android Architecture Components](https://developer.android.com/topic/architecture)

### GitHub Examples
- [CameraX Sample](https://github.com/android/camera-samples)
- [MediaPipe Android Example](https://github.com/google/mediapipe/tree/master/mediapipe/examples/android)
- [TFLite Android Inference](https://github.com/tensorflow/examples/tree/master/lite/examples/image_classification/android)

### Libraries Used
- CameraX: `androidx.camera:camera-*:1.3.0`
- MediaPipe: `com.google.mediapipe:tasks-vision:0.10.9`
- TensorFlow Lite: `org.tensorflow:tensorflow-lite:2.13.0`
- Retrofit: `com.squareup.retrofit2:*:2.9.0`
- Coroutines: `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1`

---

## 📋 Document Metadata

- **Version**: 1.0
- **Date**: May 2026
- **Author**: Technical Analysis Team
- **Developer**: Ulaş (Mobil App Lead) & Feyza (Mobile Support)
- **Status**: Ready for Development
- **Last Updated**: [Current Date]
- **Next Review**: After Week 2 development

---

## ✅ Sign-Off

**Mobil Geliştirme Ekibi Onayı:**
- [ ] Teknik Lead - Mimarisini anlıyorum, başlamaya hazırım
- [ ] Mobile Developer - UI tasarımını ve workflow'u anladım

**Teknik Lider Onayı:**
- [ ] Belgeleme tam ve açık

**Proje Yöneticisi Onayı:**
- [ ] Zaman çizelgesi gerçekçi
- [ ] Bağımlılıklar tanımlanmış

---

**END OF DOCUMENT**

Sorular ve iyileştirmeler için lütfen bu dokümanın sonunda issue açın veya teknik ekiple iletişime geçin.
