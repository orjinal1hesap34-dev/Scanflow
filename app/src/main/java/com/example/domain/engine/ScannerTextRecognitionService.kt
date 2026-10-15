package com.example.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Detailed block data extracted by [ScannerTextRecognitionService].
 */
data class RecognizedTextBlock(
    val text: String,
    val boundingBox: Rect?,
    val cornerPoints: List<Point>?,
    val lines: List<RecognizedTextLine>,
    val recognizedLanguage: String = ""
)

/**
 * Detailed line data extracted by [ScannerTextRecognitionService].
 */
data class RecognizedTextLine(
    val text: String,
    val boundingBox: Rect?,
    val confidence: Float? = null,
    val elements: List<String> = emptyList(),
    val recognizedLanguage: String = ""
)

/**
 * Comprehensive OCR result returned by [ScannerTextRecognitionService].
 */
data class ScannerTextResult(
    val fullText: String,
    val lineCount: Int,
    val wordCount: Int,
    val characterCount: Int,
    val blocks: List<RecognizedTextBlock> = emptyList(),
    val rawTextBlocks: List<String> = emptyList(),
    val detectedLanguage: String = "und",
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
)

/**
 * Utility service using Google ML Kit Text Recognition to process scanner images
 * and extract high-accuracy text strings and structured document hierarchy.
 *
 * Implements [AutoCloseable] to safely release the ML Kit [TextRecognizer] resources.
 */
class ScannerTextRecognitionService(
    private val context: Context? = null,
    customRecognizer: TextRecognizer? = null
) : AutoCloseable {

    private val recognizerLazy = lazy {
        customRecognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val recognizer: TextRecognizer get() = recognizerLazy.value

    /**
     * Processes an image captured as a [Bitmap] and returns the extracted text string.
     *
     * @param bitmap The image bitmap captured by the scanner.
     * @param rotationDegrees Optional rotation angle (0, 90, 180, 270).
     * @return Extracted plain text string trimmed of leading/trailing whitespace.
     */
    suspend fun processImage(bitmap: Bitmap, rotationDegrees: Int = 0): String = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        processInputImage(inputImage).fullText
    }

    /**
     * Processes an image captured and saved to a [Uri] and returns the extracted text string.
     *
     * @param imageUri Content or file URI pointing to the scanned image.
     * @return Extracted plain text string.
     */
    suspend fun processImage(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val appContext = context ?: throw IllegalStateException("Context must be provided to resolve Uri: $imageUri")
        val inputImage = InputImage.fromFilePath(appContext, imageUri)
        processInputImage(inputImage).fullText
    }

    /**
     * Processes an image stored on disk as a [File] and returns the extracted text string.
     *
     * @param imageFile The scanned image file.
     * @return Extracted plain text string.
     */
    suspend fun processImage(imageFile: File): String = withContext(Dispatchers.IO) {
        val appContext = context ?: throw IllegalStateException("Context must be provided to resolve File: ${imageFile.absolutePath}")
        val uri = Uri.fromFile(imageFile)
        val inputImage = InputImage.fromFilePath(appContext, uri)
        processInputImage(inputImage).fullText
    }

    /**
     * Processes a [MlKitScannedPage] obtained from the ML Kit Document Scanner flow.
     *
     * @param page The scanned page entity containing its image URI.
     * @return Extracted plain text string.
     */
    suspend fun processScannedPage(page: MlKitScannedPage): String {
        return processImage(page.imageUri)
    }

    /**
     * Batch processes multiple scanned pages in sequence and returns a list of extracted text strings.
     *
     * @param pages List of scanned pages.
     * @return List of extracted text strings corresponding to each page.
     */
    suspend fun processScannedPages(pages: List<MlKitScannedPage>): List<String> = withContext(Dispatchers.IO) {
        pages.map { page ->
            try {
                processScannedPage(page)
            } catch (e: Exception) {
                ""
            }
        }
    }

    /**
     * Processes a [Bitmap] and returns a detailed [ScannerTextResult] with blocks, lines, and metrics.
     */
    suspend fun processImageDetailed(bitmap: Bitmap, rotationDegrees: Int = 0): ScannerTextResult = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        processInputImage(inputImage)
    }

    /**
     * Processes an image from [Uri] and returns a detailed [ScannerTextResult].
     */
    suspend fun processImageDetailed(imageUri: Uri): ScannerTextResult = withContext(Dispatchers.IO) {
        val appContext = context ?: throw IllegalStateException("Context must be provided to resolve Uri: $imageUri")
        val inputImage = InputImage.fromFilePath(appContext, imageUri)
        processInputImage(inputImage)
    }

    /**
     * Core asynchronous extraction pipeline mapping [InputImage] to [ScannerTextResult].
     */
    private suspend fun processInputImage(inputImage: InputImage): ScannerTextResult =
        suspendCancellableCoroutine { continuation ->
            val task = recognizer.process(inputImage)

            task.addOnSuccessListener { visionText ->
                val result = mapVisionTextToResult(visionText, inputImage.width, inputImage.height)
                continuation.resume(result)
            }.addOnFailureListener { exception ->
                if (continuation.isCancelled) return@addOnFailureListener
                // Return empty text result gracefully or throw depending on exception
                continuation.resume(
                    ScannerTextResult(
                        fullText = "",
                        lineCount = 0,
                        wordCount = 0,
                        characterCount = 0,
                        blocks = emptyList(),
                        rawTextBlocks = emptyList(),
                        detectedLanguage = "und",
                        imageWidth = inputImage.width,
                        imageHeight = inputImage.height
                    )
                )
            }.addOnCanceledListener {
                continuation.resumeWithException(CancellationException("Text recognition task was cancelled"))
            }

            continuation.invokeOnCancellation {
                // Cannot directly cancel Play Services task, but coroutine cancellation is honored
            }
        }

    /**
     * Maps ML Kit [Text] hierarchy into structured domain models.
     */
    private fun mapVisionTextToResult(visionText: Text, imgWidth: Int = 0, imgHeight: Int = 0): ScannerTextResult {
        val fullText = visionText.text.trim()
        val textBlocks = visionText.textBlocks

        val blocks = textBlocks.map { block ->
            val lines = block.lines.map { line ->
                RecognizedTextLine(
                    text = line.text.trim(),
                    boundingBox = line.boundingBox,
                    confidence = line.confidence,
                    elements = line.elements.map { it.text },
                    recognizedLanguage = line.recognizedLanguage
                )
            }

            RecognizedTextBlock(
                text = block.text.trim(),
                boundingBox = block.boundingBox,
                cornerPoints = block.cornerPoints?.toList(),
                lines = lines,
                recognizedLanguage = block.recognizedLanguage
            )
        }

        val rawBlocks = textBlocks.map { it.text.trim() }.filter { it.isNotBlank() }
        val lines = textBlocks.flatMap { it.lines }
        val words = fullText.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // Heuristic language detection fallback
        val detectedLanguage = detectLanguage(fullText)

        return ScannerTextResult(
            fullText = fullText,
            lineCount = if (lines.isNotEmpty()) lines.size else if (fullText.isNotBlank()) 1 else 0,
            wordCount = words.size,
            characterCount = fullText.length,
            blocks = blocks,
            rawTextBlocks = rawBlocks,
            detectedLanguage = detectedLanguage,
            imageWidth = imgWidth,
            imageHeight = imgHeight
        )
    }

    private fun detectLanguage(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.any { it in "çğışöüÇĞİŞÖÜ" } -> "tr"
            lower.any { it in "äöüß" } -> "de"
            lower.any { it in "éèêàùç" } -> "fr"
            lower.any { it in "áéíóúñ¿¡" } -> "es"
            lower.any { it in '\u0600'..'\u06FF' } -> "ar"
            text.isNotBlank() -> "en"
            else -> "und"
        }
    }

    override fun close() {
        try {
            if (recognizerLazy.isInitialized()) {
                recognizerLazy.value.close()
            }
        } catch (_: Exception) {}
    }

    companion object {
        /**
         * Factory function to create a new [ScannerTextRecognitionService] instance.
         */
        fun create(context: Context): ScannerTextRecognitionService {
            return ScannerTextRecognitionService(context = context.applicationContext)
        }

        /**
         * Standalone one-shot utility function to process a [Bitmap] and extract text.
         */
        suspend fun extractTextFromBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): String {
            val service = ScannerTextRecognitionService()
            return try {
                service.processImage(bitmap, rotationDegrees)
            } finally {
                service.close()
            }
        }

        /**
         * Standalone one-shot utility function to process an image [Uri] and extract text.
         */
        suspend fun extractTextFromUri(context: Context, imageUri: Uri): String {
            val service = ScannerTextRecognitionService(context.applicationContext)
            return try {
                service.processImage(imageUri)
            } finally {
                service.close()
            }
        }
    }
}
