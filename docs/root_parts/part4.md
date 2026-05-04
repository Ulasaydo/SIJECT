# 🚀 Gemma 4 Destekli İşaret Dili Çeviri Uygulaması
## Mobil Geliştirme (Android) - Teknik Doküman | Part 4
### Bölümler: Backend API Haberleşmesi · Veri Akışı (Detaylı)

---

## 📑 İçindekiler (Bu Bölüm)
8. [Backend API Haberleşmesi](#8-backend-api-haberleşmesi)
9. [Veri Akışı (Detaylı)](#9-veri-akışı-detaylı)

---

## 8. Backend API Haberleşmesi

### 8.1 API Contract (Expected Endpoint)

```kotlin
/*
 * Backend API Endpoint (Rabia tarafından yapılacak)
 *
 * POST /api/translate (or similar)
 *
 * Request Body:
 * {
 *   "words": ["hello", "bye", "computer"],
 *   "language": "tr"  // Optional
 * }
 *
 * Response Body:
 * {
 *   "sentence": "Merhaba, hoşça kalın.",
 *   "confidence": 0.87,
 *   "alternatives": [
 *     "Merhaba, hoşça kalın.",
 *     "Selam, hoşça kalın."
 *   ]
 * }
 *
 * Status Codes:
 * - 200: Success
 * - 400: Invalid request
 * - 500: Server error
 * - Timeout: 10 seconds
 */
```

### 8.2 Retrofit Setup

```gradle
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
}
```

### 8.3 API Service Definition

```kotlin
// Data classes for API communication
data class TranslateRequest(
    @SerializedName("words")
    val words: List<String>,

    @SerializedName("language")
    val language: String = "tr"
)

data class TranslateResponse(
    @SerializedName("sentence")
    val sentence: String,

    @SerializedName("confidence")
    val confidence: Float = 0f,

    @SerializedName("alternatives")
    val alternatives: List<String> = emptyList()
)

// Retrofit API interface
interface SignLanguageAPI {

    @POST("/api/translate")
    suspend fun translateWords(
        @Body request: TranslateRequest
    ): Response<TranslateResponse>

    @GET("/api/health")
    suspend fun checkHealth(): Response<String>
}

// Retrofit client singleton
object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8000/"  // Android emulator localhost
    // For real device: "http://192.168.1.100:8000/"

    val api: SignLanguageAPI by lazy {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SignLanguageAPI::class.java)
    }
}
```

### 8.4 Repository Implementation

```kotlin
class SignLanguageRepository(
    private val api: SignLanguageAPI
) {

    suspend fun translateWords(words: List<String>): Result<String> {
        return try {
            if (words.isEmpty()) {
                return Result.success("")
            }

            val request = TranslateRequest(words = words, language = "tr")
            val response = api.translateWords(request)

            when {
                response.isSuccessful && response.body() != null -> {
                    val sentence = response.body()!!.sentence
                    LogUtils.d("API", "Translation: $sentence")
                    Result.success(sentence)
                }

                response.code() == 400 -> {
                    Result.failure(Exception("Invalid request: ${response.message()}"))
                }

                response.code() >= 500 -> {
                    Result.failure(Exception("Server error: ${response.message()}"))
                }

                else -> {
                    Result.failure(Exception("Unknown error: ${response.message()}"))
                }
            }

        } catch (e: SocketTimeoutException) {
            LogUtils.e("API", "Timeout")
            Result.failure(Exception("Request timeout"))
        } catch (e: ConnectException) {
            LogUtils.e("API", "Connection failed")
            Result.failure(Exception("No internet connection"))
        } catch (e: Exception) {
            LogUtils.e("API", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun checkBackendHealth(): Boolean {
        return try {
            val response = api.checkHealth()
            response.isSuccessful
        } catch (e: Exception) {
            LogUtils.e("API", "Health check failed: ${e.message}")
            false
        }
    }
}
```

### 8.5 ViewModel - API Integration

```kotlin
class TranslationViewModel(
    private val repository: SignLanguageRepository
) : ViewModel() {

    private val _translatedSentence = MutableLiveData<String>("")
    val translatedSentence: LiveData<String> = _translatedSentence

    private val _apiError = MutableLiveData<String?>(null)
    val apiError: LiveData<String?> = _apiError

    private val _isLoadingTranslation = MutableLiveData(false)
    val isLoadingTranslation: LiveData<Boolean> = _isLoadingTranslation

    fun translateWords(words: List<String>) {
        viewModelScope.launch {
            _isLoadingTranslation.value = true
            _apiError.value = null

            val result = repository.translateWords(words)

            result.onSuccess { sentence ->
                _translatedSentence.value = sentence
                LogUtils.d("ViewModel", "Translation successful: $sentence")
            }

            result.onFailure { exception ->
                _apiError.value = exception.message ?: "Unknown error"
                LogUtils.e("ViewModel", "Translation failed: ${exception.message}")
                // Offline fallback
                _translatedSentence.value = words.joinToString(" ")
            }

            _isLoadingTranslation.value = false
        }
    }
}
```

### 8.6 Fragment Integration

```kotlin
// In TranslationFragment
private fun callBackendAPI(words: List<String>) {
    binding.progressBar.visibility = View.VISIBLE

    viewModel.translateWords(words)

    viewModel.translatedSentence.observe(viewLifecycleOwner) { sentence ->
        if (sentence.isNotEmpty()) {
            binding.translatedSentenceText.text = sentence
            binding.translatedSentenceCard.visibility = View.VISIBLE
            textToSpeechService.speak(sentence)
        }
    }

    viewModel.apiError.observe(viewLifecycleOwner) { error ->
        if (error != null) {
            showErrorSnackbar("API Error: $error")
            binding.translatedSentenceText.text = "Error: $error"
        }
    }

    viewModel.isLoadingTranslation.observe(viewLifecycleOwner) { isLoading ->
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
```

---

## 9. Veri Akışı (Detaylı)

### 9.1 End-to-End Data Flow

```
┌──────────────────────────────────────────────────────┐
│           1. FRAME CAPTURE (30 FPS)                  │
│  CameraX.ImageAnalyzer → ImageProxy (YUV NV21)       │
│  Size: 640x480 | Timestamp: currentTimeMillis()      │
└────────────────────────┬─────────────────────────────┘
                         │ (Background Thread)
                         ↓
┌──────────────────────────────────────────────────────┐
│        2. IMAGE CONVERSION & PREPROCESSING           │
│  ImageProxy → Bitmap (640x480 RGB/YUV)               │
└────────────────────────┬─────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────┐
│        3. MEDIAPIPE HOLISTIC PROCESSING              │
│  Input: Bitmap (640x480)                             │
│  Output: 543 NormalizedLandmarks                     │
│    ├─ Face: 468 landmarks                            │
│    ├─ Pose: 33 landmarks                             │
│    ├─ Left Hand: 21 landmarks                        │
│    └─ Right Hand: 21 landmarks                       │
│  Time: ~30-50ms                                      │
└────────────────────────┬─────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────┐
│     4. LANDMARK SELECTION & FEATURE EXTRACTION       │
│  LandmarkProcessor.extractSelectedLandmarks()        │
│  TODO (From Hatice): Which 180 of 543 landmarks?     │
│                                                      │
│  For each landmark:                                  │
│    output[i]   = x                                   │
│    output[i+1] = y                                   │
│    output[i+2] = z                                   │
│                                                      │
│  Output: FloatArray(540) = 180 × 3 coordinates       │
└────────────────────────┬─────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────┐
│      5. CIRCULAR FRAME BUFFER MANAGEMENT             │
│  FrameBufferManager (size=30, feature=540)           │
│  Buffer Size: 30 × 540 = 16,200 floats ≈ 64 KB      │
│  Processing Time: ~1 second of capture               │
└────────────────────────┬─────────────────────────────┘
                         │
          [Buffer NOT full] │ [Buffer IS full]
                         │         ↓
                    [Continue]  ┌──────────────────────────┐
                                │  6. TFLITE INFERENCE     │
                                │  Input:  (1, 30, 540)    │
                                │  Output: (1, 20)         │
                                │  Time: ~50-100ms         │
                                └────────────┬─────────────┘
                                             ↓
                    ┌────────────────────────────────────┐
                    │   7. POST-PROCESSING & PREDICTION  │
                    │  argmax → index, confidence        │
                    │  Label mapping: index → "drink"    │
                    │  Threshold check: conf >= 0.70?    │
                    └────────────┬───────────────────────┘
                                 ↓
        ┌────────────────────────────────────────────┐
        │   8. SMOOTHING FILTER (3-FRAME AVERAGING)  │
        │  Last 3 predictions majority vote          │
        │  Avg confidence >= 0.70 → final prediction │
        └────────────┬───────────────────────────────┘
                     ↓
    ┌────────────────────────────────────────────┐
    │     9. UI STATE UPDATE (Main Thread)       │
    │  ViewModel.updatePrediction()              │
    │  LiveData: recognizedWord, confidence      │
    │  Fragment: update RecognizedWordsView      │
    └────────────┬───────────────────────────────┘
                 ↓
    ┌────────────────────────────────────────────┐
    │  10. OPTIONAL: BACKEND API CALL            │
    │  POST /api/translate                       │
    │  {words: ["hello","bye","drink"]}          │
    │  → Response: {sentence: "Merhaba..."}      │
    │  → UI Display + TTS                        │
    └────────────────────────────────────────────┘
```

### 9.2 Data Structure Transformations

```
Frame 0: ImageProxy (YUV 640x480)
         ↓
      [Bitmap]
         ↓
[543 NormalizedLandmarks: {x, y, z, visibility}]
         ↓
[180 Selected Landmarks: {x, y, z}]
         ↓
[FloatArray(540): x₀, y₀, z₀, x₁, y₁, z₁, ..., x₁₇₉, y₁₇₉, z₁₇₉]
         ↓ (accumulated in buffer 30×)
[FloatArray(16200): 30 frames × 540 features]
         ↓
[3D Tensor (1, 30, 540)]
         ↓
[TFLite Inference]
         ↓
[FloatArray(20): p₀, p₁, ..., p₁₉ (probabilities)]
         ↓
[argmax → index, confidence]
         ↓
[Label Mapping: index → "drink"]
         ↓
[Confidence Threshold: 0.70 check]
         ↓
[3-Frame Smoothing Average]
         ↓
[Final Prediction: "drink" (87%)]
         ↓
[API Request: {words: ["drink"]}]
         ↓
[API Response: {sentence: "İçmek istiyorum."}]
         ↓
[UI Display + TTS]
```

---

**← Önceki: [part3.md](part3.md) | → Devam: [part5.md](part5.md) — Performans · Test · Yol Haritası · TODOs**
