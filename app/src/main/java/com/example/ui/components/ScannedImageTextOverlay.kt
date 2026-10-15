package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.engine.RecognizedTextBlock
import com.example.domain.engine.RecognizedTextLine
import com.example.domain.engine.ScannerTextResult
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanCyanGlow
import com.example.ui.theme.ScanEmerald
import com.example.ui.theme.ScanGold
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Display modes for the translucent OCR text overlay.
 */
enum class TranslucentOverlayStyle(val label: String) {
    HYBRID("Hibrit"),
    TEXT_BADGES("Metin"),
    GLOW_BOXES("Vurgu"),
    FULL_VEIL("X-Ray")
}

/**
 * A UI component that renders a translucent overlay precisely mapped onto a scanned document image,
 * displaying extracted text and bounding boxes from Google ML Kit Text Recognition.
 *
 * Supports interactive block selection, touch inspection, search term highlighting,
 * customizable opacity, and multiple translucent rendering styles.
 *
 * @param textResult Extracted text hierarchy from [com.example.domain.engine.ScannerTextRecognitionService].
 * @param modifier Layout modifier applied to the overlay container.
 * @param isOverlayVisible Toggle to hide or show the translucent overlay.
 * @param overlayOpacity Translucent opacity level from 0.1f (subtle) to 0.95f (prominent).
 * @param overlayStyle The visual presentation style of the overlay.
 * @param searchQuery Optional search term to highlight matching extracted words.
 * @param selectedBlock Currently focused/selected text block, or null.
 * @param onBlockSelected Callback invoked when user taps a recognized text block.
 * @param onCopyText Callback when text is copied to clipboard.
 * @param showFloatingControls Whether to display the top-right overlay control bar (opacity slider, style toggle).
 */
@Composable
fun ScannedImageTextOverlay(
    textResult: ScannerTextResult?,
    modifier: Modifier = Modifier,
    isOverlayVisible: Boolean = true,
    overlayOpacity: Float = 0.65f,
    overlayStyle: TranslucentOverlayStyle = TranslucentOverlayStyle.HYBRID,
    searchQuery: String = "",
    selectedBlock: RecognizedTextBlock? = null,
    onBlockSelected: ((RecognizedTextBlock?) -> Unit)? = null,
    onCopyText: ((String) -> Unit)? = null,
    showFloatingControls: Boolean = true
) {
    if (textResult == null || textResult.blocks.isEmpty()) return

    val context = LocalContext.current
    var localSelectedBlock by remember { mutableStateOf(selectedBlock) }
    var currentOpacity by remember { mutableFloatStateOf(overlayOpacity) }
    var currentStyle by remember { mutableStateOf(overlayStyle) }
    var showControlsMenu by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(isOverlayVisible) }

    // Pulsing animation for selected/active block highlight
    val pulseTransition = rememberInfiniteTransition(label = "ocr_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("scanned_image_text_overlay")
    ) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value
        val density = LocalDensity.current

        // Calculate original source image dimensions
        val maxRight = textResult.blocks.mapNotNull { it.boundingBox?.right }.maxOrNull() ?: 1000
        val maxBottom = textResult.blocks.mapNotNull { it.boundingBox?.bottom }.maxOrNull() ?: 1000
        val refWidth = if (textResult.imageWidth > 0) textResult.imageWidth.toFloat() else max(maxRight.toFloat(), 1000f)
        val refHeight = if (textResult.imageHeight > 0) textResult.imageHeight.toFloat() else max(maxBottom.toFloat(), 1400f)

        // Compute aspect-fit scale and offsets within container
        val scale = min(
            with(density) { maxWidth.toPx() } / refWidth,
            with(density) { maxHeight.toPx() } / refHeight
        )
        val renderedWidthPx = refWidth * scale
        val renderedHeightPx = refHeight * scale
        val offsetXPx = (with(density) { maxWidth.toPx() } - renderedWidthPx) / 2f
        val offsetYPx = (with(density) { maxHeight.toPx() } - renderedHeightPx) / 2f

        if (isVisible) {
            // 1. Canvas Layer: Draws translucent bounding boxes, highlights, and full-veil if enabled
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(textResult, scale, offsetXPx, offsetYPx) {
                        detectTapGestures { tapOffset ->
                            // Hit-test against recognized blocks
                            val clickedBlock = textResult.blocks.firstOrNull { block ->
                                val box = block.boundingBox ?: return@firstOrNull false
                                val left = offsetXPx + (box.left * scale)
                                val top = offsetYPx + (box.top * scale)
                                val right = offsetXPx + (box.right * scale)
                                val bottom = offsetYPx + (box.bottom * scale)
                                tapOffset.x in left..right && tapOffset.y in top..bottom
                            }
                            localSelectedBlock = if (localSelectedBlock == clickedBlock) null else clickedBlock
                            onBlockSelected?.invoke(localSelectedBlock)
                        }
                    }
            ) {
                // Optional Full Veil mode: overall translucent dark film
                if (currentStyle == TranslucentOverlayStyle.FULL_VEIL) {
                    drawRect(
                        color = Color(0xAA05070B).copy(alpha = currentOpacity * 0.85f),
                        topLeft = Offset(offsetXPx, offsetYPx),
                        size = Size(renderedWidthPx, renderedHeightPx)
                    )
                }

                // Render each text block or line
                textResult.blocks.forEach { block ->
                    val isSelected = localSelectedBlock == block
                    val blockBox = block.boundingBox

                    if (blockBox != null) {
                        val left = offsetXPx + (blockBox.left * scale)
                        val top = offsetYPx + (blockBox.top * scale)
                        val width = blockBox.width() * scale
                        val height = blockBox.height() * scale

                        val hasSearchMatch = searchQuery.isNotBlank() &&
                                block.text.contains(searchQuery, ignoreCase = true)

                        val boxTint = when {
                            isSelected -> ScanEmerald
                            hasSearchMatch -> ScanGold
                            else -> ScanCyan
                        }

                        val fillAlpha = when {
                            isSelected -> (currentOpacity * 0.45f).coerceIn(0.15f, 0.7f)
                            hasSearchMatch -> (currentOpacity * 0.4f).coerceIn(0.15f, 0.65f)
                            currentStyle == TranslucentOverlayStyle.GLOW_BOXES -> (currentOpacity * 0.28f).coerceIn(0.08f, 0.5f)
                            else -> (currentOpacity * 0.18f).coerceIn(0.05f, 0.4f)
                        }

                        // Translucent Bounding Box Fill
                        drawRoundRect(
                            color = boxTint.copy(alpha = fillAlpha),
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                            style = Fill
                        )

                        // Translucent Bounding Box Outline / Glow
                        val strokeAlpha = if (isSelected) pulseAlpha else (currentOpacity * 0.75f).coerceIn(0.2f, 0.95f)
                        val strokeWidth = if (isSelected) 2.5.dp.toPx() else 1.2.dp.toPx()

                        drawRoundRect(
                            color = boxTint.copy(alpha = strokeAlpha),
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            }

            // 2. Translucent Text Badges / Labels Layer (Composables positioned over bounds)
            if (currentStyle != TranslucentOverlayStyle.GLOW_BOXES) {
                Box(modifier = Modifier.fillMaxSize()) {
                    textResult.blocks.forEach { block ->
                        val box = block.boundingBox ?: return@forEach
                        val isSelected = localSelectedBlock == block
                        val hasSearchMatch = searchQuery.isNotBlank() &&
                                block.text.contains(searchQuery, ignoreCase = true)

                        val leftPx = offsetXPx + (box.left * scale)
                        val topPx = offsetYPx + (box.top * scale)
                        val widthPx = box.width() * scale
                        val heightPx = box.height() * scale

                        val leftDp = with(density) { leftPx.toDp() }
                        val topDp = with(density) { topPx.toDp() }
                        val widthDp = with(density) { widthPx.toDp() }
                        val heightDp = with(density) { heightPx.toDp() }

                        // Minimum legible dimensions check
                        if (widthDp > 24.dp && heightDp > 10.dp) {
                            Box(
                                modifier = Modifier
                                    .offset(x = leftDp, y = topDp)
                                    .size(widthDp, heightDp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            isSelected -> Color(0xD9064E3B) // Rich Emerald translucent
                                            hasSearchMatch -> Color(0xD978350F) // Amber translucent
                                            else -> Color(0xCC0B132B).copy(alpha = (currentOpacity * 0.9f).coerceIn(0.3f, 0.92f))
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) ScanEmerald else ScanCyan.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        localSelectedBlock = if (localSelectedBlock == block) null else block
                                        onBlockSelected?.invoke(localSelectedBlock)
                                    }
                                    .padding(horizontal = 3.dp, vertical = 1.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val dynamicFontSize = (heightDp.value * 0.55f).coerceIn(8f, 13f).sp
                                Text(
                                    text = block.text,
                                    color = when {
                                        isSelected -> Color(0xFFE6FFFA)
                                        hasSearchMatch -> Color(0xFFFEF3C7)
                                        else -> Color.White
                                    },
                                    fontSize = dynamicFontSize,
                                    fontWeight = if (isSelected || hasSearchMatch) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = max(1, (heightDp.value / 14f).toInt()),
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = dynamicFontSize * 1.15f
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Top Floating Translucent HUD Controls (Toggle, Opacity, Style, Copy All)
        if (showFloatingControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Floating Pill Action Bar
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xDD0D121F),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ScanCyan.copy(alpha = 0.5f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Overlay visibility toggle
                            IconButton(
                                onClick = { isVisible = !isVisible },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isVisible) "Katmanı Gizle" else "Katmanı Göster",
                                    tint = if (isVisible) ScanCyan else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Style & Opacity settings toggle
                            IconButton(
                                onClick = { showControlsMenu = !showControlsMenu },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Opacity,
                                    contentDescription = "Katman Ayarları",
                                    tint = if (showControlsMenu) ScanBlue else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Copy All Extracted Text
                            IconButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("Extracted Text", textResult.fullText)
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(clip)
                                    Toast.makeText(context, "Tüm metin kopyalandı (${textResult.wordCount} kelime)", Toast.LENGTH_SHORT).show()
                                    onCopyText?.invoke(textResult.fullText)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Tümünü Kopyala",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Quick stats badge (word count)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ScanBlue.copy(alpha = 0.25f),
                                modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                            ) {
                                Text(
                                    text = "${textResult.wordCount} K",
                                    color = ScanCyanGlow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Expanded Controls Panel (Opacity Slider & Style Selector)
                    AnimatedVisibility(
                        visible = showControlsMenu && isVisible,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xEE0B132B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ScanBlue.copy(alpha = 0.4f)),
                            shadowElevation = 10.dp,
                            modifier = Modifier.widthIn(max = 240.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Opacity Slider Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Saydamlık",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${(currentOpacity * 100).roundToInt()}%",
                                        color = ScanCyanGlow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Slider(
                                    value = currentOpacity,
                                    onValueChange = { currentOpacity = it },
                                    valueRange = 0.15f..0.95f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ScanCyan,
                                        activeTrackColor = ScanBlue,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.height(24.dp)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Style Selector Chips
                                Text(
                                    text = "Görünüm Modu",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    TranslucentOverlayStyle.entries.forEach { style ->
                                        val isSelected = currentStyle == style
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) ScanBlue else Color(0x33FFFFFF),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { currentStyle = style }
                                        ) {
                                            Text(
                                                text = style.label,
                                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                                fontSize = 9.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                                                softWrap = false
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

        // 4. Bottom Selected Block Inspector Card (When user taps any text element)
        AnimatedVisibility(
            visible = localSelectedBlock != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            val block = localSelectedBlock
            if (block != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF20B132B),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ScanEmerald),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ScanEmerald.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.TextFields,
                                            contentDescription = null,
                                            tint = ScanEmerald,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Seçilen Metin Bloğu",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "(${block.lines.size} satır)",
                                    color = ScanCyan,
                                    fontSize = 11.sp
                                )
                            }

                            // Dismiss button
                            IconButton(
                                onClick = {
                                    localSelectedBlock = null
                                    onBlockSelected?.invoke(null)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Extracted text content box
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x44000000),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = block.text,
                                color = Color(0xFFF1F5F9),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        // Action Buttons: Kopyala & Paylaş
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ScanEmerald,
                                modifier = Modifier.clickable {
                                    val clip = ClipData.newPlainText("Extracted Block", block.text)
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(clip)
                                    Toast.makeText(context, "Seçilen blok kopyalandı", Toast.LENGTH_SHORT).show()
                                    onCopyText?.invoke(block.text)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Kopyala",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
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

/**
 * All-in-one container component that pairs a scanned image with the translucent ML Kit text overlay,
 * including pinch-to-zoom, pan gestures, and state management.
 *
 * @param imageModel File, Uri, Bitmap, or URL representing the scanned image.
 * @param textResult Extracted OCR hierarchy from ML Kit.
 * @param modifier Outer layout modifier.
 * @param contentDescription Content description for the scanned image.
 * @param isOverlayActive Whether the text overlay is currently displayed.
 * @param onOverlayToggle Callback when user toggles the overlay visibility.
 */
@Composable
fun ScannedImageWithTextOverlay(
    imageModel: Any?,
    textResult: ScannerTextResult?,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Taranmış Belge",
    isOverlayActive: Boolean = true,
    onOverlayToggle: ((Boolean) -> Unit)? = null
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
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
        // Scanned Document Image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentAlignment = Alignment.Center
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // Translucent Text Overlay directly over image
            if (isOverlayActive && textResult != null) {
                ScannedImageTextOverlay(
                    textResult = textResult,
                    isOverlayVisible = true,
                    modifier = Modifier.fillMaxSize(),
                    showFloatingControls = false
                )
            }
        }

        // Floating Controls on top edge
        if (textResult != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ScanBlue.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onOverlayToggle?.invoke(!isOverlayActive) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isOverlayActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Katman Görünürlüğü",
                            tint = if (isOverlayActive) ScanCyan else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (scale > 1f) {
                        Text(
                            text = "%.1fx".format(scale),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
