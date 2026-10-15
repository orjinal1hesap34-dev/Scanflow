package com.example.ui.screens.home

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.data.local.entity.DocumentEntity
import com.example.domain.model.DocCategory
import com.example.domain.model.ImageFilter
import com.example.domain.model.SortOption
import com.example.ui.components.AppNavigationDrawer
import com.example.ui.components.AuthModalBottomSheet
import com.example.ui.components.CategoryChipsCarousel
import com.example.ui.components.DocumentGridCard
import com.example.ui.components.DocumentListCard
import com.example.ui.components.DocumentLockSetupDialog
import com.example.ui.components.DocumentOptionsBottomSheet
import com.example.ui.components.DocumentUnlockModal
import com.example.ui.components.ExportBottomSheet
import com.example.ui.components.TagFilterBar
import com.example.ui.components.TagManagementSheet
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanBlueDark
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanCyanGlow
import com.example.ui.theme.ScanEmerald
import com.example.ui.theme.ScanGold
import com.example.ui.theme.ScanGoldLight
import com.example.ui.theme.ScanIndigo
import com.example.ui.theme.ScanPurple
import com.example.ui.viewmodel.ScanFlowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScanFlowViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToSignature: () -> Unit,
    onNavigateToPdfTools: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToCloudSync: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToOcr: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val documents by viewModel.allActiveDocuments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val allAvailableTags by viewModel.allAvailableTags.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var showAuthSheet by remember { mutableStateOf(false) }
    var selectedDocumentForOptions by remember { mutableStateOf<DocumentEntity?>(null) }
    var selectedDocumentForExport by remember { mutableStateOf<DocumentEntity?>(null) }
    var documentToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var documentToLock by remember { mutableStateOf<DocumentEntity?>(null) }
    var documentToUnlock by remember { mutableStateOf<DocumentEntity?>(null) }
    var documentForTagManagement by remember { mutableStateOf<DocumentEntity?>(null) }

    // Google Play Services ML Kit Document Scanner Launcher
    val mlKitScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val scanResult = viewModel.mlKitScannerEngine.parseResult(result.data)
                if (scanResult != null && scanResult.pages.isNotEmpty()) {
                    viewModel.importMlKitScanResult(scanResult) { docId ->
                        onNavigateToEditor(docId)
                    }
                } else {
                    onNavigateToCamera()
                }
            } else if (result.resultCode != Activity.RESULT_CANCELED) {
                onNavigateToCamera()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onNavigateToCamera()
        }
    }

    val startMlKitScanner: () -> Unit = {
        val activity = context as? Activity
        if (activity != null) {
            viewModel.mlKitScannerEngine.startScan(
                activity = activity,
                onIntentSenderReady = { intentSender ->
                    try {
                        mlKitScanLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onNavigateToCamera()
                    }
                },
                onError = {
                    onNavigateToCamera()
                }
            )
        } else {
            onNavigateToCamera()
        }
    }

    // Gallery Image Picker launcher for importing photos
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val bitmaps = uris.mapNotNull { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            android.graphics.BitmapFactory.decodeStream(stream)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmaps.isNotEmpty()) {
                    val docId = viewModel.repository.createDocumentWithPages(
                        title = "İçe Aktarılan_${System.currentTimeMillis() % 10000}",
                        category = DocCategory.DOCUMENTS,
                        pageBitmaps = bitmaps,
                        initialFilter = ImageFilter.DOCUMENT
                    )
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "${bitmaps.size} sayfa içe aktarıldı", Toast.LENGTH_SHORT).show()
                        onNavigateToEditor(docId)
                    }
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppNavigationDrawer(
                viewModel = viewModel,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onNavigateToCamera = onNavigateToCamera,
                onNavigateToPhotoStorage = {
                    galleryLauncher.launch("image/*")
                },
                onNavigateToSignature = onNavigateToSignature,
                onNavigateToOcr = {
                    if (documents.isNotEmpty()) {
                        onNavigateToOcr(documents.first().id)
                    } else {
                        Toast.makeText(context, "Önce bir belge tarayın veya içe aktarın", Toast.LENGTH_SHORT).show()
                    }
                },
                onNavigateToPdfTools = onNavigateToPdfTools,
                onNavigateToFolders = onNavigateToFolders,
                onNavigateToTrash = onNavigateToTrash,
                onNavigateToCloudSync = onNavigateToCloudSync,
                onNavigateToPremium = onNavigateToPremium,
                onNavigateToSettings = onNavigateToSettings,
                onOpenAuth = { showAuthSheet = true },
                onSelectCategory = { category ->
                    viewModel.setCategory(category)
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ObsidianBlack,
                                    Color(0xFF0A0F1D)
                                )
                            )
                        )
                        .padding(top = 12.dp, start = 14.dp, end = 14.dp, bottom = 10.dp)
                ) {
                    // Ultra Top Bar with Drawer Hamburger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { scope.launch { drawerState.open() } }
                                .testTag("open_drawer_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Uygulama Çekmecesi",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ultra Dark Search Box with Glow
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_field"),
                        placeholder = { Text("Belge, fatura, kimlik veya metin ara...", fontSize = 13.sp, color = Color(0xFF64748B)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Ara",
                                tint = ScanCyanGlow
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToCamera,
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(16.dp, CircleShape, ambientColor = ScanBlue, spotColor = ScanCyanGlow)
                        .testTag("scan_fab_button"),
                    shape = CircleShape,
                    containerColor = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(ScanBlue, ScanCyanGlow)
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Belge Tara",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            LazyVerticalGrid(
                columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg)
                    .padding(paddingValues)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
            ) {
                // Ultra 5-Way Quick Action Bar
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickActionCard(
                            title = "PDF Araçları",
                            icon = Icons.Default.PictureAsPdf,
                            color = ScanGoldLight,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToPdfTools
                        )
                        QuickActionCard(
                            title = "Dijital İmza",
                            icon = Icons.Default.Draw,
                            color = ScanEmerald,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSignature
                        )
                        QuickActionCard(
                            title = "AI OCR",
                            icon = Icons.Default.TextFields,
                            color = ScanPurple,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (documents.isNotEmpty()) {
                                    onNavigateToOcr(documents.first().id)
                                } else {
                                    Toast.makeText(context, "Önce bir belge tarayın", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        QuickActionCard(
                            title = "Galeriden Al",
                            icon = Icons.Default.Image,
                            color = ScanIndigo,
                            modifier = Modifier.weight(1f),
                            onClick = { galleryLauncher.launch("image/*") }
                        )
                        QuickActionCard(
                            title = "Klasörler",
                            icon = Icons.Default.Folder,
                            color = ScanCyanGlow,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToFolders
                        )
                    }
                }

                // Section Header & View controls
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Belgelerim",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "${documents.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ScanCyanGlow,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Sort Button
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = "Sırala",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.title) },
                                            onClick = {
                                                viewModel.setSortOption(option)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Grid / List Toggle
                            IconButton(onClick = { viewModel.toggleGridView() }) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Görünümü Değiştir",
                                    tint = if (isGridView) ScanCyanGlow else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // Empty State
                if (documents.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ScanBlue.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ScanCyanGlow.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = ScanCyanGlow,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Arama sonucu bulunamadı" else "Henüz taranmış belge yok",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Kamera ile hemen ultra HD tarayın veya galeriden içe aktarın.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(documents, key = { it.id }) { doc ->
                        if (isGridView) {
                            DocumentGridCard(
                                document = doc,
                                onClick = {
                                    if (doc.isLocked && !viewModel.isDocumentUnlocked(doc.id)) {
                                        documentToUnlock = doc
                                    } else {
                                        onNavigateToEditor(doc.id)
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(doc) },
                                onOptionsClick = { selectedDocumentForOptions = doc }
                            )
                        } else {
                            DocumentListCard(
                                document = doc,
                                onClick = {
                                    if (doc.isLocked && !viewModel.isDocumentUnlocked(doc.id)) {
                                        documentToUnlock = doc
                                    } else {
                                        onNavigateToEditor(doc.id)
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(doc) },
                                onOptionsClick = { selectedDocumentForOptions = doc }
                            )
                        }
                    }
                }
            }
        }
    }

    // Document Options Bottom Sheet
    selectedDocumentForOptions?.let { doc ->
        DocumentOptionsBottomSheet(
            document = doc,
            onDismiss = { selectedDocumentForOptions = null },
            onRename = {
                documentToRename = doc
                renameText = doc.title
                selectedDocumentForOptions = null
            },
            onLockToggle = {
                documentToLock = doc
                selectedDocumentForOptions = null
            },
            onMoveFolder = {
                onNavigateToFolders()
                selectedDocumentForOptions = null
            },
            onManageTags = {
                documentForTagManagement = doc
                selectedDocumentForOptions = null
            },
            onOcr = {
                onNavigateToOcr(doc.id)
                selectedDocumentForOptions = null
            },
            onShare = {
                selectedDocumentForExport = doc
                selectedDocumentForOptions = null
            },
            onExport = {
                selectedDocumentForExport = doc
                selectedDocumentForOptions = null
            },
            onDelete = {
                viewModel.moveToTrash(doc.id)
                Toast.makeText(context, "Çöp kutusuna taşındı", Toast.LENGTH_SHORT).show()
                selectedDocumentForOptions = null
            }
        )
    }

    // Export Bottom Sheet
    selectedDocumentForExport?.let { doc ->
        ExportBottomSheet(
            document = doc,
            isPremium = isPremium,
            onDismiss = { selectedDocumentForExport = null },
            onExportConfirmed = { format, quality, watermark, shareTarget ->
                viewModel.exportDocument(doc, format, quality, if (watermark) "ScanFlow" else null) { file ->
                    when (shareTarget) {
                        com.example.domain.model.ShareTarget.SYSTEM_CHOOSER -> {
                            viewModel.pdfEngine.shareFile(
                                file = file,
                                mimeType = format.mimeType,
                                chooserTitle = "${doc.title} - Paylaş",
                                subject = doc.title,
                                bodyText = "${doc.title} taranmış belgesi ekte yer almaktadır."
                            )
                        }
                        com.example.domain.model.ShareTarget.EMAIL -> {
                            viewModel.pdfEngine.sharePdfToEmail(
                                file = file,
                                subject = "PDF: ${doc.title}",
                                bodyText = "ScanFlow ile taranmış ${doc.title} belgesi ektedir."
                            )
                        }
                        com.example.domain.model.ShareTarget.GOOGLE_DRIVE -> {
                            viewModel.pdfEngine.sharePdfToDriveOrCloud(
                                file = file,
                                subject = doc.title
                            )
                        }
                        com.example.domain.model.ShareTarget.EXTERNAL_VIEWER -> {
                            viewModel.pdfEngine.openPdfWithExternalViewer(file)
                        }
                        com.example.domain.model.ShareTarget.PRINT -> {
                            viewModel.pdfEngine.printPdf(file, doc.title)
                        }
                    }
                }
                selectedDocumentForExport = null
            }
        )
    }

    // Rename Dialog
    documentToRename?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToRename = null },
            title = { Text("Yeniden Adlandır", fontWeight = FontWeight.Bold) },
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
                            viewModel.renameDocument(doc.id, renameText.trim())
                        }
                        documentToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToRename = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Modern Document Lock Setup Dialog
    documentToLock?.let { doc ->
        DocumentLockSetupDialog(
            document = doc,
            viewModel = viewModel,
            onDismiss = { documentToLock = null }
        )
    }

    // Modern Biometric & PIN Document Unlock Modal
    documentToUnlock?.let { doc ->
        DocumentUnlockModal(
            document = doc,
            viewModel = viewModel,
            onDismiss = { documentToUnlock = null },
            onUnlocked = {
                val targetId = doc.id
                documentToUnlock = null
                onNavigateToEditor(targetId)
            }
        )
    }

    // Firebase Auth Bottom Sheet
    if (showAuthSheet) {
        AuthModalBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showAuthSheet = false }
        )
    }

    // Document Tag Management Bottom Sheet
    documentForTagManagement?.let { doc ->
        TagManagementSheet(
            document = doc,
            viewModel = viewModel,
            onDismiss = { documentForTagManagement = null }
        )
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0),
                maxLines = 1
            )
        }
    }
}
