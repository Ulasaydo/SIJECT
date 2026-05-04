# Part 2 — Kısmen Yapılanlar

## 2.1 Bölüm 4.2 — UI Butonları

Spec satırı: `[🔊 TTS Oynat] [📋 Kopyala] [🔄 Yenile]`

| Buton | Durum |
|-------|-------|
| 🔄 Yenile | ✅ Var |
| 🔊 TTS Oynat (manuel) | ✅ Var |
| 📋 Kopyala | ✅ Var |

## 2.2 Bölüm 4.1 — History UI

- RecyclerView + Adapter yapısına geçirildi
- Boş liste durumunda `history_empty` mesajı gösteriliyor
- Geçmiş temizleme akışı korunuyor

## 2.3 Bölüm 12 — Unit Testler

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
