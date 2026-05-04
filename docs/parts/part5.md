# Part 5 — Kalan İş Planı

## Sprint 1 — UI Polish

```
[x] fragment_translation.xml'e TTS Oynat butonu ekle
    → onClick: textToSpeechService.speak(currentSentence)
[x] Kopyala butonu ekle
    → ClipboardManager ile clip kopyala, Toast göster
[ ] Settings ekranını gerçek cihaz/emülatörde manuel test et
```

## Sprint 2 — Test Coverage

```
[x] app/src/androidTest/ dizinini oluştur
[x] MainActivityTest (Espresso) - 2 test:
    - Demo pipeline translation controls görünür
    - Settings ekranı açılır
[x] TFLiteService instrumented test (gerçek asset ile)
[ ] connectedDebugAndroidTest gerçek cihaz/emülatörde koştur
```

## Sprint 3 — Documentation

```
[x] Public API'lara KDoc ekle (services, usecases)
[x] README.md setup adımları
[x] CHANGELOG.md başlat
```

## Sprint 4 (Opsiyonel) — Refinements

```
[x] HistoryFragment RecyclerView'a geçiş
[ ] Hilt DI entegrasyonu
[ ] Benchmark testleri
[ ] Hatice ile end-to-end model doğrulama
```

**Kalan tahmini süre:** gerçek cihaz/model doğrulaması hariç 0.5-1 gün polish; cihaz/model doğrulaması ekip ve cihaz erişimine bağlı.
