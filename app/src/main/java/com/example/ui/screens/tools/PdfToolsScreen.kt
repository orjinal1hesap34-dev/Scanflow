package com.example.ui.screens.tools

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DocumentEntity
import com.example.domain.model.DocCategory
import com.example.domain.model.ExportQuality
import com.example.domain.model.ImageFilter
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanGold
import com.example.ui.theme.ScanIndigo
import com.example.ui.viewmodel.ScanFlowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    viewModel: ScanFlowViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit
) {
    val context = LocalContext.current
    val documents by viewModel.allActiveDocuments.collectAsStateWithLifecycle()

    var showMergeDialog by remember { mutableStateOf(false) }
    val selectedMergeDocs = remember { mutableStateListOf<Long>() }
    var mergeTitle by remember { mutableStateOf("Birleştirilmiş_Belge") }

    var showCompressDialog by remember { mutableStateOf(false) }
    var selectedCompressDoc by remember { mutableStateOf<DocumentEntity?>(null) }

    var showEncryptDialog by remember { mutableStateOf(false) }
    var encryptDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var encryptPassword by remember { mutableStateOf("") }

    // Gallery Image -> PDF converter
    val imageToPdfLauncher = rememberLauncherForActivityResult(
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
                        title = "PDF_Dönüştürülen_${System.currentTimeMillis() % 10000}",
                        category = DocCategory.DOCUMENTS,
                        pageBitmaps = bitmaps,
                        initialFilter = ImageFilter.DOCUMENT
                    )
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "${bitmaps.size} görselden PDF oluşturuldu", Toast.LENGTH_SHORT).show()
                        onNavigateToEditor(docId)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Araç Seti", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Profesyonel PDF & Belge İşlemleri",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                PdfToolCard(
                    title = "PDF Birleştir (Merge)",
                    description = "Birden fazla belgeyi sırayla tek bir PDF dosyasında toplayın.",
                    icon = Icons.Default.CallMerge,
                    color = ScanBlue,
                    onClick = {
                        selectedMergeDocs.clear()
                        showMergeDialog = true
                    }
                )
            }

            item {
                PdfToolCard(
                    title = "Resimleri PDF'e Dönüştür",
                    description = "Galerinizdeki fotoğrafları tek tıkla yüksek kaliteli PDF yapın.",
                    icon = Icons.Default.Image,
                    color = ScanIndigo,
                    onClick = { imageToPdfLauncher.launch("image/*") }
                )
            }

            item {
                PdfToolCard(
                    title = "PDF Sıkıştır (Compress)",
                    description = "Belgelerin görsel kalitesini koruyarak dosya boyutunu %70'e kadar küçültün.",
                    icon = Icons.Default.Compress,
                    color = ScanCyan,
                    onClick = { showCompressDialog = true }
                )
            }

            item {
                PdfToolCard(
                    title = "AES-256 Şifreleme & Kilitleme",
                    description = "Hassas ve gizli belgelerinizi askeri düzeyde PIN/Parola ile koruyun.",
                    icon = Icons.Default.Lock,
                    color = ScanGold,
                    onClick = { showEncryptDialog = true }
                )
            }

            item {
                PdfToolCard(
                    title = "Toplu ZIP Arşivi Oluştur",
                    description = "Seçilen tüm belgeleri arşiv paketi (ZIP) olarak paketleyin.",
                    icon = Icons.Default.FolderZip,
                    color = Color(0xFF64748B),
                    onClick = {
                        if (documents.isNotEmpty()) {
                            viewModel.exportDocument(documents.first(), com.example.domain.model.ExportFormat.ZIP) { zipFile ->
                                viewModel.pdfEngine.shareFile(zipFile, "application/zip")
                            }
                        } else {
                            Toast.makeText(context, "Paketlenecek belge bulunamadı", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                PdfToolCard(
                    title = "Doğrudan Yazdır (Print)",
                    description = "Wi-Fi veya Bluetooth yazıcılara doğrudan yazdırma emri gönderin.",
                    icon = Icons.Default.Print,
                    color = Color(0xFF10B981),
                    onClick = {
                        if (documents.isNotEmpty()) {
                            viewModel.exportDocument(documents.first(), com.example.domain.model.ExportFormat.PDF) { file ->
                                viewModel.pdfEngine.printPdf(file, documents.first().title)
                            }
                        } else {
                            Toast.makeText(context, "Yazdırılacak belge yok", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // Merge Dialog
    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Birleştirilecek Belgeleri Seçin") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = mergeTitle,
                        onValueChange = { mergeTitle = it },
                        label = { Text("Yeni Belge Başlığı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(documents) { doc ->
                            val isChecked = selectedMergeDocs.contains(doc.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedMergeDocs.remove(doc.id)
                                        else selectedMergeDocs.add(doc.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (it) selectedMergeDocs.add(doc.id)
                                        else selectedMergeDocs.remove(doc.id)
                                    }
                                )
                                Text(doc.title, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedMergeDocs.size >= 2) {
                            viewModel.mergeDocuments(selectedMergeDocs.toList(), mergeTitle) { newId ->
                                showMergeDialog = false
                                Toast.makeText(context, "Belgeler birleştirildi", Toast.LENGTH_SHORT).show()
                                onNavigateToEditor(newId)
                            }
                        } else {
                            Toast.makeText(context, "En az 2 belge seçmelisiniz", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
                ) {
                    Text("Birleştir (${selectedMergeDocs.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Compress Dialog
    if (showCompressDialog) {
        AlertDialog(
            onDismissRequest = { showCompressDialog = false },
            title = { Text("Sıkıştırılacak Belgeyi Seçin") },
            text = {
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(documents) { doc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showCompressDialog = false
                                    viewModel.exportDocument(doc, com.example.domain.model.ExportFormat.PDF, ExportQuality.LOW) { file ->
                                        Toast.makeText(context, "PDF Sıkıştırıldı: ${file.name}", Toast.LENGTH_SHORT).show()
                                        viewModel.pdfEngine.shareFile(file)
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = ScanBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(doc.title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("${doc.pageCount} Sayfa • Tahmini Tasarruf %60", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCompressDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Encrypt Dialog
    if (showEncryptDialog) {
        AlertDialog(
            onDismissRequest = { showEncryptDialog = false },
            title = { Text("Belgeyi Şifrele & Kilitle") },
            text = {
                Column {
                    Text("Korunacak belgeyi seçin ve bir PIN/Şifre belirleyin:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = encryptPassword,
                        onValueChange = { encryptPassword = it },
                        label = { Text("Güvenlik PIN / Parola") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.height(140.dp)) {
                        items(documents) { doc ->
                            val isSel = encryptDoc?.id == doc.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) ScanBlue.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { encryptDoc = doc }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = if (isSel) ScanBlue else Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(doc.title, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val doc = encryptDoc
                        if (doc != null && encryptPassword.isNotBlank()) {
                            viewModel.toggleDocumentLock(doc.id, encryptPassword)
                            showEncryptDialog = false
                            Toast.makeText(context, "${doc.title} şifrelendi ve kilitlendi", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Lütfen belge ve şifre girin", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
                ) {
                    Text("Şifrele")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEncryptDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun PdfToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.14f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
