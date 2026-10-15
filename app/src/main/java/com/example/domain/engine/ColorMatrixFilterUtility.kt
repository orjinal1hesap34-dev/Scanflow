package com.example.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.graphics.asComposeColorFilter
import com.example.domain.model.ImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Supported color matrix filter modes for captured scanner documents and images.
 */
enum class ColorMatrixFilterType(val displayName: String, val technicalName: String) {
    BLACK_AND_WHITE("Siyah & Beyaz", "Black & White"),
    GRAYSCALE("Gri Tonlama", "Grayscale"),
    ENHANCED("Geliştirilmiş", "Enhanced");

    fun toImageFilter(): ImageFilter = when (this) {
        BLACK_AND_WHITE -> ImageFilter.BLACK_WHITE
        GRAYSCALE -> ImageFilter.GRAYSCALE
        ENHANCED -> ImageFilter.ENHANCED
    }

    companion object {
        fun fromImageFilter(filter: ImageFilter): ColorMatrixFilterType? = when (filter) {
            ImageFilter.BLACK_WHITE -> BLACK_AND_WHITE
            ImageFilter.GRAYSCALE -> GRAYSCALE
            ImageFilter.ENHANCED, ImageFilter.DOCUMENT, ImageFilter.MAGIC_COLOR -> ENHANCED
            else -> null
        }
    }
}

/**
 * Comprehensive utility that applies 'Black & White', 'Grayscale', and 'Enhanced'
 * filters to captured scanner and camera images using Android's ColorMatrix filters.
 *
 * Provides:
 * - ColorMatrix generation with fine-tuned luminance, contrast, and brightness parameters
 * - Synchronous and coroutine-friendly asynchronous Bitmap processing
 * - Direct file-to-file processing for camera and scanner output images
 * - Jetpack Compose ColorFilter generation for zero-allocation live preview
 * - Kotlin Bitmap extension functions
 */
object ColorMatrixFilterUtility {

    // Standard ITU-R BT.601 / NTSC luminance weights
    const val LUM_RED = 0.299f
    const val LUM_GREEN = 0.587f
    const val LUM_BLUE = 0.114f

    /**
     * Creates a ColorMatrix for 'Black & White' filter.
     * Converts to monochrome and sharply separates text/ink from paper background.
     *
     * @param contrast Contrast gain multiplier (default 3.5f)
     * @param brightness Brightness/threshold offset (default -280f)
     */
    fun createBlackAndWhiteMatrix(
        contrast: Float = 3.5f,
        brightness: Float = -280f
    ): ColorMatrix {
        val satMatrix = ColorMatrix().apply { setSaturation(0f) }
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(contrastMatrix)
        return satMatrix
    }

    /**
     * Flat float array representation of the 'Black & White' ColorMatrix.
     */
    fun getBlackAndWhiteMatrixArray(
        contrast: Float = 3.5f,
        brightness: Float = -280f
    ): FloatArray {
        val r = LUM_RED * contrast
        val g = LUM_GREEN * contrast
        val b = LUM_BLUE * contrast
        return floatArrayOf(
            r, g, b, 0f, brightness,
            r, g, b, 0f, brightness,
            r, g, b, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /**
     * Creates a ColorMatrix for 'Grayscale' filter.
     * Converts RGB channels to perceptual luminance with neutral saturation.
     */
    fun createGrayscaleMatrix(): ColorMatrix {
        return ColorMatrix().apply { setSaturation(0f) }
    }

    /**
     * Flat float array representation of the 'Grayscale' ColorMatrix.
     */
    fun getGrayscaleMatrixArray(): FloatArray = floatArrayOf(
        LUM_RED, LUM_GREEN, LUM_BLUE, 0f, 0f,
        LUM_RED, LUM_GREEN, LUM_BLUE, 0f, 0f,
        LUM_RED, LUM_GREEN, LUM_BLUE, 0f, 0f,
        0f,      0f,        0f,       1f, 0f
    )

    /**
     * Creates a ColorMatrix for 'Enhanced' filter.
     * Specially designed for captured documents:
     * - Whitens document background and clears shadow gradients
     * - Increases text contrast for crisp readability
     * - Boosts color saturation so colored stamps, seals, and signatures stay vibrant
     *
     * @param contrast Contrast gain (default 1.35f)
     * @param brightness Brightness boost (default 25f)
     * @param saturation Saturation boost (default 1.25f)
     */
    fun createEnhancedMatrix(
        contrast: Float = 1.35f,
        brightness: Float = 25f,
        saturation: Float = 1.25f
    ): ColorMatrix {
        val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(contrastMatrix)
        return satMatrix
    }

    /**
     * Flat float array representation of the 'Enhanced' ColorMatrix.
     */
    fun getEnhancedMatrixArray(
        contrast: Float = 1.35f,
        brightness: Float = 25f,
        saturation: Float = 1.25f
    ): FloatArray {
        val cm = createEnhancedMatrix(contrast, brightness, saturation)
        return cm.array.clone()
    }

    /**
     * Gets the Android ColorMatrix for the specified filter type.
     */
    fun getColorMatrix(filterType: ColorMatrixFilterType): ColorMatrix = when (filterType) {
        ColorMatrixFilterType.BLACK_AND_WHITE -> createBlackAndWhiteMatrix()
        ColorMatrixFilterType.GRAYSCALE -> createGrayscaleMatrix()
        ColorMatrixFilterType.ENHANCED -> createEnhancedMatrix()
    }

    /**
     * Gets the Android ColorMatrixColorFilter for the specified filter type.
     */
    fun getColorMatrixColorFilter(filterType: ColorMatrixFilterType): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(getColorMatrix(filterType))
    }

    /**
     * Gets a Jetpack Compose ColorFilter for the specified filter type.
     * Can be directly passed to Image(), AsyncImage(), or Modifier.drawWithContent().
     */
    fun getComposeColorFilter(filterType: ColorMatrixFilterType): androidx.compose.ui.graphics.ColorFilter {
        return getColorMatrixColorFilter(filterType).asComposeColorFilter()
    }

    /**
     * Applies the specified filter type to a Bitmap using ColorMatrix filters.
     *
     * @param source The source bitmap to filter
     * @param filterType The ColorMatrix filter to apply (Black & White, Grayscale, or Enhanced)
     * @return A newly allocated, filtered Bitmap in ARGB_8888 config
     */
    fun applyFilter(source: Bitmap, filterType: ColorMatrixFilterType): Bitmap {
        val colorMatrix = getColorMatrix(filterType)
        return applyColorMatrix(source, colorMatrix)
    }

    /**
     * Applies an arbitrary ColorMatrix to a Bitmap.
     */
    fun applyColorMatrix(source: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        // Ensure source config is ARGB_8888 or convert if hardware-backed
        val safeSource = if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }

        val output = Bitmap.createBitmap(
            safeSource.width,
            safeSource.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(safeSource, 0f, 0f, paint)

        if (safeSource !== source) {
            safeSource.recycle()
        }

        return output
    }

    /**
     * Convenience function to apply 'Black & White' filter to a Bitmap.
     */
    fun applyBlackAndWhite(
        source: Bitmap,
        contrast: Float = 3.5f,
        brightness: Float = -280f
    ): Bitmap {
        val matrix = createBlackAndWhiteMatrix(contrast, brightness)
        return applyColorMatrix(source, matrix)
    }

    /**
     * Convenience function to apply 'Grayscale' filter to a Bitmap.
     */
    fun applyGrayscale(source: Bitmap): Bitmap {
        val matrix = createGrayscaleMatrix()
        return applyColorMatrix(source, matrix)
    }

    /**
     * Convenience function to apply 'Enhanced' filter to a Bitmap.
     */
    fun applyEnhanced(
        source: Bitmap,
        contrast: Float = 1.35f,
        brightness: Float = 25f,
        saturation: Float = 1.25f
    ): Bitmap {
        val matrix = createEnhancedMatrix(contrast, brightness, saturation)
        return applyColorMatrix(source, matrix)
    }

    /**
     * Asynchronously applies a filter to a Bitmap on the Default dispatcher.
     */
    suspend fun applyFilterAsync(
        source: Bitmap,
        filterType: ColorMatrixFilterType
    ): Bitmap = withContext(Dispatchers.Default) {
        applyFilter(source, filterType)
    }

    /**
     * Processes an image file captured by the camera or scanner, applies the filter,
     * and saves the result to an output file.
     *
     * @param sourceFile The captured image file on disk
     * @param destinationFile The target file where filtered JPEG will be saved
     * @param filterType The filter to apply
     * @param quality JPEG compression quality (1-100)
     * @return true if successful, false otherwise
     */
    suspend fun processCapturedImageFile(
        sourceFile: File,
        destinationFile: File,
        filterType: ColorMatrixFilterType,
        quality: Int = 90
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) return@withContext false
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return@withContext false
            val filteredBitmap = withContext(Dispatchers.Default) {
                applyFilter(bitmap, filterType)
            }
            destinationFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            FileOutputStream(destinationFile).use { out ->
                filteredBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (filteredBitmap !== bitmap) {
                filteredBitmap.recycle()
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Loads a bitmap from a content Uri, applies the selected filter, and returns the filtered Bitmap.
     */
    suspend fun processCapturedUri(
        context: Context,
        uri: Uri,
        filterType: ColorMatrixFilterType
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext null

            withContext(Dispatchers.Default) {
                applyFilter(bitmap, filterType)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// ---------------------------------------------------------------------------
// Bitmap Extension Functions
// ---------------------------------------------------------------------------

/**
 * Extension on [Bitmap] to apply a ColorMatrix filter.
 */
fun Bitmap.applyColorMatrix(colorMatrix: ColorMatrix): Bitmap {
    return ColorMatrixFilterUtility.applyColorMatrix(this, colorMatrix)
}

/**
 * Extension on [Bitmap] to apply Black & White ColorMatrix filter.
 */
fun Bitmap.applyBlackAndWhite(contrast: Float = 3.5f, brightness: Float = -280f): Bitmap {
    return ColorMatrixFilterUtility.applyBlackAndWhite(this, contrast, brightness)
}

/**
 * Extension on [Bitmap] to apply Grayscale ColorMatrix filter.
 */
fun Bitmap.applyGrayscale(): Bitmap {
    return ColorMatrixFilterUtility.applyGrayscale(this)
}

/**
 * Extension on [Bitmap] to apply Enhanced ColorMatrix filter.
 */
fun Bitmap.applyEnhanced(contrast: Float = 1.35f, brightness: Float = 25f, saturation: Float = 1.25f): Bitmap {
    return ColorMatrixFilterUtility.applyEnhanced(this, contrast, brightness, saturation)
}

/**
 * Extension on [Bitmap] to apply any [ColorMatrixFilterType].
 */
fun Bitmap.applyFilter(filterType: ColorMatrixFilterType): Bitmap {
    return ColorMatrixFilterUtility.applyFilter(this, filterType)
}
