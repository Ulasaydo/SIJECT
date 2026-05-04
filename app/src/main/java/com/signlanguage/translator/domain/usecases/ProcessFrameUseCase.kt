package com.signlanguage.translator.domain.usecases

import androidx.camera.core.ImageProxy
import com.signlanguage.translator.data.model.LandmarkPoint
import com.signlanguage.translator.data.model.PredictionResult
import com.signlanguage.translator.domain.services.FrameBufferManager
import com.signlanguage.translator.domain.services.LandmarkProcessor
import com.signlanguage.translator.domain.services.MediaPipeService
import com.signlanguage.translator.domain.services.TFLiteService
import com.signlanguage.translator.utils.Constants
import com.signlanguage.translator.utils.PerformanceMonitor

/**
 * Orchestrates one camera frame through MediaPipe, landmark processing, buffering, and TFLite.
 */
class ProcessFrameUseCase(
    private val mediaPipeService: MediaPipeService,
    private val landmarkProcessor: LandmarkProcessor,
    private val frameBufferManager: FrameBufferManager,
    private val tfliteService: TFLiteService,
    private val performanceMonitor: PerformanceMonitor = PerformanceMonitor()
) {
    private var processedFrameCount = PREDICTION_INTERVAL_FRAMES - 1

    suspend operator fun invoke(
        imageProxy: ImageProxy,
        confidenceThreshold: Float = Constants.CONFIDENCE_THRESHOLD
    ): FrameProcessingResult {
        val sourceImageWidth = imageProxy.displayOrientedWidth()
        val sourceImageHeight = imageProxy.displayOrientedHeight()
        performanceMonitor.startTiming("MediaPipe")
        val landmarks = mediaPipeService.extractLandmarks(imageProxy)
        val landmarkTimeMillis = performanceMonitor.endTiming("MediaPipe")
        val flattenedFrame = landmarkProcessor.normalizeAndFlatten(landmarks)
        val isBufferFull = frameBufferManager.addFrame(flattenedFrame)
        val shouldRunPrediction = isBufferFull && shouldRunPrediction()
        val prediction = if (shouldRunPrediction) {
            performanceMonitor.startTiming("TFLite")
            tfliteService.predict(frameBufferManager.getBufferOrdered(), confidenceThreshold)
        } else {
            null
        }
        val measuredInferenceTime = if (shouldRunPrediction) {
            performanceMonitor.endTiming("TFLite")
        } else {
            0L
        }
        val inferenceTimeMillis = prediction?.inferenceTimeMillis?.takeIf { it > 0L }
            ?: measuredInferenceTime
        return FrameProcessingResult(
            landmarks = landmarks,
            prediction = prediction,
            landmarkTimeMillis = landmarkTimeMillis,
            inferenceTimeMillis = inferenceTimeMillis,
            sourceImageWidth = sourceImageWidth,
            sourceImageHeight = sourceImageHeight
        )
    }

    fun release() {
        mediaPipeService.release()
        tfliteService.close()
        frameBufferManager.reset()
    }

    private fun shouldRunPrediction(): Boolean {
        processedFrameCount += 1
        if (processedFrameCount >= PREDICTION_INTERVAL_FRAMES) {
            processedFrameCount = 0
            return true
        }
        return false
    }

    private fun ImageProxy.displayOrientedWidth(): Int {
        return if (imageInfo.rotationDegrees.isQuarterTurn()) height else width
    }

    private fun ImageProxy.displayOrientedHeight(): Int {
        return if (imageInfo.rotationDegrees.isQuarterTurn()) width else height
    }

    private fun Int.isQuarterTurn(): Boolean {
        val normalizedRotation = ((this % 360) + 360) % 360
        return normalizedRotation == 90 || normalizedRotation == 270
    }

    private companion object {
        const val PREDICTION_INTERVAL_FRAMES = 5
    }
}

data class FrameProcessingResult(
    val landmarks: List<LandmarkPoint>,
    val prediction: PredictionResult?,
    val landmarkTimeMillis: Long,
    val inferenceTimeMillis: Long,
    val sourceImageWidth: Int,
    val sourceImageHeight: Int
)
