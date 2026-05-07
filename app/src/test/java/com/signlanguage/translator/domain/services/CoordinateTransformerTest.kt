package com.signlanguage.translator.domain.services

import com.signlanguage.translator.data.model.LandmarkPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateTransformerTest {
    @Test
    fun mirrorOff_preservesX() {
        val p = LandmarkPoint(x = 0.3f, y = 0.5f, z = 0f, visibility = 1f)
        val out = CoordinateTransformer.applyMirror(p, mirrorHorizontally = false)
        assertEquals(0.3f, out.x, 1e-6f)
        assertEquals(0.5f, out.y, 1e-6f)
    }

    @Test
    fun mirrorOn_flipsX() {
        val p = LandmarkPoint(x = 0.2f, y = 0.7f, z = 0f, visibility = 1f)
        val out = CoordinateTransformer.applyMirror(p, mirrorHorizontally = true)
        assertEquals(0.8f, out.x, 1e-6f)
        assertEquals(0.7f, out.y, 1e-6f)
    }

    @Test
    fun mirrorOn_listMapsAll() {
        val list = listOf(
            LandmarkPoint(x = 0.1f, y = 0f, z = 0f, visibility = 1f),
            LandmarkPoint(x = 0.9f, y = 1f, z = 0f, visibility = 1f)
        )
        val out = CoordinateTransformer.applyMirror(list, mirrorHorizontally = true)
        assertEquals(0.9f, out[0].x, 1e-6f)
        assertEquals(0.1f, out[1].x, 1e-6f)
    }

    @Test
    fun rotateNormalized_zeroIsIdentity() {
        val (x, y) = CoordinateTransformer.rotateNormalized(0.3f, 0.7f, 0)
        assertEquals(0.3f, x, 1e-6f); assertEquals(0.7f, y, 1e-6f)
    }

    @Test
    fun rotateNormalized_90() {
        val (x, y) = CoordinateTransformer.rotateNormalized(0.3f, 0.7f, 90)
        assertEquals(1f - 0.7f, x, 1e-6f); assertEquals(0.3f, y, 1e-6f)
    }

    @Test
    fun rotateNormalized_180() {
        val (x, y) = CoordinateTransformer.rotateNormalized(0.3f, 0.7f, 180)
        assertEquals(0.7f, x, 1e-6f); assertEquals(0.3f, y, 1e-6f)
    }

    @Test
    fun rotateNormalized_270() {
        val (x, y) = CoordinateTransformer.rotateNormalized(0.3f, 0.7f, 270)
        assertEquals(0.7f, x, 1e-6f); assertEquals(1f - 0.3f, y, 1e-6f)
    }

    @Test
    fun rotateNormalized_negativeRotationNormalizes() {
        val (x, y) = CoordinateTransformer.rotateNormalized(0.3f, 0.7f, -90)
        // -90 == 270
        assertEquals(0.7f, x, 1e-6f); assertEquals(1f - 0.3f, y, 1e-6f)
    }
}
