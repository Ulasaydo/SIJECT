package com.signlanguage.translator.ui.viewmodels

import android.app.Application
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.signlanguage.translator.R
import com.signlanguage.translator.data.model.LandmarkPoint
import com.signlanguage.translator.data.repository.AppSettingsRepository
import com.signlanguage.translator.data.repository.LocalDataRepository
import com.signlanguage.translator.data.repository.RemoteDataRepository
import com.signlanguage.translator.data.repository.SignLanguageRepository
import com.signlanguage.translator.domain.services.FrameBufferManager
import com.signlanguage.translator.domain.services.LandmarkProcessor
import com.signlanguage.translator.domain.services.MediaPipeService
import com.signlanguage.translator.domain.services.PredictionProcessor
import com.signlanguage.translator.domain.services.RetrofitClient
import com.signlanguage.translator.domain.services.TFLiteService
import com.signlanguage.translator.domain.services.TextToSpeechService
import com.signlanguage.translator.domain.usecases.ProcessFrameUseCase
import com.signlanguage.translator.domain.usecases.SmoothedPredictionUseCase
import com.signlanguage.translator.domain.usecases.TranslateWordsUseCase
import com.signlanguage.translator.utils.Constants
import com.signlanguage.translator.utils.Debouncer
import com.signlanguage.translator.utils.LogUtils
import com.signlanguage.translator.utils.TensorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class TranslationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SignLanguageRepository(
        localDataRepository = LocalDataRepository(application),
        remoteDataRepository = RemoteDataRepository(RetrofitClient.api)
    )
    private val settingsRepository = AppSettingsRepository(application)
    private val processFrameUseCase = ProcessFrameUseCase(
        mediaPipeService = MediaPipeService(application),
        landmarkProcessor = LandmarkProcessor(application),
        frameBufferManager = FrameBufferManager(),
        tfliteService = TFLiteService(application)
    )
    private val smoothedPredictionUseCase = SmoothedPredictionUseCase()
    private val translateWordsUseCase = TranslateWordsUseCase(repository)
    private val textToSpeechService = TextToSpeechService(application)

    private val acceptedWordsLock = Any()
    private val acceptedWords = mutableListOf<String>()
    private val isProcessingFrame = AtomicBoolean(false)
    private val isTranslating = AtomicBoolean(false)
    private val pendingTranslation = AtomicBoolean(false)
    private val translationDebouncer = Debouncer(viewModelScope, TRANSLATION_DEBOUNCE_MS)
    @Volatile
    private var lastTranslatedSnapshot: List<String>? = null
    @Volatile
    private var currentSettings = settingsRepository.getSettings()
    private var frameCount = 0
    private var fpsWindowStartedAt = System.currentTimeMillis()
    private var currentFps = 0
    private var latestState = TranslationUiState(
        translatedSentence = application.getString(R.string.translated_sentence_placeholder)
    )
    private val _uiState = MutableLiveData(
        latestState
    )
    val uiState: LiveData<TranslationUiState> = _uiState

    fun processFrame(imageProxy: ImageProxy) {
        if (!isProcessingFrame.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val settings = currentSettings
                val t0 = System.currentTimeMillis()
                val result = processFrameUseCase(imageProxy, settings.confidenceThreshold)
                val t1 = System.currentTimeMillis()
                val prediction = result.prediction?.let {
                    smoothedPredictionUseCase(it, settings.confidenceThreshold)
                }
                val t2 = System.currentTimeMillis()
                LogUtils.d("PerfProfile", "frame=${t1 - t0}ms (lm=${result.landmarkTimeMillis} inf=${result.inferenceTimeMillis}) smooth=${t2 - t1}ms")
                var acceptedWordAdded = false
                if (prediction?.accepted == true) {
                    acceptedWordAdded = synchronized(acceptedWordsLock) {
                        if (acceptedWords.lastOrNull() != prediction.label) {
                            acceptedWords.add(prediction.label)
                            true
                        } else {
                            false
                        }
                    }
                    if (acceptedWordAdded) {
                        repository.persistPrediction(prediction)
                    }
                }

                updateFps()
                val t3 = System.currentTimeMillis()
                LogUtils.d("PerfProfile", "post=${t3 - t2}ms total=${t3 - t0}ms fps=$currentFps")
                postState(latestState.copy(
                    recognizedWords = prediction?.candidates ?: latestState.recognizedWords,
                    landmarks = result.landmarks,
                    sourceImageWidth = result.sourceImageWidth,
                    sourceImageHeight = result.sourceImageHeight,
                    fps = currentFps,
                    landmarkTimeMillis = result.landmarkTimeMillis,
                    inferenceTimeMillis = result.inferenceTimeMillis,
                    debugMode = latestState.debugMode,
                    showLandmarkOverlay = settings.showLandmarkOverlay,
                    showFpsCounter = settings.showFpsCounter,
                    errorMessage = null
                ))
                if (acceptedWordAdded) {
                    translateAcceptedWordsDebounced()
                }
            } catch (throwable: Throwable) {
                LogUtils.e("ViewModel", "Frame processing failed", throwable)
                postState(latestState.copy(errorMessage = throwable.message))
            } finally {
                imageProxy.close()
                isProcessingFrame.set(false)
            }
        }
    }

    fun toggleDebugMode() {
        postState(latestState.copy(debugMode = !latestState.debugMode))
    }

    fun refreshSettings() {
        currentSettings = settingsRepository.getSettings()
        postState(latestState.copy(
            showLandmarkOverlay = currentSettings.showLandmarkOverlay,
            showFpsCounter = currentSettings.showFpsCounter
        ))
    }

    fun clearRecognizedWords() {
        translationDebouncer.cancel()
        lastTranslatedSnapshot = null
        synchronized(acceptedWordsLock) {
            acceptedWords.clear()
        }
        postState(latestState.copy(
            recognizedWords = emptyList(),
            translatedSentence = getApplication<Application>().getString(R.string.translated_sentence_placeholder),
            apiError = null,
            translationConfidence = 0f,
            alternatives = emptyList()
        ))
    }

    fun refreshTranslation() {
        translationDebouncer.cancel()
        lastTranslatedSnapshot = null
        translateAcceptedWords()
    }

    internal fun translateAcceptedWordsDebounced() {
        translationDebouncer.submit { translateAcceptedWords() }
    }

    fun speakCurrentSentence() {
        val placeholder = getApplication<Application>().getString(R.string.translated_sentence_placeholder)
        val sentence = latestState.translatedSentence
        if (sentence.isNotBlank() && sentence != placeholder) {
            textToSpeechService.speak(sentence)
        }
    }

    fun runLandmarkPipelineDemo() {
        viewModelScope.launch(Dispatchers.Default) {
            synchronized(acceptedWordsLock) {
                acceptedWords.clear()
            }

            val labels = MutableList(Constants.MODEL_OUTPUT_CLASSES) { "class_$it" }.apply {
                this[DEMO_HELLO_INDEX] = "hello"
                this[DEMO_DRINK_INDEX] = "drink"
            }
            val demoFrameBuffer = FrameBufferManager()
            val demoSmoothing = SmoothedPredictionUseCase(windowSize = 3)
            val demoWords = mutableListOf<String>()

            postState(latestState.copy(
                recognizedWords = emptyList(),
                translatedSentence = "Demo pipeline baslatiliyor",
                isLoading = false,
                apiError = null,
                backendOnline = null,
                errorMessage = null,
                landmarks = fakeDemoLandmarks(DEMO_HELLO_GESTURE, 0),
                sourceImageWidth = 480,
                sourceImageHeight = 640,
                fps = 30,
                landmarkTimeMillis = 12L,
                inferenceTimeMillis = 0L,
                debugMode = true,
                showLandmarkOverlay = true,
                showFpsCounter = true
            ))

            listOf(DEMO_HELLO_GESTURE, DEMO_DRINK_GESTURE).forEach { gestureCode ->
                repeat(Constants.FRAME_BUFFER_SIZE + 2) { frameIndex ->
                    val landmarks = fakeDemoLandmarks(gestureCode, frameIndex)
                    val flattenedFrame = TensorUtils.flattenLandmarks(landmarks)
                    val isBufferFull = demoFrameBuffer.addFrame(flattenedFrame)
                    val prediction = if (isBufferFull) {
                        val rawPrediction = PredictionProcessor.processPrediction(
                            output = fakeDemoModelOutput(demoFrameBuffer.getBufferOrdered()),
                            labels = labels,
                            confidenceThreshold = DEMO_CONFIDENCE_THRESHOLD,
                            inferenceTimeMillis = 5L,
                            debugMode = true
                        )
                        demoSmoothing(rawPrediction, DEMO_CONFIDENCE_THRESHOLD)
                    } else {
                        null
                    }

                    if (prediction?.accepted == true && demoWords.lastOrNull() != prediction.label) {
                        demoWords.add(prediction.label)
                    }

                    postState(latestState.copy(
                        recognizedWords = prediction?.candidates.orEmpty(),
                        translatedSentence = demoWords.joinToString(" ").ifBlank {
                            "Landmark frame buffer doluyor"
                        },
                        isLoading = false,
                        apiError = null,
                        backendOnline = null,
                        errorMessage = null,
                        landmarks = landmarks,
                        sourceImageWidth = 480,
                        sourceImageHeight = 640,
                        fps = 30,
                        landmarkTimeMillis = 12L,
                        inferenceTimeMillis = if (isBufferFull) 5L else 0L,
                        debugMode = true,
                        showLandmarkOverlay = true,
                        showFpsCounter = true
                    ))
                    delay(80L)
                }
            }
        }
    }

    fun checkBackendHealth() {
        viewModelScope.launch(Dispatchers.IO) {
            val online = repository.checkBackendHealth()
            postState(latestState.copy(backendOnline = online))
        }
    }

    private fun updateFps() {
        frameCount += 1
        val now = System.currentTimeMillis()
        val elapsed = now - fpsWindowStartedAt
        if (elapsed >= 1000L) {
            currentFps = ((frameCount * 1000L) / elapsed).toInt()
            frameCount = 0
            fpsWindowStartedAt = now
        }
    }

    private fun fakeDemoLandmarks(gestureCode: Float, frameIndex: Int): List<LandmarkPoint> {
        return List(Constants.SELECTED_LANDMARK_COUNT) { index ->
            LandmarkPoint(
                x = if (index == 0) gestureCode else (index % 10) / 10f,
                y = frameIndex / 100f,
                z = index / 1000f,
                visibility = 1f
            )
        }
    }

    private fun fakeDemoModelOutput(orderedBuffer: FloatArray): FloatArray {
        val latestFrameOffset = orderedBuffer.size - Constants.MODEL_FEATURE_SIZE
        val gestureCode = orderedBuffer[latestFrameOffset]
        val predictedIndex = if (gestureCode < 0.5f) DEMO_HELLO_INDEX else DEMO_DRINK_INDEX
        return FloatArray(Constants.MODEL_OUTPUT_CLASSES) { index ->
            when (index) {
                predictedIndex -> 0.92f
                else -> 0.01f
            }
        }
    }

    override fun onCleared() {
        processFrameUseCase.release()
        textToSpeechService.release()
        super.onCleared()
    }

    private fun translateAcceptedWords() {
        val wordsSnapshot = synchronized(acceptedWordsLock) { acceptedWords.toList() }
        if (wordsSnapshot.isEmpty()) return
        if (lastTranslatedSnapshot == wordsSnapshot) return
        if (!isTranslating.compareAndSet(false, true)) {
            pendingTranslation.set(true)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            postState(latestState.copy(isLoading = true, apiError = null))
            val result = translateWordsUseCase(wordsSnapshot)
            result
                .onSuccess { translation ->
                    val sentence = translation.sentence.ifBlank { wordsSnapshot.joinToString(" ") }
                    postState(latestState.copy(
                        translatedSentence = sentence,
                        isLoading = false,
                        apiError = null,
                        translationConfidence = translation.confidence,
                        alternatives = translation.alternatives,
                        backendOnline = true
                    ))
                    lastTranslatedSnapshot = wordsSnapshot
                    LogUtils.d("ViewModel", "Translation successful: ${translation.sentence}")
                }
                .onFailure { exception ->
                    val fallbackSentence = wordsSnapshot.joinToString(" ")
                    postState(latestState.copy(
                        translatedSentence = fallbackSentence,
                        isLoading = false,
                        apiError = exception.message ?: "Unknown error",
                        translationConfidence = 0f,
                        alternatives = emptyList(),
                        backendOnline = false
                    ))
                    LogUtils.e("ViewModel", "Translation failed", exception)
                }
            isTranslating.set(false)

            val wordsChanged = synchronized(acceptedWordsLock) {
                acceptedWords != wordsSnapshot
            }
            if (pendingTranslation.getAndSet(false) || wordsChanged) {
                translateAcceptedWords()
            }
        }
    }

    @Synchronized
    private fun postState(state: TranslationUiState) {
        latestState = state
        _uiState.postValue(state)
    }

    companion object {
        internal const val TRANSLATION_DEBOUNCE_MS = 800L
        private const val DEMO_HELLO_INDEX = 0
        private const val DEMO_DRINK_INDEX = 1
        private const val DEMO_HELLO_GESTURE = 0.2f
        private const val DEMO_DRINK_GESTURE = 0.8f
        private const val DEMO_CONFIDENCE_THRESHOLD = 0.7f
    }
}
