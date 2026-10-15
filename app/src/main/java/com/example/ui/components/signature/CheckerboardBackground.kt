package com.example.ui.components.signature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CheckerboardBackground(
    modifier: Modifier = Modifier,
    squareSizeDp: Float = 10f,
    lightColor: Color = Color(0xFFF8FAFC),
    darkColor: Color = Color(0xFFE2E8F0)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val squarePx = squareSizeDp * density
        val cols = (size.width / squarePx).toInt() + 1
        val rows = (size.height / squarePx).toInt() + 1

        drawRect(color = lightColor)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if ((r + c) % 2 == 1) {
                    drawRect(
                        color = darkColor,
                        topLeft = Offset(c * squarePx, r * squarePx),
                        size = Size(squarePx, squarePx)
                    )
                }
            }
        }
    }
}
