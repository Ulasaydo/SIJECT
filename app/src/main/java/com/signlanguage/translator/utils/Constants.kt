package com.signlanguage.translator.utils

object Constants {
    const val MODEL_FILE = "asl_model_final.tflite"
    const val LABELS_FILE = "labels.txt"
    const val LANDMARK_INDICES_FILE = "landmark_indices.json"
    const val POSE_LANDMARKER_TASK_FILE = "pose_landmarker.task"
    const val FACE_LANDMARKER_TASK_FILE = "face_landmarker.task"
    const val HAND_LANDMARKER_TASK_FILE = "hand_landmarker.task"
    const val FRAME_BUFFER_SIZE = 30
    const val TOTAL_LANDMARK_COUNT = 543
    const val SELECTED_LANDMARK_COUNT = 48
    const val LANDMARK_AXIS_COUNT = 3
    const val MODEL_FEATURE_SIZE = SELECTED_LANDMARK_COUNT * LANDMARK_AXIS_COUNT
    const val MODEL_OUTPUT_CLASSES = 250
    const val CONFIDENCE_THRESHOLD = 0.70f
    const val LANDMARK_VISIBILITY_THRESHOLD = 0.5f
    const val SMOOTHING_WINDOW_SIZE = 3
}
