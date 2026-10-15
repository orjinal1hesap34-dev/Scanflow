package com.example.ui.screens.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.engine.DocumentBoundaryDetector
import com.example.domain.model.DetectedQuad
import com.example.domain.model.DocCategory
import com.example.domain.model.ImageFilter
import com.example.ui.components.DocumentEdgeOverlay
import com.example.ui.components.FilterSelectorRow
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanBlueDark
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanCyanGlow
import com.example.ui.theme.ScanEmerald
import com.example.ui.theme.ScanGold
import com.example.ui.viewmodel.ScanFlowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class ScanMode(val title: String) {
    SINGLE("Tek Sayfa"),
    BATCH("Çoklu Sayfa"),
    ID_CARD("Kimlik / Kart"),
    BOOK("Kitap / Not")
}

@Composable
fun CameraScanScreen(
    viewModel: ScanFlowViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val batchBitmaps by viewModel.batchScannedBitmaps.collectAsStateWithLifecycle()

    var scanMode by remember { mutableStateOf(ScanMode.SINGLE) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isGridVisible by remember { mutableStateOf(false) }
    var isEdgeDetectionEnabled by remember { mutableStateOf(true) }
    var isAutoCapture by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(ImageFilter.DOCUMENT) }
    var isCapturing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Real-time boundary detector and latest detected quad state
    val boundaryDetector = remember { DocumentBoundaryDetector() }
    var detectedQuad by remember { mutableStateOf(DetectedQuad.defaultViewport()) }

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
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val startMlKitScan: () -> Unit = {
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
                    }
                },
                onError = { exc ->
                    Toast.makeText(context, "Dahili kamera tarayıcısı aktif", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Gallery Picker
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
                                viewModel.addCapturedBitmap(bmp)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Function to trigger photo capture
    val triggerCapture: () -> Unit = {
        if (!isCapturing) {
            isCapturing = true
            val capture = imageCapture
            val currentQuad = detectedQuad

            if (capture != null) {
                val executor = Executors.newSingleThreadExecutor()
                capture.takePicture(
                    executor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            val rotation = image.imageInfo.rotationDegrees

                            val finalBitmap = if (rotation != 0) {
                                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            } else {
                                bitmap
                            }
                            image.close()

                            CoroutineScope(Dispatchers.Main).launch {
                                isCapturing = false
                                val targetCategory = when (scanMode) {
                                    ScanMode.ID_CARD -> DocCategory.ID_CARD
                                    ScanMode.BOOK -> DocCategory.BOOK
                                    else -> DocCategory.DOCUMENTS
                                }

                                if (scanMode == ScanMode.BATCH) {
                                    if (isEdgeDetectionEnabled && currentQuad.isDetected) {
                                        viewModel.addCapturedBitmapWithQuad(finalBitmap, currentQuad)
                                    } else {
                                        viewModel.addCapturedBitmap(finalBitmap)
                                    }
                                    Toast.makeText(context, "Sayfa eklendi (${batchBitmaps.size + 1})", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.clearBatchBitmaps()
                                    if (isEdgeDetectionEnabled && currentQuad.isDetected) {
                                        viewModel.addCapturedBitmapWithQuad(finalBitmap, currentQuad)
                                    } else {
                                        viewModel.addCapturedBitmap(finalBitmap)
                                    }
                                    viewModel.finishBatchScan(
                                        title = when (scanMode) {
                                            ScanMode.ID_CARD -> "Kimlik Kartı Tarama"
                                            ScanMode.BOOK -> "Kitap Notu"
                                            else -> "Taranan Belge"
                                        },
                                        category = targetCategory,
                                        filter = selectedFilter
                                    ) { docId ->
                                        onNavigateToEditor(docId)
                                    }
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            isCapturing = false
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "Çekim hatası: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            } else {
                // Fallback simulation capture for virtual container testing
                isCapturing = false
                val mockBmp = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(mockBmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                val p = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 48f
                }
                canvas.drawText("ScanFlow Akıllı Belge Tarama", 80f, 200f, p)
                if (scanMode == ScanMode.BATCH) {
                    viewModel.addCapturedBitmap(mockBmp)
                    Toast.makeText(context, "Sayfa eklendi", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.clearBatchBitmaps()
                    viewModel.addCapturedBitmap(mockBmp)
                    viewModel.finishBatchScan("Taranan Belge", DocCategory.DOCUMENTS, selectedFilter) { docId ->
                        onNavigateToEditor(docId)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera Preview with Real-Time ImageAnalysis
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            // 1. Preview
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            // 2. Image Capture
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .setFlashMode(flashMode)
                                .build()
                            imageCapture = capture

                            // 3. Real-time Image Analysis for Document Edge Detection
                            val analysisExecutor = Executors.newSingleThreadExecutor()
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(640, 480))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                .build()

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                if (isEdgeDetectionEnabled) {
                                    val analyzedQuad = boundaryDetector.analyzeFrame(
                                        imageProxy = imageProxy,
                                        scanMode = scanMode,
                                        isAutoCaptureEnabled = isAutoCapture
                                    )
                                    CoroutineScope(Dispatchers.Main).launch {
                                        detectedQuad = analyzedQuad
                                        // Trigger auto-capture if stability reaches 100%
                                        if (isAutoCapture && analyzedQuad.stabilityProgress >= 0.98f && !isCapturing) {
                                            triggerCapture()
                                        }
                                    }
                                }
                                imageProxy.close()
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture,
                                imageAnalysis
                            )
                            camera = cam
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )
        } else {
            // Permission fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = ScanBlue,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Kamera İzni Gerekli",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Belgeleri canlı kenar algılama ile otomatik taramak için kamera izni verin.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
                    ) {
                        Text("İzin Ver")
                    }
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                    ) {
                        Text("Galeriden Seç", color = Color.White)
                    }
                }
            }
        }

        // REAL-TIME DOCUMENT BOUNDARY & EDGE OVERLAY
        DocumentEdgeOverlay(
            detectedQuad = detectedQuad,
            isEdgeDetectionEnabled = isEdgeDetectionEnabled,
            isAutoCaptureEnabled = isAutoCapture,
            isGridVisible = isGridVisible,
            scanMode = scanMode
        )

        // TOP CONTROL BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, start = 14.dp, end = 14.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
            ) {
                // 1. Batch Mode Toggle Button
                item {
                    val isBatch = scanMode == ScanMode.BATCH
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isBatch) ScanEmerald.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            if (isBatch) ScanEmerald else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clickable {
                                if (isBatch) {
                                    scanMode = ScanMode.SINGLE
                                    Toast.makeText(context, "Tek Sayfa Moduna Geçildi", Toast.LENGTH_SHORT).show()
                                } else {
                                    scanMode = ScanMode.BATCH
                                    Toast.makeText(context, "Toplu Tarama (Batch) Aktif - Sayfaları art arda çekin", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("batch_mode_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Toplu Tarama Modu",
                                tint = if (isBatch) ScanEmerald else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isBatch) "Toplu Mod: Açık" else "Toplu Mod",
                                color = if (isBatch) ScanEmerald else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (batchBitmaps.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = ScanEmerald,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${batchBitmaps.size}",
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Real-time Edge Detection Toggle Button
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isEdgeDetectionEnabled) ScanCyanGlow.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isEdgeDetectionEnabled) ScanCyanGlow else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .clickable {
                                isEdgeDetectionEnabled = !isEdgeDetectionEnabled
                                if (!isEdgeDetectionEnabled) boundaryDetector.reset()
                            }
                            .testTag("toggle_edge_detection_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropFree,
                                contentDescription = "Kenar Algılama",
                                tint = if (isEdgeDetectionEnabled) ScanCyanGlow else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isEdgeDetectionEnabled) "Kenar Açık" else "Kenar Kapalı",
                                color = if (isEdgeDetectionEnabled) ScanCyanGlow else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. Auto Capture Toggle
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isAutoCapture) ScanEmerald.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAutoCapture) ScanEmerald else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .clickable { isAutoCapture = !isAutoCapture }
                            .testTag("toggle_auto_capture_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Otomatik",
                                tint = if (isAutoCapture) ScanEmerald else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isAutoCapture) "Oto-Çekim" else "Manuel",
                                color = if (isAutoCapture) ScanEmerald else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4. ML Kit Smart Scan Button
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ScanBlue.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ScanBlue),
                        modifier = Modifier
                            .clickable { startMlKitScan() }
                            .testTag("launch_mlkit_scanner_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "ML Kit Akıllı Tarayıcı",
                                tint = ScanCyanGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ML Kit",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 5. Flash Toggle
                item {
                    IconButton(
                        onClick = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                            imageCapture?.flashMode = flashMode
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        val icon = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Flaş",
                            tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) ScanGold else Color.White
                        )
                    }
                }

                // 6. Grid Toggle
                item {
                    IconButton(
                        onClick = { isGridVisible = !isGridVisible },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Izgara",
                            tint = if (isGridVisible) ScanCyan else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // BATCH MODE FLOATING BANNER
        AnimatedVisibility(
            visible = scanMode == ScanMode.BATCH,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ScanEmerald.copy(alpha = 0.7f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ScanEmerald)
                    )
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = ScanEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (batchBitmaps.isEmpty()) {
                            "Toplu Tarama: Sayfaları art arda çekin"
                        } else {
                            "Toplu Tarama: ${batchBitmaps.size} sayfa çekildi • Çekime devam edin"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // BOTTOM CONTROLS & CAPTURE STRIP
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Batch Thumbnails Strip (if any pages captured)
            AnimatedVisibility(
                visible = batchBitmaps.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ScanCyan.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header: Count + Clear All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = ScanCyanGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Taranan Sayfalar (${batchBitmaps.size})",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.clearBatchBitmaps() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Temizle",
                                    tint = Color.Red.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Temizle",
                                    color = Color.Red.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Thumbnail Carousel
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(batchBitmaps) { index, bmp ->
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.5.dp, ScanCyan, RoundedCornerShape(8.dp))
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Sayfa ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Page Number Badge
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(topEnd = 6.dp),
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            color = ScanCyanGlow,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    // Remove Page Button
                                    IconButton(
                                        onClick = { viewModel.removeCapturedBitmap(index) },
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color.Red.copy(alpha = 0.9f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Sil",
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Process Batch into Single PDF Button
                        Button(
                            onClick = {
                                val timeStr = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                val title = "Çoklu Belge ($timeStr)"
                                viewModel.finishBatchScan(
                                    title = title,
                                    category = when (scanMode) {
                                        ScanMode.ID_CARD -> DocCategory.ID_CARD
                                        ScanMode.BOOK -> DocCategory.BOOK
                                        else -> DocCategory.DOCUMENTS
                                    },
                                    filter = selectedFilter
                                ) { docId ->
                                    onNavigateToEditor(docId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScanBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("process_batch_pdf_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Tek PDF Olarak Birleştir (${batchBitmaps.size} Sayfa)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Mode Selector: Tek Sayfa, Çoklu Sayfa, Kimlik, Kitap
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                itemsIndexed(ScanMode.values()) { _, mode ->
                    val isSel = mode == scanMode
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSel) DarkSurfaceElevated else Color.Transparent,
                        border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, ScanCyan) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                scanMode = mode
                                boundaryDetector.reset()
                            }
                    ) {
                        Text(
                            text = mode.title,
                            color = if (isSel) ScanCyan else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filter Selector Pills
            FilterSelectorRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Shutter & Gallery Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery import button
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .testTag("camera_gallery_button")
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Galeri", tint = Color.White)
                }

                // Main Shutter Button with Batch Counter Badge
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .border(3.dp, if (scanMode == ScanMode.BATCH) ScanEmerald else ScanCyan, CircleShape)
                            .clickable(enabled = !isCapturing) {
                                triggerCapture()
                            }
                            .testTag("camera_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (detectedQuad.isStable) ScanEmerald else Color.White)
                        )
                    }

                    if (scanMode == ScanMode.BATCH && batchBitmaps.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = ScanEmerald,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Black),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${batchBitmaps.size}",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Batch finish / Single shortcut
                if (batchBitmaps.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val timeStr = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                            val title = "Çoklu Belge ($timeStr)"
                            viewModel.finishBatchScan(title, DocCategory.DOCUMENTS, selectedFilter) { docId ->
                                onNavigateToEditor(docId)
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(ScanBlue, CircleShape)
                            .testTag("shutter_finish_batch_button")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF Oluştur", tint = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }
        }
    }
}
