# 🚀 Gemma 4 Destekli İşaret Dili Çeviri Uygulaması
## Mobil Geliştirme (Android) - Teknik Doküman | Part 3
### Bölümler: MediaPipe SDK Entegrasyonu · TFLite Model Entegrasyonu

---

## 📑 İçindekiler (Bu Bölüm)
6. [MediaPipe SDK Entegrasyonu](#6-mediapipe-sdk-entegrasyonu)
7. [TFLite Model Entegrasyonu](#7-tflite-model-entegrasyonu)

---

## 6. MediaPipe SDK Entegrasyonu

### 6.1 MediaPipe Setup

**Bağımlılıklar:**
```gradle
dependencies {
    implementation 'com.google.mediapipe:tasks-vision:0.10.9'
}
```

### 6.2 MediaPipe Service (Landmark Extraction)

```kotlin
class MediaPipeService(private val context: Context) {
    private lateinit var holistic: Holistic
    private var confidenceThreshold: Float = 0.5f

    init {
        initializeHolistic()
    }

    private fun initializeHolistic() {
        val options = Holistic.HolisticOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("mediapipe_model_path_here")  // TODO: Check correct path
                    .build()
            )
            .setRunningMode(RunningMode.VIDEO)
            .build()

        holistic = Holistic(context, options)
        LogUtils.d("MediaPipe", "Holistic initialized successfully")
    }

    fun process(bitmap: Bitmap): List<NormalizedLandmark> {
        return try {
            val timestamp = SystemClock.uptimeMillis()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = holistic.detectAsync(mpImage, timestamp)

            val allLandmarks = mutableListOf<NormalizedLandmark>()

            // Face landmarks (468)
            result.faceLandmarks()?.forEach { landmark ->
                allLandmarks.add(landmark)
            }

            // Pose landmarks (33)
            result.poseLandmarks()?.forEach { landmark ->
                allLandmarks.add(landmark)
            }

            // Left hand (21)
            result.leftHandLandmarks()?.forEach { landmark ->
                allLandmarks.add(landmark)
            }

            // Right hand (21)
            result.rightHandLandmarks()?.forEach { landmark ->
                allLandmarks.add(landmark)
            }

            // Total: 468 + 33 + 21 + 21 = 543 landmarks
            require(allLandmarks.size == 543) {
                "Expected 543 landmarks, got ${allLandmarks.size}"
            }

            LogUtils.d("MediaPipe", "Extracted 543 landmarks successfully")
            allLandmarks

        } catch (e: Exception) {
            LogUtils.e("MediaPipe", "Error: ${e.message}")
            emptyList()
        }
    }

    fun setConfidenceThreshold(threshold: Float) {
        this.confidenceThreshold = threshold
    }

    fun release() {
        holistic.close()
        LogUtils.d("MediaPipe", "Holistic released")
    }
}

// Data class for landmarks
data class NormalizedLandmark(
    val x: Float,          // [0, 1] - normalized X coordinate
    val y: Float,          // [0, 1] - normalized Y coordinate
    val z: Float,          // Depth or relative Z (varies by model)
    val visibility: Float  // Confidence that landmark is visible
)
```

### 6.3 Landmark Visualization (Debug)

```kotlin
class LandmarkOverlayView(context: Context, attrs: AttributeSet? = null)
    : View(context, attrs) {

    private var landmarks: List<NormalizedLandmark> = emptyList()
    private val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 3f
    }

    fun setLandmarks(newLandmarks: List<NormalizedLandmark>) {
        this.landmarks = newLandmarks
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (landmarks.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()

        landmarks.forEach { landmark ->
            val x = landmark.x * width
            val y = landmark.y * height
            val visibility = landmark.visibility

            paint.color = when {
                visibility > 0.7f -> Color.GREEN
                visibility > 0.5f -> Color.YELLOW
                else -> Color.RED
            }

            canvas.drawCircle(x, y, 4f, paint)
        }

        drawConnections(canvas, width, height)
    }

    private fun drawConnections(canvas: Canvas, width: Float, height: Float) {
        val poseConnections = listOf(
            Pair(11, 12),  // Shoulders
            Pair(12, 14),  // Right arm
            Pair(14, 16),  // Right forearm
            Pair(11, 13),  // Left arm
            Pair(13, 15),  // Left forearm
            // ... more connections
        )

        paint.color = Color.CYAN
        paint.strokeWidth = 2f

        poseConnections.forEach { (idx1, idx2) ->
            if (idx1 < landmarks.size && idx2 < landmarks.size) {
                val start = landmarks[idx1]
                val end = landmarks[idx2]

                canvas.drawLine(
                    start.x * width, start.y * height,
                    end.x * width, end.y * height,
                    paint
                )
            }
        }
    }
}
```

### 6.4 Potential Issues & Solutions

| Issue | Symptom | Solution |
|-------|---------|----------|
| **Landmarks Out of Order** | Model predictions random | Check landmark extraction order matches training |
| **Missing Landmarks** | Some body parts not detected | Low light, fast movement, camera angle |
| **Visibility = 0** | Invisible landmarks used anyway | Filter by visibility threshold (>0.5) |
| **High Latency** | FPS drops | Run MediaPipe on GPU (if available) |
| **Memory Leak** | App crash after ~2 min | Call `release()` in Fragment.onDestroy |

---

## 7. TFLite Model Entegrasyonu

### 7.1 Model Files Setup

**Files to obtain from Hatice:**
- ✅ `sign_language_model.tflite` (1.6-2.5 MB)
- ✅ `labels.txt` (250 words, line by line)
- 🔴 **TODO:** `landmark_indices.json` (which 180 of 543 landmarks to use)
- 🔴 **TODO:** Normalization information (scale/range of x, y, z)

**Place files in:**
```
app/src/main/assets/
├── sign_language_model.tflite
├── labels.txt
└── landmark_indices.json  (when received)
```

### 7.2 Model Input/Output Specification

```kotlin
/*
 * MODEL SPECIFICATION
 *
 * Input Shape:  (1, 30, 540)
 *   - 1: Batch size (always 1)
 *   - 30: Number of frames (temporal dimension)
 *   - 540: Features per frame (180 landmarks × 3 coordinates)
 *
 * Input Data Type: Float32
 * Input Range: [0, 1] normalized (CONFIRM with Hatice)
 *
 * Output Shape: (1, 20)
 *   - 1: Batch size
 *   - 20: Number of word classes (softmax probabilities)
 *
 * Output Data Type: Float32
 * Output Values: [0, 1] - normalized probabilities (sums to ~1.0)
 *
 * Latency on Mid-Range Device: ~50-100 ms (estimated)
 * Model Size: ~2 MB (after TFLite conversion)
 */
```

### 7.3 TFLite Service (Inference Engine)

```kotlin
class TFLiteService(private val context: Context) {

    private lateinit var interpreter: Interpreter
    private lateinit var labels: List<String>
    private var modelLoaded = false

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel() {
        try {
            val modelBuffer = context.assets.open("sign_language_model.tflite")
                .readBytes()
                .let { ByteBuffer.wrap(it).order(ByteOrder.nativeOrder()) }

            val options = Interpreter.Options()
            options.setNumThreads(4)
            options.setUseNNAPI(true)

            interpreter = Interpreter(modelBuffer, options)
            modelLoaded = true

            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)

            LogUtils.d("TFLite", "Model loaded successfully")
            LogUtils.d("TFLite", "Input shape: ${inputTensor.shape().contentToString()}")
            LogUtils.d("TFLite", "Output shape: ${outputTensor.shape().contentToString()}")

        } catch (e: Exception) {
            LogUtils.e("TFLite", "Failed to load model: ${e.message}")
            modelLoaded = false
        }
    }

    private fun loadLabels() {
        try {
            labels = context.assets.open("labels.txt")
                .bufferedReader()
                .readLines()
                .map { it.trim() }

            LogUtils.d("TFLite", "Loaded ${labels.size} labels: $labels")

        } catch (e: Exception) {
            LogUtils.e("TFLite", "Failed to load labels: ${e.message}")
            labels = emptyList()
        }
    }

    fun inference(
        inputBuffer: FloatArray,
        confidenceThreshold: Float = 0.70f
    ): PredictionResult {

        if (!modelLoaded || labels.isEmpty()) {
            return PredictionResult(
                predictedIndex = -1,
                confidence = 0f,
                label = "Model Not Loaded",
                allProbabilities = emptyList()
            )
        }

        return try {
            val startTime = System.currentTimeMillis()

            val input = Array(1) { Array(30) { FloatArray(540) } }

            var idx = 0
            for (t in 0..29) {
                for (f in 0..539) {
                    input[0][t][f] = inputBuffer[idx++]
                }
            }

            val output = Array(1) { FloatArray(20) }
            interpreter.run(input, output)

            val inferenceTime = System.currentTimeMillis() - startTime
            val predictions = output[0]
            val predictedIndex = predictions.indices.maxByOrNull { predictions[it] } ?: -1
            val confidence = if (predictedIndex >= 0) predictions[predictedIndex] else 0f

            val label = when {
                predictedIndex < 0 -> "No Prediction"
                confidence < confidenceThreshold -> "Uncertain (${String.format("%.0f%%", confidence * 100)})"
                predictedIndex >= labels.size -> "Index Out of Range"
                else -> labels[predictedIndex]
            }

            LogUtils.d(
                "TFLite",
                "Inference: ${inferenceTime}ms | Prediction: $label (${String.format("%.1f%%", confidence * 100)})"
            )

            PredictionResult(
                predictedIndex = predictedIndex,
                confidence = confidence,
                label = label,
                allProbabilities = predictions.toList(),
                inferenceTime = inferenceTime
            )

        } catch (e: Exception) {
            LogUtils.e("TFLite", "Inference failed: ${e.message}")
            PredictionResult(
                predictedIndex = -1,
                confidence = 0f,
                label = "Error: ${e.message}",
                allProbabilities = emptyList()
            )
        }
    }

    fun close() {
        if (::interpreter.isInitialized) {
            interpreter.close()
            LogUtils.d("TFLite", "Interpreter closed")
        }
    }
}

// Data classes
data class PredictionResult(
    val predictedIndex: Int,
    val confidence: Float,
    val label: String,
    val allProbabilities: List<Float>,
    val inferenceTime: Long = 0L
)
```

### 7.4 FrameBufferManager

```kotlin
class FrameBufferManager(
    private val frameSize: Int = 540,  // 180 landmarks × 3 coordinates
    private val maxFrames: Int = 30,
    private val debugMode: Boolean = false
) {

    private val buffer = FloatArray(maxFrames * frameSize)
    private var frameCount = 0
    private var currentIndex = 0
    private val frameTimestamps = mutableListOf<Long>()

    fun addFrame(frame: FloatArray): Boolean {
        require(frame.size == frameSize) {
            "Frame size mismatch: expected $frameSize, got ${frame.size}"
        }

        System.arraycopy(frame, 0, buffer, currentIndex * frameSize, frameSize)

        frameTimestamps.add(System.currentTimeMillis())
        if (frameTimestamps.size > maxFrames) {
            frameTimestamps.removeAt(0)
        }

        currentIndex = (currentIndex + 1) % maxFrames

        if (frameCount < maxFrames) {
            frameCount++
        }

        return frameCount == maxFrames
    }

    fun getBuffer(): FloatArray = buffer.copyOf()

    fun getBufferOrdered(): FloatArray {
        val result = FloatArray(maxFrames * frameSize)

        if (frameCount < maxFrames) {
            System.arraycopy(buffer, 0, result, 0, frameCount * frameSize)
        } else {
            val oldestIndex = currentIndex
            System.arraycopy(
                buffer, oldestIndex * frameSize,
                result, 0,
                (maxFrames - oldestIndex) * frameSize
            )
            System.arraycopy(
                buffer, 0,
                result, (maxFrames - oldestIndex) * frameSize,
                oldestIndex * frameSize
            )
        }

        return result
    }

    fun calculateFPS(): Int {
        if (frameTimestamps.size < 2) return 0
        val timeDiff = frameTimestamps.last() - frameTimestamps.first()
        return if (timeDiff > 0) ((frameTimestamps.size - 1) * 1000 / timeDiff).toInt() else 0
    }

    fun isFull(): Boolean = frameCount == maxFrames

    fun reset() {
        buffer.fill(0f)
        frameCount = 0
        currentIndex = 0
        frameTimestamps.clear()
        LogUtils.d("FrameBuffer", "Buffer reset")
    }

    fun getFrameCount(): Int = frameCount
}
```

### 7.5 Output Post-Processing

```kotlin
object PredictionProcessor {

    fun processPrediction(
        output: FloatArray,
        labels: List<String>,
        confidenceThreshold: Float = 0.70f,
        debugMode: Boolean = false
    ): PredictionResult {

        require(output.size == 250) { "Expected 250 output values, got ${output.size}" }
        require(labels.size == 250) { "Expected 250 labels, got ${labels.size}" }

        var maxIdx = 0
        var maxValue = output[0]

        for (i in output.indices) {
            if (output[i] > maxValue) {
                maxValue = output[i]
                maxIdx = i
            }
        }

        val confidence = maxValue
        val label = labels[maxIdx]

        val result = PredictionResult(
            predictedIndex = maxIdx,
            confidence = confidence,
            label = if (confidence >= confidenceThreshold) label else "Uncertain",
            allProbabilities = output.toList()
        )

        if (debugMode) {
            val top3 = output.withIndex()
                .sortedByDescending { it.value }
                .take(3)
                .map { "${labels[it.index]}: ${String.format("%.1f%%", it.value * 100)}" }

            LogUtils.d("Prediction", "Top3: ${top3.joinToString(" | ")}")
        }

        return result
    }
}
```

---

**← Önceki: [part2.md](part2.md) | → Devam: [part4.md](part4.md) — Backend API & Veri Akışı**
