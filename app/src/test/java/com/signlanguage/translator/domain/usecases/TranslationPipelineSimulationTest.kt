package com.signlanguage.translator.domain.usecases

import com.signlanguage.translator.data.model.LandmarkPoint
import com.signlanguage.translator.domain.services.FrameBufferManager
import com.signlanguage.translator.domain.services.PredictionProcessor
import com.signlanguage.translator.utils.Constants
import com.signlanguage.translator.utils.TensorUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationPipelineSimulationTest {
    @Test
    fun landmarksFlowToAcceptedWordsAndSentence() {
        val labels = MutableList(Constants.MODEL_OUTPUT_CLASSES) { "class_$it" }.apply {
            this[HELLO_INDEX] = "hello"
            this[DRINK_INDEX] = "drink"
        }
        val frameBufferManager = FrameBufferManager()
        val smoothedPredictionUseCase = SmoothedPredictionUseCase(windowSize = 3, requiredConsecutive = 2)
        val acceptedWords = mutableListOf<String>()

        feedGesture(
            gestureCode = HELLO_GESTURE,
            frameBufferManager = frameBufferManager,
            labels = labels,
            smoothedPredictionUseCase = smoothedPredictionUseCase,
            acceptedWords = acceptedWords
        )
        feedGesture(
            gestureCode = DRINK_GESTURE,
            frameBufferManager = frameBufferManager,
            labels = labels,
            smoothedPredictionUseCase = smoothedPredictionUseCase,
            acceptedWords = acceptedWords
        )

        assertEquals(listOf("hello", "drink"), acceptedWords)
        assertEquals("hello drink", acceptedWords.joinToString(" "))
    }

    private fun feedGesture(
        gestureCode: Float,
        frameBufferManager: FrameBufferManager,
        labels: List<String>,
        smoothedPredictionUseCase: SmoothedPredictionUseCase,
        acceptedWords: MutableList<String>
    ) {
        repeat(Constants.FRAME_BUFFER_SIZE + 2) { frameIndex ->
            val landmarks = fakeLandmarksForGesture(gestureCode, frameIndex)
            val flattenedFrame = TensorUtils.flattenLandmarks(landmarks)

            assertEquals(Constants.MODEL_FEATURE_SIZE, flattenedFrame.size)
            assertTrue(frameBufferManager.addFrame(flattenedFrame) || frameBufferManager.getFrameCount() < Constants.FRAME_BUFFER_SIZE)

            if (frameBufferManager.isFull()) {
                val orderedBuffer = frameBufferManager.getBufferOrdered()
                assertEquals(Constants.FRAME_BUFFER_SIZE * Constants.MODEL_FEATURE_SIZE, orderedBuffer.size)
                assertArrayEquals(
                    flattenedFrame,
                    orderedBuffer.copyOfRange(
                        orderedBuffer.size - Constants.MODEL_FEATURE_SIZE,
                        orderedBuffer.size
                    ),
                    0.0001f
                )

                val rawPrediction = PredictionProcessor.processPrediction(
                    output = fakeModelOutputFromLandmarks(orderedBuffer),
                    labels = labels,
                    confidenceThreshold = CONFIDENCE_THRESHOLD
                )
                val smoothedPrediction = smoothedPredictionUseCase(
                    rawPrediction,
                    confidenceThreshold = CONFIDENCE_THRESHOLD
                )

                if (smoothedPrediction.accepted && acceptedWords.lastOrNull() != smoothedPrediction.label) {
                    acceptedWords.add(smoothedPrediction.label)
                }
            }
        }
    }

    private fun fakeLandmarksForGesture(gestureCode: Float, frameIndex: Int): List<LandmarkPoint> {
        return List(Constants.SELECTED_LANDMARK_COUNT) { index ->
            LandmarkPoint(
                x = if (index == 0) gestureCode else (index % 10) / 10f,
                y = frameIndex / 100f,
                z = index / 1000f,
                visibility = 1f
            )
        }
    }

    private fun fakeModelOutputFromLandmarks(orderedBuffer: FloatArray): FloatArray {
        val latestFrameOffset = orderedBuffer.size - Constants.MODEL_FEATURE_SIZE
        val gestureCode = orderedBuffer[latestFrameOffset]
        val predictedIndex = if (gestureCode < 0.5f) HELLO_INDEX else DRINK_INDEX
        return FloatArray(Constants.MODEL_OUTPUT_CLASSES) { index ->
            when (index) {
                predictedIndex -> 0.92f
                else -> 0.01f
            }
        }
    }

    companion object {
        private const val HELLO_INDEX = 0
        private const val DRINK_INDEX = 1
        private const val HELLO_GESTURE = 0.2f
        private const val DRINK_GESTURE = 0.8f
        private const val CONFIDENCE_THRESHOLD = 0.7f
    }
}
