# Part 6 — Hatice'ye Sorular Durumu

CLAUDE.md Bölüm 15'teki sorular:

## 🔴 Critical (Çözüldü mü?)

| Soru | Durum |
|------|-------|
| Landmark seçimi | ✅ `landmark_indices.json` 543 indeksle configured |
| Coordinate normalization | ⚠️ Asset'te belirtilmiş ama **end-to-end gerçek cihaz/model ile doğrulama** yapılmamış |
| Model accuracy | ❓ Hatice'den bilgi yok (skor) |
| Label order | ✅ `labels.txt` 250 label ile model output kontratına uyumlu |
| Model file details | ✅ `.tflite` mevcut, `[1, 30, 1629] → [1, 250]` shape validation kod tarafında var |

## 🟡 Important

| Soru | Durum |
|------|-------|
| Confidence threshold (0.70) | ✅ Settings'ten ayarlanabilir |
| Real-time performance | ⚠️ Emülatörde test edildi, gerçek cihaz testi gerekli |
| Edge cases (light, speed) | ❓ Manuel test yapılmadı |

## 🟢 Nice to Have

| Soru | Durum |
|------|-------|
| Fine-tuning training kodu | ❓ Talep edilmedi |
| Test data sample | ❓ Talep edilmedi |

---

## 📌 Genel Özet

**Çalışır durumda:** Tam pipeline (camera → landmark → inference → API → TTS).
**Tamamlanan son işler:** UI'da TTS/kopyala butonları, RecyclerView history, temel androidTest/Espresso, TFLite instrumented test, README, CHANGELOG, KDoc.
**Eksik:** Gerçek cihazda instrumented test koşumu, benchmark testleri, Hilt DI (opsiyonel), Hatice ile model/label kontratı doğrulaması.
**Risk:** Gerçek cihaz/gerçek model ile end-to-end accuracy testi yapılmadı; label sayısı artık model output kontratıyla uyumlu.

**Hackathon teslimi için kullanılabilir** — kalan işler "polish" kategorisinde.
