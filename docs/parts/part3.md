# Part 3 — Eksikler

## 3.1 Kritik Olmayanlar

| Eksik | Spec Bölüm | Etki | Tahmin Süre |
|-------|------------|------|-------------|
| TTS manuel butonu | 4.2 | ✅ Tamamlandı | - |
| Kopyala butonu | 4.2 | ✅ Tamamlandı | - |
| RecyclerView History | 4.1 | ✅ Tamamlandı | - |
| Hilt DI | 3.2 ("Optional") | Düşük (test yazılabilir) | 1 gün |

## 3.2 Test Eksikleri

| Eksik | Spec Bölüm | Etki |
|-------|------------|------|
| `androidTest/` dizini tamamen yok | 12 | ✅ Tamamlandı |
| Espresso UI testleri | 12.3 | ✅ Temel demo pipeline testleri eklendi |
| Integration testler (TranslationIntegrationTest) | 12.2 | ⚠️ TFLite asset yükleme instrumented testi eklendi; tam pipeline testleri eksik |
| Benchmark testleri | 12.4 | Düşük |

## 3.3 Polish Eksikleri

- KDoc dokümantasyonu services/usecases için tamamlandı
- README setup guide güncellendi
- CHANGELOG başlatıldı
- Settings ekranı gerçek cihaz/emülatörde manuel smoke test edilmeli
