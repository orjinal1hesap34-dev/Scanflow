package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.PointF
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.DocumentPageEntity
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.SignatureEntity
import com.example.data.repository.DocumentRepository
import com.example.domain.engine.AuthUserState
import com.example.domain.engine.BiometricAuthManager
import com.example.domain.engine.BiometricStatus
import com.example.domain.engine.CloudAccountState
import com.example.domain.engine.CloudBackupEngine
import com.example.domain.engine.ColorMatrixFilterType
import com.example.domain.engine.ColorMatrixFilterUtility
import com.example.domain.engine.FirebaseAuthManager
import com.example.domain.engine.ImageProcessingEngine
import com.example.domain.engine.MlKitDocumentScannerEngine
import com.example.domain.engine.MlKitScanResult
import com.example.domain.engine.OcrEngine
import com.example.domain.engine.OcrResult
import com.example.domain.engine.PdfEngine
import com.example.domain.engine.ScannerTextRecognitionService
import com.example.domain.engine.ScannerTextResult
import com.example.domain.engine.SecurityEngine
import com.example.domain.model.DetectedQuad
import com.example.domain.model.DocCategory
import com.example.domain.model.ExportFormat
import com.example.domain.model.ExportQuality
import com.example.domain.model.ImageFilter
import com.example.domain.model.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ScanFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getInstance(context)
    val imageEngine = ImageProcessingEngine(context)
    val pdfEngine = PdfEngine(context)
    val ocrEngine = OcrEngine(context)
    val securityEngine = SecurityEngine(context)
    val cloudEngine = CloudBackupEngine(context)

    val repository = DocumentRepository(context, database, imageEngine, pdfEngine, ocrEngine)
    val authManager = FirebaseAuthManager(context)
    val biometricManager = BiometricAuthManager(context)
    val mlKitScannerEngine = MlKitDocumentScannerEngine(context)

    val authState: StateFlow<AuthUserState> = authManager.userState
    val isAuthLoading: StateFlow<Boolean> = authManager.isLoading
    val authErrorMessage: StateFlow<String?> = authManager.errorMessage

    // Sensitive documents unlocked during current active session
    private val _unlockedDocumentIds = MutableStateFlow<Set<Long>>(emptySet())
    val unlockedDocumentIds: StateFlow<Set<Long>> = _unlockedDocumentIds.asStateFlow()

    private val _showOnlyLocked = MutableStateFlow(false)
    val showOnlyLocked = _showOnlyLocked.asStateFlow()

    // UI Search & Filter States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(DocCategory.ALL)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    val sortOption = _sortOption.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView = _isGridView.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId = _selectedFolderId.asStateFlow()

    // Premium status (Unlock all Pro features)
    private val _isPremium = MutableStateFlow(true)
    val isPremium = _isPremium.asStateFlow()

    // Dark Mode preference (Ultra Black by default)
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()

    // Selected Language
    private val _selectedLanguage = MutableStateFlow("tr") // "tr", "en", "de", "fr", "es", "ar"
    val selectedLanguage = _selectedLanguage.asStateFlow()

    // Security Pin App Lock
    private val _isAppLocked = MutableStateFlow(securityEngine.isPinSet() && securityEngine.shouldAutoLock())
    val isAppLocked = _isAppLocked.asStateFlow()

    // Active Document in Editor
    private val _currentDocument = MutableStateFlow<DocumentEntity?>(null)
    val currentDocument = _currentDocument.asStateFlow()

    private val _currentPages = MutableStateFlow<List<DocumentPageEntity>>(emptyList())
    val currentPages = _currentPages.asStateFlow()

    private val _selectedPageIndex = MutableStateFlow(0)
    val selectedPageIndex = _selectedPageIndex.asStateFlow()

    // Camera batch scan buffer
    private val _batchScannedBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val batchScannedBitmaps = _batchScannedBitmaps.asStateFlow()

    // OCR State
    private val _ocrResult = MutableStateFlow<OcrResult?>(null)
    val ocrResult = _ocrResult.asStateFlow()

    val scannerTextService = ScannerTextRecognitionService(context)
    private val _detailedOcrResult = MutableStateFlow<ScannerTextResult?>(null)
    val detailedOcrResult: StateFlow<ScannerTextResult?> = _detailedOcrResult.asStateFlow()

    private val _isOcrLoading = MutableStateFlow(false)
    val isOcrLoading = _isOcrLoading.asStateFlow()

    private val _editableOcrText = MutableStateFlow("")
    val editableOcrText = _editableOcrText.asStateFlow()

    private val _isTextOverlayActive = MutableStateFlow(false)
    val isTextOverlayActive: StateFlow<Boolean> = _isTextOverlayActive.asStateFlow()

    // Cloud backup state
    private val _cloudAccounts = MutableStateFlow(cloudEngine.getAccountsState())
    val cloudAccounts = _cloudAccounts.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp = _isBackingUp.asStateFlow()

    // Filtered Documents Stream
    val allActiveDocuments: StateFlow<List<DocumentEntity>> = combine(
        repository.allActiveDocuments,
        _searchQuery,
        _selectedCategory,
        _selectedFolderId,
        _sortOption,
        _showOnlyLocked,
        _selectedTag
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val docs = args[0] as List<DocumentEntity>
        val query = args[1] as String
        val cat = args[2] as DocCategory
        val folderId = args[3] as Long?
        val sort = args[4] as SortOption
        val onlyLocked = args[5] as Boolean
        val tag = args[6] as String?

        var list = docs

        // Show only locked/sensitive filter
        if (onlyLocked) {
            list = list.filter { it.isLocked }
        }

        // Folder filter
        if (folderId != null) {
            list = list.filter { it.folderId == folderId }
        }

        // Category filter
        if (cat != DocCategory.ALL) {
            list = list.filter { it.category == cat }
        }

        // Tag filter (e.g. Work, Personal, Receipts, etc.)
        if (!tag.isNullOrBlank()) {
            val tLower = tag.trim().lowercase()
            list = list.filter { doc ->
                doc.tags.split(",")
                    .map { it.trim().lowercase() }
                    .any { it == tLower || it.contains(tLower) }
            }
        }

        // Query filter
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.tags.lowercase().contains(q) ||
                it.ocrFullText.lowercase().contains(q)
            }
        }

        // Sorting
        when (sort) {
            SortOption.DATE_DESC -> list.sortedByDescending { it.updatedAt }
            SortOption.DATE_ASC -> list.sortedBy { it.updatedAt }
            SortOption.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.fileSize }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available unique tags dynamically extracted from documents + popular presets
    val allAvailableTags: StateFlow<List<String>> = repository.allActiveDocuments.map { docs ->
        val presetTags = listOf("Work", "Personal", "Receipts", "Important", "Official", "Tax", "Contract")
        val docTags = docs.flatMap { doc ->
            doc.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
        (presetTags + docTags).distinctBy { it.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Work", "Personal", "Receipts", "Important", "Official"))

    val favoriteDocuments = repository.favoriteDocuments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trashedDocuments = repository.trashedDocuments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allFolders = repository.allFolders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSignatures = repository.allSignatures.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    // Search and Filter updates
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategory(category: DocCategory) { _selectedCategory.value = category }
    fun setSelectedTag(tag: String?) { _selectedTag.value = tag }
    fun setSortOption(option: SortOption) { _sortOption.value = option }
    fun toggleGridView() { _isGridView.value = !_isGridView.value }
    fun setFolderFilter(folderId: Long?) { _selectedFolderId.value = folderId }
    fun toggleDarkMode() { _isDarkMode.value = !_isDarkMode.value }
    fun setDarkMode(isDark: Boolean) { _isDarkMode.value = isDark }
    fun setLanguage(lang: String) { _selectedLanguage.value = lang }
    fun setPremium(isPro: Boolean) { _isPremium.value = isPro }

    fun toggleShowOnlyLocked() { _showOnlyLocked.value = !_showOnlyLocked.value }
    fun setShowOnlyLocked(onlyLocked: Boolean) { _showOnlyLocked.value = onlyLocked }

    // Sensitive Document Protection & Biometrics
    fun isDocumentUnlocked(docId: Long): Boolean {
        return _unlockedDocumentIds.value.contains(docId)
    }

    fun unlockDocumentSession(docId: Long) {
        _unlockedDocumentIds.value = _unlockedDocumentIds.value + docId
    }

    fun lockDocumentSession(docId: Long) {
        _unlockedDocumentIds.value = _unlockedDocumentIds.value - docId
    }

    fun lockAllDocumentSessions() {
        _unlockedDocumentIds.value = emptySet()
    }

    fun verifyAndUnlockDocument(doc: DocumentEntity, enteredPin: String): Boolean {
        val valid = securityEngine.verifyDocumentPin(enteredPin, doc.lockPin)
        if (valid) {
            unlockDocumentSession(doc.id)
            securityEngine.updateLastActive()
        }
        return valid
    }

    fun authenticateWithBiometrics(
        activity: androidx.fragment.app.FragmentActivity,
        title: String = "ScanFlow Biyometrik Doğrulama",
        subtitle: String = "Parmak izi veya Yüz tanıma",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        biometricManager.promptBiometric(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = {
                _isAppLocked.value = false
                securityEngine.updateLastActive()
                onSuccess()
            },
            onError = onError
        )
    }

    fun authenticateDocumentWithBiometrics(
        activity: androidx.fragment.app.FragmentActivity,
        doc: DocumentEntity,
        onSuccess: () -> Unit,
        onPinFallback: () -> Unit = {},
        onError: (String) -> Unit
    ) {
        biometricManager.promptBiometricForDocument(
            activity = activity,
            documentTitle = doc.title,
            onSuccess = {
                unlockDocumentSession(doc.id)
                securityEngine.updateLastActive()
                onSuccess()
            },
            onPinFallback = onPinFallback,
            onError = onError
        )
    }

    // Firebase Auth Operations
    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authManager.signInWithEmail(email, pass)
            res.fold(
                onSuccess = { user ->
                    Toast.makeText(context, "Hoş geldiniz, ${user.displayName ?: user.email}", Toast.LENGTH_SHORT).show()
                    onResult(true, null)
                },
                onFailure = { error ->
                    onResult(false, error.localizedMessage)
                }
            )
        }
    }

    fun signUpWithEmail(email: String, pass: String, displayName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authManager.signUpWithEmail(email, pass, displayName)
            res.fold(
                onSuccess = { user ->
                    Toast.makeText(context, "Hesap başarıyla oluşturuldu", Toast.LENGTH_SHORT).show()
                    onResult(true, null)
                },
                onFailure = { error ->
                    onResult(false, error.localizedMessage)
                }
            )
        }
    }

    fun signInWithGoogle(activityContext: android.content.Context, webClientId: String = "", onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authManager.signInWithGoogle(activityContext, webClientId)
            res.fold(
                onSuccess = { user ->
                    Toast.makeText(context, "Google ile giriş yapıldı: ${user.displayName ?: user.email}", Toast.LENGTH_SHORT).show()
                    onResult(true, null)
                },
                onFailure = { error ->
                    onResult(false, error.localizedMessage)
                }
            )
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authManager.sendPasswordReset(email)
            res.fold(
                onSuccess = {
                    Toast.makeText(context, "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi", Toast.LENGTH_LONG).show()
                    onResult(true, null)
                },
                onFailure = { error ->
                    onResult(false, error.localizedMessage)
                }
            )
        }
    }

    fun signOut() {
        authManager.signOut()
        Toast.makeText(context, "Oturum kapatıldı", Toast.LENGTH_SHORT).show()
    }

    fun clearAuthError() {
        authManager.clearError()
    }

    // Security PIN Lock
    fun verifyPin(pin: String): Boolean {
        val success = securityEngine.verifyAppPin(pin)
        if (success) {
            _isAppLocked.value = false
            securityEngine.updateLastActive()
        }
        return success
    }

    fun lockApp() {
        if (securityEngine.isPinSet()) {
            _isAppLocked.value = true
        }
    }

    fun setAppPin(pin: String) {
        securityEngine.setAppPin(pin)
        _isAppLocked.value = false
    }

    fun removeAppPin() {
        securityEngine.removeAppPin()
        _isAppLocked.value = false
    }

    // Camera batch operations
    fun addCapturedBitmap(bitmap: Bitmap) {
        _batchScannedBitmaps.value = _batchScannedBitmaps.value + bitmap
    }

    fun addCapturedBitmapWithQuad(bitmap: Bitmap, quad: DetectedQuad?) {
        viewModelScope.launch {
            val processedBitmap = if (quad != null && quad.isDetected && quad.confidence > 0.60f) {
                try {
                    imageEngine.warpPerspective(
                        src = bitmap,
                        tl = quad.topLeft,
                        tr = quad.topRight,
                        br = quad.bottomRight,
                        bl = quad.bottomLeft
                    )
                } catch (e: Exception) {
                    bitmap
                }
            } else {
                bitmap
            }
            _batchScannedBitmaps.value = _batchScannedBitmaps.value + processedBitmap
        }
    }

    fun removeCapturedBitmap(index: Int) {
        val current = _batchScannedBitmaps.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _batchScannedBitmaps.value = current
        }
    }

    fun clearBatchBitmaps() {
        _batchScannedBitmaps.value = emptyList()
    }

    fun finishBatchScan(
        title: String = "Yeni Belge",
        category: DocCategory = DocCategory.DOCUMENTS,
        filter: ImageFilter = ImageFilter.DOCUMENT,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val bitmaps = _batchScannedBitmaps.value
            if (bitmaps.isNotEmpty()) {
                val docId = repository.createDocumentWithPages(title, category, bitmaps, _selectedFolderId.value, filter)
                clearBatchBitmaps()
                loadDocument(docId)
                onComplete(docId)
            }
        }
    }

    /**
     * Imports ML Kit Document Scanner result (automatically perspective-corrected & cropped)
     */
    fun importMlKitScanResult(
        result: MlKitScanResult,
        customTitle: String? = null,
        category: DocCategory = DocCategory.DOCUMENTS,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val bitmaps = mutableListOf<Bitmap>()
                for (page in result.pages) {
                    val bitmap = imageEngine.loadBitmapFromUri(page.imageUri)
                    if (bitmap != null) {
                        bitmaps.add(bitmap)
                    }
                }

                if (bitmaps.isNotEmpty()) {
                    val timeStr = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    val title = customTitle ?: "ML Kit Belge $timeStr"
                    val docId = repository.createDocumentWithPages(
                        title = title,
                        category = category,
                        pageBitmaps = bitmaps,
                        folderId = _selectedFolderId.value,
                        initialFilter = ImageFilter.ORIGINAL
                    )
                    loadDocument(docId)
                    Toast.makeText(context, "${bitmaps.size} sayfa ML Kit ile tarandı ve işlendi", Toast.LENGTH_SHORT).show()
                    onComplete(docId)
                } else {
                    Toast.makeText(context, "Taranan belgeler yüklenemedi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Tarama hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Document Management
    fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            val doc = repository.getDocument(documentId)
            _currentDocument.value = doc
            val pages = repository.getPagesList(documentId)
            _currentPages.value = pages
            _selectedPageIndex.value = 0
            if (doc != null) {
                _editableOcrText.value = doc.ocrFullText
            }
        }
    }

    fun setSelectedPageIndex(index: Int) {
        if (index in _currentPages.value.indices) {
            _selectedPageIndex.value = index
        }
    }

    fun applyFilterToCurrentPage(filter: ImageFilter) {
        viewModelScope.launch {
            val pages = _currentPages.value
            val index = _selectedPageIndex.value
            if (index in pages.indices) {
                val updatedPage = repository.updatePageFilter(pages[index], filter)
                val newPages = pages.toMutableList().apply { set(index, updatedPage) }
                _currentPages.value = newPages
            }
        }
    }

    fun applyColorMatrixFilterToCurrentPage(filterType: ColorMatrixFilterType) {
        applyFilterToCurrentPage(filterType.toImageFilter())
    }

    fun rotateCurrentPage(degrees: Int = 90) {
        viewModelScope.launch {
            val pages = _currentPages.value
            val index = _selectedPageIndex.value
            if (index in pages.indices) {
                val updatedPage = repository.rotatePage(pages[index], degrees)
                val newPages = pages.toMutableList().apply { set(index, updatedPage) }
                _currentPages.value = newPages
            }
        }
    }

    fun deleteCurrentPage() {
        viewModelScope.launch {
            val doc = _currentDocument.value ?: return@launch
            val pages = _currentPages.value
            val index = _selectedPageIndex.value
            if (index in pages.indices) {
                val pageToDelete = pages[index]
                repository.deletePage(doc.id, pageToDelete.id)
                loadDocument(doc.id)
            }
        }
    }

    fun addPageFromBitmap(bitmap: Bitmap, filter: ImageFilter = ImageFilter.DOCUMENT) {
        viewModelScope.launch {
            val doc = _currentDocument.value ?: return@launch
            repository.addPageToDocument(doc.id, bitmap, filter)
            loadDocument(doc.id)
        }
    }

    fun duplicateCurrentPage() {
        viewModelScope.launch {
            val doc = _currentDocument.value ?: return@launch
            val pages = _currentPages.value
            val index = _selectedPageIndex.value
            if (index in pages.indices) {
                val page = pages[index]
                val bitmap = imageEngine.loadBitmapFromFile(page.imagePath)
                if (bitmap != null) {
                    repository.addPageToDocument(doc.id, bitmap, page.filterType)
                    loadDocument(doc.id)
                }
            }
        }
    }

    fun toggleFavorite(document: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(document.id, !document.isFavorite)
        }
    }

    fun renameDocument(documentId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameDocument(documentId, newTitle)
            if (_currentDocument.value?.id == documentId) {
                _currentDocument.value = _currentDocument.value?.copy(title = newTitle)
            }
        }
    }

    fun moveToTrash(documentId: Long) {
        viewModelScope.launch {
            repository.moveToTrash(documentId)
        }
    }

    fun restoreFromTrash(documentId: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(documentId)
        }
    }

    fun deletePermanently(documentId: Long) {
        viewModelScope.launch {
            repository.deletePermanently(documentId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun moveDocumentToFolder(documentId: Long, folderId: Long?) {
        viewModelScope.launch {
            repository.moveDocumentToFolder(documentId, folderId)
        }
    }

    fun toggleDocumentLock(documentId: Long, pin: String?) {
        viewModelScope.launch {
            val doc = repository.getDocument(documentId) ?: return@launch
            val newLocked = !doc.isLocked
            repository.updateDocumentLock(documentId, newLocked, if (newLocked) pin else null)
            if (_currentDocument.value?.id == documentId) {
                _currentDocument.value = _currentDocument.value?.copy(isLocked = newLocked, lockPin = pin)
            }
        }
    }

    // Tag Operations
    fun updateDocumentTags(documentId: Long, tags: String) {
        viewModelScope.launch {
            repository.updateDocumentTags(documentId, tags)
            if (_currentDocument.value?.id == documentId) {
                _currentDocument.value = _currentDocument.value?.copy(tags = tags)
            }
        }
    }

    fun updateDocumentTagsList(documentId: Long, tagsList: List<String>) {
        val cleanTags = tagsList.map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ")
        updateDocumentTags(documentId, cleanTags)
    }

    fun addTagToDocument(documentId: Long, newTag: String) {
        if (newTag.isBlank()) return
        viewModelScope.launch {
            val doc = repository.getDocument(documentId) ?: return@launch
            val existing = doc.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toMutableList()
            val trimmed = newTag.trim()
            if (!existing.any { it.equals(trimmed, ignoreCase = true) }) {
                existing.add(trimmed)
                val formatted = existing.joinToString(", ")
                repository.updateDocumentTags(documentId, formatted)
                if (_currentDocument.value?.id == documentId) {
                    _currentDocument.value = _currentDocument.value?.copy(tags = formatted)
                }
            }
        }
    }

    fun removeTagFromDocument(documentId: Long, tagToRemove: String) {
        viewModelScope.launch {
            val doc = repository.getDocument(documentId) ?: return@launch
            val existing = doc.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals(tagToRemove.trim(), ignoreCase = true) }
            val formatted = existing.joinToString(", ")
            repository.updateDocumentTags(documentId, formatted)
            if (_currentDocument.value?.id == documentId) {
                _currentDocument.value = _currentDocument.value?.copy(tags = formatted)
            }
        }
    }

    // OCR Actions
    fun performOcrOnCurrentPage(language: String = _selectedLanguage.value) {
        viewModelScope.launch {
            val pages = _currentPages.value
            val index = _selectedPageIndex.value
            if (index in pages.indices) {
                _isOcrLoading.value = true
                val page = pages[index]
                val bitmap = imageEngine.loadBitmapFromFile(page.imagePath)
                if (bitmap != null) {
                    try {
                        val detailedResult = scannerTextService.processImageDetailed(bitmap)
                        _detailedOcrResult.value = detailedResult
                    } catch (e: Exception) {
                        // Keep fallback
                    }

                    val result = ocrEngine.recognizeText(bitmap, language)
                    _ocrResult.value = result
                    _editableOcrText.value = result.fullText

                    // Update document OCR full text
                    val doc = _currentDocument.value
                    if (doc != null) {
                        val combinedText = if (doc.ocrFullText.isBlank()) result.fullText else "${doc.ocrFullText}\n\n${result.fullText}"
                        repository.updateDocumentOcr(doc.id, combinedText)
                    }
                }
                _isOcrLoading.value = false
            }
        }
    }

    fun toggleTextOverlay() {
        val next = !_isTextOverlayActive.value
        _isTextOverlayActive.value = next
        if (next && _detailedOcrResult.value == null) {
            performOcrOnCurrentPage()
        }
    }

    fun setTextOverlayActive(active: Boolean) {
        _isTextOverlayActive.value = active
        if (active && _detailedOcrResult.value == null) {
            performOcrOnCurrentPage()
        }
    }

    fun setEditableOcrText(text: String) {
        _editableOcrText.value = text
    }

    fun saveEditedOcrText() {
        viewModelScope.launch {
            val doc = _currentDocument.value ?: return@launch
            repository.updateDocumentOcr(doc.id, _editableOcrText.value)
            Toast.makeText(context, "OCR Metni Kaydedildi", Toast.LENGTH_SHORT).show()
        }
    }

    // Export and PDF generation
    fun exportDocument(
        document: DocumentEntity,
        format: ExportFormat,
        quality: ExportQuality = ExportQuality.HIGH,
        watermark: String? = null,
        onComplete: (File) -> Unit
    ) {
        viewModelScope.launch {
            val pages = repository.getPagesList(document.id)
            val bitmaps = pages.mapNotNull { imageEngine.loadBitmapFromFile(it.imagePath) }

            if (bitmaps.isEmpty()) return@launch

            when (format) {
                ExportFormat.PDF -> {
                    val pdfFile = pdfEngine.createPdfFromBitmaps(
                        bitmaps,
                        "${document.title}.pdf",
                        quality,
                        if (_isPremium.value) null else (watermark ?: "ScanFlow")
                    )
                    onComplete(pdfFile)
                }
                ExportFormat.JPG, ExportFormat.PNG -> {
                    val firstBmp = bitmaps.first()
                    val path = imageEngine.saveBitmapToFile(firstBmp, quality.compressionQuality)
                    onComplete(File(path))
                }
                ExportFormat.ZIP -> {
                    val files = pages.map { File(it.imagePath) }
                    val zip = pdfEngine.exportZipArchive(files, "${document.title}_bundle.zip")
                    onComplete(zip)
                }
                ExportFormat.TXT -> {
                    val txtFile = File(context.cacheDir, "${document.title}.txt").apply {
                        writeText(document.ocrFullText.ifBlank { "Belge Metni: ${document.title}" })
                    }
                    onComplete(txtFile)
                }
            }
        }
    }

    /**
     * Share PDF to other installed apps via Android Intent Chooser
     */
    fun shareDocumentAsPdf(
        document: DocumentEntity,
        quality: ExportQuality = ExportQuality.HIGH,
        watermark: String? = null
    ) {
        exportDocument(document, ExportFormat.PDF, quality, watermark) { file ->
            pdfEngine.shareFile(
                file = file,
                mimeType = "application/pdf",
                chooserTitle = "${document.title} - Paylaş",
                subject = document.title,
                bodyText = "${document.title} adlı taranmış PDF belgesi ekte yer almaktadır."
            )
        }
    }

    /**
     * Export PDF directly to Email app with pre-filled subject and body
     */
    fun shareDocumentViaEmail(
        document: DocumentEntity,
        recipientEmail: String? = null,
        quality: ExportQuality = ExportQuality.HIGH,
        watermark: String? = null
    ) {
        exportDocument(document, ExportFormat.PDF, quality, watermark) { file ->
            pdfEngine.sharePdfToEmail(
                file = file,
                subject = "PDF: ${document.title}",
                bodyText = "ScanFlow ile taranmış ${document.title} belgesi ekte sunulmuştur.",
                recipientEmail = recipientEmail
            )
        }
    }

    /**
     * Export PDF to Google Drive or Cloud Storage
     */
    fun shareDocumentToCloud(
        document: DocumentEntity,
        quality: ExportQuality = ExportQuality.HIGH,
        watermark: String? = null
    ) {
        exportDocument(document, ExportFormat.PDF, quality, watermark) { file ->
            pdfEngine.sharePdfToDriveOrCloud(
                file = file,
                subject = document.title
            )
        }
    }

    /**
     * Open PDF in External Viewer (e.g. Adobe Reader, Google Drive PDF Viewer)
     */
    fun openDocumentInExternalViewer(
        document: DocumentEntity,
        quality: ExportQuality = ExportQuality.HIGH,
        watermark: String? = null
    ) {
        exportDocument(document, ExportFormat.PDF, quality, watermark) { file ->
            pdfEngine.openPdfWithExternalViewer(file)
        }
    }

    /**
     * Print PDF using Android PrintManager
     */
    fun printDocumentPdf(
        document: DocumentEntity,
        quality: ExportQuality = ExportQuality.HIGH,
        watermark: String? = null
    ) {
        exportDocument(document, ExportFormat.PDF, quality, watermark) { file ->
            pdfEngine.printPdf(file, document.title)
        }
    }

    // PDF Tools
    fun mergeDocuments(docIds: List<Long>, outputTitle: String, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val allBitmaps = mutableListOf<Bitmap>()
            for (id in docIds) {
                val pages = repository.getPagesList(id)
                pages.forEach { p ->
                    imageEngine.loadBitmapFromFile(p.imagePath)?.let { allBitmaps.add(it) }
                }
            }
            if (allBitmaps.isNotEmpty()) {
                val newDocId = repository.createDocumentWithPages(
                    outputTitle,
                    DocCategory.DOCUMENTS,
                    allBitmaps,
                    _selectedFolderId.value
                )
                onComplete(newDocId)
            }
        }
    }

    fun splitDocument(documentId: Long, pageIndices: List<Int>, outputTitle: String, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val pages = repository.getPagesList(documentId)
            val selectedBitmaps = pageIndices.mapNotNull { idx ->
                if (idx in pages.indices) imageEngine.loadBitmapFromFile(pages[idx].imagePath) else null
            }
            if (selectedBitmaps.isNotEmpty()) {
                val newDocId = repository.createDocumentWithPages(
                    outputTitle,
                    DocCategory.DOCUMENTS,
                    selectedBitmaps,
                    _selectedFolderId.value
                )
                onComplete(newDocId)
            }
        }
    }

    // Folders
    fun createFolder(name: String, colorHex: String = "#2563EB") {
        viewModelScope.launch {
            repository.createFolder(name, colorHex)
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (_selectedFolderId.value == folderId) {
                _selectedFolderId.value = null
            }
        }
    }

    // Signatures
    fun saveSignature(title: String, bitmap: Bitmap, colorHex: String) {
        viewModelScope.launch {
            repository.saveSignatureTemplate(title, bitmap, colorHex)
            Toast.makeText(context, "İmza Başarıyla Kaydedildi", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteSignature(id: Long) {
        viewModelScope.launch {
            repository.deleteSignatureTemplate(id)
        }
    }

    suspend fun processUploadedSignature(
        srcBitmap: Bitmap,
        threshold: Float = 0.80f,
        tintColor: Int? = null
    ): Bitmap {
        return imageEngine.extractTransparentSignature(srcBitmap, threshold, tintColor)
    }

    fun stampSignatureOnPage(
        signatureBitmap: Bitmap,
        targetPageIndex: Int = _selectedPageIndex.value,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        rotationDegrees: Float = 0f,
        alpha: Float = 1.0f,
        tintColor: Int? = null,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val pages = _currentPages.value
            if (targetPageIndex in pages.indices) {
                val page = pages[targetPageIndex]
                val pageBitmap = imageEngine.loadBitmapFromFile(page.imagePath)
                if (pageBitmap != null) {
                    val stamped = imageEngine.stampSignature(
                        documentBitmap = pageBitmap,
                        signatureBitmap = signatureBitmap,
                        normalizedX = x,
                        normalizedY = y,
                        normalizedWidth = w,
                        normalizedHeight = h,
                        rotationDegrees = rotationDegrees,
                        alpha = alpha,
                        tintColor = tintColor
                    )
                    val newPath = imageEngine.saveBitmapToFile(stamped, 95)
                    val updatedPage = page.copy(imagePath = newPath)
                    database.documentPageDao().updatePage(updatedPage)
                    val newPages = pages.toMutableList().apply { set(targetPageIndex, updatedPage) }
                    _currentPages.value = newPages
                    Toast.makeText(context, "İmza Sayfaya Başarıyla Eklendi", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke()
                }
            }
        }
    }

    fun stampSignatureOnCurrentPage(
        signatureBitmap: Bitmap,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        alpha: Float = 1.0f
    ) {
        stampSignatureOnPage(
            signatureBitmap = signatureBitmap,
            targetPageIndex = _selectedPageIndex.value,
            x = x,
            y = y,
            w = w,
            h = h,
            rotationDegrees = 0f,
            alpha = alpha
        )
    }

    // Cloud Backup
    fun backupToCloud() {
        viewModelScope.launch {
            _isBackingUp.value = true
            val docs = allActiveDocuments.value
            cloudEngine.createFullBackupArchive(docs.size)
            _cloudAccounts.value = cloudEngine.getAccountsState()
            _isBackingUp.value = false
            Toast.makeText(context, "Bulut Yedekleme Tamamlandı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            scannerTextService.close()
        } catch (_: Exception) {}
    }
}
