package com.example

import android.graphics.Point
import android.graphics.Rect
import com.example.domain.engine.RecognizedTextBlock
import com.example.domain.engine.RecognizedTextLine
import com.example.domain.engine.ScannerTextResult
import com.example.ui.components.TranslucentOverlayStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScannedImageTextOverlayTest {

    @Test
    fun testOverlayStyleLabels() {
        assertEquals("Hibrit", TranslucentOverlayStyle.HYBRID.label)
        assertEquals("Metin", TranslucentOverlayStyle.TEXT_BADGES.label)
        assertEquals("Vurgu", TranslucentOverlayStyle.GLOW_BOXES.label)
        assertEquals("X-Ray", TranslucentOverlayStyle.FULL_VEIL.label)
    }

    @Test
    fun testScannerTextResultWithImageDimensions() {
        val line = RecognizedTextLine(
            text = "Total Amount: $150.00",
            boundingBox = Rect(50, 100, 450, 140),
            confidence = 0.98f
        )
        val block = RecognizedTextBlock(
            text = "Total Amount: $150.00",
            boundingBox = Rect(50, 100, 450, 140),
            cornerPoints = listOf(Point(50, 100), Point(450, 100), Point(450, 140), Point(50, 140)),
            lines = listOf(line)
        )
        val result = ScannerTextResult(
            fullText = "Total Amount: $150.00",
            lineCount = 1,
            wordCount = 3,
            characterCount = 21,
            blocks = listOf(block),
            rawTextBlocks = listOf("Total Amount: $150.00"),
            detectedLanguage = "en",
            imageWidth = 1080,
            imageHeight = 1920
        )

        assertNotNull(result)
        assertEquals(1080, result.imageWidth)
        assertEquals(1920, result.imageHeight)
        assertEquals(1, result.blocks.size)

        val firstBlock = result.blocks.first()
        assertEquals(Rect(50, 100, 450, 140), firstBlock.boundingBox)
        assertTrue(firstBlock.text.contains("Total Amount"))
    }

    @Test
    fun testBoundingBoxScaleMath() {
        val imgWidth = 1000f
        val imgHeight = 2000f
        val containerWidth = 500f
        val containerHeight = 1000f

        // Fit scale
        val scale = minOf(containerWidth / imgWidth, containerHeight / imgHeight)
        assertEquals(0.5f, scale, 0.0001f)

        val renderedW = imgWidth * scale
        val renderedH = imgHeight * scale
        val offsetX = (containerWidth - renderedW) / 2f
        val offsetY = (containerHeight - renderedH) / 2f

        assertEquals(0f, offsetX, 0.0001f)
        assertEquals(0f, offsetY, 0.0001f)

        // Point at (100, 200) maps to (50, 100)
        val targetX = offsetX + 100f * scale
        val targetY = offsetY + 200f * scale

        assertEquals(50f, targetX, 0.0001f)
        assertEquals(100f, targetY, 0.0001f)
    }
}
