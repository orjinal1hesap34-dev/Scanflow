package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.domain.engine.ColorMatrixFilterType
import com.example.domain.engine.ColorMatrixFilterUtility
import com.example.domain.engine.applyBlackAndWhite
import com.example.domain.engine.applyColorMatrix
import com.example.domain.engine.applyEnhanced
import com.example.domain.engine.applyFilter
import com.example.domain.engine.applyGrayscale
import com.example.domain.model.ImageFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColorMatrixFilterUtilityTest {

    @Test
    fun testBlackAndWhiteMatrixStructure() {
        val matrix = ColorMatrixFilterUtility.createBlackAndWhiteMatrix(contrast = 3.5f, brightness = -280f)
        assertNotNull(matrix)

        val array = ColorMatrixFilterUtility.getBlackAndWhiteMatrixArray(contrast = 3.5f, brightness = -280f)
        assertEquals(20, array.size)
        // Check luminance coefficients multiplied by contrast
        assertEquals(ColorMatrixFilterUtility.LUM_RED * 3.5f, array[0], 0.001f)
        assertEquals(ColorMatrixFilterUtility.LUM_GREEN * 3.5f, array[1], 0.001f)
        assertEquals(ColorMatrixFilterUtility.LUM_BLUE * 3.5f, array[2], 0.001f)
        assertEquals(-280f, array[4], 0.001f)
    }

    @Test
    fun testGrayscaleMatrixStructure() {
        val matrix = ColorMatrixFilterUtility.createGrayscaleMatrix()
        assertNotNull(matrix)

        val array = ColorMatrixFilterUtility.getGrayscaleMatrixArray()
        assertEquals(20, array.size)
        assertEquals(0.299f, array[0], 0.001f)
        assertEquals(0.587f, array[1], 0.001f)
        assertEquals(0.114f, array[2], 0.001f)
        assertEquals(0f, array[4], 0.001f)
    }

    @Test
    fun testEnhancedMatrixStructure() {
        val matrix = ColorMatrixFilterUtility.createEnhancedMatrix(
            contrast = 1.35f,
            brightness = 25f,
            saturation = 1.25f
        )
        assertNotNull(matrix)

        val array = ColorMatrixFilterUtility.getEnhancedMatrixArray(
            contrast = 1.35f,
            brightness = 25f,
            saturation = 1.25f
        )
        assertEquals(20, array.size)
    }

    @Test
    fun testFilterTypeMapping() {
        assertEquals(ImageFilter.BLACK_WHITE, ColorMatrixFilterType.BLACK_AND_WHITE.toImageFilter())
        assertEquals(ImageFilter.GRAYSCALE, ColorMatrixFilterType.GRAYSCALE.toImageFilter())
        assertEquals(ImageFilter.ENHANCED, ColorMatrixFilterType.ENHANCED.toImageFilter())

        assertEquals(ColorMatrixFilterType.BLACK_AND_WHITE, ColorMatrixFilterType.fromImageFilter(ImageFilter.BLACK_WHITE))
        assertEquals(ColorMatrixFilterType.GRAYSCALE, ColorMatrixFilterType.fromImageFilter(ImageFilter.GRAYSCALE))
        assertEquals(ColorMatrixFilterType.ENHANCED, ColorMatrixFilterType.fromImageFilter(ImageFilter.ENHANCED))
        assertEquals(ColorMatrixFilterType.ENHANCED, ColorMatrixFilterType.fromImageFilter(ImageFilter.DOCUMENT))
    }

    @Test
    fun testApplyGrayscaleFilterToBitmap() {
        // Create a 10x10 colored bitmap (pure red)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)

        val grayscaleBitmap = ColorMatrixFilterUtility.applyGrayscale(bitmap)
        assertNotNull(grayscaleBitmap)
        assertEquals(10, grayscaleBitmap.width)
        assertEquals(10, grayscaleBitmap.height)

        val pixel = grayscaleBitmap.getPixel(5, 5)
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)

        // In grayscale, R, G, and B values should be nearly equal
        assertTrue("R and G should match in grayscale", Math.abs(r - g) <= 2)
        assertTrue("G and B should match in grayscale", Math.abs(g - b) <= 2)
    }

    @Test
    fun testApplyBlackAndWhiteFilterToBitmap() {
        // Create a 10x10 bitmap with light gray (paper) and dark gray (text)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(240, 240, 240)) // Light paper background
        bitmap.setPixel(5, 5, Color.rgb(40, 40, 40)) // Dark ink

        val bwBitmap = ColorMatrixFilterUtility.applyBlackAndWhite(bitmap)
        assertNotNull(bwBitmap)

        val bgPixel = bwBitmap.getPixel(1, 1)
        val textPixel = bwBitmap.getPixel(5, 5)

        // Background should be pushed towards pure white, text pushed towards black
        val bgLuminance = (Color.red(bgPixel) + Color.green(bgPixel) + Color.blue(bgPixel)) / 3
        val textLuminance = (Color.red(textPixel) + Color.green(textPixel) + Color.blue(textPixel)) / 3

        assertTrue("Background should be brighter than text", bgLuminance > textLuminance)
        assertTrue("Background should be white in B&W", bgLuminance >= 240)
        assertTrue("Text should be very dark in B&W", textLuminance <= 50)
    }

    @Test
    fun testApplyEnhancedFilterToBitmap() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(180, 180, 180))

        val enhancedBitmap = ColorMatrixFilterUtility.applyEnhanced(bitmap)
        assertNotNull(enhancedBitmap)
        assertEquals(10, enhancedBitmap.width)
        assertEquals(10, enhancedBitmap.height)

        val pixel = enhancedBitmap.getPixel(5, 5)
        val avg = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3

        // Enhanced should boost brightness and contrast
        assertTrue("Enhanced should brighten image", avg > 180)
    }

    @Test
    fun testBitmapExtensions() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)

        val bw = bitmap.applyBlackAndWhite()
        val gray = bitmap.applyGrayscale()
        val enhanced = bitmap.applyEnhanced()
        val typed = bitmap.applyFilter(ColorMatrixFilterType.ENHANCED)

        assertNotNull(bw)
        assertNotNull(gray)
        assertNotNull(enhanced)
        assertNotNull(typed)
    }

    @Test
    fun testComposeColorFilterGeneration() {
        val composeFilterBw = ColorMatrixFilterUtility.getComposeColorFilter(ColorMatrixFilterType.BLACK_AND_WHITE)
        val composeFilterGray = ColorMatrixFilterUtility.getComposeColorFilter(ColorMatrixFilterType.GRAYSCALE)
        val composeFilterEnhanced = ColorMatrixFilterUtility.getComposeColorFilter(ColorMatrixFilterType.ENHANCED)

        assertNotNull(composeFilterBw)
        assertNotNull(composeFilterGray)
        assertNotNull(composeFilterEnhanced)
    }
}
