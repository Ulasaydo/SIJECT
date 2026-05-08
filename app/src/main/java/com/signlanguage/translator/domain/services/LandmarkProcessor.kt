package com.signlanguage.translator.domain.services

import android.content.Context
import com.signlanguage.translator.data.model.LandmarkPoint
import com.signlanguage.translator.utils.Constants
import com.signlanguage.translator.utils.LogUtils
import com.signlanguage.translator.utils.TensorUtils
import org.json.JSONObject

/**
 * Applies the configured landmark index contract and flattens landmarks into model features.
 */
class LandmarkProcessor(context: Context) {
    private val selectedIndicesResult = loadLandmarkIndices(context)

    fun normalizeAndFlatten(points: List<LandmarkPoint>): FloatArray {
        val selectedIndices = selectedIndicesResult.getOrElse {
            throw IllegalStateException(
                "Invalid ${Constants.LANDMARK_INDICES_FILE}: expected exactly " +
                    "${Constants.SELECTED_LANDMARK_COUNT} indices for the loaded model input contract.",
                it
            )
        }
        val selectedPoints = selectedIndices.map { index ->
            points.getOrNull(index)?.takeIf { it.visibility >= Constants.LANDMARK_VISIBILITY_THRESHOLD }
                ?: LandmarkPoint(x = 0f, y = 0f, z = 0f, visibility = 0f)
        }
        val flat = TensorUtils.flattenLandmarks(selectedPoints)
        return standardizePerFrame(flat)
    }

    private fun standardizePerFrame(values: FloatArray): FloatArray {
        if (values.isEmpty()) return values
        var sum = 0.0
        for (v in values) sum += v
        val mean = (sum / values.size).toFloat()
        var sqSum = 0.0
        for (v in values) {
            val d = v - mean
            sqSum += d * d
        }
        val std = kotlin.math.sqrt(sqSum / values.size).toFloat()
        val out = FloatArray(values.size)
        for (i in values.indices) out[i] = (values[i] - mean) / (std + EPSILON)
        return out
    }

    private fun loadLandmarkIndices(context: Context): Result<List<Int>> {
        return runCatching {
            val json = context.assets.open(Constants.LANDMARK_INDICES_FILE)
                .bufferedReader()
                .use { it.readText() }
            parseLandmarkIndices(json)
        }.onFailure { throwable ->
            LogUtils.e("LandmarkProcessor", "Failed to load landmark indices", throwable)
        }
    }

    internal companion object {
        private const val EPSILON = 1e-6f

        fun parseLandmarkIndices(json: String): List<Int> {
            val root = JSONObject(json)
            val indicesArray = root.optJSONArray("indices")
                ?: throw IllegalStateException("${Constants.LANDMARK_INDICES_FILE} is missing an indices array.")

            if (indicesArray.length() != Constants.SELECTED_LANDMARK_COUNT) {
                throw IllegalStateException(
                    "${Constants.LANDMARK_INDICES_FILE} has ${indicesArray.length()} indices; " +
                        "expected ${Constants.SELECTED_LANDMARK_COUNT}."
                )
            }

            return List(indicesArray.length()) { index ->
                indicesArray.getInt(index).also { landmarkIndex ->
                    require(landmarkIndex in 0 until Constants.TOTAL_LANDMARK_COUNT) {
                        "${Constants.LANDMARK_INDICES_FILE} contains out-of-range index $landmarkIndex " +
                            "at position $index; valid range is 0..${Constants.TOTAL_LANDMARK_COUNT - 1}."
                    }
                }
            }
        }
    }
}
