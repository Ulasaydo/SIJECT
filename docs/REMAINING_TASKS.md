# SIJECT Kalan Gorevler

Bu liste, `sign_language_model.tflite` dosyasi projeye eklendikten sonra son asamaya gelmek icin tamamlanmasi gereken isleri icerir.

## Mevcut Durum

- [x] Android proje iskeleti kuruldu.
- [x] CameraX preview ve frame analyzer eklendi.
- [x] MVVM, repository ve use case yapisi eklendi.
- [x] TFLite model dosyasi eklendi:
  - `app/src/main/assets/sign_language_model.tflite`
- [x] `labels.txt` dosyasi 250 label ile guncellendi.
- [x] Backend API icin Retrofit altyapisi eklendi.
- [x] Offline fallback eklendi.
- [x] Unit test altyapisi eklendi.
- [x] Temel androidTest/Espresso altyapisi eklendi.
- [x] UI polish: manuel TTS, kopyalama ve RecyclerView history tamamlandi.

## Kritik Kalan Isler

### 1. Landmark Indeks Listesini Dogrula

Su dosya configured durumda:

```text
app/src/main/assets/landmark_indices.json
```

Kontrol:

- [x] `indices` tam 543 eleman iceriyor.
- [ ] Indeksler MediaPipe'in 543 landmark sirasiyla uyumlu olmali.
- [ ] Android tarafindaki secim sirasi, egitimdeki sirayla birebir ayni olmali.

### 2. Model Tensor Shape Kontrolu

Uygulama acildiginda Logcat'te su loglari kontrol et:

```text
SIJECT:TFLite Input shape: [...]
SIJECT:TFLite Output shape: [...]
```

Kod tarafinda dogrulanan kontrat:

```text
Input  = [1, 30, 1629]
Output = [1, 250]
```

Kontrol:

- [x] Kod ve unit test kontrati `[1, 30, 1629] -> [1, 250]`.
- [ ] Logcat'te gercek cihaz/emulatorde ayni shape goruldu mu?
- [ ] Degilse `TFLiteService` modele gore uyarlanacak.

### 3. Label Sirasi Dogrula

Su dosya guncel:

```text
app/src/main/assets/labels.txt
```

Mevcut sira:

```text
hello
bye
drink
computer
book
yes
no
thank you
please
help
name
what
where
when
why
eat
water
more
finish
good
```

Kontrol:

- [x] Bu sira model output indeksleriyle birebir ayni mi?
- [x] Model sahibi bu sirayi onayladi mi?
- [x] Model output 250 sinif; tum siniflar `labels.txt` icinde mevcut.

### 4. Gercek MediaPipe Entegrasyonu

`MediaPipeService`, MediaPipe Tasks pipeline'ini VIDEO mode ile bagliyor.

Yapilacak:

- [x] Gercek MediaPipe Tasks pipeline'i baglandi.
- [x] Kamera frame'i MediaPipe'in bekledigi image formatina cevriliyor.
- [x] Face, pose, left hand, right hand landmark sirasi kodda sabitlendi.
- [x] Eksik landmark durumunda sifir doldurma stratejisi var.
- [x] `MediaPipeService.release()` native kaynaklari serbest birakiyor.
- [ ] Gercek cihazda uzun sureli smoke test yapildi mi?

### 5. Gercek Cihazda Model Testi

Android cihazda veya emulator'da uygulamayi calistir.

Kontrol:

- [ ] Kamera izni isteniyor.
- [ ] Kamera goruntusu geliyor.
- [ ] Uygulama crash olmadan aciliyor.
- [ ] TFLite model yukleniyor.
- [ ] Inference hata vermiyor.
- [ ] Top 3 tahmin ekranda gorunuyor.
- [ ] Confidence degerleri 0-1 araliginda.
- [ ] Yanlis label gorunuyorsa label sirasi tekrar kontrol ediliyor.

### 6. Backend URL Kontrolu

Varsayilan URL:

```kotlin
http://10.0.2.2:8000/
```

Dosya:

```text
app/src/main/java/com/signlanguage/translator/domain/services/RetrofitClient.kt
```

Kontrol:

- [ ] Emulator kullaniliyorsa `10.0.2.2` dogru.
- [ ] Gercek cihaz kullaniliyorsa backend bilgisayarinin LAN IP'si yazilmali.
- [ ] `GET /api/health` calisiyor.
- [ ] `POST /api/translate` beklenen response'u donuyor.

### 7. Performans ve Stabilite Testi

Hedefler:

- FPS: 25+
- TFLite inference: <100 ms
- Total frame time: <150 ms
- Crash olmadan calisma: 5+ dakika

Kontrol:

- [ ] FPS counter 25 altina dusuyor mu?
- [ ] Inference latency 100 ms ustune cikiyor mu?
- [ ] 5 dakika kullanimda memory leak veya crash var mi?
- [ ] Kamera kapatip acinca sorun oluyor mu?
- [ ] Backend kapaliyken offline fallback calisiyor mu?

## Calistirilacak Komutlar

Debug APK derleme:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

Unit testler:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
```

Android test APK derleme:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebugAndroidTest
```

Bagli cihaz/emulatorde instrumented test:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDebugAndroidTest
```

## Son Asamada Bana Geri Don

Asagidakiler hazir olunca final entegrasyon icin geri don:

- [x] `landmark_indices.json` 543 indeksle dolu.
- [ ] Model input/output shape Logcat'te gercek cihaz/emulatorde dogrulandi.
- [ ] Label sirasi model sahibi tarafindan onaylandi.
- [ ] Backend URL netlesti.
- [ ] Gercek cihazda ilk kamera ve model testi yapildi.
- [ ] Tahminlerde gorulen problem varsa ornek log veya ekran goruntusu hazir.

Sonraki final isler:

- Gercek MediaPipe cikisini baglama.
- Tensor shape veya normalization uyumsuzlugu varsa duzeltme.
- Threshold tuning.
- FPS optimizasyonu.
- Son UI ve teslim temizligi.
