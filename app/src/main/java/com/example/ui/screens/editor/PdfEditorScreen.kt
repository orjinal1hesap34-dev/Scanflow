package com.example.ui.screens.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.DocCategory
import com.example.domain.model.ImageFilter
import com.example.ui.components.DocumentTagRow
import com.example.ui.components.ExportBottomSheet
import com.example.ui.components.FilterSelectorRow
import com.example.ui.components.ScannedImageTextOverlay
import com.example.ui.components.TagManagementSheet
import com.example.ui.components.signature.SignatureCreationModal
import com.example.ui.components.signature.SignatureOverlayView
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanEmerald
import com.example.ui.theme.ScanGold
import com.example.ui.viewmodel.ScanFlowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    documentId: Long,
    viewModel: ScanFlowViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCrop: (Long) -> Unit,
    onNavigateToSignature: () -> Unit,
    onNavigateToOcr: (Long) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    val currentDocument by viewModel.currentDocument.collectAsStateWithLifecycle()
    val pages by viewModel.currentPages.collectAsStateWithLifecycle()
    val selectedPageIndex by viewModel.selectedPageIndex.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val signatures by viewModel.allSignatures.collectAsStateWithLifecycle()
    val isOcrLoading by viewModel.isOcrLoading.collectAsStateWithLifecycle()
    val editableOcrText by viewModel.editableOcrText.collectAsStateWithLifecycle()
    val ocrResult by viewModel.ocrResult.collectAsStateWithLifecycle()
    val detailedOcrResult by viewModel.detailedOcrResult.collectAsStateWithLifecycle()
    val isTextOverlayActive by viewModel.isTextOverlayActive.collectAsStateWithLifecycle()

    var showExportSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showOcrExtractionSheet by remember { mutableStateOf(false) }
    var ocrSelectedLanguage by remember { mutableStateOf("tr") }
    var showSignatureCreationModal by remember { mutableStateOf(false) }
    var showSignatureOverlayMode by remember { mutableStateOf(false) }
    var activeOverlaySignatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showTagManagementSheet by remember { mutableStateOf(false) }

    // Interactive zoom & pan for current preview
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Add page launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) {
                                viewModel.addPageFromBitmap(bmp)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val activePage = pages.getOrNull(selectedPageIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable {
                            currentDocument?.let {
                                renameText = it.title
                                showRenameDialog = true
                            }
                        }
                    ) {
                        Text(
                            text = currentDocument?.title ?: "Belge Düzenleyici",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Sayfa ${if (pages.isEmpty()) 0 else selectedPageIndex + 1} / ${pages.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleTextOverlay() }) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Metin Katmanı (Translucent Overlay)",
                            tint = if (isTextOverlayActive) ScanEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        viewModel.performOcrOnCurrentPage("tr")
                        showOcrExtractionSheet = true
                    }) {
                        Icon(imageVector = Icons.Default.TextFields, contentDescription = "OCR Metin Çıkar", tint = ScanBlue)
                    }
                    IconButton(onClick = { showExportSheet = true }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Paylaş", tint = ScanCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Document Tags Bar
            currentDocument?.let { doc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentTagRow(
                        tags = doc.tags,
                        maxVisible = 4,
                        onAddTagClick = { showTagManagementSheet = true }
                    )
                }
            }

            // Main Interactive Page Viewer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (activePage != null && File(activePage.imagePath).exists()) {
                    AsyncImage(
                        model = File(activePage.imagePath),
                        contentDescription = "Sayfa ${selectedPageIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )

                    // Translucent ML Kit Text Recognition Overlay
                    if (isTextOverlayActive && detailedOcrResult != null) {
                        ScannedImageTextOverlay(
                            textResult = detailedOcrResult,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                            isOverlayVisible = true,
                            showFloatingControls = true
                        )
                    } else if (isTextOverlayActive && isOcrLoading) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ScanCyan),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = ScanCyan, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                                Text("ML Kit Metin Katmanı Çıkarılıyor...", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sayfa yükleniyor...", color = Color.White)
                    }
                }

                // Reset zoom pill if zoomed in
                if (scale > 1f) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clickable {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                    ) {
                        Text(
                            text = "Yakınlaştırmayı Sıfırla (%.1fx)".format(scale),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Horizontal Thumbnail Page Strip
            if (pages.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pages) { index, page ->
                        val isSelected = index == selectedPageIndex
                        Box(
                            modifier = Modifier
                                .size(54.dp, 72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ScanBlue else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.setSelectedPageIndex(index)
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                        ) {
                            AsyncImage(
                                model = File(page.imagePath),
                                contentDescription = "Sayfa ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(2.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Editing Tools Strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        EditorToolButton(
                            icon = Icons.Default.DocumentScanner,
                            label = "Metin Çıkar",
                            tint = ScanBlue,
                            onClick = {
                                viewModel.performOcrOnCurrentPage("tr")
                                showOcrExtractionSheet = true
                            }
                        )
                    }
                    item {
                        EditorToolButton(
                            icon = Icons.Default.Layers,
                            label = "Metin Katmanı",
                            tint = if (isTextOverlayActive) ScanEmerald else ScanCyan,
                            onClick = { viewModel.toggleTextOverlay() }
                        )
                    }
                    item {
                        EditorToolButton(
                            icon = Icons.Default.LocalOffer,
                            label = "Etiketler",
                            tint = ScanCyan,
                            onClick = { showTagManagementSheet = true }
                        )
                    }
                    item {
                        EditorToolButton(
                            icon = Icons.Default.Filter,
                            label = "Filtre",
                            onClick = { showFilterSheet = true }
                        )
                    }
                    item {
                        EditorToolButton(
                            icon = Icons.Default.RotateRight,
                            label = "Döndür",
                            onClick = { viewModel.rotateCurrentPage(90) }
                        )
                    }
                    item {
                        EditorToolButton(
                            icon = Icons.Default.Crop,
                            label = "Kırp",
                            onClick = { activePage?.let { onNavigateToCrop(it.id) } }
                        )
                    }
                    item {
                        EditorToolButton(
                            icon = Icons.Default.Draw,
                            label = "İmza Ekle",
                            onClick = { showSignatureCreationModal = true }
                        )
                    }
                    item {
                        EditorToolButton(
                            icon = Icons.Default.Add,
                            label = "Sayfa Ekle",
                            onClick = { galleryLauncher.launch("image/*") }
                        )
                    }
                    item {
                        EditorToolButton(
                            icon = Icons.Default.ContentCopy,
                            label = "Çoğalt",
                            onClick = {
                                viewModel.duplicateCurrentPage()
                                Toast.makeText(context, "Sayfa çoğaltıldı", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    item {
                        EditorToolButton(
                            icon = Icons.Default.Delete,
                            label = "Sayfayı Sil",
                            tint = Color(0xFFEF4444),
                            onClick = {
                                viewModel.deleteCurrentPage()
                                Toast.makeText(context, "Sayfa silindi", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    item {
                        EditorToolButton(
                            icon = Icons.Default.Print,
                            label = "Yazdır",
                            onClick = {
                                currentDocument?.let { doc ->
                                    viewModel.exportDocument(doc, com.example.domain.model.ExportFormat.PDF) { file ->
                                        viewModel.pdfEngine.printPdf(file, doc.title)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Filter Selection Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Görsel Filtresi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilterSelectorRow(
                    selectedFilter = activePage?.filterType ?: ImageFilter.DOCUMENT,
                    onFilterSelected = { filter ->
                        viewModel.applyFilterToCurrentPage(filter)
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
                ) {
                    Text("Uygula")
                }
            }
        }
    }

    // Signature Creation & Selection Modal (Draw, Upload Transparent PNG/JPG, or pick Saved)
    if (showSignatureCreationModal) {
        SignatureCreationModal(
            viewModel = viewModel,
            onDismiss = { showSignatureCreationModal = false },
            onSignatureReadyForOverlay = { signatureBmp, _, _ ->
                activeOverlaySignatureBitmap = signatureBmp
                showSignatureCreationModal = false
                showSignatureOverlayMode = true
            }
        )
    }

    // Interactive Signature Overlay Mode on Selected PDF Page
    if (showSignatureOverlayMode && activeOverlaySignatureBitmap != null && pages.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
        ) {
            SignatureOverlayView(
                signatureBitmap = activeOverlaySignatureBitmap!!,
                pages = pages,
                initialPageIndex = selectedPageIndex,
                onPageSelected = { newIdx ->
                    viewModel.setSelectedPageIndex(newIdx)
                },
                onChangeSignatureRequest = {
                    showSignatureOverlayMode = false
                    showSignatureCreationModal = true
                },
                onCancel = {
                    showSignatureOverlayMode = false
                },
                onApplySignature = { targetIdx, x, y, w, h, rotationDegrees, alpha, tintColor ->
                    viewModel.stampSignatureOnPage(
                        signatureBitmap = activeOverlaySignatureBitmap!!,
                        targetPageIndex = targetIdx,
                        x = x,
                        y = y,
                        w = w,
                        h = h,
                        rotationDegrees = rotationDegrees,
                        alpha = alpha,
                        tintColor = tintColor,
                        onComplete = {
                            showSignatureOverlayMode = false
                        }
                    )
                }
            )
        }
    }

    // ML Kit Text Recognition (OCR) Bottom Sheet
    if (showOcrExtractionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOcrExtractionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with ML Kit Branding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ScanBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "ML Kit OCR",
                                    tint = ScanBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Metin Çıkar (ML Kit OCR)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Google ML Kit Text Recognition",
                                fontSize = 11.sp,
                                color = ScanBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Full Screen OCR link
                    IconButton(
                        onClick = {
                            showOcrExtractionSheet = false
                            onNavigateToOcr(documentId)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Tam Ekran OCR",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Language Quick Selector
                val availableLangs = listOf(
                    "tr" to "Türkçe",
                    "en" to "English",
                    "de" to "Deutsch",
                    "fr" to "Français",
                    "es" to "Español",
                    "ar" to "العربية"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(availableLangs) { _, (code, label) ->
                        val isSelected = ocrSelectedLanguage == code
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ScanBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    ocrSelectedLanguage = code
                                    viewModel.performOcrOnCurrentPage(code)
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Stats Bar (Words, Lines, Characters)
                val wordCount = ocrResult?.wordCount ?: editableOcrText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                val lineCount = ocrResult?.lineCount ?: editableOcrText.lines().filter { it.isNotBlank() }.size
                val charCount = ocrResult?.charCount ?: editableOcrText.length

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Kelime", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$wordCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ScanBlue)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Satır", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$lineCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ScanCyan)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Karakter", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$charCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Dil", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ocrResult?.languageDetected?.uppercase() ?: ocrSelectedLanguage.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Block chips for fast selective copy
                if (ocrResult != null && ocrResult!!.blocks.size > 1) {
                    Text(
                        text = "Algılanan Paragraflar (Hızlı Seçim):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(ocrResult!!.blocks) { bIdx, blockText ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("ScanFlow Paragraf", blockText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Paragraf ${bIdx + 1} kopyalandı", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = ScanBlue
                                    )
                                    Text(
                                        text = "P${bIdx + 1} (${blockText.take(15)}...)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Editable Text Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (isOcrLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = ScanBlue, strokeWidth = 3.dp)
                                Text("Google ML Kit ile taranıyor...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = editableOcrText,
                            onValueChange = { viewModel.setEditableOcrText(it) },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Algılanan metin burada düzenlenebilir...", fontSize = 13.sp) }
                        )
                    }
                }

                // Action Buttons Strip: Kopyala, Paylaş, Belgeye Kaydet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy to clipboard
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("ScanFlow OCR", editableOcrText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Tüm metin panoya kopyalandı", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kopyala", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }

                    // Share text
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, editableOcrText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Metni Paylaş"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paylaş", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }

                    // Save to document
                    Button(
                        onClick = {
                            viewModel.saveEditedOcrText()
                            showOcrExtractionSheet = false
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = ScanBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kaydet", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Export Bottom Sheet
    if (showExportSheet && currentDocument != null) {
        ExportBottomSheet(
            document = currentDocument!!,
            isPremium = isPremium,
            onDismiss = { showExportSheet = false },
            onExportConfirmed = { format, quality, watermark, shareTarget ->
                viewModel.exportDocument(currentDocument!!, format, quality, if (watermark) "ScanFlow" else null) { file ->
                    when (shareTarget) {
                        com.example.domain.model.ShareTarget.SYSTEM_CHOOSER -> {
                            viewModel.pdfEngine.shareFile(
                                file = file,
                                mimeType = format.mimeType,
                                chooserTitle = "${currentDocument!!.title} - Paylaş",
                                subject = currentDocument!!.title,
                                bodyText = "${currentDocument!!.title} taranmış belgesi ekte yer almaktadır."
                            )
                        }
                        com.example.domain.model.ShareTarget.EMAIL -> {
                            viewModel.pdfEngine.sharePdfToEmail(
                                file = file,
                                subject = "PDF: ${currentDocument!!.title}",
                                bodyText = "ScanFlow ile taranmış ${currentDocument!!.title} belgesi ektedir."
                            )
                        }
                        com.example.domain.model.ShareTarget.GOOGLE_DRIVE -> {
                            viewModel.pdfEngine.sharePdfToDriveOrCloud(
                                file = file,
                                subject = currentDocument!!.title
                            )
                        }
                        com.example.domain.model.ShareTarget.EXTERNAL_VIEWER -> {
                            viewModel.pdfEngine.openPdfWithExternalViewer(file)
                        }
                        com.example.domain.model.ShareTarget.PRINT -> {
                            viewModel.pdfEngine.printPdf(file, currentDocument!!.title)
                        }
                    }
                }
                showExportSheet = false
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && currentDocument != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Yeniden Adlandır") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Belge Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameDocument(currentDocument!!.id, renameText.trim())
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Tag Management Sheet
    if (showTagManagementSheet && currentDocument != null) {
        TagManagementSheet(
            document = currentDocument!!,
            viewModel = viewModel,
            onDismiss = { showTagManagementSheet = false }
        )
    }
}

@Composable
fun EditorToolButton(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
