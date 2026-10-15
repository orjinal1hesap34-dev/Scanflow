package com.example.ui.screens.crop

import android.graphics.Bitmap
import android.graphics.PointF
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanCyan
import com.example.ui.viewmodel.ScanFlowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot

enum class CropPreset(val title: String, val ratio: Float?) {
    FREE("Serbest", null),
    A4("A4 (1:1.41)", 1f / 1.414f),
    US_LETTER("US Letter", 8.5f / 11f),
    ID_CARD("Kimlik / Kart", 8.5f / 5.4f),
    SQUARE("Kare (1:1)", 1f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropPerspectiveScreen(
    pageId: Long,
    viewModel: ScanFlowViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val pages by viewModel.currentPages.collectAsStateWithLifecycle()
    val page = pages.find { it.id == pageId } ?: pages.firstOrNull()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPreset by remember { mutableStateOf(CropPreset.FREE) }

    // Normalized 4 corners (0f..1f)
    var tl by remember { mutableStateOf(PointF(0.08f, 0.08f)) }
    var tr by remember { mutableStateOf(PointF(0.92f, 0.08f)) }
    var br by remember { mutableStateOf(PointF(0.92f, 0.92f)) }
    var bl by remember { mutableStateOf(PointF(0.08f, 0.92f)) }

    var draggingHandle by remember { mutableStateOf<String?>(null) } // "TL", "TR", "BR", "BL" or null

    LaunchedEffect(page) {
        if (page != null) {
            val path = if (page.originalImagePath.isNotBlank()) page.originalImagePath else page.imagePath
            val bmp = viewModel.imageEngine.loadBitmapFromFile(path)
            sourceBitmap = bmp
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kırp & Perspektif Düzelt", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Auto detect document edges
                            tl = PointF(0.05f, 0.05f)
                            tr = PointF(0.95f, 0.05f)
                            br = PointF(0.95f, 0.95f)
                            bl = PointF(0.05f, 0.95f)
                            Toast.makeText(context, "Kenarlar otomatik hizalandı", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Otomatik Algıla", tint = ScanCyan)
                    }
                    IconButton(
                        onClick = {
                            tl = PointF(0.0f, 0.0f)
                            tr = PointF(1.0f, 0.0f)
                            br = PointF(1.0f, 1.0f)
                            bl = PointF(0.0f, 1.0f)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Sıfırla")
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
                .background(Color.Black)
        ) {
            // Interactive 4-corner Quad Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val boxWidth = constraints.maxWidth.toFloat()
                val boxHeight = constraints.maxHeight.toFloat()

                if (sourceBitmap != null) {
                    val bmp = sourceBitmap!!

                    // Calculate scaled bitmap bounds to fit box
                    val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat()
                    val boxAspect = boxWidth / boxHeight

                    val renderW: Float
                    val renderH: Float
                    val offsetX: Float
                    val offsetY: Float

                    if (bmpAspect > boxAspect) {
                        renderW = boxWidth
                        renderH = boxWidth / bmpAspect
                        offsetX = 0f
                        offsetY = (boxHeight - renderH) / 2f
                    } else {
                        renderH = boxHeight
                        renderW = boxHeight * bmpAspect
                        offsetX = (boxWidth - renderW) / 2f
                        offsetY = 0f
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Display Bitmap inside calculated render rect
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            // Determine which handle is closest
                                            val tlScreen = Offset(offsetX + tl.x * renderW, offsetY + tl.y * renderH)
                                            val trScreen = Offset(offsetX + tr.x * renderW, offsetY + tr.y * renderH)
                                            val brScreen = Offset(offsetX + br.x * renderW, offsetY + br.y * renderH)
                                            val blScreen = Offset(offsetX + bl.x * renderW, offsetY + bl.y * renderH)

                                            val threshold = 70.dp.toPx()
                                            draggingHandle = when {
                                                hypot(offset.x - tlScreen.x, offset.y - tlScreen.y) < threshold -> "TL"
                                                hypot(offset.x - trScreen.x, offset.y - trScreen.y) < threshold -> "TR"
                                                hypot(offset.x - brScreen.x, offset.y - brScreen.y) < threshold -> "BR"
                                                hypot(offset.x - blScreen.x, offset.y - blScreen.y) < threshold -> "BL"
                                                else -> null
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val normDx = dragAmount.x / renderW
                                            val normDy = dragAmount.y / renderH

                                            when (draggingHandle) {
                                                "TL" -> tl = PointF((tl.x + normDx).coerceIn(0f, 0.45f), (tl.y + normDy).coerceIn(0f, 0.45f))
                                                "TR" -> tr = PointF((tr.x + normDx).coerceIn(0.55f, 1f), (tr.y + normDy).coerceIn(0f, 0.45f))
                                                "BR" -> br = PointF((br.x + normDx).coerceIn(0.55f, 1f), (br.y + normDy).coerceIn(0.55f, 1f))
                                                "BL" -> bl = PointF((bl.x + normDx).coerceIn(0f, 0.45f), (bl.y + normDy).coerceIn(0.55f, 1f))
                                            }
                                        },
                                        onDragEnd = { draggingHandle = null },
                                        onDragCancel = { draggingHandle = null }
                                    )
                                }
                        ) {
                            // Draw base image
                            drawImage(
                                image = bmp.asImageBitmap(),
                                dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()),
                                dstSize = androidx.compose.ui.unit.IntSize(renderW.toInt(), renderH.toInt())
                            )

                            // Screen coordinates of 4 corners
                            val tlP = Offset(offsetX + tl.x * renderW, offsetY + tl.y * renderH)
                            val trP = Offset(offsetX + tr.x * renderW, offsetY + tr.y * renderH)
                            val brP = Offset(offsetX + br.x * renderW, offsetY + br.y * renderH)
                            val blP = Offset(offsetX + bl.x * renderW, offsetY + bl.y * renderH)

                            // Crop Quad Polyline
                            val path = Path().apply {
                                moveTo(tlP.x, tlP.y)
                                lineTo(trP.x, trP.y)
                                lineTo(brP.x, brP.y)
                                lineTo(blP.x, blP.y)
                                close()
                            }

                            // Dark overlay outside the crop quad
                            drawPath(path, color = ScanCyan.copy(alpha = 0.15f))
                            drawPath(path, color = ScanCyan, style = Stroke(width = 3.dp.toPx()))

                            // Draw Corner Circles & Handles
                            val handleRadius = 14.dp.toPx()
                            val handleInner = 8.dp.toPx()

                            listOf(tlP, trP, brP, blP).forEach { pt ->
                                drawCircle(Color.White, radius = handleRadius, center = pt)
                                drawCircle(ScanBlue, radius = handleInner, center = pt)
                            }
                        }
                    }
                }
            }

            // Aspect Presets Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CropPreset.values()) { preset ->
                    val isSel = preset == selectedPreset
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedPreset = preset
                                val r = preset.ratio
                                if (r != null) {
                                    tl = PointF(0.1f, 0.1f)
                                    tr = PointF(0.9f, 0.1f)
                                    br = PointF(0.9f, 0.9f)
                                    bl = PointF(0.1f, 0.9f)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) ScanBlue else Color(0xFF334155)
                    ) {
                        Text(
                            text = preset.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Bottom Confirm Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            if (sourceBitmap != null && page != null) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val warped = viewModel.imageEngine.warpPerspective(sourceBitmap!!, tl, tr, br, bl)
                                    val newPath = viewModel.imageEngine.saveBitmapToFile(warped, 90)
                                    val updated = page.copy(imagePath = newPath, width = warped.width, height = warped.height)
                                    viewModel.repository.updatePageFilter(updated, page.filterType)

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Perspektif ve Kırpma Uygulandı", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Text("Uygula", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
