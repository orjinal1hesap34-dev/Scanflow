package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.DocumentPageEntity
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.SignatureEntity
import com.example.domain.engine.ImageProcessingEngine
import com.example.domain.engine.OcrEngine
import com.example.domain.engine.PdfEngine
import com.example.domain.model.DocCategory
import com.example.domain.model.ImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepository(
    private val context: Context,
    private val database: AppDatabase,
    val imageProcessingEngine: ImageProcessingEngine,
    val pdfEngine: PdfEngine,
    val ocrEngine: OcrEngine
) {
    private val docDao = database.documentDao()
    private val pageDao = database.documentPageDao()
    private val folderDao = database.folderDao()
    private val signatureDao = database.signatureDao()

    val allActiveDocuments: Flow<List<DocumentEntity>> = docDao.getAllActiveDocuments()
    val favoriteDocuments: Flow<List<DocumentEntity>> = docDao.getFavoriteDocuments()
    val trashedDocuments: Flow<List<DocumentEntity>> = docDao.getTrashedDocuments()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val allSignatures: Flow<List<SignatureEntity>> = signatureDao.getAllSignatures()

    fun getDocumentsByFolder(folderId: Long): Flow<List<DocumentEntity>> = docDao.getDocumentsByFolder(folderId)

    fun getDocumentFlow(id: Long): Flow<DocumentEntity?> = docDao.getDocumentFlowById(id)

    suspend fun getDocument(id: Long): DocumentEntity? = docDao.getDocumentById(id)

    fun getPagesForDocument(documentId: Long): Flow<List<DocumentPageEntity>> = pageDao.getPagesForDocument(documentId)

    suspend fun getPagesList(documentId: Long): List<DocumentPageEntity> = pageDao.getPagesListForDocument(documentId)

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> = docDao.searchDocuments(query)

    suspend fun createDocumentWithPages(
        title: String,
        category: DocCategory,
        pageBitmaps: List<Bitmap>,
        folderId: Long? = null,
        initialFilter: ImageFilter = ImageFilter.DOCUMENT,
        tags: String = ""
    ): Long = withContext(Dispatchers.IO) {
        if (pageBitmaps.isEmpty()) return@withContext 0L

        // Process pages
        val pagePaths = mutableListOf<Pair<String, String>>() // (filteredPath, originalPath)
        var totalSize = 0L

        for (bmp in pageBitmaps) {
            val originalPath = imageProcessingEngine.saveBitmapToFile(bmp, 95)
            val filteredBmp = imageProcessingEngine.applyFilter(bmp, initialFilter)
            val filteredPath = imageProcessingEngine.saveBitmapToFile(filteredBmp, 90)
            pagePaths.add(Pair(filteredPath, originalPath))

            val f = File(filteredPath)
            if (f.exists()) totalSize += f.length()
        }

        val thumbnailPath = pagePaths.firstOrNull()?.first ?: ""

        val docEntity = DocumentEntity(
            title = title.ifBlank { "Belge_${System.currentTimeMillis()}" },
            category = category,
            folderId = folderId,
            tags = tags,
            pageCount = pageBitmaps.size,
            thumbnailPath = thumbnailPath,
            fileSize = totalSize,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val docId = docDao.insertDocument(docEntity)

        val pages = pagePaths.mapIndexed { index, pair ->
            DocumentPageEntity(
                documentId = docId,
                pageIndex = index,
                imagePath = pair.first,
                originalImagePath = pair.second,
                filterType = initialFilter,
                width = pageBitmaps[index].width,
                height = pageBitmaps[index].height
            )
        }
        pageDao.insertPages(pages)

        docId
    }

    suspend fun addPageToDocument(documentId: Long, bitmap: Bitmap, filter: ImageFilter = ImageFilter.DOCUMENT): Long = withContext(Dispatchers.IO) {
        val originalPath = imageProcessingEngine.saveBitmapToFile(bitmap, 95)
        val filteredBmp = imageProcessingEngine.applyFilter(bitmap, filter)
        val filteredPath = imageProcessingEngine.saveBitmapToFile(filteredBmp, 90)

        val currentPages = pageDao.getPagesListForDocument(documentId)
        val newIndex = currentPages.size

        val page = DocumentPageEntity(
            documentId = documentId,
            pageIndex = newIndex,
            imagePath = filteredPath,
            originalImagePath = originalPath,
            filterType = filter,
            width = bitmap.width,
            height = bitmap.height
        )
        val pageId = pageDao.insertPage(page)

        // Update document pageCount and updatedAt
        val doc = docDao.getDocumentById(documentId)
        if (doc != null) {
            val f = File(filteredPath)
            docDao.updateDocument(
                doc.copy(
                    pageCount = newIndex + 1,
                    fileSize = doc.fileSize + (if (f.exists()) f.length() else 0L),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        pageId
    }

    suspend fun updatePageFilter(page: DocumentPageEntity, newFilter: ImageFilter): DocumentPageEntity = withContext(Dispatchers.IO) {
        val baseImage = if (page.originalImagePath.isNotBlank()) {
            imageProcessingEngine.loadBitmapFromFile(page.originalImagePath)
        } else {
            imageProcessingEngine.loadBitmapFromFile(page.imagePath)
        }

        if (baseImage != null) {
            var transformed = imageProcessingEngine.applyFilter(baseImage, newFilter)
            if (page.rotationDegrees != 0) {
                transformed = imageProcessingEngine.rotateBitmap(transformed, page.rotationDegrees.toFloat())
            }
            val newPath = imageProcessingEngine.saveBitmapToFile(transformed, 90)
            val updated = page.copy(
                imagePath = newPath,
                filterType = newFilter
            )
            pageDao.updatePage(updated)
            updated
        } else {
            page
        }
    }

    suspend fun rotatePage(page: DocumentPageEntity, degreesDelta: Int = 90): DocumentPageEntity = withContext(Dispatchers.IO) {
        val currentBitmap = imageProcessingEngine.loadBitmapFromFile(page.imagePath)
        if (currentBitmap != null) {
            val rotated = imageProcessingEngine.rotateBitmap(currentBitmap, degreesDelta.toFloat())
            val newPath = imageProcessingEngine.saveBitmapToFile(rotated, 90)
            val newRotation = (page.rotationDegrees + degreesDelta) % 360
            val updated = page.copy(
                imagePath = newPath,
                rotationDegrees = newRotation,
                width = rotated.width,
                height = rotated.height
            )
            pageDao.updatePage(updated)
            updated
        } else {
            page
        }
    }

    suspend fun deletePage(documentId: Long, pageId: Long) = withContext(Dispatchers.IO) {
        pageDao.deletePage(pageId)
        val remaining = pageDao.getPagesListForDocument(documentId)
        // Re-index pages
        val reindexed = remaining.mapIndexed { idx, p -> p.copy(pageIndex = idx) }
        pageDao.insertPages(reindexed)

        val doc = docDao.getDocumentById(documentId)
        if (doc != null) {
            if (reindexed.isEmpty()) {
                docDao.deletePermanently(documentId)
            } else {
                val newThumb = reindexed.first().imagePath
                docDao.updateDocument(
                    doc.copy(
                        pageCount = reindexed.size,
                        thumbnailPath = newThumb,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        docDao.updateFavorite(id, isFavorite)
    }

    suspend fun renameDocument(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        docDao.renameDocument(id, newTitle)
    }

    suspend fun moveToTrash(id: Long) = withContext(Dispatchers.IO) {
        docDao.moveToTrash(id)
    }

    suspend fun restoreFromTrash(id: Long) = withContext(Dispatchers.IO) {
        docDao.restoreFromTrash(id)
    }

    suspend fun deletePermanently(id: Long) = withContext(Dispatchers.IO) {
        val pages = pageDao.getPagesListForDocument(id)
        pages.forEach { p ->
            try {
                File(p.imagePath).delete()
                if (p.originalImagePath.isNotBlank()) File(p.originalImagePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        pageDao.deletePagesForDocument(id)
        docDao.deletePermanently(id)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        docDao.emptyTrash()
    }

    suspend fun moveDocumentToFolder(documentId: Long, folderId: Long?) = withContext(Dispatchers.IO) {
        docDao.moveDocumentToFolder(documentId, folderId)
    }

    suspend fun updateDocumentLock(documentId: Long, isLocked: Boolean, pin: String?) = withContext(Dispatchers.IO) {
        docDao.updateDocumentLock(documentId, isLocked, pin)
    }

    suspend fun updateDocumentTags(documentId: Long, tags: String) = withContext(Dispatchers.IO) {
        docDao.updateDocumentTags(documentId, tags)
    }

    suspend fun updateDocumentOcr(documentId: Long, ocrText: String) = withContext(Dispatchers.IO) {
        val doc = docDao.getDocumentById(documentId)
        if (doc != null) {
            docDao.updateDocument(doc.copy(ocrFullText = ocrText, updatedAt = System.currentTimeMillis()))
        }
    }

    // Folder actions
    suspend fun createFolder(name: String, colorHex: String = "#2563EB", parentFolderId: Long? = null): Long = withContext(Dispatchers.IO) {
        folderDao.insertFolder(
            FolderEntity(
                name = name,
                colorHex = colorHex,
                parentFolderId = parentFolderId
            )
        )
    }

    suspend fun deleteFolder(folderId: Long) = withContext(Dispatchers.IO) {
        folderDao.deleteFolder(folderId)
    }

    // Signature templates
    suspend fun saveSignatureTemplate(title: String, bitmap: Bitmap, colorHex: String = "#000000"): Long = withContext(Dispatchers.IO) {
        val path = imageProcessingEngine.saveBitmapToFile(bitmap, 95)
        signatureDao.insertSignature(
            SignatureEntity(
                title = title,
                imagePath = path,
                colorHex = colorHex
            )
        )
    }

    suspend fun deleteSignatureTemplate(id: Long) = withContext(Dispatchers.IO) {
        signatureDao.deleteSignature(id)
    }

    // Seed sample documents on first launch so user has instant rich interactive experience
    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val count = docDao.getDocumentById(1)
        if (count == null) {
            // Seed 4 sample documents with popular tags
            createSampleDocument("Kimlik Kartı (Ön & Arka)", DocCategory.ID_CARD, "TC Kimlik Kartı Örneği\nAd: Ahmet Yılmaz\nDoğum: 1990", 2, "Personal, Official")
            createSampleDocument("ScanFlow Hoş Geldiniz Rehberi", DocCategory.DOCUMENTS, "ScanFlow - Scan Anything. Anywhere.\nÖzellikler: HD Tarama, Filtreler, OCR, PDF Araçları, Dijital İmza.", 3, "Work, Guides")
            createSampleDocument("Hizmet Faturası #2026-08", DocCategory.INVOICE, "Fatura No: 2026-89412\nTutar: 1.450,00 TL\nKDV: %20\nDurum: Ödendi", 1, "Receipts, Work")
            createSampleDocument("Pasaport & Vize Belgesi", DocCategory.PASSPORT, "Pasaport Belgesi - No: U12345678\nGeçerlilik: 2030", 1, "Personal, Travel")

            // Seed default folders
            folderDao.insertFolder(FolderEntity(name = "Kişisel Belgeler", colorHex = "#2563EB", iconName = "person"))
            folderDao.insertFolder(FolderEntity(name = "Faturalar & Makbuzlar", colorHex = "#10B981", iconName = "receipt"))
            folderDao.insertFolder(FolderEntity(name = "İş & Sözleşmeler", colorHex = "#6366F1", iconName = "work"))
        }
    }

    private suspend fun createSampleDocument(
        title: String,
        category: DocCategory,
        sampleOcr: String,
        pagesCount: Int,
        tags: String = ""
    ) {
        val bitmaps = mutableListOf<Bitmap>()
        for (i in 1..pagesCount) {
            val w = 600
            val h = 850
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(37, 99, 235)
                textSize = 28f
                isFakeBoldText = true
            }

            // Header bar
            val headerPaint = Paint().apply { color = Color.rgb(240, 246, 255) }
            canvas.drawRect(20f, 20f, (w - 20).toFloat(), 120f, headerPaint)
            canvas.drawText("ScanFlow Smart Scan", 40f, 75f, paint)

            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 20f
            }

            canvas.drawText("Sayfa $i / $pagesCount - $title", 40f, 170f, bodyPaint)

            // Draw clean document simulation lines
            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 3f
            }
            for (y in 220 until 750 step 35) {
                canvas.drawLine(40f, y.toFloat(), (w - 40).toFloat(), y.toFloat(), linePaint)
            }

            // OCR stamp text simulation
            val ocrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 16f
            }
            sampleOcr.lines().forEachIndexed { lIdx, line ->
                canvas.drawText(line, 40f, 250f + (lIdx * 35f), ocrPaint)
            }

            bitmaps.add(bmp)
        }

        createDocumentWithPages(title, category, bitmaps, null, ImageFilter.DOCUMENT, tags)
    }
}
