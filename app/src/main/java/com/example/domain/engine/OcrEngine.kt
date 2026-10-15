package com.example.domain.engine

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class TextBlockItem(
    val id: Int,
    val text: String,
    val lineCount: Int
)

data class OcrResult(
    val fullText: String,
    val languageDetected: String,
    val lineCount: Int,
    val wordCount: Int,
    val charCount: Int = 0,
    val blocks: List<String> = emptyList(),
    val blockItems: List<TextBlockItem> = emptyList()
)

class OcrEngine(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeText(bitmap: Bitmap, preferredLanguage: String = "tr"): OcrResult = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text.trim()
                        val blocks = visionText.textBlocks.map { it.text.trim() }.filter { it.isNotBlank() }
                        val blockItems = visionText.textBlocks.mapIndexed { idx, block ->
                            TextBlockItem(
                                id = idx + 1,
                                text = block.text.trim(),
                                lineCount = block.lines.size
                            )
                        }.filter { it.text.isNotBlank() }

                        val lines = visionText.textBlocks.flatMap { it.lines.map { line -> line.text } }
                        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
                        val detectedLang = detectLanguage(text, preferredLanguage)

                        continuation.resume(
                            OcrResult(
                                fullText = text,
                                languageDetected = detectedLang,
                                lineCount = if (lines.isNotEmpty()) lines.size else if (text.isNotBlank()) 1 else 0,
                                wordCount = words.size,
                                charCount = text.length,
                                blocks = blocks,
                                blockItems = blockItems
                            )
                        )
                    }
                    .addOnFailureListener { exception ->
                        val fallbackText = "Metin okunamadı veya görselde algılanabilir metin bulunamadı."
                        continuation.resume(
                            OcrResult(
                                fullText = fallbackText,
                                languageDetected = preferredLanguage,
                                lineCount = 1,
                                wordCount = fallbackText.split(" ").size,
                                charCount = fallbackText.length,
                                blocks = listOf(fallbackText),
                                blockItems = listOf(TextBlockItem(1, fallbackText, 1))
                            )
                        )
                    }
            } catch (e: Exception) {
                continuation.resume(
                    OcrResult(
                        fullText = "",
                        languageDetected = preferredLanguage,
                        lineCount = 0,
                        wordCount = 0,
                        charCount = 0,
                        blocks = emptyList(),
                        blockItems = emptyList()
                    )
                )
            }
        }
    }

    private fun detectLanguage(text: String, fallback: String): String {
        val lower = text.lowercase()
        return when {
            lower.any { it in "çğışöüÇĞİŞÖÜ" } -> "tr"
            lower.any { it in "äöüß" } -> "de"
            lower.any { it in "éèêàùç" } -> "fr"
            lower.any { it in "áéíóúñ¿¡" } -> "es"
            lower.any { it in '\u0600'..'\u06FF' } -> "ar"
            else -> fallback
        }
    }
}
