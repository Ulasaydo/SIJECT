package com.signlanguage.translator.utils

object TensorUtils {
    fun flattenLandmarks(points: List<com.signlanguage.translator.data.model.LandmarkPoint>): FloatArray {
        val outputSize = Constants.SELECTED_LANDMARK_COUNT * Constants.LANDMARK_AXIS_COUNT
        val output = FloatArray(outputSize)
        points.take(Constants.SELECTED_LANDMARK_COUNT).forEachIndexed { index, point ->
            val offset = index * Constants.LANDMARK_AXIS_COUNT
            output[offset] = point.x
            output[offset + 1] = point.y
            output[offset + 2] = point.z
        }
        return output
    }
}
