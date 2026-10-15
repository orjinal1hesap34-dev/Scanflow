package com.example.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import com.example.domain.model.ImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

class ImageProcessingEngine(private val context: Context) {

    private val imagesDir: File
        get() = File(context.filesDir, "scanflow_images").apply {
            if (!exists()) mkdirs()
        }

    suspend fun saveBitmapToFile(bitmap: Bitmap, quality: Int = 90): String = withContext(Dispatchers.IO) {
        val fileName = "scan_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val file = File(imagesDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        file.absolutePath
    }

    suspend fun loadBitmapFromFile(path: String, maxDimension: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            var sampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / sampleSize) >= maxDimension && (halfWidth / sampleSize) >= maxDimension) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadBitmapFromUri(uri: android.net.Uri, maxDimension: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)

                var sampleSize = 1
                if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while ((halfHeight / sampleSize) >= maxDimension && (halfWidth / sampleSize) >= maxDimension) {
                        sampleSize *= 2
                    }
                }

                context.contentResolver.openInputStream(uri)?.use { secondStream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    BitmapFactory.decodeStream(secondStream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun applyFilter(srcBitmap: Bitmap, filter: ImageFilter): Bitmap = withContext(Dispatchers.Default) {
        when (filter) {
            ImageFilter.ORIGINAL -> srcBitmap.copy(srcBitmap.config ?: Bitmap.Config.ARGB_8888, true)
            ImageFilter.DOCUMENT -> applyDocumentFilter(srcBitmap)
            ImageFilter.MAGIC_COLOR -> applyMagicColorFilter(srcBitmap)
            ImageFilter.BLACK_WHITE -> ColorMatrixFilterUtility.applyBlackAndWhite(srcBitmap)
            ImageFilter.GRAYSCALE -> ColorMatrixFilterUtility.applyGrayscale(srcBitmap)
            ImageFilter.ENHANCED -> ColorMatrixFilterUtility.applyEnhanced(srcBitmap)
            ImageFilter.HIGH_CONTRAST -> applyHighContrastFilter(srcBitmap)
        }
    }

    private fun applyGrayscaleFilter(src: Bitmap): Bitmap {
        return ColorMatrixFilterUtility.applyGrayscale(src)
    }

    private fun applyDocumentFilter(src: Bitmap): Bitmap {
        // Brighten paper background, increase contrast, mild saturation boost for colored stamps
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix()
        // Contrast = 1.3, Brightness = 25
        val contrast = 1.35f
        val brightness = 20f
        val mat = floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )
        cm.set(mat)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    private fun applyMagicColorFilter(src: Bitmap): Bitmap {
        // Boost saturation, flatten shadows, whiten background
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val satMatrix = ColorMatrix().apply { setSaturation(1.4f) }
        val contrast = 1.45f
        val brightness = 35f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(satMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    fun applyBlackAndWhiteFilter(src: Bitmap): Bitmap {
        return ColorMatrixFilterUtility.applyBlackAndWhite(src)
    }

    fun applyEnhancedFilter(src: Bitmap): Bitmap {
        return ColorMatrixFilterUtility.applyEnhanced(src)
    }

    private fun applyHighContrastFilter(src: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val contrast = 1.8f
        val brightness = -10f
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    suspend fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap = withContext(Dispatchers.Default) {
        if (degrees % 360 == 0f) return@withContext src
        val matrix = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    suspend fun cropBitmap(src: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Bitmap = withContext(Dispatchers.Default) {
        val l = max(0, (left * src.width).toInt())
        val t = max(0, (top * src.height).toInt())
        val r = min(src.width, (right * src.width).toInt())
        val b = min(src.height, (bottom * src.height).toInt())
        val w = max(1, r - l)
        val h = max(1, b - t)
        Bitmap.createBitmap(src, l, t, w, h)
    }

    suspend fun warpPerspective(
        src: Bitmap,
        tl: PointF,
        tr: PointF,
        br: PointF,
        bl: PointF
    ): Bitmap = withContext(Dispatchers.Default) {
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()

        val pTL = PointF(tl.x * srcW, tl.y * srcH)
        val pTR = PointF(tr.x * srcW, tr.y * srcH)
        val pBR = PointF(br.x * srcW, br.y * srcH)
        val pBL = PointF(bl.x * srcW, bl.y * srcH)

        val widthTop = kotlin.math.hypot(pTR.x - pTL.x, pTR.y - pTL.y)
        val widthBottom = kotlin.math.hypot(pBR.x - pBL.x, pBR.y - pBL.y)
        val targetWidth = max(200, max(widthTop, widthBottom).toInt()).coerceAtMost(src.width)

        val heightLeft = kotlin.math.hypot(pBL.x - pTL.x, pBL.y - pTL.y)
        val heightRight = kotlin.math.hypot(pBR.x - pTR.x, pBR.y - pTR.y)
        val targetHeight = max(200, max(heightLeft, heightRight).toInt()).coerceAtMost(src.height)

        val srcPts = floatArrayOf(
            pTL.x, pTL.y,
            pTR.x, pTR.y,
            pBR.x, pBR.y,
            pBL.x, pBL.y
        )

        val dstPts = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(src, matrix, paint)
        output
    }

    suspend fun extractTransparentSignature(
        srcBitmap: Bitmap,
        threshold: Float = 0.80f,
        tintColor: Int? = null,
        autoTrim: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = srcBitmap.width
        val height = srcBitmap.height
        val pixels = IntArray(width * height)
        srcBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0
        var hasInk = false

        val outPixels = IntArray(width * height)

        val tintR = if (tintColor != null) android.graphics.Color.red(tintColor) else 0
        val tintG = if (tintColor != null) android.graphics.Color.green(tintColor) else 0
        val tintB = if (tintColor != null) android.graphics.Color.blue(tintColor) else 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val pixel = pixels[idx]
                val a = (pixel shr 24) and 0xFF

                // If already transparent in source PNG
                if (a < 20) {
                    outPixels[idx] = 0
                    continue
                }

                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Relative luminance formula
                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

                if (lum < threshold) {
                    // Dark pixel = ink
                    val inkAlpha = (((threshold - lum) / threshold) * 255f).toInt().coerceIn(10, 255)
                    val finalAlpha = (inkAlpha * (a / 255f)).toInt().coerceIn(0, 255)

                    if (finalAlpha > 20) {
                        hasInk = true
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }

                    if (tintColor != null) {
                        outPixels[idx] = (finalAlpha shl 24) or (tintR shl 16) or (tintG shl 8) or tintB
                    } else {
                        outPixels[idx] = (finalAlpha shl 24) or (r shl 16) or (g shl 8) or b
                    }
                } else {
                    // Bright pixel = paper background -> transparent
                    outPixels[idx] = 0
                }
            }
        }

        val rawTransparentBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(outPixels, 0, width, 0, 0, width, height)
        }

        if (autoTrim && hasInk && maxX > minX && maxY > minY) {
            val pad = 12
            val cropLeft = max(0, minX - pad)
            val cropTop = max(0, minY - pad)
            val cropRight = min(width, maxX + pad)
            val cropBottom = min(height, maxY + pad)
            val cropW = max(1, cropRight - cropLeft)
            val cropH = max(1, cropBottom - cropTop)
            Bitmap.createBitmap(rawTransparentBmp, cropLeft, cropTop, cropW, cropH)
        } else {
            rawTransparentBmp
        }
    }

    suspend fun stampSignature(
        documentBitmap: Bitmap,
        signatureBitmap: Bitmap,
        normalizedX: Float,
        normalizedY: Float,
        normalizedWidth: Float,
        normalizedHeight: Float,
        rotationDegrees: Float = 0f,
        alpha: Float = 1.0f,
        tintColor: Int? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val output = documentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            if (tintColor != null) {
                colorFilter = android.graphics.PorterDuffColorFilter(
                    tintColor,
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }

        val targetLeft = normalizedX * documentBitmap.width
        val targetTop = normalizedY * documentBitmap.height
        val targetW = (normalizedWidth * documentBitmap.width).coerceAtLeast(20f)
        val targetH = (normalizedHeight * documentBitmap.height).coerceAtLeast(20f)

        val matrix = Matrix()
        val scaleX = targetW / signatureBitmap.width.toFloat()
        val scaleY = targetH / signatureBitmap.height.toFloat()

        matrix.postScale(scaleX, scaleY)
        if (rotationDegrees != 0f) {
            matrix.postRotate(rotationDegrees, targetW / 2f, targetH / 2f)
        }
        matrix.postTranslate(targetLeft, targetTop)

        canvas.drawBitmap(signatureBitmap, matrix, paint)
        output
    }

    suspend fun addWatermark(
        documentBitmap: Bitmap,
        watermarkText: String = "ScanFlow Pro"
    ): Bitmap = withContext(Dispatchers.Default) {
        val output = documentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(70, 37, 99, 235)
            textSize = (documentBitmap.width / 16f).coerceAtLeast(24f)
            isFakeBoldText = true
        }

        canvas.save()
        canvas.rotate(-30f, documentBitmap.width / 2f, documentBitmap.height / 2f)
        val textWidth = paint.measureText(watermarkText)
        val x = (documentBitmap.width - textWidth) / 2f
        val y = documentBitmap.height / 2f
        canvas.drawText(watermarkText, x, y, paint)
        canvas.restore()
        output
    }
}
