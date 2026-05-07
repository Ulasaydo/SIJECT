package com.signlanguage.translator.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.signlanguage.translator.data.model.LandmarkPoint
import kotlin.math.max

class LandmarkOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val landmarkConnections = buildList {
        addAll(POSE_CONNECTIONS)
        addAll(handConnections(LEFT_HAND_OFFSET))
        addAll(handConnections(RIGHT_HAND_OFFSET))
    }
    private var landmarks: List<LandmarkPoint> = emptyList()
    private var mirrorHorizontally: Boolean = true
    private var sourceImageWidth: Int = 0
    private var sourceImageHeight: Int = 0

    fun submitLandmarks(points: List<LandmarkPoint>) {
        landmarks = points
        invalidate()
    }

    fun setMirrorHorizontally(enabled: Boolean) {
        if (mirrorHorizontally != enabled) {
            mirrorHorizontally = enabled
            invalidate()
        }
    }

    fun setSourceImageSize(width: Int, height: Int) {
        val coercedWidth = width.coerceAtLeast(0)
        val coercedHeight = height.coerceAtLeast(0)
        if (sourceImageWidth != coercedWidth || sourceImageHeight != coercedHeight) {
            sourceImageWidth = coercedWidth
            sourceImageHeight = coercedHeight
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t0 = System.nanoTime()
        drawConnections(canvas)
        landmarks.asSequence().filter { it.isVisible() }.forEach { point ->
            pointPaint.color = when {
                point.visibility > 0.7f -> Color.GREEN
                point.visibility > 0.5f -> Color.YELLOW
                else -> Color.RED
            }
            canvas.drawCircle(point.toCanvasX(), point.toCanvasY(), 4f, pointPaint)
        }
        val ms = (System.nanoTime() - t0) / 1_000_000.0
        Log.d("PerfProfile", "onDraw=${"%.2f".format(ms)}ms lm=${landmarks.size}")
    }

    private fun drawConnections(canvas: Canvas) {
        landmarkConnections.forEach { (startIndex, endIndex) ->
            val start = landmarks.getOrNull(startIndex)
            val end = landmarks.getOrNull(endIndex)
            if (start?.isVisible() == true && end?.isVisible() == true) {
                canvas.drawLine(
                    start.toCanvasX(),
                    start.toCanvasY(),
                    end.toCanvasX(),
                    end.toCanvasY(),
                    linePaint
                )
            }
        }
    }

    private fun LandmarkPoint.isVisible(): Boolean {
        return visibility > 0f && x in 0f..1f && y in 0f..1f
    }

    private fun LandmarkPoint.toCanvasX(): Float {
        val normalizedX = if (mirrorHorizontally) 1f - x else x
        val contentRect = previewContentRect()
        return contentRect.left + normalizedX.coerceIn(0f, 1f) * contentRect.width()
    }

    private fun LandmarkPoint.toCanvasY(): Float {
        val contentRect = previewContentRect()
        return contentRect.top + y.coerceIn(0f, 1f) * contentRect.height()
    }

    private fun previewContentRect(): RectF {
        if (sourceImageWidth <= 0 || sourceImageHeight <= 0 || width <= 0 || height <= 0) {
            return RectF(0f, 0f, width.toFloat(), height.toFloat())
        }

        val scale = max(
            width.toFloat() / sourceImageWidth.toFloat(),
            height.toFloat() / sourceImageHeight.toFloat()
        )
        val scaledWidth = sourceImageWidth * scale
        val scaledHeight = sourceImageHeight * scale
        val left = (width - scaledWidth) / 2f
        val top = (height - scaledHeight) / 2f
        return RectF(left, top, left + scaledWidth, top + scaledHeight)
    }

    companion object {
        private const val LEFT_HAND_OFFSET = 501
        private const val RIGHT_HAND_OFFSET = 522

        private val POSE_CONNECTIONS = listOf(
            0 to 1,
            1 to 2,
            2 to 3,
            3 to 7,
            0 to 4,
            4 to 5,
            5 to 6,
            6 to 8,
            9 to 10,
            11 to 12,
            11 to 13,
            13 to 15,
            15 to 17,
            15 to 19,
            15 to 21,
            17 to 19,
            12 to 14,
            14 to 16,
            16 to 18,
            16 to 20,
            16 to 22,
            18 to 20,
            11 to 23,
            12 to 24,
            23 to 24,
            23 to 25,
            24 to 26,
            25 to 27,
            26 to 28,
            27 to 29,
            28 to 30,
            29 to 31,
            30 to 32,
            27 to 31,
            28 to 32
        )

        private fun handConnections(offset: Int): List<Pair<Int, Int>> {
            return listOf(
                0 to 1,
                1 to 2,
                2 to 3,
                3 to 4,
                0 to 5,
                5 to 6,
                6 to 7,
                7 to 8,
                5 to 9,
                9 to 10,
                10 to 11,
                11 to 12,
                9 to 13,
                13 to 14,
                14 to 15,
                15 to 16,
                13 to 17,
                17 to 18,
                18 to 19,
                19 to 20,
                0 to 17
            ).map { (start, end) -> (offset + start) to (offset + end) }
        }
    }
}
