package com.example.ui.components.signature

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.DocumentPageEntity
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanGold
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureOverlayView(
    signatureBitmap: Bitmap,
    pages: List<DocumentPageEntity>,
    initialPageIndex: Int = 0,
    onPageSelected: (Int) -> Unit,
    onChangeSignatureRequest: () -> Unit,
    onCancel: () -> Unit,
    onApplySignature: (pageIndex: Int, x: Float, y: Float, w: Float, h: Float, rotationDegrees: Float, alpha: Float, tintColor: Int?) -> Unit
) {
    var activePageIndex by remember { mutableIntStateOf(initialPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))) }
    val activePage = pages.getOrNull(activePageIndex)

    // Interactive Signature Overlay Parameters (Normalized 0.0f .. 1.0f)
    var normX by remember { mutableFloatStateOf(0.55f) }
    var normY by remember { mutableFloatStateOf(0.75f) }
    var normWidth by remember { mutableFloatStateOf(0.35f) }
    var normHeight by remember { mutableFloatStateOf(0.15f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var alphaVal by remember { mutableFloatStateOf(0.95f) }
    var selectedTintColor by remember { mutableStateOf<Color?>(null) }
    var isApplying by remember { mutableStateOf(false) }

    // Active bottom adjustment tab
    var activeControlTab by remember { mutableStateOf(0) } // 0: Presets & Move, 1: Size & Scale, 2: Rotate & Opacity, 3: Color Tint

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("signature_overlay_view"),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "İptal",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "İmzayı Konumlandır",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "İmzayı sürükleyin veya boyutlandırın",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                TextButton(
                    onClick = onChangeSignatureRequest,
                    modifier = Modifier.testTag("change_signature_button")
                ) {
                    Text("Değiştir", color = ScanCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Page Selector Bar (if multi-page document)
            if (pages.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pages) { idx, page ->
                        val isSel = idx == activePageIndex
                        Box(
                            modifier = Modifier
                                .size(48.dp, 64.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) ScanBlue else Color.Gray.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    activePageIndex = idx
                                    onPageSelected(idx)
                                }
                        ) {
                            AsyncImage(
                                model = File(page.imagePath),
                                contentDescription = "Sayfa ${idx + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(2.dp)
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Document Canvas with Interactive Signature Stamp
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                val containerWidthPx = constraints.maxWidth.toFloat()
                val containerHeightPx = constraints.maxHeight.toFloat()

                // Document Background Page
                if (activePage != null && File(activePage.imagePath).exists()) {
                    AsyncImage(
                        model = File(activePage.imagePath),
                        contentDescription = "Belge Sayfası",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sayfa yükleniyor...", color = Color.White)
                    }
                }

                // Interactive Draggable Signature Stamp Box
                val sigPixelW = (normWidth * containerWidthPx).coerceAtLeast(60f)
                val sigPixelH = (normHeight * containerHeightPx).coerceAtLeast(30f)
                val sigPixelX = (normX * containerWidthPx).coerceIn(0f, (containerWidthPx - sigPixelW).coerceAtLeast(0f))
                val sigPixelY = (normY * containerHeightPx).coerceIn(0f, (containerHeightPx - sigPixelH).coerceAtLeast(0f))

                Box(
                    modifier = Modifier
                        .offset { IntOffset(sigPixelX.roundToInt(), sigPixelY.roundToInt()) }
                        .size(
                            width = with(LocalDensity.current) { sigPixelW.toDp() },
                            height = with(LocalDensity.current) { sigPixelH.toDp() }
                        )
                        .rotate(rotationDegrees)
                        .graphicsLayer(alpha = alphaVal)
                        .border(
                            width = 1.5.dp,
                            color = ScanCyan,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                        .testTag("signature_stamp_box")
                        .pointerInput(containerWidthPx, containerHeightPx, sigPixelW, sigPixelH) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newX = (sigPixelX + dragAmount.x) / containerWidthPx
                                val newY = (sigPixelY + dragAmount.y) / containerHeightPx
                                normX = newX.coerceIn(0f, (1f - normWidth).coerceAtLeast(0f))
                                normY = newY.coerceIn(0f, (1f - normHeight).coerceAtLeast(0f))
                            }
                        }
                ) {
                    // Signature Bitmap Image
                    Image(
                        bitmap = signatureBitmap.asImageBitmap(),
                        contentDescription = "İmza Damgası",
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = selectedTintColor?.let { ColorFilter.tint(it) }
                    )

                    // Corner Indicators
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ScanCyan, CircleShape)
                            .align(Alignment.TopStart)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ScanCyan, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ScanCyan, CircleShape)
                            .align(Alignment.BottomStart)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ScanCyan, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            // Bottom Floating Controls Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Control Tabs (Konum, Boyut, Döndür, Renk)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Hızlı Konum", "Boyut", "Açı & Şeffaflık", "Renk").forEachIndexed { index, label ->
                            val isSel = activeControlTab == index
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) ScanBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { activeControlTab = index }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Tab Sub-Controls
                    when (activeControlTab) {
                        0 -> {
                            // Preset Alignment Shortcuts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        normX = 0.58f
                                        normY = 0.78f
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("Sağ Alt", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        normX = 0.08f
                                        normY = 0.78f
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("Sol Alt", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        normX = 0.33f
                                        normY = 0.78f
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("Alt Orta", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        normX = 0.33f
                                        normY = 0.42f
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("Merkez", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                }
                            }
                        }

                        1 -> {
                            // Size & Scale Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("İmza Boyutu", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("${(normWidth * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScanBlue)
                                }
                                Slider(
                                    value = normWidth,
                                    onValueChange = {
                                        normWidth = it
                                        val aspect = signatureBitmap.height.toFloat() / signatureBitmap.width.toFloat()
                                        normHeight = (it * aspect).coerceIn(0.05f, 0.40f)
                                    },
                                    valueRange = 0.15f..0.70f,
                                    colors = SliderDefaults.colors(thumbColor = ScanBlue, activeTrackColor = ScanBlue)
                                )
                            }
                        }

                        2 -> {
                            // Rotation & Opacity
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Döndürme", fontSize = 11.sp)
                                        Text("${rotationDegrees.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = rotationDegrees,
                                        onValueChange = { rotationDegrees = it },
                                        valueRange = -45f..45f,
                                        colors = SliderDefaults.colors(thumbColor = ScanBlue, activeTrackColor = ScanBlue)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Opaklık", fontSize = 11.sp)
                                        Text("${(alphaVal * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = alphaVal,
                                        onValueChange = { alphaVal = it },
                                        valueRange = 0.30f..1.0f,
                                        colors = SliderDefaults.colors(thumbColor = ScanBlue, activeTrackColor = ScanBlue)
                                    )
                                }
                            }
                        }

                        3 -> {
                            // Color Tint Swatches
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Mürekkep Tonu:", fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        null to "Orijinal",
                                        Color(0xFF0F172A) to "Siyah",
                                        Color(0xFF1D4ED8) to "Mavi",
                                        Color(0xFF1E3A8A) to "Lacivert",
                                        Color(0xFFDC2626) to "Kırmızı"
                                    ).forEach { (color, label) ->
                                        val isSel = selectedTintColor == color
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(color ?: Color.LightGray)
                                                .border(
                                                    width = if (isSel) 2.5.dp else 1.dp,
                                                    color = if (isSel) ScanBlue else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedTintColor = color },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSel) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Main Action Button: Apply / Stamp
                    Button(
                        onClick = {
                            isApplying = true
                            onApplySignature(
                                activePageIndex,
                                normX,
                                normY,
                                normWidth,
                                normHeight,
                                rotationDegrees,
                                alphaVal,
                                selectedTintColor?.toArgb()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_signature_to_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ScanBlue),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isApplying
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("İmza Belgeye Ekleniyor...", fontSize = 14.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "İmzayı Sayfa ${activePageIndex + 1}'e Damgala",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
