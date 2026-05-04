# 🚀 Gemma 4 Destekli İşaret Dili Çeviri Uygulaması
## Mobil Geliştirme (Android) - Teknik Doküman | Part 5
### Bölümler: Performans · Teknik Sorunlar · Test Planı · Dosya Yapısı · Yol Haritası · TODOs

---

## 📑 İçindekiler (Bu Bölüm)
10. [Performans ve Optimizasyon Planı](#10-performans-ve-optimizasyon-planı)
11. [Olası Teknik Sorunlar ve Çözümleri](#11-olası-teknik-sorunlar-ve-çözümleri)
12. [Test Planı](#12-test-planı)
13. [Dosya / Klasör Yapısı Önerisi](#13-dosya--klasör-yapısı-önerisi)
14. [Geliştirme Yol Haritası](#14-geliştirme-yol-haritası)
15. [Hatice'ye Sorulması Gerekenler (TODOs)](#15-hatiçeye-sorulması-gerekenler-todos)

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
   imageAnalyzer.setAnalyzer(
       Executors.newSingleThreadExecutor(),
       { imageProxy -> /* processing */ }
   )
   ```

5. **Avoid Frame Dropping**
   ```kotlin
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
           processFrame(imageProxy)
       }
   }
   ```

2. **Reduce Resolution**
   ```kotlin
   preview.setTargetResolution(Size(480, 360))
   ```

3. **Reduce MediaPipe Confidence**
   ```kotlin
   mediapibeService.setConfidenceThreshold(0.3f)
   ```

### 10.4 Profiling & Monitoring

```kotlin
class PerformanceMonitor {
    private val timings = mutableMapOf<String, Long>()

    fun startTiming(label: String) {
        timings[label] = System.currentTimeMillis()
    }

    fun endTiming(label: String) {
        val duration = System.currentTimeMillis() - (timings[label] ?: 0)
        LogUtils.d("Timing", "$label: ${duration}ms")
    }
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
| **Very High Latency (>200ms)** | Uygulamaya kayıp | GPU/NNAPI acceleration aç |
| **OutOfMemory Exception** | Crash after 1-2 inference | Model quantization (INT8) sor, bellek optimize et |
| **Input Tensor Shape Mismatch** | "Shape must be [1,30,540]" | reshape() doğru parametrelerle çağrıldığını kontrol et |

### 11.4 Network & API Issues

| Sorun | Semptom | Çözüm |
|-------|---------|-------|
| **Connection Refused** | API response null | Backend URL doğru mu? (localhost vs real IP) |
| **Socket Timeout** | 10s after API call | Backend yanıt yok, timeout=10s kontrol et |
| **401 Unauthorized** | API returns 401 | Auth token/headers gerekli mi? |
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
@RunWith(AndroidTestRunner::class)
class TFLiteServiceTest {

    private lateinit var service: TFLiteService

    @Before
    fun setup() {
        service = TFLiteService(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testInferenceOutputShape() {
        val input = FloatArray(1 * 30 * 540)
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
        val frame = captureTestFrame()

        val landmarks = mediapibeService.process(frame)
        assertEquals(landmarks.size, 543)

        val processedFrame = landmarkProcessor.extractSelectedLandmarks(landmarks)
        assertEquals(processedFrame.size, 540)

        frameBufferManager.addFrame(processedFrame)
        // ... repeat until buffer full

        val prediction = tfliteService.inference(frameBufferManager.getBuffer())

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
        Thread.sleep(2000)
        onView(withId(R.id.recognizedWordsView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTranslateSentenceDisplay() {
        onView(withId(R.id.translatedSentenceText))
            .check(matches(isDisplayed()))
    }
}
```

### 12.4 Performance Tests

```kotlin
@BenchmarkRule
fun benchmarkMediaPipe() {
    val bitmap = createTestBitmap(640, 480)
    benchmarkRule.measureRepeated {
        mediapibeService.process(bitmap)
    }
}

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
│   │   │   │   │   ├── activities/   (MainActivity, PermissionHelper, BaseActivity)
│   │   │   │   │   ├── fragments/    (TranslationFragment, HistoryFragment, SettingsFragment)
│   │   │   │   │   ├── viewmodels/   (TranslationViewModel, HistoryViewModel, SharedViewModel)
│   │   │   │   │   └── views/        (LandmarkOverlayView, ConfidenceBar, WordDisplayView)
│   │   │   │   │
│   │   │   │   ├── data/
│   │   │   │   │   ├── repository/   (SignLanguageRepository, LocalDataRepository, RemoteDataRepository)
│   │   │   │   │   └── model/        (PredictionResult, TranslateRequest, TranslateResponse, WordHistory)
│   │   │   │   │
│   │   │   │   ├── domain/
│   │   │   │   │   ├── services/     (CameraService, MediaPipeService, TFLiteService,
│   │   │   │   │   │                  LandmarkProcessor, FrameBufferManager, APIService, TTS)
│   │   │   │   │   └── usecases/     (ProcessFrameUseCase, TranslateWordsUseCase, SmoothedPredictionUseCase)
│   │   │   │   │
│   │   │   │   └── utils/            (Constants, Extensions, TensorUtils, PermissionUtils, LogUtils, DebugConfig)
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── layout/           (activity_main.xml, fragment_translation.xml, ...)
│   │   │   │   ├── values/           (strings.xml, dimens.xml, colors.xml, styles.xml, attrs.xml)
│   │   │   │   ├── menu/             (bottom_nav_menu.xml)
│   │   │   │   └── drawable/         (ic_camera.xml, ic_history.xml, ic_settings.xml)
│   │   │   │
│   │   │   ├── assets/
│   │   │   │   ├── sign_language_model.tflite  ⭐ FROM HATICE
│   │   │   │   ├── labels.txt                  ⭐ FROM HATICE
│   │   │   │   └── landmark_indices.json       ⭐ TODO: FROM HATICE
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── androidTest/  (TFLiteServiceTest, TranslationFragmentTest, TranslationIntegrationTest)
│   │   └── test/         (FrameBufferManagerTest, LandmarkProcessorTest)
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
- [ ] Android Studio project setup
- [ ] CameraX integration & permission handling
- [ ] UI layouts (XML) oluştur
- [ ] Navigation setup (Fragments)
- [ ] Dependency injection structure (Hilt)

**Deliverables:** Camera preview working · Basic UI · Permission flow

---

### Week 2-3: MediaPipe & Landmark Processing
- [ ] MediaPipe Holistic integration
- [ ] Landmark extraction implementation
- [ ] LandmarkProcessor (180 landmark selection)
- [ ] LandmarkOverlayView (Canvas drawing)
- [ ] Performance profiling

**Deliverables:** Live landmark detection (543 pts) · Skeleton visualization · FPS counter

---

### Week 3-4: TFLite Model Integration
- [ ] TFLite model loading
- [ ] Model inference pipeline
- [ ] FrameBufferManager (30-frame buffer)
- [ ] Post-processing (argmax, confidence)
- [ ] Smoothing filter (3-frame avg)

**Deliverables:** Model inference · Predicted words on screen · <100ms latency

---

### Week 4-5: Backend API Integration
- [ ] Retrofit setup
- [ ] API service definition
- [ ] Repository pattern implementation
- [ ] ViewModel API integration
- [ ] Error handling & offline fallback

**Deliverables:** API calls working · Translated sentence displayed

---

### Week 5-6: Optimization & Testing
- [ ] Performance tuning
- [ ] Memory optimization
- [ ] FPS profiling
- [ ] Unit tests · Integration tests · Manual device testing

**Deliverables:** 30 FPS · <250 MB · No crashes 5+ min · API 26+ support

---

### Week 6-7: Polish & Documentation
- [ ] UI/UX refinements
- [ ] Settings screen
- [ ] Code documentation (KDoc)
- [ ] README & setup guide

**Deliverables:** Production-ready app · Full documentation

**Total Timeline: 7 weeks** for MVP

---

## 15. Hatice'ye Sorulması Gerekenler (TODOs)

### 🔴 CRITICAL - Modeli Nasıl Eğittin

1. **180 Landmark Selection**
   - [ ] Hangi MediaPipe landmark'ları seçildi?
   - [ ] İndeks listesi: (Örn: [0-33] pose, [468-488] right hand, ...)
   - [ ] Face landmarks dahil mi? Kaç tane?

   **Beklenen format:**
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
   - [ ] x, y, z değerleri hangi range'de? ([0,1] / pixel / other)
   - [ ] MediaPipe çıktıları direkt mi, yoksa ön-işlem var mı?

3. **Model Validation & Accuracy**
   - [ ] Test set accuracy'si?
   - [ ] En sık yanlış tahmin edilen kelimeler?

4. **Label Order Confirmation**
   - [ ] 20 kelimenin sırası (hello, bye, drink, computer, book, yes, no, ...)
   - [ ] Model'de hard-coded, değişmedi mi?

5. **Model File Details**
   - [ ] Quantization type: INT8 mi Float32 mi?
   - [ ] Expected inference time on CPU?

---

### 🟡 IMPORTANT - Geliştirme Sırasında

6. **Confidence Threshold Tuning**
   - [ ] 0.70 threshold uygun mu?
   - [ ] Önerilen threshold değeri?

7. **Real-Time Performance**
   - [ ] Kamera real-time ile test edildi mi?
   - [ ] 30 FPS'te model çalışıyor mu?

8. **Edge Cases & Robustness**
   - [ ] Işık koşulları değiştiğinde model robust mu?
   - [ ] Kamera açısı kritik mi?

---

### 🟢 NICE TO HAVE

9. **Model Improvements**
   - [ ] Fine-tuning için training kodu paylaşılabilir mi?

10. **Debugging Support**
    - [ ] Input preprocessing kodu paylaş
    - [ ] Test data sample (landmark arrays) gönderme imkanı?

---

## 📞 Bağımlılık Zinciri

1. **Hatice** → Modeli ve landmark indekslerini hazırla
2. **Ulaş** → Android altyapısını kur
3. **Rabia** → Backend API'yi hazırla
4. **Tüm ekip** → Integration ve testing

---

## 📚 Referanslar

- [CameraX Documentation](https://developer.android.com/training/camerax)
- [MediaPipe Holistic](https://developers.google.com/mediapipe/solutions/vision/holistic_pose_landmarker)
- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [Android Architecture Components](https://developer.android.com/topic/architecture)

---

**← Önceki: [part4.md](part4.md) | Doküman Sonu**
