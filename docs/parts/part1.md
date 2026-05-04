# Part 1 — Tamamlanan Bölümler

**Proje:** SIJECT — İşaret Dili Çeviri Uygulaması
**Referans:** `app/CLAUDE.md`

---

## 1.1 Bölüm 3.3 — Paket Yapısı (TAM)

Spec'teki tüm dosyalar mevcut.

| Katman | Dosyalar | Durum |
|--------|----------|-------|
| `ui/activities/` | BaseActivity, MainActivity, PermissionHelper | ✅ |
| `ui/fragments/` | TranslationFragment, HistoryFragment, SettingsFragment | ✅ |
| `ui/viewmodels/` | TranslationViewModel, HistoryViewModel, SharedViewModel | ✅ |
| `ui/views/` | LandmarkOverlayView, ConfidenceBar, WordDisplayView | ✅ |
| `data/model/` | PredictionResult, TranslationRequest/Response, WordHistory | ✅ |
| `data/repository/` | SignLanguageRepository, LocalDataRepository, RemoteDataRepository | ✅ |
| `domain/services/` | Camera, MediaPipe, TFLite, LandmarkProcessor, FrameBuffer, API, TTS | ✅ |
| `domain/usecases/` | ProcessFrame, TranslateWords, SmoothedPrediction | ✅ |
| `utils/` | Constants, Extensions, TensorUtils, Permission, Log, Debug, PerformanceMonitor | ✅ |

## 1.2 Bölüm 5 — CameraX

- `CameraService.kt` — RGBA_8888 format
- `STRATEGY_KEEP_ONLY_LATEST` backpressure
- Lifecycle-aware, executor shutdown
- Ön/arka kamera seçimi

## 1.3 Bölüm 6 — MediaPipe

- 3 ayrı landmarker (pose / face / hand)
- 543 nokta birleştirme
- **VIDEO mode** + `detectForVideo()` (timestamp ile)
- `ensureLandmarkers()` cache (frame başına I/O yok)
- Stride-safe bitmap kopyalama

## 1.4 Bölüm 7 — TFLite

- TFLiteService (NNAPI, 4 thread)
- Tensor shape validation
- FrameBufferManager (circular + `getBufferOrdered()`)
- PredictionProcessor (argmax, top-3, threshold)

## 1.5 Bölüm 8 — Backend API

- Retrofit + OkHttp + GsonConverter
- BuildConfig.BACKEND_URL
- 10s timeout, health check
- Offline fallback (`words.joinToString(" ")`)

## 1.6 Bölüm 9 — Veri Akışı

`ProcessFrameUseCase` end-to-end pipeline:
MediaPipe → LandmarkProcessor → FrameBuffer → TFLite → PredictionProcessor → Smoothing.

## 1.7 Bölüm 10 — Performans

- `utils/PerformanceMonitor.kt` mevcut
- FPS counter UI'da, settings toggle ile gösterim
- Inference latency tracking

## 1.8 Bölüm 15 — Assets

- `sign_language_model.tflite` ✅
- `labels.txt` ✅
- `landmark_indices.json` (569 satır, `status: configured`) ✅
- 3 MediaPipe `.task` dosyası ✅

## 1.9 TTS Entegrasyonu

- `TextToSpeechService` gerçek `android.speech.tts.TextToSpeech` ile bağlı
- Backend yanıtı sonrası otomatik konuşma
- `onCleared()`'da `shutdown()`
