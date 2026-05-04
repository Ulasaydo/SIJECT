package com.signlanguage.translator.data.repository

import android.content.Context
import androidx.camera.core.CameraSelector
import com.signlanguage.translator.utils.Constants

class AppSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "siject_settings",
        Context.MODE_PRIVATE
    )

    fun getSettings(): AppSettings {
        return AppSettings(
            cameraLensFacing = preferences.getInt(KEY_CAMERA_LENS_FACING, CameraSelector.LENS_FACING_FRONT),
            confidenceThreshold = preferences.getFloat(KEY_CONFIDENCE_THRESHOLD, Constants.CONFIDENCE_THRESHOLD),
            showLandmarkOverlay = preferences.getBoolean(KEY_SHOW_LANDMARK_OVERLAY, true),
            showFpsCounter = preferences.getBoolean(KEY_SHOW_FPS_COUNTER, true)
        )
    }

    fun setCameraLensFacing(lensFacing: Int) {
        preferences.edit().putInt(KEY_CAMERA_LENS_FACING, lensFacing).apply()
    }

    fun setConfidenceThreshold(threshold: Float) {
        preferences.edit().putFloat(
            KEY_CONFIDENCE_THRESHOLD,
            threshold.coerceIn(MIN_CONFIDENCE_THRESHOLD, MAX_CONFIDENCE_THRESHOLD)
        ).apply()
    }

    fun setShowLandmarkOverlay(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_LANDMARK_OVERLAY, enabled).apply()
    }

    fun setShowFpsCounter(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_FPS_COUNTER, enabled).apply()
    }

    companion object {
        const val MIN_CONFIDENCE_THRESHOLD = 0.5f
        const val MAX_CONFIDENCE_THRESHOLD = 0.9f
        const val CONFIDENCE_SEEK_STEPS = 40

        private const val KEY_CAMERA_LENS_FACING = "camera_lens_facing"
        private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold"
        private const val KEY_SHOW_LANDMARK_OVERLAY = "show_landmark_overlay"
        private const val KEY_SHOW_FPS_COUNTER = "show_fps_counter"

        fun thresholdToProgress(threshold: Float): Int {
            val normalized = (threshold - MIN_CONFIDENCE_THRESHOLD) /
                (MAX_CONFIDENCE_THRESHOLD - MIN_CONFIDENCE_THRESHOLD)
            return (normalized.coerceIn(0f, 1f) * CONFIDENCE_SEEK_STEPS).toInt()
        }

        fun progressToThreshold(progress: Int): Float {
            val normalized = progress.coerceIn(0, CONFIDENCE_SEEK_STEPS).toFloat() /
                CONFIDENCE_SEEK_STEPS
            return MIN_CONFIDENCE_THRESHOLD +
                normalized * (MAX_CONFIDENCE_THRESHOLD - MIN_CONFIDENCE_THRESHOLD)
        }
    }
}

data class AppSettings(
    val cameraLensFacing: Int,
    val confidenceThreshold: Float,
    val showLandmarkOverlay: Boolean,
    val showFpsCounter: Boolean
)
