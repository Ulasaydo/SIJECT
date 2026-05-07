# SIJECT Model Contract

This document is the canonical specification for the on-device sign language model.
All source files (`Constants.kt`, `landmark_indices.json`, `labels.txt`) and any
training pipeline must agree with the values below.

## Tensor shapes

```
Input  = [1, 30, 144]   # [batch, frames, features]
Output = [1, 250]       # [batch, classes]
```

## Frame buffer

- Window size: 30 frames (circular buffer).
- Inference cadence: every 5 frames once the buffer is full.
- File: `domain/services/FrameBufferManager.kt`.

## Selected landmarks (48)

```
6  pose upper body  (indices 11..16)
21 left hand        (indices 501..521)
21 right hand       (indices 522..542)
--
48 selected × 3 axes (x, y, z) = 144 features per frame
```

The 48 indices live in `app/src/main/assets/landmark_indices.json` (`indices` field).

## Source landmark order (543)

MediaPipe Holistic output the model is selected from:

```
pose_33      indices  0..32     (33 points)
face_468     indices 33..500    (468 points)
left_hand_21 indices 501..521   (21 points)
right_hand_21 indices 522..542  (21 points)
```

Constant: `Constants.TOTAL_LANDMARK_COUNT = 543`.

## Coordinate space

- MediaPipe Tasks Vision is invoked with `ImageProcessingOptions.setRotationDegrees(rotationDegrees)`,
  so output landmarks are upright (display-oriented).
- Model path consumes upright coordinates **without** further rotation.
- Overlay path applies horizontal mirror for the front-facing camera only.

## Per-frame normalization

After selecting and flattening the 48 × 3 = 144 features, each frame is z-score
standardized:

```
mean = avg(values)
std  = sqrt(avg((v - mean)^2))
out  = (values - mean) / std    (zero-vector if std < 1e-6)
```

File: `domain/services/LandmarkProcessor.normalizeAndFlatten()`.

## Labels

- File: `app/src/main/assets/labels.txt`.
- Count: 250 (one per output class).
- `Constants.MODEL_OUTPUT_CLASSES = 250`.

## Validation guarantees enforced at runtime

`TFLiteService.validateTensorShapes()` rejects any model whose input or output
shape diverges from this contract.
