# SIJECT — Spec vs Gerçek Durum Raporu

**Proje:** Gemma 4 Destekli İşaret Dili Çeviri Uygulaması (Android)
**Tarih:** 2026-05-03
**Referans Doküman:** `app/CLAUDE.md` (15 bölüm)
**Durum:** ~%93 MVP tamamlandı

---

## 📑 İçindekiler

1. [Part 1 — Tamamlanan Bölümler](#part-1--tamamlanan-bölümler)
2. [Part 2 — Kısmen Yapılanlar](#part-2--kısmen-yapılanlar)
3. [Part 3 — Eksikler](#part-3--eksikler)
4. [Part 4 — Hafta Bazında İlerleme](#part-4--hafta-bazında-i̇lerleme)
5. [Part 5 — Kalan İş Planı](#part-5--kalan-i̇ş-planı)
6. [Part 6 — Hatice'ye Sorular Durumu](#part-6--haticeye-sorular-durumu)

---

## Part 1 — Tamamlanan Bölümler

### 1.1 Bölüm 3.3 — Paket Yapısı (TAM)

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

### 1.2 Bölüm 5 — CameraX

- `CameraService.kt` — RGBA_8888 format
- `STRATEGY_KEEP_ONLY_LATEST` backpressure
- Lifecycle-aware, executor shutdown
- Ön/arka kamera seçimi

### 1.3 Bölüm 6 — MediaPipe

- 3 ayrı landmarker (pose / face / hand)
- 543 nokta birleştirme
- **VIDEO mode** + `detectForVideo()` (timestamp ile)
- `ensureLandmarkers()` cache (frame başına I/O yok)
- Stride-safe bitmap kopyalama

### 1.4 Bölüm 7 — TFLite

- TFLiteService (NNAPI, 4 thread)
- Tensor shape validation
- FrameBufferManager (circular + `getBufferOrdered()`)
- PredictionProcessor (argmax, top-3, threshold)

### 1.5 Bölüm 8 — Backend API

- Retrofit + OkHttp + GsonConverter
- BuildConfig.BACKEND_URL
- 10s timeout, health check
- Offline fallback (`words.joinToString(" ")`)

### 1.6 Bölüm 9 — Veri Akışı

`ProcessFrameUseCase` end-to-end pipeline:
MediaPipe → LandmarkProcessor → FrameBuffer → TFLite → PredictionProcessor → Smoothing.

### 1.7 Bölüm 10 — Performans

- `utils/PerformanceMonitor.kt` mevcut
- FPS counter UI'da, settings toggle ile gösterim
- Inference latency tracking

### 1.8 Bölüm 15 — Assets

- `sign_language_model.tflite` ✅
- `labels.txt` ✅
- `landmark_indices.json` (569 satır, `status: configured`) ✅
- 3 MediaPipe `.task` dosyası ✅

### 1.9 TTS Entegrasyonu

- `TextToSpeechService` gerçek `android.speech.tts.TextToSpeech` ile bağlı
- Backend yanıtı sonrası otomatik konuşma
- `onCleared()`'da `shutdown()`

---

## Part 2 — Kısmen Yapılanlar

### 2.1 Bölüm 4.2 — UI Butonları

Spec satırı: `[🔊 TTS Oynat] [📋 Kopyala] [🔄 Yenile]`

| Buton | Durum |
|-------|-------|
| 🔄 Yenile | ✅ Var |
| 🔊 TTS Oynat (manuel) | ✅ Var |
| 📋 Kopyala | ✅ Var |

### 2.2 Bölüm 4.1 — History UI

- RecyclerView + Adapter yapısına geçirildi
- Boş liste durumunda `history_empty` mesajı gösteriliyor
- Geçmiş temizleme akışı korunuyor

### 2.3 Bölüm 12 — Testler

7 dosya, **19 @Test**:

| Dosya | Test Sayısı |
|-------|-------------|
| FrameBufferManagerTest | 4 |
| LandmarkProcessorTest | 4 |
| PredictionProcessorTest | 3 |
| TFLiteServiceTest | 3 |
| SmoothedPredictionUseCaseTest | 3 |
| TranslationPipelineSimulationTest | 1 |
| PerformanceMonitorTest | 1 |

Build green: `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL

Instrumented testler:

| Dosya | Test Sayısı |
|-------|-------------|
| MainActivityTest | 2 |
| TFLiteServiceInstrumentedTest | 1 |

Android test APK derleme green: `./gradlew :app:assembleDebugAndroidTest` → BUILD SUCCESSFUL

---

## Part 3 — Eksikler

### 3.1 Kapatılan Polish Eksikleri

| Eksik | Spec Bölüm | Etki | Tahmin Süre |
|-------|------------|------|-------------|
| TTS manuel butonu | 4.2 | ✅ Tamamlandı | - |
| Kopyala butonu | 4.2 | ✅ Tamamlandı | - |
| RecyclerView History | 4.1 | ✅ Tamamlandı | - |
| README setup guide | 12 / Doc | ✅ Tamamlandı | - |
| CHANGELOG | Doc | ✅ Başlatıldı | - |
| Public API KDoc | Doc | ✅ Services/usecases için eklendi | - |

### 3.2 Test Eksikleri

| Eksik | Spec Bölüm | Etki |
|-------|------------|------|
| `androidTest/` dizini tamamen yok | 12 | ✅ Tamamlandı |
| Espresso UI testleri | 12.3 | ✅ Temel demo pipeline testleri eklendi |
| Integration testler (TranslationIntegrationTest) | 12.2 | ⚠️ TFLite asset yükleme instrumented testi eklendi; tam cihaz pipeline testi eksik |
| Benchmark testleri | 12.4 | Düşük |

### 3.3 Açık Kalanlar

- Gerçek cihaz/emülatörde `connectedDebugAndroidTest` koşturulmalı.
- Gerçek cihazda 5+ dakika kamera/model/backend smoke testi yapılmalı.
- Model sahibi ile `543 landmark / [1, 30, 1629] / [1, 250]` kontratı ve label sırası doğrulanmalı.
- Benchmark testleri ve Hilt DI opsiyonel iyileştirme olarak kalıyor.

---

## Part 4 — Hafta Bazında İlerleme

CLAUDE.md Bölüm 14 yol haritasına göre:

| Hafta | Kapsam | Tamamlanma |
|-------|--------|------------|
| **W1-2** Temel Altyapı | CameraX, UI, Navigation, Permission | **%95** ✅ |
| **W2-3** MediaPipe | Landmark + overlay + VIDEO mode | **%95** ✅ |
| **W3-4** TFLite | Inference + buffer + post-process | **%95** ✅ |
| **W4-5** Backend API | Retrofit + repo + TTS | **%90** ✅ |
| **W5-6** Test + Optimizasyon | 19 unit test, temel androidTest/Espresso, monitor var | **%75** ⚠️ |
| **W6-7** Polish + Doc | UI butonları, RecyclerView, README, KDoc, CHANGELOG tamam | **%85** ✅ |

**Net MVP:** ~%93

---

## Part 5 — Kalan İş Planı

### Sprint 1 — UI Polish

```
[x] fragment_translation.xml'e TTS Oynat butonu ekle
    → onClick: textToSpeechService.speak(currentSentence)
[x] Kopyala butonu ekle
    → ClipboardManager ile clip kopyala, Toast göster
[ ] Settings ekranını gerçek cihaz/emülatörde manuel test et
```

### Sprint 2 — Test Coverage

```
[x] app/src/androidTest/ dizinini oluştur
[x] MainActivityTest (Espresso) - 2 test:
    - Demo pipeline translation controls görünür
    - Settings ekranı açılır
[x] TFLiteService instrumented test (gerçek asset ile)
[ ] connectedDebugAndroidTest gerçek cihaz/emülatörde koştur
```

### Sprint 3 — Documentation

```
[x] Public API'lara KDoc ekle (services, usecases)
[x] README.md setup adımları
[x] CHANGELOG.md başlat
```

### Sprint 4 (Opsiyonel) — Refinements

```
[x] HistoryFragment RecyclerView'a geçiş
[ ] Hilt DI entegrasyonu
[ ] Benchmark testleri
[ ] Hatice ile end-to-end model doğrulama
```

**Kalan tahmini süre:** gerçek cihaz/model doğrulaması hariç 0.5-1 gün polish; cihaz/model doğrulaması ekip ve cihaz erişimine bağlı.

---

## Part 6 — Hatice'ye Sorular Durumu

CLAUDE.md Bölüm 15'teki sorular:

### 🔴 Critical (Çözüldü mü?)

| Soru | Durum |
|------|-------|
| Landmark seçimi | ✅ `landmark_indices.json` 543 indeksle configured |
| Coordinate normalization | ⚠️ Asset'te belirtilmiş ama **end-to-end gerçek cihaz/model ile doğrulama** yapılmamış |
| Model accuracy | ❓ Hatice'den bilgi yok (skor) |
| Label order | ✅ `labels.txt` 250 label ile model output kontratına uyumlu |
| Model file details | ✅ `.tflite` mevcut, `[1, 30, 1629] → [1, 250]` shape validation kod tarafında var |

### 🟡 Important

| Soru | Durum |
|------|-------|
| Confidence threshold (0.70) | ✅ Settings'ten ayarlanabilir |
| Real-time performance | ⚠️ Emülatörde test edildi, gerçek cihaz testi gerekli |
| Edge cases (light, speed) | ❓ Manuel test yapılmadı |

### 🟢 Nice to Have

| Soru | Durum |
|------|-------|
| Fine-tuning training kodu | ❓ Talep edilmedi |
| Test data sample | ❓ Talep edilmedi |

---

## 📌 Özet

**Çalışır durumda:** Tam pipeline (camera → landmark → inference → API → TTS).
**Tamamlanan son işler:** UI'da TTS/kopyala butonları, RecyclerView history, temel androidTest/Espresso, TFLite instrumented test, README, CHANGELOG, KDoc.
**Eksik:** Gerçek cihazda instrumented test koşumu, benchmark testleri, Hilt DI (opsiyonel), Hatice ile model/label kontratı doğrulaması.
**Risk:** Gerçek cihaz/gerçek model ile end-to-end accuracy testi yapılmadı; label sayısı artık model output kontratıyla uyumlu.

**Hackathon teslimi için kullanılabilir** — kalan işler "polish" kategorisinde.

---

*Rapor sonu — kanıt için ilgili dosya yolları gerekiyorsa `app/src/main/java/com/signlanguage/translator/` ağacına bakın.*
