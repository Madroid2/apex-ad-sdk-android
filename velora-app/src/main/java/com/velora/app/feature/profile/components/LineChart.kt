package com.velora.app.feature.profile.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Smooth animated line chart drawn entirely on [Canvas] — zero third-party libraries.
 *
 * Demonstrates Compose drawing API: [Path], [Brush.verticalGradient], cubic Bézier
 * interpolation, and [animateFloatAsState] for the draw-on reveal animation.
 */
@Composable
fun LineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.15f),
    lineWidth: Dp = 2.5.dp,
    dotRadius: Dp = 4.dp,
    animate: Boolean = true,
) {
    if (dataPoints.size < 2) return

    var targetProgress by remember { mutableFloatStateOf(if (animate) 0f else 1f) }
    val drawProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1400, easing = EaseOutCubic),
        label = "chart_draw_progress",
    )

    LaunchedEffect(dataPoints) { targetProgress = 1f }

    val dotColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = dotRadius.toPx() + 4.dp.toPx()
        val chartW = w - padding * 2
        val chartH = h - padding * 2

        val minVal = dataPoints.min()
        val maxVal = dataPoints.max()
        val range = (maxVal - minVal).coerceAtLeast(0.01f)

        fun xOf(index: Int) = padding + index * chartW / (dataPoints.size - 1)
        fun yOf(value: Float) = padding + chartH - (value - minVal) / range * chartH

        // Compute all point positions
        val pts = dataPoints.indices.map { i -> Offset(xOf(i), yOf(dataPoints[i])) }

        // Clamp visible points based on draw progress
        val visibleCount = (pts.size * drawProgress).toInt().coerceAtLeast(1)

        // ── Gradient fill area ────────────────────────────────────────────
        val fillPath = Path().apply {
            moveTo(pts.first().x, h)
            pts.take(visibleCount).forEachIndexed { i, pt ->
                if (i == 0) lineTo(pt.x, pt.y)
                else {
                    val prev = pts[i - 1]
                    val cpX = (prev.x + pt.x) / 2f
                    cubicTo(cpX, prev.y, cpX, pt.y, pt.x, pt.y)
                }
            }
            lineTo(pts[visibleCount - 1].x, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = padding,
                endY = h,
            ),
        )

        // ── Smooth cubic Bézier line ──────────────────────────────────────
        val linePath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until visibleCount) {
                val prev = pts[i - 1]
                val curr = pts[i]
                val cpX = (prev.x + curr.x) / 2f
                cubicTo(cpX, prev.y, cpX, curr.y, curr.x, curr.y)
            }
        }
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = lineWidth.toPx(), cap = StrokeCap.Round),
        )

        // ── Dots at each data point ───────────────────────────────────────
        pts.take(visibleCount).forEach { pt ->
            drawCircle(color = lineColor, radius = dotRadius.toPx(), center = pt)
            drawCircle(color = dotColor, radius = (dotRadius.toPx() - 1.5.dp.toPx()), center = pt)
        }
    }
}

/** Ease-out cubic easing for the chart reveal — starts fast, decelerates. */
private val EaseOutCubic = Easing { t -> 1f - (1f - t) * (1f - t) * (1f - t) }
