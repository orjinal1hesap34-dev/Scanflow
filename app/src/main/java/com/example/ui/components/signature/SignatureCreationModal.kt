package com.example.ui.components.signature

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.SignatureEntity
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanCyan
import com.example.ui.viewmodel.ScanFlowViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class SignatureCreationTab(val title: String) {
    DRAW("İmza Çiz"),
    UPLOAD("Görsel Yükle"),
    SAVED("Kayıtlı İmzalar")
}

enum class UploadInkColor(val label: String, val colorInt: Int?) {
    ORIGINAL("Orijinal", null),
    BLACK("Siyah", 0xFF0F172A.toInt()),
    ROYAL_BLUE("Mavi", 0xFF1D4ED8.toInt()),
    DEEP_NAVY("Lacivert", 0xFF1E3A8A.toInt()),
    CRIMSON("Kırmızı", 0xFFDC2626.toInt())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureCreationModal(
    viewModel: ScanFlowViewModel,
    onDismiss: () -> Unit,
    onSignatureReadyForOverlay: (Bitmap, String, String) -> Unit // (transparentBitmap, title, colorHex)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val savedSignatures by viewModel.allSignatures.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(SignatureCreationTab.DRAW) }
    var signatureTitle by remember { mutableStateOf("İmzam") }

    // Drawing state
    val paths = remember { mutableStateListOf<SignaturePath>() }
    val currentPoints = remember { mutableStateListOf<Offset>() }
    var drawColor by remember { mutableStateOf(SignatureColorPalette[0]) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    // Upload state
    var uploadedRawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedTransparentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var transparencyThreshold by remember { mutableFloatStateOf(0.80f) }
    var selectedInkColor by remember { mutableStateOf(UploadInkColor.ORIGINAL) }
    var isProcessingUpload by remember { mutableStateOf(false) }

    // Reprocess uploaded signature when parameters change
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("signature_creation_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "İmza Ekle & Yerleştir",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Çizin veya şeffaf arkaplanlı imza yükleyin",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Kapat",
                        tint = ScanBlue
                    )
                }
            }

            // Tab Selection (Draw, Upload, Saved)
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                SignatureCreationTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Tab Contents
            when (selectedTab) {
                SignatureCreationTab.DRAW -> {
                    // Title Input
                    OutlinedTextField(
                        value = signatureTitle,
                        onValueChange = { signatureTitle = it },
                        label = { Text("İmza Başlığı / İsim") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signature_title_input")
                    )

                    // Interactive Drawing Canvas Component
                    SignatureDrawingPad(
                        paths = paths,
                        currentPoints = currentPoints,
                        selectedColor = drawColor,
                        onColorSelected = { drawColor = it },
                        strokeWidth = strokeWidth,
                        onStrokeWidthChange = { strokeWidth = it },
                        onUndo = { if (paths.isNotEmpty()) paths.removeAt(paths.size - 1) },
                        onClear = {
                            paths.clear()
                            currentPoints.clear()
                        }
                    )

                    // Action Buttons for Draw
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (paths.isEmpty()) {
                                    Toast.makeText(context, "Lütfen önce imzanızı çizin", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val transparentBmp = renderSignaturePathsToBitmap(paths)
                                val hexColor = "#%06X".format(0xFFFFFF and drawColor.toArgb())
                                viewModel.saveSignature(signatureTitle.ifBlank { "İmzam" }, transparentBmp, hexColor)
                                onSignatureReadyForOverlay(transparentBmp, signatureTitle, hexColor)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("signature_save_and_apply_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ScanBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Belgeye Yerleştir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                SignatureCreationTab.UPLOAD -> {
                    // Image Upload Section with Transparency Threshold & Tinting
                    if (uploadedRawBitmap == null) {
                        // Empty state: Call to action to select image
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { imagePickerLauncher.launch("image/*") }
                                .testTag("signature_upload_card"),
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
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Görsel Seç",
                                            tint = ScanBlue,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "İmza Görseli / Fotoğrafı Seç",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Kağıda atılmış imza fotoğrafları otomatik olarak şeffaf PNG'ye dönüştürülür",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Image loaded: Show transparent checkerboard preview + threshold controls
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
                                    androidx.compose.foundation.Image(
                                        bitmap = processedTransparentBitmap!!.asImageBitmap(),
                                        contentDescription = "Şeffaf İmza Önizleme",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                if (isProcessingUpload) {
                                    CircularProgressIndicator(
                                        color = ScanBlue,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                TextButton(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    Text("Değiştir", fontSize = 12.sp, color = ScanBlue, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Transparency / Paper Background Removal Sensitivity
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Arkaplan Temizleme Hassasiyeti",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${(transparencyThreshold * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ScanBlue
                                    )
                                }
                                Slider(
                                    value = transparencyThreshold,
                                    onValueChange = { transparencyThreshold = it },
                                    valueRange = 0.35f..0.98f,
                                    colors = SliderDefaults.colors(thumbColor = ScanBlue, activeTrackColor = ScanBlue),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Tint Ink Color Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Mürekkep Rengi:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Use Button for Uploaded Signature
                            Button(
                                onClick = {
                                    val bmp = processedTransparentBitmap ?: uploadedRawBitmap
                                    if (bmp != null) {
                                        val hexColor = if (selectedInkColor.colorInt != null) {
                                            "#%06X".format(0xFFFFFF and selectedInkColor.colorInt!!)
                                        } else "#000000"
                                        viewModel.saveSignature(signatureTitle.ifBlank { "Yüklenen İmza" }, bmp, hexColor)
                                        onSignatureReadyForOverlay(bmp, signatureTitle.ifBlank { "Yüklenen İmza" }, hexColor)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("apply_uploaded_signature_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ScanBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Şeffaf İmzayı Belgeye Yerleştir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                SignatureCreationTab.SAVED -> {
                    // Saved Signatures List
                    if (savedSignatures.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Kayıtlı imzanız bulunmuyor", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Yukarıdaki 'İmza Çiz' veya 'Görsel Yükle' sekmesinden imzanızı ekleyebilirsiniz.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Seçmek istediğiniz imzaya dokunun:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(savedSignatures) { sig ->
                                    Card(
                                        modifier = Modifier
                                            .size(140.dp, 100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                scope.launch(Dispatchers.IO) {
                                                    val bmp = viewModel.imageEngine.loadBitmapFromFile(sig.imagePath)
                                                    if (bmp != null) {
                                                        withContext(Dispatchers.Main) {
                                                            onSignatureReadyForOverlay(bmp, sig.title, sig.colorHex)
                                                        }
                                                    }
                                                }
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, ScanBlue.copy(alpha = 0.5f))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            CheckerboardBackground()

                                            AsyncImage(
                                                model = File(sig.imagePath),
                                                contentDescription = sig.title,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp),
                                                contentScale = ContentScale.Fit
                                            )

                                            // Delete icon
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.Black.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(24.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.deleteSignature(sig.id) },
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Sil", tint = Color.White, modifier = Modifier.size(14.dp))
                                                }
                                            }

                                            // Title pill
                                            Surface(
                                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                                color = Color.Black.copy(alpha = 0.75f),
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = sig.title,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
