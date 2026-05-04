# SIJECT Proje Inceleme Raporu

Tarih: 2026-05-03

Bu rapor, mevcut Android projesinin klasor yapisi, kaynak kodlari, asset dosyalari, build durumu ve tespit edilen hata/eksikler uzerinden hazirlanmistir.

## Dogrulama Durumu

Asagidaki komut Android Studio gomulu JBR ile calistirildi ve basarili oldu:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug testDebugUnitTest --rerun-tasks
```

Sonuc:

- Debug APK derlemesi basarili.
- Unit testler basarili.
- Build sirasinda native `.so` dosyalarinin strip edilemedigine dair debug build icin kritik olmayan bir uyari goruldu.

## Proje Yapisi

Proje tek modullu Android uygulamasi olarak yapilandirilmis:

```text
app/
  src/main/java/com/signlanguage/translator/
    data/
      model/
      repository/
    domain/
      services/
      usecases/
    ui/
      activities/
      fragments/
      viewmodels/
      views/
    utils/
  src/main/assets/
  src/main/res/
  src/test/
docs/
gradle/
```

Genel mimari MVVM, repository ve use case katmanlarina ayrilmis. Bu ayrim okunabilirlik acisindan iyi, ancak bazi servisler henuz gercek entegrasyon yerine fallback veya placeholder davranisla calisiyor.

## Review Findings

### Finding 1: [P1] Model input shape kodla uyusmuyor

Dosya:

```text
app/src/main/java/com/signlanguage/translator/utils/Constants.kt:7-11
```

README ve kalan isler dokumani modelin `[1, 30, 540]` yani 180 landmark x 3 eksen bekledigini soyluyor. Kod ise `SELECTED_LANDMARK_COUNT = TOTAL_LANDMARK_COUNT = 543` yapip modele `[1, 30, 1629]` besliyor.

Bu durum gercek TFLite inference sirasinda shape hatasina veya surekli fallback tahminlere yol acabilir.

Oncelik: P1

Oneri:

- `SELECTED_LANDMARK_COUNT` degeri modelin bekledigi landmark sayisiyla uyumlu hale getirilmeli.
- Model input shape Logcat uzerinden dogrulanmali.
- `landmark_indices.json` ve `Constants.kt` ayni tensor sozlesmesini kullanmali.

### Finding 2: [P1] MediaPipe gercek frame islemiyor

Dosya:

```text
app/src/main/java/com/signlanguage/translator/domain/services/MediaPipeService.kt:15-22
```

`extractLandmarks` kamera goruntusunden landmark cikarmiyor; timestamp bazli sentetik 543 nokta uretiyor. Bu yuzden kamera aciliyor olsa bile uygulama su an gercek isaret dili tanima yapmiyor.

Oncelik: P1

Oneri:

- Gercek MediaPipe Tasks pipeline baglanmali.
- CameraX `ImageProxy`, MediaPipe'in bekledigi image formatina dogru cevrilmeli.
- Pose, face, left hand ve right hand landmark sirasi netlestirilmeli.
- Eksik landmark durumlari icin sifir doldurma stratejisi test edilmeli.

### Finding 3: [P1] Inference hatalari fallback ile gizleniyor

Dosya:

```text
app/src/main/java/com/signlanguage/translator/domain/services/TFLiteService.kt:55-58
```

Model yuklenemezse veya `activeInterpreter.run` hata verirse servis sessizce fallback tahmine geciyor. Bu gelistirme icin kullanisli olabilir, ancak uretim benzeri testlerde shape, model veya asset hatalarini gizler ve sahte basarili tahmin akisi olusturur.

Oncelik: P1

Oneri:

- Debug/development fallback davranisi ile gercek hata modu ayrilmali.
- Inference hatalari UI state veya Logcat uzerinden acik sekilde gorunur olmali.
- Model yuklenemedi veya shape uyusmadi gibi durumlar fallback tahmin yerine net hata olarak raporlanmali.

### Finding 4: [P2] Landmark indeks dosyasi placeholder

Dosya:

```text
app/src/main/java/com/signlanguage/translator/domain/services/LandmarkProcessor.kt:21-37
app/src/main/assets/landmark_indices.json
```

Asset dosyasindaki `indices` alani bos. Kod bos veya yanlis uzunluk gorunce default olarak ilk landmark indekslerini kullanmaya geciyor. Dokumantasyon ise egitimde kullanilan 180 indeksin birebir ayni sirayla girilmesini istiyor.

Bu tamamlanmadan model dogrulugu saglanamaz.

Oncelik: P2

Oneri:

- Model egitiminde kullanilan gercek 180 landmark indeksi alinmali.
- `landmark_indices.json` dosyasina ayni sirayla yazilmali.
- Kod, indeks sayisi beklenen degerden farkliysa sessiz fallback yerine hata veya uyari uretmeli.

### Finding 5: [P2] Ayarlar ekrani davranisa bagli degil

Dosya:

```text
app/src/main/java/com/signlanguage/translator/ui/fragments/SettingsFragment.kt:1-6
```

Ayarlar layout'unda kamera secimi, confidence threshold, overlay, FPS ve gecmis temizleme kontrolleri var. Ancak `SettingsFragment` sadece statik layout aciyor. Bu kontroller hicbir servise, ViewModel'e veya kalici ayara baglanmadigi icin kullanici acisindan calismiyor.

Oncelik: P2

Oneri:

- Ayarlar icin ViewModel veya shared preferences entegrasyonu eklenmeli.
- Kamera secimi `CameraService` ile baglanmali.
- Threshold degeri tahmin kabul mekanizmasina uygulanmali.
- Overlay ve FPS switchleri UI state'e baglanmali.
- Gecmis temizleme butonu gercek local history depolamasini temizlemeli.

## Ek Bulgular

### Label ve sinif sayisi net degil

`labels.txt` dosyasi 20 label iceriyor. Dokumanda model output shape `[1, 20]` olarak belirtilmis. Ancak `Constants.kt` icinde `MODEL_OUTPUT_CLASSES = 250` tanimli.

Bu fallback tahminlerde ve output mapping tarafinda karisiklik yaratabilir.

### Gecmis ekrani gercek gecmis gostermiyor

`LocalDataRepository` sadece son tahmini shared preferences icine kaydediyor. Gecmis ekrani ise statik bos durum metni gosteriyor.

Eksikler:

- Listeleme yok.
- Silme yok.
- Zaman bazli gecmis yok.
- `HistoryViewModel` aktif kullanilmiyor.

### Backend URL hardcoded

Backend URL su dosyada sabit:

```text
app/src/main/java/com/signlanguage/translator/domain/services/RetrofitClient.kt
```

Varsayilan URL:

```text
http://10.0.2.2:8000/
```

Bu emulator icin dogru olabilir, ancak fiziksel cihazda backend makinesinin LAN IP adresi gerekir.

### HTTP body logging tum buildlerde acik

`HttpLoggingInterceptor.Level.BODY` tum istek ve cevap govdelerini loglar. Debug icin yararli olsa da production benzeri buildlerde kapatilmalidir.

### CameraService executor kapatilmiyor

`CameraService` icinde `Executors.newSingleThreadExecutor()` kullaniliyor. `stopCamera()` yalnizca camera provider'i unbind ediyor, executor kapatilmiyor. Fragment tekrar acilip kapandikca kaynak sizintisi riski olusabilir.

### Repo hijyeni daginik

`git status` cok sayida `AD`, `AM` ve untracked dosya gosteriyor. Eski `com.example.siject` yapisindan yeni `com.signlanguage.translator` yapisina gecis yapilmis gibi duruyor.

Dikkat edilmesi gerekenler:

- Hangi dosyalarin gercekten projede kalacagi netlestirilmeli.
- `.DS_Store` gibi yerel sistem dosyalari temizlenmeli.
- Eski silinmis dosyalar ve yeni untracked dosyalar kontrollu sekilde stage edilmeli.

## Oncelikli Yapilacaklar

1. Model tensor sozlesmesini netlestir:
   - Model input shape nedir?
   - 180 landmark mi, 543 landmark mi kullaniliyor?
   - Output sinif sayisi 20 mi, 250 mi?

2. `landmark_indices.json` dosyasini gercek indekslerle tamamla.

3. `Constants.kt` degerlerini model sozlesmesine gore duzelt.

4. Gercek MediaPipe entegrasyonunu bagla.

5. TFLite fallback davranisini debug modu ile sinirla veya hatalari UI'da gorunur yap.

6. Ayarlar ekranini gercek uygulama state'i ile bagla.

7. Gecmis ekranini gercek local data ile bagla.

8. Backend URL ve logging ayarlarini build type veya config uzerinden yonet.

## Genel Degerlendirme

Proje derlenebilir durumda ve temel mimari ayrimlari kurulmus. Ancak su anki uygulama gercek isaret dili tanima urunu olmaktan cok MVP iskeleti ve entegrasyon hazirligi seviyesinde.

En kritik risk model pipeline uyumsuzlugu ve MediaPipe'in gercek veri uretmemesi. Bu iki konu cozulmeden UI veya backend tarafindaki iyilestirmeler uygulamanin dogru tanima yapmasini saglamaz.
