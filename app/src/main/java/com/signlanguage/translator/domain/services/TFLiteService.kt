package com.signlanguage.translator.domain.services

import android.content.Context
import com.signlanguage.translator.data.model.PredictionResult
import com.signlanguage.translator.utils.Constants
import com.signlanguage.translator.utils.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Loads the bundled TensorFlow Lite model and executes sequence inference off the main thread.
 */
class TFLiteService(context: Context) {
    private var interpreter: Interpreter? = null
    private var initializationError: Throwable? = null
    private val inferenceLock = Any()
    private val inputTensorBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(Constants.FRAME_BUFFER_SIZE * Constants.MODEL_FEATURE_SIZE * FLOAT_BYTE_SIZE)
        .order(ByteOrder.nativeOrder())
    private val inputTensorFloatBuffer = inputTensorBuffer.asFloatBuffer()
    private var outputTensorBuffer: ByteBuffer? = null
    private var outputTensorSize: Int = 0
    private val labels: List<String> = context.assets.open(Constants.LABELS_FILE).bufferedReader().useLines { lines ->
        lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }

    init {
        runCatching {
            interpreter = loadInterpreter(context)
        }.onFailure { throwable ->
            initializationError = throwable
            LogUtils.e("TFLite", "Failed to initialize model", throwable)
        }
    }

    suspend fun predict(
        inputBuffer: FloatArray,
        confidenceThreshold: Float = Constants.CONFIDENCE_THRESHOLD
    ): PredictionResult = withContext(Dispatchers.Default) {
        initializationError?.let { throwable ->
            throw IllegalStateException("TFLite initialization failed: ${throwable.message}", throwable)
        }

        require(inputBuffer.size == Constants.FRAME_BUFFER_SIZE * Constants.MODEL_FEATURE_SIZE) {
            "Input size mismatch: expected ${Constants.FRAME_BUFFER_SIZE * Constants.MODEL_FEATURE_SIZE}, got ${inputBuffer.size}"
        }

        val activeInterpreter = interpreter
            ?: throw IllegalStateException("TFLite model is not loaded.")

        synchronized(inferenceLock) {
            runCatching {
                val startedAt = System.currentTimeMillis()
                val outputShape = activeInterpreter.getOutputTensor(0).shape()
                val outputSize = outputShape.lastOrNull() ?: labels.size
                val modelInput = prepareInputBuffer(inputBuffer)
                val modelOutput = prepareOutputBuffer(outputSize)
                activeInterpreter.run(modelInput, modelOutput)
                val output = FloatArray(outputSize)
                modelOutput.rewind()
                modelOutput.asFloatBuffer().get(output)
                val inferenceTimeMillis = System.currentTimeMillis() - startedAt

                PredictionProcessor.processPrediction(
                    output = output,
                    labels = labels,
                    confidenceThreshold = confidenceThreshold,
                    inferenceTimeMillis = inferenceTimeMillis,
                    debugMode = true
                )
            }.getOrElse { throwable ->
                LogUtils.e("TFLite", "Inference failed", throwable)
                throw IllegalStateException("TFLite inference failed: ${throwable.message}", throwable)
            }
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        LogUtils.d("TFLite", "Interpreter closed")
    }

    private fun loadInterpreter(context: Context): Interpreter {
        if (!context.assets.list("").orEmpty().contains(Constants.MODEL_FILE)) {
            throw IllegalStateException("Model asset not found: ${Constants.MODEL_FILE}")
        }

        return runCatching {
            val modelBytes = context.assets.open(Constants.MODEL_FILE).use { it.readBytes() }
            val modelBuffer: ByteBuffer = ByteBuffer.allocateDirect(modelBytes.size)
                .order(ByteOrder.LITTLE_ENDIAN)
            modelBuffer.put(modelBytes)
            modelBuffer.rewind()

            createInterpreter(modelBuffer).also { loadedInterpreter ->
                val inputShape = loadedInterpreter.getInputTensor(0).shape()
                val outputShape = loadedInterpreter.getOutputTensor(0).shape()
                LogUtils.d("TFLite", "Input shape: ${inputShape.contentToString()}")
                LogUtils.d("TFLite", "Output shape: ${outputShape.contentToString()}")
                runCatching {
                    validateTensorShapes(inputShape, outputShape)
                }.onFailure { throwable ->
                    loadedInterpreter.close()
                    throw throwable
                }
            }
        }.getOrThrow()
    }

    private fun createInterpreter(modelBuffer: ByteBuffer): Interpreter {
        return runCatching {
            modelBuffer.rewind()
            Interpreter(
                modelBuffer,
                Interpreter.Options()
                    .setNumThreads(4)
                    .setUseNNAPI(true)
            )
        }.getOrElse { throwable ->
            LogUtils.e("TFLite", "NNAPI initialization failed; retrying with CPU", throwable)
            modelBuffer.rewind()
            Interpreter(
                modelBuffer,
                Interpreter.Options()
                    .setNumThreads(4)
                    .setUseNNAPI(false)
            )
        }
    }

    private fun prepareInputBuffer(input: FloatArray): ByteBuffer {
        inputTensorBuffer.rewind()
        inputTensorFloatBuffer.rewind()
        inputTensorFloatBuffer.put(input)
        inputTensorBuffer.rewind()
        return inputTensorBuffer
    }

    private fun prepareOutputBuffer(outputSize: Int): ByteBuffer {
        if (outputTensorBuffer == null || outputTensorSize != outputSize) {
            outputTensorBuffer = ByteBuffer
                .allocateDirect(outputSize * FLOAT_BYTE_SIZE)
                .order(ByteOrder.nativeOrder())
            outputTensorSize = outputSize
        }
        return requireNotNull(outputTensorBuffer).also { buffer ->
            buffer.rewind()
            buffer.asFloatBuffer().rewind()
        }
    }

    internal companion object {
        private const val FLOAT_BYTE_SIZE = 4

        fun validateTensorShapes(inputShape: IntArray, outputShape: IntArray) {
            val expectedInputShape = intArrayOf(
                1,
                Constants.FRAME_BUFFER_SIZE,
                Constants.MODEL_FEATURE_SIZE
            )
            val expectedOutputShape = intArrayOf(1, Constants.MODEL_OUTPUT_CLASSES)

            require(inputShape.contentEquals(expectedInputShape)) {
                "Model input shape mismatch: expected ${expectedInputShape.contentToString()}, " +
                    "got ${inputShape.contentToString()}"
            }
            require(outputShape.contentEquals(expectedOutputShape)) {
                "Model output shape mismatch: expected ${expectedOutputShape.contentToString()}, " +
                    "got ${outputShape.contentToString()}"
            }
        }
    }
}
