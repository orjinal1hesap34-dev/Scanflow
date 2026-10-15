package com.example.domain.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

data class MlKitScannedPage(
    val imageUri: Uri
)

data class MlKitScannedPdf(
    val pdfUri: Uri,
    val pageCount: Int
)

data class MlKitScanResult(
    val pages: List<MlKitScannedPage>,
    val pdf: MlKitScannedPdf?
)

/**
 * ML Kit Document Scanner Engine
 * Integrates Google Play Services Document Scanner API with real-time edge detection,
 * automatic perspective warping/correction, shadow removal, and multi-format export.
 */
class MlKitDocumentScannerEngine(private val context: Context) {

    /**
     * Builds document scanner configuration options.
     */
    fun createOptions(
        pageLimit: Int = 50,
        galleryImportAllowed: Boolean = true,
        scannerMode: Int = GmsDocumentScannerOptions.SCANNER_MODE_FULL
    ): GmsDocumentScannerOptions {
        return GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(galleryImportAllowed)
            .setPageLimit(pageLimit)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(scannerMode)
            .build()
    }

    /**
     * Obtains a GmsDocumentScanner instance with the given or default options.
     */
    fun getScannerClient(options: GmsDocumentScannerOptions? = null): GmsDocumentScanner {
        val scannerOptions = options ?: createOptions()
        return GmsDocumentScanning.getClient(scannerOptions)
    }

    /**
     * Launches the ML Kit Document Scanner flow.
     */
    fun startScan(
        activity: Activity,
        options: GmsDocumentScannerOptions? = null,
        onIntentSenderReady: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val scanner = getScannerClient(options)
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    onIntentSenderReady(intentSender)
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Extracts pages and PDF information from the Activity Result intent.
     */
    fun parseResult(intent: Intent?): MlKitScanResult? {
        if (intent == null) return null
        return try {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(intent) ?: return null

            val pages = result.pages?.mapNotNull { page ->
                page.imageUri?.let { uri -> MlKitScannedPage(imageUri = uri) }
            } ?: emptyList()

            val pdf = try {
                result.pdf?.let { pdfData ->
                    pdfData.uri?.let { uri ->
                        MlKitScannedPdf(
                            pdfUri = uri,
                            pageCount = pdfData.pageCount
                        )
                    }
                }
            } catch (e: Throwable) {
                null
            }

            if (pages.isEmpty() && pdf == null) null else MlKitScanResult(pages = pages, pdf = pdf)
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}
