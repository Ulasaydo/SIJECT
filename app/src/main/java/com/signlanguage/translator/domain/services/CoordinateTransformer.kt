package com.signlanguage.translator.domain.services

import com.signlanguage.translator.data.model.LandmarkPoint

/**
 * Pure functions to transform upright normalized landmarks for overlay rendering.
 *
 * MediaPipe Tasks Vision returns landmarks already rotated to display orientation
 * when ImageProcessingOptions.setRotationDegrees(rotationDegrees) is provided.
 * The model consumes these upright landmarks directly. The overlay path additionally
 * mirrors horizontally for the front-facing camera so the user sees a natural
 * "mirror" view.
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
