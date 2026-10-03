package com.hvlg.guide.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 手绘的太阳 / 月亮图标。
 * 只用 material-icons-core（它没有明暗主题图标），避免为两个图标引入
 * material-icons-extended 那套体积巨大的依赖。
 */
@Composable
fun ThemeModeIcon(dark: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val c = center
        val r = size.minDimension / 2f
        if (dark) {
            drawCircle(color = tint, radius = r * 0.92f, center = c)
            drawCircle(
                color = Color.Black,
                radius = r * 0.80f,
                center = Offset(c.x + r * 0.46f, c.y - r * 0.42f),
                blendMode = BlendMode.Clear,
            )
        } else {
            drawCircle(color = tint, radius = r * 0.40f, center = c)
            for (i in 0 until 8) {
                val angle = i * PI / 4.0
                val dx = cos(angle).toFloat()
                val dy = sin(angle).toFloat()
                drawLine(
                    color = tint,
                    start = Offset(c.x + dx * r * 0.66f, c.y + dy * r * 0.66f),
                    end = Offset(c.x + dx * r * 0.98f, c.y + dy * r * 0.98f),
                    strokeWidth = r * 0.16f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
