package com.signlanguage.translator.domain.services

import android.content.Context
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.signlanguage.translator.data.model.LandmarkPoint
import com.signlanguage.translator.utils.Constants
import com.signlanguage.translator.utils.LogUtils
import java.util.concurrent.TimeUnit

/**
 * Runs MediaPipe Tasks in video mode and returns landmarks in the model's fixed holistic order.
 */
class MediaPipeService(
    private val context: Context
) {
    private var confidenceThreshold: Float = Constants.LANDMARK_VISIBILITY_THRESHOLD
    private var poseLandmarker: PoseLandmarker? = null
    private var faceLandmarker: FaceLandmarker? = null
    private var handLandmarker: HandLandmarker? = null
    private var handDelegate: Delegate? = null
    private var requiredAssetsVerified = false

    @OptIn(ExperimentalGetImage::class)
    fun extractLandmarks(imageProxy: ImageProxy): List<LandmarkPoint> {
        ensureLandmarkers()
        val mediaImage = imageProxy.image
            ?: throw IllegalStateException("Camera frame image is unavailable.")
        val mpImage = MediaImageBuilder(mediaImage).build()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val timestampMillis = TimeUnit.NANOSECONDS.toMillis(imageProxy.imageInfo.timestamp)
        val imageOptions = ImageProcessingOptions.builder().build()

        return try {
            val pose = if (ENABLE_POSE_LANDMARKER) {
                poseLandmarker
                    ?.detectForVideo(mpImage, imageOptions, timestampMillis)
                    ?.landmarks()
                    ?.firstOrNull()
                    .orEmpty()
            } else {
                emptyList()
            }
            val face = if (ENABLE_FACE_LANDMARKER) {
                faceLandmarker
                    ?.detectForVideo(mpImage, imageOptions, timestampMillis)
                    ?.faceLandmarks()
                    ?.firstOrNull()
                    .orEmpty()
            } else {
                emptyList()
            }
            val handsResult = detectHands(mpImage, imageOptions, timestampMillis)
            val hands = handsResult?.landmarks().orEmpty()
            val handedness = handsResult?.handedness().orEmpty()

            val raw = buildList(Constants.TOTAL_LANDMARK_COUNT) {
                appendLandmarks(pose, POSE_LANDMARK_COUNT)
                appendLandmarks(face, FACE_LANDMARK_COUNT)
                appendHandLandmarks(hands, handedness, "Left")
                appendHandLandmarks(hands, handedness, "Right")
            }
            applyDisplayRotation(raw, rotationDegrees)
        } finally {
            mpImage.close()
        }
    }

    private fun applyDisplayRotation(
        points: List<LandmarkPoint>,
        rotationDegrees: Int
    ): List<LandmarkPoint> {
        if ((((rotationDegrees % 360) + 360) % 360) == 0) return points
        return points.map { p ->
            val (rx, ry) = CoordinateTransformer.rotateNormalized(p.x, p.y, rotationDegrees)
            p.copy(x = rx, y = ry)
        }
    }

    fun setConfidenceThreshold(threshold: Float) {
        confidenceThreshold = threshold.coerceIn(0f, 1f)
    }

    fun release() {
        poseLandmarker?.close()
        faceLandmarker?.close()
        handLandmarker?.close()
        poseLandmarker = null
        faceLandmarker = null
        handLandmarker = null
        LogUtils.d("MediaPipe", "MediaPipe resources released")
    }

    private fun ensureLandmarkers() {
        if (!requiredAssetsVerified) {
            val assets = context.assets.list("").orEmpty().toSet()
            val missingAssets = REQUIRED_TASK_ASSETS.filterNot(assets::contains)
            check(missingAssets.isEmpty()) {
                "Missing MediaPipe task assets: ${missingAssets.joinToString()}. " +
                    "Add them under app/src/main/assets/ to enable real frame processing."
            }
            requiredAssetsVerified = true
        }

        if (ENABLE_POSE_LANDMARKER && poseLandmarker == null) {
            poseLandmarker = PoseLandmarker.createFromOptions(
                context,
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(taskBaseOptions(Constants.POSE_LANDMARKER_TASK_FILE, Delegate.GPU))
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(confidenceThreshold)
                    .setMinPosePresenceConfidence(confidenceThreshold)
                    .setMinTrackingConfidence(confidenceThreshold)
                    .build()
            )
        }
        if (ENABLE_FACE_LANDMARKER && faceLandmarker == null) {
            faceLandmarker = FaceLandmarker.createFromOptions(
                context,
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(taskBaseOptions(Constants.FACE_LANDMARKER_TASK_FILE))
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumFaces(1)
                    .setMinFaceDetectionConfidence(confidenceThreshold)
                    .setMinFacePresenceConfidence(confidenceThreshold)
                    .setMinTrackingConfidence(confidenceThreshold)
                    .build()
            )
        }
        if (handLandmarker == null) {
            handLandmarker = createHandLandmarkerWithFallback()
        }
    }

    private fun detectHands(
        mpImage: com.google.mediapipe.framework.image.MPImage,
        imageOptions: ImageProcessingOptions,
        timestampMillis: Long
    ): HandLandmarkerResult? {
        return runCatching {
            handLandmarker?.detectForVideo(mpImage, imageOptions, timestampMillis)
        }.getOrElse { throwable ->
            if (handDelegate == Delegate.GPU) {
                LogUtils.e("MediaPipe", "GPU hand detection failed; falling back to CPU", throwable)
                handLandmarker?.close()
                handLandmarker = createHandLandmarker(Delegate.CPU)
                handDelegate = Delegate.CPU
                handLandmarker?.detectForVideo(mpImage, imageOptions, timestampMillis)
            } else {
                throw throwable
            }
        }
    }

    private fun createHandLandmarkerWithFallback(): HandLandmarker {
        return runCatching {
            createHandLandmarker(Delegate.GPU).also {
                handDelegate = Delegate.GPU
                LogUtils.d("PerfProfile", "Hand landmarker initialized with GPU delegate")
            }
        }.getOrElse { throwable ->
            LogUtils.e("MediaPipe", "GPU hand landmarker unavailable; falling back to CPU", throwable)
            createHandLandmarker(Delegate.CPU).also {
                handDelegate = Delegate.CPU
                LogUtils.d("PerfProfile", "Hand landmarker initialized with CPU delegate")
            }
        }
    }

    private fun createHandLandmarker(delegate: Delegate): HandLandmarker {
        return HandLandmarker.createFromOptions(
            context,
            HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(taskBaseOptions(Constants.HAND_LANDMARKER_TASK_FILE, delegate))
                .setRunningMode(RunningMode.VIDEO)
                .setNumHands(2)
                .setMinHandDetectionConfidence(confidenceThreshold)
                .setMinHandPresenceConfidence(confidenceThreshold)
                .setMinTrackingConfidence(confidenceThreshold)
                .build()
        )
    }

    private fun taskBaseOptions(assetPath: String, delegate: Delegate = Delegate.CPU): BaseOptions {
        return BaseOptions.builder()
            .setModelAssetPath(assetPath)
            .setDelegate(delegate)
            .build()
    }

    private fun MutableList<LandmarkPoint>.appendLandmarks(
        landmarks: List<NormalizedLandmark>,
        expectedCount: Int
    ) {
        repeat(expectedCount) { index ->
            add(landmarks.getOrNull(index).toLandmarkPoint())
        }
    }

    private fun MutableList<LandmarkPoint>.appendHandLandmarks(
        hands: List<List<NormalizedLandmark>>,
        handedness: List<List<Category>>,
        expectedLabel: String
    ) {
        val handIndex = handedness.indexOfFirst { categories ->
            categories.firstOrNull()?.categoryName()?.equals(expectedLabel, ignoreCase = true) == true
        }
        appendLandmarks(hands.getOrNull(handIndex).orEmpty(), HAND_LANDMARK_COUNT)
    }

    private fun NormalizedLandmark?.toLandmarkPoint(): LandmarkPoint {
        if (this == null) return ZERO_LANDMARK
        return LandmarkPoint(
            x = x(),
            y = y(),
            z = z(),
            visibility = visibility().orElse(presence().orElse(1f))
        )
    }

    companion object {
        private const val ENABLE_POSE_LANDMARKER = true
        private const val ENABLE_FACE_LANDMARKER = false
        private const val POSE_LANDMARK_COUNT = 33
        private const val FACE_LANDMARK_COUNT = 468
        private const val HAND_LANDMARK_COUNT = 21
        private val ZERO_LANDMARK = LandmarkPoint(x = 0f, y = 0f, z = 0f, visibility = 0f)
        private val REQUIRED_TASK_ASSETS = buildList {
            if (ENABLE_POSE_LANDMARKER) add(Constants.POSE_LANDMARKER_TASK_FILE)
            if (ENABLE_FACE_LANDMARKER) add(Constants.FACE_LANDMARKER_TASK_FILE)
            add(Constants.HAND_LANDMARKER_TASK_FILE)
        }
    }
}
