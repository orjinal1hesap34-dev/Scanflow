package com.example.ui.components

import android.graphics.PointF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DetectedQuad
import com.example.ui.screens.camera.ScanMode
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanBlueDark
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanCyanGlow
import com.example.ui.theme.ScanEmerald
import com.example.ui.theme.ScanGold
import com.example.ui.theme.ScanGoldLight
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DocumentEdgeOverlay(
    detectedQuad: DetectedQuad,
    isEdgeDetectionEnabled: Boolean,
    isAutoCaptureEnabled: Boolean,
    isGridVisible: Boolean,
    scanMode: ScanMode,
    modifier: Modifier = Modifier
) {
    // Pulse and scanning laser transitions
    val transition = rememberInfiniteTransition(label = "edge_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val laserProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser_progress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Drawing Canvas for the real-time boundary polygon, corners & grids
        Canvas(modifier = Modifier.fillMaxSize().testTag("realtime_edge_canvas")) {
            val width = size.width
            val height = size.height

            // Calculate pixel positions for the 4 corners
            val tl = Offset(detectedQuad.topLeft.x * width, detectedQuad.topLeft.y * height)
            val tr = Offset(detectedQuad.topRight.x * width, detectedQuad.topRight.y * height)
            val br = Offset(detectedQuad.bottomRight.x * width, detectedQuad.bottomRight.y * height)
            val bl = Offset(detectedQuad.bottomLeft.x * width, detectedQuad.bottomLeft.y * height)

            // Draw Grid Lines if enabled
            if (isGridVisible) {
                drawDocumentGrid(tl, tr, br, bl)
            }

            if (isEdgeDetectionEnabled) {
                val boundaryColor = when {
                    detectedQuad.isStable -> ScanEmerald
                    detectedQuad.isDetected -> ScanCyan
                    else -> ScanCyan.copy(alpha = 0.6f)
                }

                // 1.1 Translucent holographic interior fill
                val polygonPath = Path().apply {
                    moveTo(tl.x, tl.y)
                    lineTo(tr.x, tr.y)
                    lineTo(br.x, br.y)
                    lineTo(bl.x, bl.y)
                    close()
                }

                val fillAlpha = if (detectedQuad.isStable) 0.16f else 0.08f
                drawPath(
                    path = polygonPath,
                    color = boundaryColor.copy(alpha = fillAlpha),
                    style = Fill
                )

                // 1.2 Boundary Outer Glow Outline
                val strokeWidth = if (detectedQuad.isStable) 3.5.dp.toPx() else 2.5.dp.toPx()
                val pathEffect = if (detectedQuad.isDetected) null else PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)

                drawPath(
                    path = polygonPath,
                    color = boundaryColor.copy(alpha = pulseAlpha),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = pathEffect
                    )
                )

                // 1.3 Animated Laser Scan line through polygon
                if (detectedQuad.isDetected) {
                    val topEdgeX = tl.x + (tr.x - tl.x) * laserProgress
                    val topEdgeY = tl.y + (tr.y - tl.y) * laserProgress
                    val botEdgeX = bl.x + (br.x - bl.x) * laserProgress
                    val botEdgeY = bl.y + (br.y - bl.y) * laserProgress

                    val laserStart = Offset(
                        tl.x + (bl.x - tl.x) * laserProgress,
                        tl.y + (bl.y - tl.y) * laserProgress
                    )
                    val laserEnd = Offset(
                        tr.x + (br.x - tr.x) * laserProgress,
                        tr.y + (br.y - tr.y) * laserProgress
                    )

                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                boundaryColor.copy(alpha = 0.8f),
                                Color.White.copy(alpha = 0.9f),
                                boundaryColor.copy(alpha = 0.8f),
                                Color.Transparent
                            ),
                            startX = laserStart.x,
                            endX = laserEnd.x
                        ),
                        start = laserStart,
                        end = laserEnd,
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // 1.4 Precision Target Reticles on 4 Corners
                drawCornerReticle(tl, "TL", boundaryColor)
                drawCornerReticle(tr, "TR", boundaryColor)
                drawCornerReticle(br, "BR", boundaryColor)
                drawCornerReticle(bl, "BL", boundaryColor)
            }
        }

        // 2. Real-time Status HUD (Top Center)
        if (isEdgeDetectionEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 88.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD0B132B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (detectedQuad.isStable) ScanEmerald.copy(alpha = 0.8f) else ScanCyan.copy(alpha = 0.6f)
                    ),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (detectedQuad.isStable) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ScanEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Belge Kilitlendi (${(detectedQuad.confidence * 100).toInt()}%)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ScanEmerald.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, ScanEmerald)
                            ) {
                                Text(
                                    text = detectedQuad.detectedCategoryName,
                                    color = ScanEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        } else if (detectedQuad.isDetected) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ScanCyanGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Belge Algılandı • Hizalanıyor...",
                                color = ScanCyanGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ScanCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = detectedQuad.detectedCategoryName,
                                    color = ScanCyan,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CropFree,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Belge aranıyor... Kamerayı yaklaştırın",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Auto-Capture Countdown Ring HUD (Center Screen)
        if (isAutoCaptureEnabled && detectedQuad.stabilityProgress > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ScanEmerald),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { detectedQuad.stabilityProgress },
                            modifier = Modifier.size(24.dp),
                            color = ScanEmerald,
                            strokeWidth = 3.dp,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Column {
                            Text(
                                text = "Sabit Tutun...",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Otomatik taranıyor (${(detectedQuad.stabilityProgress * 100).toInt()}%)",
                                color = ScanEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawDocumentGrid(
    tl: Offset,
    tr: Offset,
    br: Offset,
    bl: Offset
) {
    val gridColor = Color.White.copy(alpha = 0.15f)
    val stroke = 1.dp.toPx()

    // 1/3 horizontal lines
    val left1 = Offset(tl.x + (bl.x - tl.x) / 3f, tl.y + (bl.y - tl.y) / 3f)
    val right1 = Offset(tr.x + (br.x - tr.x) / 3f, tr.y + (br.y - tr.y) / 3f)
    drawLine(gridColor, left1, right1, stroke)

    val left2 = Offset(tl.x + 2 * (bl.x - tl.x) / 3f, tl.y + 2 * (bl.y - tl.y) / 3f)
    val right2 = Offset(tr.x + 2 * (br.x - tr.x) / 3f, tr.y + 2 * (br.y - tr.y) / 3f)
    drawLine(gridColor, left2, right2, stroke)

    // 1/3 vertical lines
    val top1 = Offset(tl.x + (tr.x - tl.x) / 3f, tl.y + (tr.y - tl.y) / 3f)
    val bot1 = Offset(bl.x + (br.x - bl.x) / 3f, bl.y + (br.y - bl.y) / 3f)
    drawLine(gridColor, top1, bot1, stroke)

    val top2 = Offset(tl.x + 2 * (tr.x - tl.x) / 3f, tl.y + 2 * (tr.y - tl.y) / 3f)
    val bot2 = Offset(bl.x + 2 * (br.x - bl.x) / 3f, bl.y + 2 * (br.y - bl.y) / 3f)
    drawLine(gridColor, top2, bot2, stroke)
}

private fun DrawScope.drawCornerReticle(
    corner: Offset,
    position: String,
    color: Color
) {
    val armLength = 24.dp.toPx()
    val strokeWidth = 3.5.dp.toPx()
    val dotRadius = 4.dp.toPx()
    val outerRingRadius = 9.dp.toPx()

    // Corner Vertex Dot & Outer Ring
    drawCircle(
        color = color.copy(alpha = 0.35f),
        radius = outerRingRadius,
        center = corner,
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawCircle(
        color = Color.White,
        radius = dotRadius,
        center = corner
    )

    // Directional bracket arms
    val dx = when (position) {
        "TL", "BL" -> 1f
        else -> -1f
    }
    val dy = when (position) {
        "TL", "TR" -> 1f
        else -> -1f
    }

    // Horizontal arm
    drawLine(
        color = color,
        start = corner,
        end = Offset(corner.x + (dx * armLength), corner.y),
        strokeWidth = strokeWidth
    )

    // Vertical arm
    drawLine(
        color = color,
        start = corner,
        end = Offset(corner.x, corner.y + (dy * armLength)),
        strokeWidth = strokeWidth
    )
}
