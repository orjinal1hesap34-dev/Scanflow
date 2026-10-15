package com.example.ui.screens.signature

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.signature.CheckerboardBackground
import com.example.ui.components.signature.SignatureDrawingPad
import com.example.ui.components.signature.SignaturePath
import com.example.ui.components.signature.SignatureColorPalette
import com.example.ui.components.signature.UploadInkColor
import com.example.ui.components.signature.renderSignaturePathsToBitmap
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanCyan
import com.example.ui.viewmodel.ScanFlowViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreen(
    viewModel: ScanFlowViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedSignatures by viewModel.allSignatures.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Draw, 1: Upload Image
    var signatureTitle by remember { mutableStateOf("İmzam") }

    // Drawing state
    val paths = remember { mutableStateListOf<SignaturePath>() }
    val currentPoints = remember { mutableStateListOf<Offset>() }
    var selectedColor by remember { mutableStateOf(SignatureColorPalette[0]) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    // Image Upload state
    var uploadedRawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedTransparentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var transparencyThreshold by remember { mutableFloatStateOf(0.80f) }
    var selectedInkColor by remember { mutableStateOf(UploadInkColor.ORIGINAL) }
    var isProcessingUpload by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            uploadedRawBitmap = bmp
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Görsel yüklenemedi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Live processing when raw image or transparency slider changes
    LaunchedEffect(uploadedRawBitmap, transparencyThreshold, selectedInkColor) {
        val raw = uploadedRawBitmap ?: return@LaunchedEffect
        isProcessingUpload = true
        withContext(Dispatchers.Default) {
            val transparent = viewModel.imageEngine.extractTransparentSignature(
                srcBitmap = raw,
                threshold = transparencyThreshold,
                tintColor = selectedInkColor.colorInt,
                autoTrim = true
            )
            processedTransparentBitmap = transparent
        }
        isProcessingUpload = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dijital İmza Stüdyosu", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Tabs (Çiz vs Görsel Yükle)
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("İmza Çiz", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Görsel / Fotoğraf Yükle", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            // Title input
            OutlinedTextField(
                value = signatureTitle,
                onValueChange = { signatureTitle = it },
                label = { Text("İmza Başlığı / Sahibi") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signature_screen_title_input")
            )

            // Content per tab
            if (selectedTabIndex == 0) {
                // Drawing Canvas Pad
                SignatureDrawingPad(
                    paths = paths,
                    currentPoints = currentPoints,
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it },
                    strokeWidth = strokeWidth,
                    onStrokeWidthChange = { strokeWidth = it },
                    onUndo = { if (paths.isNotEmpty()) paths.removeAt(paths.size - 1) },
                    onClear = {
                        paths.clear()
                        currentPoints.clear()
                    }
                )
            } else {
                // Image Upload and Transparent Background Extractor
                if (uploadedRawBitmap == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .testTag("signature_screen_upload_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ScanBlue.copy(alpha = 0.12f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Görsel Seç",
                                        tint = ScanBlue,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("İmza Fotoğrafı veya Görseli Yükle", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Beyaz kağıt üzerindeki imzanızı otomatik şeffaflaştırır",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CheckerboardBackground()

                            if (processedTransparentBitmap != null) {
                                Image(
                                    bitmap = processedTransparentBitmap!!.asImageBitmap(),
                                    contentDescription = "Şeffaf İmza Önizleme",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            if (isProcessingUpload) {
                                CircularProgressIndicator(color = ScanBlue, modifier = Modifier.size(28.dp))
                            }

                            TextButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            ) {
                                Text("Görseli Değiştir", fontSize = 12.sp, color = ScanBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Sensitivity Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Arkaplan Temizleme Hassasiyeti", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${(transparencyThreshold * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScanBlue)
                            }
                            Slider(
                                value = transparencyThreshold,
                                onValueChange = { transparencyThreshold = it },
                                valueRange = 0.35f..0.98f,
                                colors = SliderDefaults.colors(thumbColor = ScanBlue, activeTrackColor = ScanBlue)
                            )
                        }

                        // Ink Color Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mürekkep:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(UploadInkColor.values()) { ink ->
                                    val isSel = selectedInkColor == ink
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) ScanBlue else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { selectedInkColor = ink }
                                    ) {
                                        Text(
                                            text = ink.label,
                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Saved Signatures List
            if (savedSignatures.isNotEmpty()) {
                Text(text = "Kayıtlı İmzalarım (${savedSignatures.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(savedSignatures) { sig ->
                        Card(
                            modifier = Modifier
                                .size(120.dp, 80.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CheckerboardBackground()

                                AsyncImage(
                                    model = File(sig.imagePath),
                                    contentDescription = sig.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    contentScale = ContentScale.Fit
                                )
                                IconButton(
                                    onClick = { viewModel.deleteSignature(sig.id) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Save Button
            Button(
                onClick = {
                    if (selectedTabIndex == 0) {
                        if (paths.isEmpty()) {
                            Toast.makeText(context, "Lütfen önce bir imza çizin", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val bmp = renderSignaturePathsToBitmap(paths)
                        val hexColor = "#%06X".format(0xFFFFFF and selectedColor.toArgb())
                        viewModel.saveSignature(signatureTitle.ifBlank { "İmzam" }, bmp, hexColor)
                        onNavigateBack()
                    } else {
                        val bmp = processedTransparentBitmap ?: uploadedRawBitmap
                        if (bmp == null) {
                            Toast.makeText(context, "Lütfen bir imza görseli seçin", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val hexColor = if (selectedInkColor.colorInt != null) {
                            "#%06X".format(0xFFFFFF and selectedInkColor.colorInt!!)
                        } else "#000000"
                        viewModel.saveSignature(signatureTitle.ifBlank { "Yüklenen İmza" }, bmp, hexColor)
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_signature_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Text("İmzayı Kaydet", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
