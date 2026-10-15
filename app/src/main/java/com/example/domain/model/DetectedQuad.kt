package com.example.domain.model

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class DetectedQuad(
    val topLeft: PointF = PointF(0.12f, 0.18f),
    val topRight: PointF = PointF(0.88f, 0.18f),
    val bottomRight: PointF = PointF(0.88f, 0.82f),
    val bottomLeft: PointF = PointF(0.12f, 0.82f),
    val confidence: Float = 0.85f,
    val isDetected: Boolean = true,
    val isStable: Boolean = false,
    val isAligned: Boolean = true,
    val stabilityProgress: Float = 0f // 0f to 1f for auto-capture progress
) {
    val width: Float
        get() {
            val topW = hypot(topRight.x - topLeft.x, topRight.y - topLeft.y)
            val botW = hypot(bottomRight.x - bottomLeft.x, bottomRight.y - bottomLeft.y)
            return (topW + botW) / 2f
        }

    val height: Float
        get() {
            val leftH = hypot(bottomLeft.x - topLeft.x, bottomLeft.y - topLeft.y)
            val rightH = hypot(bottomRight.x - topRight.x, bottomRight.y - topRight.y)
            return (leftH + rightH) / 2f
        }

    val aspectRatio: Float
        get() = if (width > 0.001f) height / width else 1.414f

    val detectedCategoryName: String
        get() = when {
            aspectRatio in 1.30f..1.55f -> "A4 Belge"
            aspectRatio in 0.55f..0.72f -> "Kimlik / Kart"
            aspectRatio in 1.15f..1.30f -> "US Letter"
            aspectRatio in 0.90f..1.10f -> "Kare Belge"
            aspectRatio > 1.55f -> "Uzun Fiş / Makbuz"
            else -> "Özel Belge"
        }

    fun lerpWith(target: DetectedQuad, alpha: Float = 0.35f): DetectedQuad {
        return DetectedQuad(
            topLeft = PointF(
                topLeft.x + (target.topLeft.x - topLeft.x) * alpha,
                topLeft.y + (target.topLeft.y - topLeft.y) * alpha
            ),
            topRight = PointF(
                topRight.x + (target.topRight.x - topRight.x) * alpha,
                topRight.y + (target.topRight.y - topRight.y) * alpha
            ),
            bottomRight = PointF(
                bottomRight.x + (target.bottomRight.x - bottomRight.x) * alpha,
                bottomRight.y + (target.bottomRight.y - bottomRight.y) * alpha
            ),
            bottomLeft = PointF(
                bottomLeft.x + (target.bottomLeft.x - bottomLeft.x) * alpha,
                bottomLeft.y + (target.bottomLeft.y - bottomLeft.y) * alpha
            ),
            confidence = confidence + (target.confidence - confidence) * alpha,
            isDetected = target.isDetected,
            isStable = target.isStable,
            isAligned = target.isAligned,
            stabilityProgress = target.stabilityProgress
        )
    }

    companion object {
        fun defaultViewport(modeRatio: Float = 1.414f): DetectedQuad {
            val left = 0.10f
            val right = 0.90f
            val top = 0.18f
            val bottom = 0.82f
            return DetectedQuad(
                topLeft = PointF(left, top),
                topRight = PointF(right, top),
                bottomRight = PointF(right, bottom),
                bottomLeft = PointF(left, bottom),
                confidence = 0.5f,
                isDetected = false,
                isStable = false,
                isAligned = true,
                stabilityProgress = 0f
            )
        }
    }
}
