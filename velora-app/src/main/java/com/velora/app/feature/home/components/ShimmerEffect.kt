package com.velora.app.feature.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Reusable shimmer brush — a bright sheen that sweeps left-to-right over
 * placeholder shapes, created without any third-party library.
 */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 600f, 400f),
    )
}

/** Full home-screen skeleton shown while data loads. */
@Composable
fun HomeShimmer() {
    val brush = shimmerBrush()
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
    ) {
        // Hero carousel placeholder
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(brush),
            )
        }
        // Category chips placeholder
        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.75f)
                    .height(36.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(brush),
            )
        }
        // Product card placeholders — alternating heights for staggered effect
        items(8) { index ->
            val h = if (index % 3 == 0) 260.dp else if (index % 3 == 1) 220.dp else 240.dp
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(h)
                        .clip(MaterialTheme.shapes.medium)
                        .background(brush),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(brush),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(brush),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
