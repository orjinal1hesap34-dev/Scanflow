package com.example.ui.components.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ScanBlue

data class SignaturePath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

val SignatureColorPalette = listOf(
    Color(0xFF0F172A), // Dark Slate / Black
    Color(0xFF1D4ED8), // Royal Blue
    Color(0xFF1E3A8A), // Deep Navy
    Color(0xFFDC2626), // Crimson Red
    Color(0xFF059669)  // Emerald Green
)

@Composable
fun SignatureDrawingPad(
    modifier: Modifier = Modifier,
    paths: MutableList<SignaturePath>,
    currentPoints: MutableList<Offset>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Drawing Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .testTag("signature_drawing_canvas")
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.clear()
                            currentPoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                paths.add(SignaturePath(currentPoints.toList(), selectedColor, strokeWidth))
                                currentPoints.clear()
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Guideline line
                val lineY = size.height * 0.78f
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(24f, lineY),
                    end = Offset(size.width - 24f, lineY),
                    strokeWidth = 2f
                )

                // Render completed paths
                paths.forEach { pathItem ->
                    if (pathItem.points.size > 1) {
                        val path = Path().apply {
                            moveTo(pathItem.points.first().x, pathItem.points.first().y)
                            for (i in 1 until pathItem.points.size) {
                                lineTo(pathItem.points[i].x, pathItem.points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = pathItem.color,
                            style = Stroke(
                                width = pathItem.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Render current drawing path
                if (currentPoints.size > 1) {
                    val path = Path().apply {
                        moveTo(currentPoints.first().x, currentPoints.first().y)
                        for (i in 1 until currentPoints.size) {
                            lineTo(currentPoints[i].x, currentPoints[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = selectedColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (paths.isEmpty() && currentPoints.isEmpty()) {
                Text(
                    text = "Buraya imzanızı çizin",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Quick Canvas Actions (Undo & Clear)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.06f),
                    modifier = Modifier.size(34.dp)
                ) {
                    IconButton(
                        onClick = onUndo,
                        enabled = paths.isNotEmpty(),
                        modifier = Modifier.testTag("signature_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Geri Al",
                            tint = if (paths.isNotEmpty()) Color(0xFF334155) else Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.06f),
                    modifier = Modifier.size(34.dp)
                ) {
                    IconButton(
                        onClick = onClear,
                        enabled = paths.isNotEmpty() || currentPoints.isNotEmpty(),
                        modifier = Modifier.testTag("signature_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Temizle",
                            tint = if (paths.isNotEmpty() || currentPoints.isNotEmpty()) Color(0xFFDC2626) else Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Ink Colors and Stroke Width Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Swatches
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SignatureColorPalette.forEach { color ->
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) ScanBlue else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Stroke slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(140.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = "Kalınlık",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Slider(
                    value = strokeWidth,
                    onValueChange = onStrokeWidthChange,
                    valueRange = 2f..14f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = ScanBlue, activeTrackColor = ScanBlue)
                )
            }
        }
    }
}

fun renderSignaturePathsToBitmap(
    paths: List<SignaturePath>,
    targetWidth: Int = 800,
    targetHeight: Int = 500
): Bitmap {
    val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    paths.forEach { drawnPath ->
        paint.color = drawnPath.color.toArgb()
        paint.strokeWidth = drawnPath.strokeWidth * 2f
        val p = android.graphics.Path()
        if (drawnPath.points.size > 1) {
            p.moveTo(drawnPath.points.first().x * 2f, drawnPath.points.first().y * 2f)
            for (i in 1 until drawnPath.points.size) {
                p.lineTo(drawnPath.points[i].x * 2f, drawnPath.points[i].y * 2f)
            }
            canvas.drawPath(p, paint)
        }
    }

    // Auto trim transparent padding
    var minX = targetWidth
    var minY = targetHeight
    var maxX = 0
    var maxY = 0
    var hasInk = false

    val pixels = IntArray(targetWidth * targetHeight)
    bmp.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

    for (y in 0 until targetHeight) {
        for (x in 0 until targetWidth) {
            val a = (pixels[y * targetWidth + x] shr 24) and 0xFF
            if (a > 10) {
                hasInk = true
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }

    return if (hasInk && maxX > minX && maxY > minY) {
        val pad = 16
        val cropL = kotlin.math.max(0, minX - pad)
        val cropT = kotlin.math.max(0, minY - pad)
        val cropR = kotlin.math.min(targetWidth, maxX + pad)
        val cropB = kotlin.math.min(targetHeight, maxY + pad)
        Bitmap.createBitmap(bmp, cropL, cropT, cropR - cropL, cropB - cropT)
    } else {
        bmp
    }
}
