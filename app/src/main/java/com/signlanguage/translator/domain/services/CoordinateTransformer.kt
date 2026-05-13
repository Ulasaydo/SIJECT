package com.signlanguage.translator.domain.services

import com.signlanguage.translator.data.model.LandmarkPoint

/**
 * Pure functions to transform normalized landmarks.
 *
 * ROTATION CONTRACT — DO NOT CHANGE without device testing.
 * We do NOT pass rotationDegrees to MediaPipe (ImageProcessingOptions). Detection
 * runs on the raw sensor-orientation image and MediaPipeService rotates the output
 * landmarks to display orientation via [rotateNormalized]. This combination is the
 * only one verified on real hardware (Samsung A21s) to place landmarks correctly on
 * the user's body. Earlier attempts to "let MediaPipe rotate" produced mis-aligned
 * overlays even though it sounds cleaner in theory.
 *
 * For the front-facing camera the resulting landmarks are also mirrored horizontally
 * via [applyMirror] so the model sees the orientation matching the training data.
 */
object CoordinateTransformer {

    fun applyMirror(point: LandmarkPoint, mirrorHorizontally: Boolean): LandmarkPoint {
        if (!mirrorHorizontally) return point
        return point.copy(x = (1f - point.x).coerceIn(0f, 1f))
    }

    fun applyMirror(points: List<LandmarkPoint>, mirrorHorizontally: Boolean): List<LandmarkPoint> {
        if (!mirrorHorizontally) return points
        return points.map { applyMirror(it, true) }
    }

    /**
     * Rotates a normalized point in [0,1] x [0,1] by the given clockwise display
     * rotation degrees. Provided for completeness; not used when MediaPipe already
     * applies rotation via ImageProcessingOptions.
     */
    fun rotateNormalized(x: Float, y: Float, rotationDegrees: Int): Pair<Float, Float> {
        return when (((rotationDegrees % 360) + 360) % 360) {
            90 -> (1f - y) to x
            180 -> (1f - x) to (1f - y)
            270 -> y to (1f - x)
            else -> x to y
        }
    }
}
