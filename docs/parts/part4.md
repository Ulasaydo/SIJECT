# Part 4 — Hafta Bazında İlerleme

CLAUDE.md Bölüm 14 yol haritasına göre:

| Hafta | Kapsam | Tamamlanma |
|-------|--------|------------|
| **W1-2** Temel Altyapı | CameraX, UI, Navigation, Permission | **%95** ✅ |
| **W2-3** MediaPipe | Landmark + overlay + VIDEO mode | **%95** ✅ |
| **W3-4** TFLite | Inference + buffer + post-process | **%95** ✅ |
| **W4-5** Backend API | Retrofit + repo + TTS | **%90** ✅ |
| **W5-6** Test + Optimizasyon | 19 unit test, temel androidTest/Espresso, monitor var | **%75** ⚠️ |
| **W6-7** Polish + Doc | UI butonları ve RecyclerView tamam, KDoc/CHANGELOG eksik | **%70** ⚠️ |

**Net MVP:** ~%92

## Güncelleme Notları

- Manuel TTS oynatma ve kopyalama butonları eklendi.
- History ekranı RecyclerView + Adapter yapısına geçirildi.
- `androidTest/` altında temel Espresso testleri ve TFLite asset yükleme testi eklendi.
- Açık kalan başlıklar: tam instrumented pipeline testleri, benchmark testleri, KDoc ve CHANGELOG.
