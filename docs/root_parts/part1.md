# 🚀 Gemma 4 Destekli İşaret Dili Çeviri Uygulaması
## Mobil Geliştirme (Android) - Teknik Doküman | Part 1
### Bölümler: Sorumluluk Alanı · Genel Amaç · Android Mimari Tasarımı

---

## 📑 İçindekiler (Bu Bölüm)
1. [Projedeki Sorumluluk Alanım](#1-projedeki-sorumluluk-alanım)
2. [Mobil Uygulama Genel Amacı](#2-mobil-uygulama-genel-amacı)
3. [Android Mimari Tasarımı](#3-android-mimari-tasarımı)

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
│   ├── labels.txt (20 word labels, line by line) ⭐ FROM HATICE
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

**→ Devam: [part2.md](part2.md) — UI / Kullanıcı Akışı & CameraX Entegrasyonu**
