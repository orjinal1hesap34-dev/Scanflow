package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.engine.RecognizedTextBlock
import com.example.domain.engine.RecognizedTextLine
import com.example.domain.engine.ScannerTextRecognitionService
import com.example.domain.engine.ScannerTextResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScannerTextRecognitionServiceTest {

    @Test
    fun testScannerTextResultDataModel() {
        val line = RecognizedTextLine(
            text = "Invoice Number 12345",
            boundingBox = null,
            confidence = 0.95f,
            elements = listOf("Invoice", "Number", "12345")
        )
        val block = RecognizedTextBlock(
            text = "Invoice Number 12345",
            boundingBox = null,
            cornerPoints = null,
            lines = listOf(line)
        )
        val result = ScannerTextResult(
            fullText = "Invoice Number 12345",
            lineCount = 1,
            wordCount = 3,
            characterCount = 20,
            blocks = listOf(block),
            rawTextBlocks = listOf("Invoice Number 12345"),
            detectedLanguage = "en"
        )

        assertEquals("Invoice Number 12345", result.fullText)
        assertEquals(1, result.lineCount)
        assertEquals(3, result.wordCount)
        assertEquals(20, result.characterCount)
        assertEquals("en", result.detectedLanguage)
        assertEquals(1, result.blocks.size)
    }

    @Test
    fun testServiceFactoryCreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = ScannerTextRecognitionService.create(context)
        assertNotNull(service)
        service.close()
    }
}
