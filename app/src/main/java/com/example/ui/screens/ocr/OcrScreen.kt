package com.example.ui.screens.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.ScannedImageTextOverlay
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanEmerald
import java.io.File
import com.example.ui.viewmodel.ScanFlowViewModel

data class LanguageOption(val code: String, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    documentId: Long,
    viewModel: ScanFlowViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentDocument by viewModel.currentDocument.collectAsStateWithLifecycle()
    val isOcrLoading by viewModel.isOcrLoading.collectAsStateWithLifecycle()
    val editableOcrText by viewModel.editableOcrText.collectAsStateWithLifecycle()
    val ocrResult by viewModel.ocrResult.collectAsStateWithLifecycle()
    val currentPages by viewModel.currentPages.collectAsStateWithLifecycle()
    val selectedPageIndex by viewModel.selectedPageIndex.collectAsStateWithLifecycle()
    val detailedOcrResult by viewModel.detailedOcrResult.collectAsStateWithLifecycle()

    var isOverlayViewActive by remember { mutableStateOf(false) }

    val languages = listOf(
        LanguageOption("tr", "Türkçe"),
        LanguageOption("en", "English"),
        LanguageOption("de", "Deutsch"),
        LanguageOption("fr", "Français"),
        LanguageOption("es", "Español"),
        LanguageOption("ar", "العربية")
    )
    var selectedLang by remember { mutableStateOf(languages[0]) }
    var inTextSearchQuery by remember { mutableStateOf("") }
    var showSearchBox by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
        viewModel.performOcrOnCurrentPage(selectedLang.code)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metin Tanıma (OCR)", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { isOverlayViewActive = !isOverlayViewActive }) {
                        Icon(
                            imageVector = if (isOverlayViewActive) Icons.Default.TextFields else Icons.Default.Layers,
                            contentDescription = if (isOverlayViewActive) "Metin Düzenleyici" else "Görsel Katmanı",
                            tint = if (isOverlayViewActive) ScanEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSearchBox = !showSearchBox }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Metinde Ara")
                    }
                    IconButton(onClick = { viewModel.performOcrOnCurrentPage(selectedLang.code) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Yeniden Tara")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language Selector Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(languages) { lang ->
                    val isSel = lang.code == selectedLang.code
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedLang = lang
                                viewModel.performOcrOnCurrentPage(lang.code)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) ScanBlue else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = lang.name,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // In-text search box if active
            if (showSearchBox) {
                OutlinedTextField(
                    value = inTextSearchQuery,
                    onValueChange = { inTextSearchQuery = it },
                    placeholder = { Text("Metin içinde ara...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Kelime", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${ocrResult?.wordCount ?: editableOcrText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScanBlue)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Satır", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${ocrResult?.lineCount ?: editableOcrText.lines().size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScanCyan)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Algılanan Dil", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selectedLang.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Segmented View Mode Switcher: Metin Düzenleyici vs Görsel Katmanı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isOverlayViewActive) ScanBlue else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isOverlayViewActive = false }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            tint = if (!isOverlayViewActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Metin Düzenleyici",
                            color = if (!isOverlayViewActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (!isOverlayViewActive) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOverlayViewActive) ScanEmerald else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isOverlayViewActive = true }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (isOverlayViewActive) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Görsel Katmanı",
                            color = if (isOverlayViewActive) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isOverlayViewActive) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Main Content Area: Either Translucent Overlay on Image OR Editable Text Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isOcrLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = ScanBlue)
                            Text("Metin taranıyor ve çözümleniyor...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (isOverlayViewActive) {
                    val activePage = currentPages.getOrNull(selectedPageIndex) ?: currentPages.firstOrNull()
                    if (activePage != null && File(activePage.imagePath).exists()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = File(activePage.imagePath),
                                contentDescription = "Taranmış Belge Görseli",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            // Translucent ML Kit Text Overlay
                            ScannedImageTextOverlay(
                                textResult = detailedOcrResult,
                                searchQuery = inTextSearchQuery,
                                modifier = Modifier.fillMaxSize(),
                                isOverlayVisible = true,
                                showFloatingControls = true
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Belge görseli bulunamadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = editableOcrText,
                        onValueChange = { viewModel.setEditableOcrText(it) },
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("Tanınan metin burada görüntülenecek ve düzenlenebilecek...") }
                    )
                }
            }

            // Quick Actions Bar: Kopyala, Paylaş, TXT Dışa Aktar, Kaydet
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
                        Toast.makeText(context, "Metin panoya kopyalandı", Toast.LENGTH_SHORT).show()
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
                    onClick = { viewModel.saveEditedOcrText() },
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
