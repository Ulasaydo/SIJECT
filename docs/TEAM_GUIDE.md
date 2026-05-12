# SIJECT Takim Rehberi

Hazirlanma tarihi: 2026-05-12

Bu dosya, `docs/` klasorundeki tum Markdown dokumanlari ve mevcut Android kod yapisi incelenerek hazirlandi. Amaci yeni gelen veya farkli bir takimdan gelen bir kisinin projede neyin nerede oldugunu, calisma akisinin nasil ilerledigini, hangi kisimlarin tamamlandigini ve hangi risklerin kaldigini tek dosyadan anlayabilmesidir.

## 1. Proje Ozeti

SIJECT, gercek zamanli isaret dili tanima ve cumle cevirisi icin yazilmis tek modullu bir Android uygulamasidir. Uygulama kameradan frame alir, MediaPipe ile landmark cikarir, 30 frame'lik zaman penceresini TensorFlow Lite modeline verir, tahmini kelime olarak yorumlar, kabul edilen kelimeleri backend'e gonderip cumleye cevirir ve sonucu Android Text-to-Speech ile seslendirebilir.

Guncel hedef MVP'dir. Dokumanlara gore Android pipeline buyuk oranda kurulmus durumdadir; asil dogrulama ihtiyaci model kontrati, gercek cihaz performansi, label sirasi ve backend URL/endpoint uyumu etrafindadir.

## 2. Kaynak Dokumanlarin Durumu

Okunan Markdown kaynaklari:

| Dosya | Anlami |
|---|---|
| `docs/README.md` | Guncel proje ozeti, mimari, kurulum, QA ve roadmap. |
| `docs/SPEC_VS_REALITY_REPORT.md` | Eski spec ile mevcut implementasyon arasindaki durum raporu. |
| `docs/PROJECT_REVIEW_REPORT.md` | 2026-05-03 tarihli kod inceleme bulgulari. Bazi bulgular sonraki degisikliklerle kapanmis olabilir. |
| `docs/REMAINING_TASKS.md` | Son asama icin kalan dogrulama ve entegrasyon listesi. |
| `docs/HATICE_TODOS.md` | Model sahibinden beklenen model/landmark/label bilgileri. |
| `docs/CHANGELOG.md` | Son polish, test ve dokuman guncellemeleri. |
| `docs/parts/part1.md` - `part7_recheck.md` | Tamamlanan/kismen yapilan/eksik islerin parca parca durum notlari. |
| `docs/root_parts/part1.md` - `part5.md` | Ilk teknik spec'in bolunmus eski parcalari. |
| `docs/claude.md` | Ilk kapsamli teknik tasarim dokumani. Eski hedefleri anlatir; guncel kontrat icin tek basina kaynak alinmamalidir. |

Onemli ayrim:

- `docs/claude.md` ve `docs/root_parts/` eski tasarim notlarinda yer yer `180 landmark`, `[1, 30, 540]` input ve `[1, 20]` output anlatilir.
- Guncel kod ve guncel README artik `543 landmark`, `[1, 30, 1629]` input ve `[1, 250]` output kontratini kullanir.
- Bu rehberde "guncel durum" denildiginde mevcut kod ve guncel README/remaining task raporlari baz alinmistir.

## 3. Guncel Teknik Kontrat

Guncel sabitler `app/src/main/java/com/signlanguage/translator/utils/Constants.kt` icindedir:

```text
FRAME_BUFFER_SIZE        = 30
TOTAL_LANDMARK_COUNT     = 543
SELECTED_LANDMARK_COUNT  = 543
LANDMARK_AXIS_COUNT      = 3
MODEL_FEATURE_SIZE       = 1629
MODEL_OUTPUT_CLASSES     = 250
CONFIDENCE_THRESHOLD     = 0.70
SMOOTHING_WINDOW_SIZE    = 3
```

Model ve asset kontrati:

```text
Input  = [1, 30, 1629]
Output = [1, 250]
Landmark order = pose_33, face_468, left_hand_21, right_hand_21
```

Asset dosyalari:

| Dosya | Konum | Durum |
|---|---|---|
| `sign_language_model.tflite` | `app/src/main/assets/` | Var, yaklasik 12 MB. |
| `labels.txt` | `app/src/main/assets/` | 250 satir. Model output sinif sayisi ile uyumlu. |
| `landmark_indices.json` | `app/src/main/assets/` | 543 indeks, identity sirasi `0..542`. |
| `pose_landmarker.task` | `app/src/main/assets/` | Var. |
| `face_landmarker.task` | `app/src/main/assets/` | Var. |
| `hand_landmarker.task` | `app/src/main/assets/` | Var. |

Kritik not: `MediaPipeService` icinde performans icin `ENABLE_POSE_LANDMARKER = false` ve `ENABLE_FACE_LANDMARKER = false`. Bu nedenle guncel hizli modda el landmark'i aktif, pose ve face kisimlari sifir landmark ile doldurulur. Model egitiminde pose/face aktif kullanildiysa accuracy dogrudan etkilenir.

## 4. Repo Haritasi

Proje tek Android moduludur:

```text
SIJECT/
+-- app/                         Android uygulama modulu
+-- docs/                        Proje dokumanlari
+-- gradle/                      Gradle wrapper dosyalari
+-- build.gradle.kts             Root plugin versiyonu
+-- settings.gradle.kts          Root proje adi ve :app include
+-- gradle.properties            Gradle ayarlari
+-- local.properties             Lokal SDK/backend ayarlari, commit edilmez
+-- README.md                    Root README
```

Android kaynak agaci:

```text
app/src/main/
+-- AndroidManifest.xml
+-- assets/
+-- java/com/signlanguage/translator/
|   +-- data/
|   |   +-- model/
|   |   +-- repository/
|   +-- domain/
|   |   +-- services/
|   |   +-- usecases/
|   +-- ui/
|   |   +-- activities/
|   |   +-- fragments/
|   |   +-- viewmodels/
|   |   +-- views/
|   +-- utils/
+-- res/
    +-- drawable/
    +-- layout/
    +-- menu/
    +-- values/
```

## 5. Build ve Bagimliliklar

`settings.gradle.kts`:

- Root proje adi: `SIJECT`
- Tek modul: `:app`

`build.gradle.kts`:

- Android application plugin versiyonu: `9.2.0`

`app/build.gradle.kts`:

- Namespace/applicationId: `com.signlanguage.translator`
- compileSdk: `35`
- minSdk: `26`
- targetSdk: `34`
- versionName: `0.1.0`
- ViewBinding ve BuildConfig acik.
- `BACKEND_URL`, `local.properties` icinden okunur; yoksa default `http://10.0.2.2:8000/`.

Ana bagimliliklar:

| Alan | Kutuphaneler |
|---|---|
| Android UI | AppCompat, Fragment KTX, Lifecycle, RecyclerView, Material |
| Kamera | CameraX `camera-camera2`, `camera-lifecycle`, `camera-view` |
| Landmark | MediaPipe Tasks Vision |
| Model | TensorFlow Lite 2.14, Select TF Ops, lokal `tensorflow-lite-api-2.14.0-classes.jar` |
| Network | Retrofit, Gson converter, OkHttp, logging interceptor |
| Async | Kotlin Coroutines Android |
| Test | JUnit, AndroidX Test, Espresso |

## 6. Uygulama Calisma Akisi

Runtime akis:

```text
MainActivity
  -> CAMERA izni
  -> TranslationFragment
  -> CameraService
  -> ProcessFrameUseCase
  -> MediaPipeService
  -> LandmarkProcessor
  -> FrameBufferManager
  -> TFLiteService
  -> PredictionProcessor
  -> SmoothedPredictionUseCase
  -> TranslationViewModel
  -> SignLanguageRepository
  -> RemoteDataRepository / LocalDataRepository
  -> APIService / SharedPreferences
  -> UI + TextToSpeechService
```

Frame seviyesinde detay:

```text
CameraX ImageProxy
  -> MediaPipe Tasks detectForVideo()
  -> 543 LandmarkPoint listesi
  -> landmark_indices.json sirasina gore secim
  -> FloatArray(1629)
  -> 30 frame circular buffer
  -> FloatArray(30 * 1629)
  -> TFLite input [1, 30, 1629]
  -> model output [1, 250]
  -> top-3 aday + confidence
  -> 3 tahminlik smoothing
  -> kabul edilen kelime
  -> backend cevirisi veya offline fallback
```

Prediction her frame'de degil, buffer dolduktan sonra `ProcessFrameUseCase` icindeki `PREDICTION_INTERVAL_FRAMES = 5` nedeniyle her 5 frame'de bir calisir. Bu performans icin yapilmistir.

## 7. UI Katmani

Ana activity:

- `app/src/main/java/com/signlanguage/translator/ui/activities/MainActivity.kt`
- Kamera iznini ister.
- Demo pipeline modu varsa izin istemeden `TranslationFragment` acabilir.
- `openHistory()` ve `openSettings()` ile fragment navigation yapar.

Ana ekran:

- `TranslationFragment.kt`
- Kamera preview, landmark overlay, top-3 tahmin, cevrilen cumle, debug, settings, history, pipeline demo, TTS ve kopyalama aksiyonlarini baglar.
- `TranslationViewModel` state'ini observe eder.
- Ayar degisirse `onResume()` icinde kamera lens degisikligini kontrol edip kamerayi yeniden baslatir.

Ayarlar:

- `SettingsFragment.kt`
- `AppSettingsRepository` ile SharedPreferences uzerinden sunlari yazar:
  - on/arka kamera
  - confidence threshold, 0.5 - 0.9
  - landmark overlay
  - FPS counter
- Gecmis temizleme butonu `LocalDataRepository.clearHistory()` cagirir.

Gecmis:

- `HistoryFragment.kt`
- `HistoryViewModel` ile local history okur.
- `WordHistoryAdapter` ile RecyclerView listesi gosterir.
- Bos liste icin `history_empty` mesajini gosterir.

Ozel view'lar:

| Dosya | Gorev |
|---|---|
| `LandmarkOverlayView.kt` | Landmark noktalarini kamera uzerine cizer, on kamera icin mirror destekler. |
| `ConfidenceBar.kt` | Confidence degerini gorsellestirir. |
| `WordDisplayView.kt` | Kelime gosterimi icin custom view. |

## 8. Domain Katmani

| Dosya | Gorev |
|---|---|
| `CameraService.kt` | CameraX preview ve analysis use-case'lerini bind eder. Preview 640x480, analysis 160x120 hedefler. `STRATEGY_KEEP_ONLY_LATEST` ve `RGBA_8888` kullanir. `release()` executor'u kapatir. |
| `MediaPipeService.kt` | MediaPipe Tasks ile video mode landmark cikarir. Hand landmarker GPU ile denenir, hata olursa CPU fallback yapar. Pose/face su an devre disi. |
| `LandmarkProcessor.kt` | `landmark_indices.json` okur, 543 indeks bekler, gorunurlugu dusuk veya eksik landmark'lari sifirlar, modeli besleyecek FloatArray uretir. |
| `FrameBufferManager.kt` | 30 frame'lik circular buffer tutar ve temporal sirayi `getBufferOrdered()` ile korur. |
| `TFLiteService.kt` | Model ve label dosyasini yukler, input/output shape validasyonu yapar, NNAPI ile baslatir; basaramazsa CPU ile devam eder. Inference hatalarini exception olarak yuzeye tasir. |
| `PredictionProcessor.kt` | Raw output'u top-3 aday, label, confidence ve accepted durumuna cevirir. |
| `TextToSpeechService.kt` | Android TTS lifecycle wrapper'i. |
| `APIService.kt` | Retrofit interface: `GET /api/health`, `POST /api/translate`. |
| `RetrofitClient.kt` | `BuildConfig.BACKEND_URL` ile Retrofit/OkHttp client olusturur. Debug build'de body logging acar, release'te kapatir. |

Use case'ler:

| Dosya | Gorev |
|---|---|
| `ProcessFrameUseCase.kt` | Bir kamera frame'ini MediaPipe -> landmark -> buffer -> TFLite akisindan gecirir. |
| `SmoothedPredictionUseCase.kt` | Son 3 tahminde cogunluk ve ortalama confidence saglaninca accepted yapar. |
| `TranslateWordsUseCase.kt` | Kabul edilen kelime listesini repository uzerinden backend'e gonderir. |

## 9. Data Katmani

Model dosyalari:

| Dosya | Icerik |
|---|---|
| `LandmarkPoint.kt` | x/y/z/visibility landmark verisi. |
| `PredictionCandidate.kt` | Top-3 aday label/confidence verisi. |
| `PredictionResult.kt` | Nihai prediction verisi: index, label, confidence, accepted, adaylar, tum olasiliklar, inference sureleri. |
| `TranslationRequest.kt` | Backend request body. |
| `TranslationResponse.kt` | Backend response body. |
| `TranslationResult.kt` | Uygulama ici ceviri sonucu. |
| `WordHistory.kt` | Lokal gecmis kaydi. |

Repository dosyalari:

| Dosya | Gorev |
|---|---|
| `AppSettingsRepository.kt` | SharedPreferences ile kamera, threshold, overlay, FPS ayarlari. |
| `LocalDataRepository.kt` | Son 50 kabul edilen kelimeyi JSON olarak SharedPreferences'a yazar. |
| `RemoteDataRepository.kt` | Retrofit backend cagrilarini yapar, HTTP ve network hatalarini Result olarak dondurur. |
| `SignLanguageRepository.kt` | Local ve remote repository'leri birlestiren facade. |

## 10. Backend Entegrasyonu

Default backend:

```text
http://10.0.2.2:8000/
```

Bu sadece Android Emulator icin host makineyi isaret eder. Gercek telefonda backend makinesinin LAN IP'si gerekir:

```properties
BACKEND_URL=http://192.168.1.20:8000/
```

Bu deger lokal `local.properties` icine yazilir ve commit edilmez.

Beklenen endpoint'ler:

```text
GET  /api/health
POST /api/translate
```

Beklenen translate request:

```json
{
  "words": ["hello", "drink"],
  "language": "tr"
}
```

Beklenen translate response:

```json
{
  "sentence": "Merhaba, su istiyorum.",
  "confidence": 0.87,
  "alternatives": []
}
```

Backend offline ise `RemoteDataRepository` hata dondurur, `TranslationViewModel` accepted word listesini boslukla birlestirerek fallback cumle gosterir ve `backendOnline = false` yazar.

## 11. Model, Label ve Landmark Konulari

Guncel `labels.txt` 250 label icerir. Ilk label'lar `TV`, `after`, `airplane`, `all`, `alligator` seklinde baslar; son label'lar `why`, `will`, `wolf`, `yellow`, `yes`, `yesterday`, `yourself`, `yucky`, `zebra`, `zipper` seklinde biter. Bu eski dokumanlardaki 20 kelimelik ornek listeden farklidir.

`landmark_indices.json`:

```text
status = configured
source_total_landmarks = 543
indices length = 543
indices = 0..542 identity order
```

Bu dosya modelin su an tum 543 landmark'i kullandigi varsayimina gore ayarlanmistir. Eger model sahibi farkli bir landmark secimi, farkli normalizasyon veya farkli label sirasi dogrularsa su dosyalar birlikte guncellenmelidir:

- `app/src/main/assets/sign_language_model.tflite`
- `app/src/main/assets/labels.txt`
- `app/src/main/assets/landmark_indices.json`
- `app/src/main/java/com/signlanguage/translator/utils/Constants.kt`
- `app/src/test/java/com/signlanguage/translator/domain/services/TFLiteServiceTest.kt`
- `app/src/test/java/com/signlanguage/translator/domain/services/LandmarkProcessorTest.kt`

Hatice/model sahibinden hala netlestirilmesi gerekenler:

- Egitim sirasinda kullanilan landmark sirasi Android'deki `pose_33, face_468, left_hand_21, right_hand_21` ile birebir ayni mi?
- Egitimde koordinatlar raw MediaPipe `[0, 1]` normalizasyonu ile mi kullanildi?
- `z` ekseni icin ek preprocessing var mi?
- 250 label sirasi model output index sirasi ile birebir ayni mi?
- Gercek cihazda beklenen confidence araligi ve threshold nedir?
- Test accuracy ve en cok karisan label ciftleri nelerdir?

## 12. Performans Durumu

Dokumanlardaki hedefler:

| Metrik | Hedef | Kabul edilebilir | Kritik |
|---|---:|---:|---:|
| FPS | 30 | 25+ | <20 |
| MediaPipe latency | <40 ms | <50 ms | >100 ms |
| TFLite inference | <80 ms | <100 ms | >150 ms |
| Total frame time | <130 ms | <150 ms | >200 ms |
| Crash olmadan calisma | 5+ dakika | 5+ dakika | crash |

Guncel dokumanlardaki sinir:

- Samsung A21s gibi dusuk seviye cihazda fiziksel FPS yaklasik 6-9 FPS olarak gozlenmis.
- Bu nedenle pose/face landmarker kapali, hand landmarker aktif hizli mod kullaniliyor.
- Analysis resolution `160x120`, preview resolution `640x480`.
- TFLite prediction her 5 frame'de bir calisiyor.

Performans dusukse ilk bakilacak yerler:

- `MediaPipeService` icindeki pose/face flag'leri.
- `CameraService` analysis resolution.
- `ProcessFrameUseCase.PREDICTION_INTERVAL_FRAMES`.
- Hand landmarker GPU fallback loglari.
- `PerformanceMonitor` loglari ve UI latency/FPS textleri.

## 13. Testler ve Calistirma

Derleme:

```bash
./gradlew :app:assembleDebug
```

macOS'ta terminal Java bulamazsa Android Studio JBR ile:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

Unit test:

```bash
./gradlew testDebugUnitTest
```

Android test APK:

```bash
./gradlew :app:assembleDebugAndroidTest
```

Bagli cihaz/emulator instrumented test:

```bash
./gradlew connectedDebugAndroidTest
```

Manuel kurulum:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.signlanguage.translator/.ui.activities.MainActivity
```

Mevcut test dosyalari:

| Dosya | Amac |
|---|---|
| `FrameBufferManagerTest.kt` | Circular buffer sirasi ve reset davranisi. |
| `LandmarkProcessorTest.kt` | Landmark indeks parse/validasyon. |
| `PredictionProcessorTest.kt` | Top-3, threshold ve accepted davranisi. |
| `TFLiteServiceTest.kt` | Tensor shape kontrati. |
| `SmoothedPredictionUseCaseTest.kt` | 3 tahminlik smoothing. |
| `TranslationPipelineSimulationTest.kt` | Pipeline simule akis. |
| `PerformanceMonitorTest.kt` | Timing yardimcisi. |
| `MainActivityTest.kt` | Espresso ile demo pipeline/settings navigation. |
| `TFLiteServiceInstrumentedTest.kt` | Gercek asset ile TFLite service yukleme. |

## 14. Manuel QA Checklist

Merge oncesi ozellikle kamera, model, backend veya UI degisikliklerinde:

- Kamera izni dogru isteniyor.
- Kamera preview siyah degil.
- On/arka kamera ayari calisiyor.
- Landmark overlay on kamerada mirror/rotation olarak dogru.
- Pose/face kapali oldugunda beklenen zero-fill davranisi biliniyor.
- App elde landmark yokken crash olmuyor.
- FPS ve latency textleri guncelleniyor.
- Top-3 tahmin ve confidence degerleri gorunuyor.
- Ayni label arka arkaya geldiyse duplicate accepted word eklenmiyor.
- Confidence threshold settings'ten degisince prediction davranisina yansiyor.
- Backend kapaliyken offline fallback cumle gosteriliyor.
- Backend acikken `/api/health` ve `/api/translate` calisiyor.
- TTS placeholder metni seslendirmiyor, gercek cumleyi seslendiriyor.
- Kopyala butonu placeholder metni kopyalamiyor.
- History ekrani accepted word listesini gosteriyor.
- History clear hem settings hem history uzerinden calisiyor.
- Gercek cihazda en az 5 dakika crash olmadan calisiyor.

## 15. Bilinen Riskler ve Kalan Isler

Yuksek oncelik:

1. Model kontratini gercek cihazda Logcat ile dogrula:

```text
SIJECT:TFLite Input shape: [...]
SIJECT:TFLite Output shape: [...]
```

Beklenen:

```text
[1, 30, 1629] -> [1, 250]
```

2. Hatice/model sahibi ile landmark sirasi, normalizasyon ve label sirasi birebir dogrulanmali.

3. Gercek cihazda camera + MediaPipe + TFLite + backend E2E smoke test yapilmali.

4. Fiziksel cihaz backend URL'i her gelistiricide `local.properties` ile ayarlanmali.

Orta/dusuk oncelik:

- `connectedDebugAndroidTest` gercek cihaz/emulatorde kosulmali.
- Benchmark/performance testleri eklenmeli.
- Hilt DI opsiyonel olarak degerlendirilebilir.
- Cihazlar arasi FPS/latency matrisi tutulmali.
- Release build icin logging, cleartext traffic ve backend config stratejisi tekrar gozden gecirilmeli.

## 16. Sik Karsilasilan Problemler

| Problem | Olasilik | Bakilacak yer |
|---|---|---|
| Model yuklenmiyor | Asset eksik, TFLite uyumsuzlugu, shape mismatch | `TFLiteService.kt`, `Constants.kt`, `app/src/main/assets/` |
| Tahminler anlamsiz | Landmark order/normalization mismatch, pose/face zero-fill | `MediaPipeService.kt`, `LandmarkProcessor.kt`, `landmark_indices.json` |
| Top-3 bos veya confidence dusuk | Model output, threshold, input frame buffer | `PredictionProcessor.kt`, `SmoothedPredictionUseCase.kt` |
| Kamera siyah ekran | Permission, CameraX bind, lifecycle | `MainActivity.kt`, `CameraService.kt` |
| FPS cok dusuk | MediaPipe maliyeti, resolution, prediction interval | `MediaPipeService.kt`, `CameraService.kt`, `ProcessFrameUseCase.kt` |
| Backend baglanmiyor | Emulator/telefon URL farki | `local.properties`, `RetrofitClient.kt` |
| History gorunmuyor | Accepted prediction yok veya local history temiz | `LocalDataRepository.kt`, `HistoryViewModel.kt` |
| TTS calismiyor | TTS init hazir degil veya placeholder metin | `TextToSpeechService.kt`, `TranslationFragment.kt` |

## 17. Ekip Sorumluluklari

Dokumanlarda gecen rol dagilimi:

| Kisi/Takim | Sorumluluk |
|---|---|
| Mobil ekip | Android mimarisi, CameraX, MediaPipe baglantisi, TFLite inference, UI, settings/history, TTS, performans. |
| Hatice | Model dosyasi, label sirasi, landmark secimi/sirasi, preprocessing/normalization, accuracy bilgisi. |
| Rabia | Backend API, `/api/health`, `/api/translate`, response schema ve dev/prod URL stratejisi. |
| Tum ekip | Gercek cihaz entegrasyon testi, model accuracy dogrulamasi, final QA. |

## 18. Git ve Repo Hijyeni

Commit edilmemesi gereken dosyalar:

- `local.properties`
- `.idea/`
- `.claude/`
- `.gradle/`
- `.DS_Store`
- `build/`
- `app/build/`

Takim akisi:

```bash
git pull
git checkout -b feature/<kisa-ad>
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug
```

PR veya merge oncesi acikca yazilmasi gerekenler:

- Ne degisti?
- Nasil test edildi?
- Gercek cihaz testi yapildi mi?
- Model/backend varsayimi var mi?
- Dokuman guncellemesi gerekiyor mu?

## 19. En Kisa Onboarding Rotasi

Yeni gelen biri icin onerilen okuma ve kontrol sirasi:

1. Bu dosyayi bastan sona oku.
2. `docs/README.md` ile guncel ozet ve komutlari teyit et.
3. `app/src/main/java/com/signlanguage/translator/utils/Constants.kt` ile model kontratini kontrol et.
4. `TranslationViewModel.kt` ve `ProcessFrameUseCase.kt` ile asil runtime akisina bak.
5. `MediaPipeService.kt`, `LandmarkProcessor.kt`, `TFLiteService.kt` dosyalarini birlikte incele.
6. `RetrofitClient.kt` ve `APIService.kt` ile backend baglantisini kontrol et.
7. `REMAINING_TASKS.md` ile kapanmamis validasyon maddelerini gor.
8. Degisiklik yapmadan once `./gradlew testDebugUnitTest` calistir.

## 20. Son Karar Noktalari

Projeyi "gercek urun gibi calisiyor" demeden once su kararlar kapanmali:

- Model sahibi guncel Android inputunun egitim inputuyla ayni oldugunu onayladi mi?
- Pose/face kapali hizli mod accuracy icin kabul edilebilir mi?
- 250 label sirasi model output sirasi ile birebir ayni mi?
- Gercek telefon backend'e ulasabiliyor mu?
- 5+ dakika kamera/model smoke testi crash olmadan gecti mi?
- Dusuk FPS kabul ediliyor mu, yoksa mode/quality secimi mi eklenecek?

Bu maddeler kapanmadan proje hackathon/MVP demosu icin kullanilabilir seviyede gorunse de, model accuracy ve gercek cihaz performansi icin "tam dogrulandi" denmemelidir.
