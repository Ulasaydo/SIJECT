# Hatice'den Beklenen Model Bilgileri

## Kritik

- 180 landmark secimi: 543 MediaPipe landmark icinden kullanilan kesin indeks listesi.
- `landmark_indices.json` formati:

```json
{
  "indices": [0, 1, 2],
  "total_landmarks": 180,
  "breakdown": {
    "pose": [0, 33],
    "face": [33, 138],
    "left_hand": [138, 159],
    "right_hand": [159, 180]
  }
}
```

- Koordinat normalizasyonu: x/y/z range bilgisi ve ek preprocessing olup olmadigi.
- `labels.txt` sirasi: model output indeksleri ile birebir ayni 250 kelime.
- Model tipi: Float32, Float16 veya INT8 quantization.
- Kod tarafında doğrulanan mevcut input shape: `1 x 30 x 1629`.
- Kod tarafında doğrulanan mevcut output shape: `1 x 250`.
- Hatice ile `543 landmark / 250 sınıf` model kontratı ve label listesi alındı; gerçek cihaz accuracy doğrulaması yapılmalı.
- Beklenen output shape: `1 x 20`.

## Onemli

- Onerilen confidence threshold degeri.
- CPU/NNAPI/GPU icin beklenen inference suresi.
- Dusuk isik ve kamera acisi icin bilinen edge case'ler.
- Test icin ornek landmark array ve beklenen label.
