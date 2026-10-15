package com.example.domain.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.domain.model.ExportFormat
import com.example.domain.model.ExportQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PdfEngine(private val context: Context) {

    private val exportDir: File
        get() = File(context.cacheDir, "scanflow_exports").apply {
            if (!exists()) mkdirs()
        }

    suspend fun createPdfFromBitmaps(
        bitmaps: List<Bitmap>,
        outputFileName: String = "ScanFlow_${System.currentTimeMillis()}.pdf",
        quality: ExportQuality = ExportQuality.HIGH,
        watermark: String? = null
    ): File = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        bitmaps.forEachIndexed { index, bitmap ->
            // Standard A4 aspect ratio ~ 595 x 842 pt
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            // Fill clean white background
            canvas.drawColor(Color.WHITE)

            // Scale and center bitmap within A4 margins
            val margin = 20f
            val availWidth = 595f - (2 * margin)
            val availHeight = 842f - (2 * margin)

            val scale = minOf(availWidth / bitmap.width.toFloat(), availHeight / bitmap.height.toFloat())
            val targetW = bitmap.width * scale
            val targetH = bitmap.height * scale
            val left = margin + (availWidth - targetW) / 2f
            val top = margin + (availHeight - targetH) / 2f

            val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = android.graphics.RectF(left, top, left + targetW, top + targetH)
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)

            if (!watermark.isNullOrBlank()) {
                val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = Color.argb(45, 100, 116, 139)
                    this.textSize = 20f
                    this.isFakeBoldText = true
                }
                canvas.drawText(watermark, margin, 842f - margin, wmPaint)
            }

            pdfDoc.finishPage(page)
        }

        val file = File(exportDir, if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf")
        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        file
    }

    suspend fun renderPdfToBitmaps(pdfFile: File): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = (page.width * 1.5).toInt()
                val height = (page.height * 1.5).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmaps
    }

    suspend fun mergePdfs(pdfFiles: List<File>, outputFileName: String): File = withContext(Dispatchers.IO) {
        val allBitmaps = mutableListOf<Bitmap>()
        for (pdf in pdfFiles) {
            val pages = renderPdfToBitmaps(pdf)
            allBitmaps.addAll(pages)
        }
        createPdfFromBitmaps(allBitmaps, outputFileName)
    }

    suspend fun splitPdf(pdfFile: File, pageIndices: List<Int>, outputFileName: String): File = withContext(Dispatchers.IO) {
        val allPages = renderPdfToBitmaps(pdfFile)
        val selectedPages = pageIndices.mapNotNull { if (it in allPages.indices) allPages[it] else null }
        createPdfFromBitmaps(selectedPages, outputFileName)
    }

    suspend fun compressPdf(pdfFile: File, quality: ExportQuality, outputFileName: String): File = withContext(Dispatchers.IO) {
        val bitmaps = renderPdfToBitmaps(pdfFile)
        val compressedBitmaps = bitmaps.map { bmp ->
            val scaleFactor = when (quality) {
                ExportQuality.LOW -> 0.6f
                ExportQuality.MEDIUM -> 0.8f
                ExportQuality.HIGH -> 0.95f
                ExportQuality.ORIGINAL -> 1.0f
            }
            if (scaleFactor < 1.0f) {
                val w = (bmp.width * scaleFactor).toInt().coerceAtLeast(100)
                val h = (bmp.height * scaleFactor).toInt().coerceAtLeast(100)
                Bitmap.createScaledBitmap(bmp, w, h, true)
            } else {
                bmp
            }
        }
        createPdfFromBitmaps(compressedBitmaps, outputFileName, quality)
    }

    suspend fun exportZipArchive(
        files: List<File>,
        zipFileName: String = "ScanFlow_Archive_${System.currentTimeMillis()}.zip"
    ): File = withContext(Dispatchers.IO) {
        val zipFile = File(exportDir, if (zipFileName.endsWith(".zip")) zipFileName else "$zipFileName.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for (file in files) {
                if (!file.exists()) continue
                val entry = ZipEntry(file.name)
                zos.putNextEntry(entry)
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
        zipFile
    }

    fun getShareUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Share file via Android Intent System Chooser (Email, Google Drive, WhatsApp, Telegram, Quick Share, etc.)
     */
    fun shareFile(
        file: File,
        mimeType: String = "application/pdf",
        chooserTitle: String = "ScanFlow ile Paylaş",
        subject: String? = null,
        bodyText: String? = null
    ) {
        try {
            val uri = getShareUri(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                if (!subject.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                }
                if (!bodyText.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, bodyText)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Direct Intent to export/send PDF via Email apps (Gmail, Outlook, Mail client)
     */
    fun sharePdfToEmail(
        file: File,
        subject: String = "ScanFlow PDF Belgesi",
        bodyText: String = "ScanFlow uygulaması ile oluşturulmuş PDF belgesi ekte yer almaktadır.",
        recipientEmail: String? = null
    ) {
        try {
            val uri = getShareUri(file)
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                if (!recipientEmail.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, bodyText)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(emailIntent, "E-Posta ile Gönder").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Export/Share to Google Drive & Cloud Storage applications
     */
    fun sharePdfToDriveOrCloud(
        file: File,
        subject: String = "ScanFlow PDF Belgesi"
    ) {
        try {
            val uri = getShareUri(file)
            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TITLE, file.nameWithoutExtension)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(driveIntent, "Google Drive / Bulut'a Kaydet").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Open generated PDF in external PDF reader app (Google Drive PDF Viewer, Adobe Acrobat, etc.)
     */
    fun openPdfWithExternalViewer(file: File) {
        try {
            val uri = getShareUri(file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(viewIntent, "PDF Okuyucu ile Aç").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun printPdf(file: File, jobName: String = "ScanFlow Document") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(jobName)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        FileInputStream(file).use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
