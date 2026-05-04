package com.signlanguage.translator.domain.services

import org.junit.Assert.assertTrue
import org.junit.Test

class TFLiteServiceTest {
    @Test
    fun validateTensorShapes_acceptsExpectedModelContract() {
        TFLiteService.validateTensorShapes(
            inputShape = intArrayOf(1, 30, 1629),
            outputShape = intArrayOf(1, 250)
        )
    }

    @Test
    fun validateTensorShapes_rejectsInputShapeMismatch() {
        val error = runCatching {
            TFLiteService.validateTensorShapes(
                inputShape = intArrayOf(1, 30, 540),
                outputShape = intArrayOf(1, 250)
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("input shape mismatch"))
    }

    @Test
    fun validateTensorShapes_rejectsOutputShapeMismatch() {
        val error = runCatching {
            TFLiteService.validateTensorShapes(
                inputShape = intArrayOf(1, 30, 1629),
                outputShape = intArrayOf(1, 20)
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("output shape mismatch"))
    }
}
