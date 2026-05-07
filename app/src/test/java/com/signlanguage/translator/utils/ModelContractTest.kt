package com.signlanguage.translator.utils

import com.signlanguage.translator.domain.services.LandmarkProcessor
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ModelContractTest {
    private val assetsRoot = File("src/main/assets")

    @Test
    fun landmarkIndicesJson_has48Indices() {
        val file = File(assetsRoot, Constants.LANDMARK_INDICES_FILE)
        assert(file.exists()) { "${file.absolutePath} not found" }
        val indices = LandmarkProcessor.parseLandmarkIndices(file.readText())
        assertEquals(Constants.SELECTED_LANDMARK_COUNT, indices.size)
        assertEquals(48, indices.size)
    }

    @Test
    fun landmarkIndicesJson_indicesInRange() {
        val file = File(assetsRoot, Constants.LANDMARK_INDICES_FILE)
        val indices = LandmarkProcessor.parseLandmarkIndices(file.readText())
        indices.forEach { i ->
            assert(i in 0 until Constants.TOTAL_LANDMARK_COUNT) {
                "Index $i out of range [0, ${Constants.TOTAL_LANDMARK_COUNT})"
            }
        }
    }

    @Test
    fun landmarkIndicesJson_metadataDescribesContract() {
        val file = File(assetsRoot, Constants.LANDMARK_INDICES_FILE)
        val root = JSONObject(file.readText())
        val description = root.optString("description")
        assert(description.contains("48")) { "description must mention 48 landmarks" }
        assert(description.contains("144")) { "description must mention 144 features" }
    }

    @Test
    fun labelsTxt_has250Lines() {
        val file = File(assetsRoot, Constants.LABELS_FILE)
        assert(file.exists())
        val nonBlank = file.readLines().count { it.isNotBlank() }
        assertEquals(Constants.MODEL_OUTPUT_CLASSES, nonBlank)
        assertEquals(250, nonBlank)
    }

    @Test
    fun modelFeatureSize_is144() {
        assertEquals(144, Constants.MODEL_FEATURE_SIZE)
        assertEquals(Constants.SELECTED_LANDMARK_COUNT * Constants.LANDMARK_AXIS_COUNT, Constants.MODEL_FEATURE_SIZE)
    }

    @Test
    fun frameBufferSize_is30() {
        assertEquals(30, Constants.FRAME_BUFFER_SIZE)
    }
}
