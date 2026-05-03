package com.velora.app.feature.home.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.velora.app.data.model.HeroBanner
import com.velora.app.core.design.White
import com.velora.app.core.design.White40
import com.velora.app.core.design.White70
import kotlinx.coroutines.delay

@Composable
fun HeroCarousel(
    banners: List<HeroBanner>,
    onCtaClick: (HeroBanner) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { banners.size })

    // Auto-advance every 4 seconds
    LaunchedEffect(pagerState.pageCount) {
        while (true) {
            delay(4_000)
            val next = (pagerState.currentPage + 1) % pagerState.pageCount
            pagerState.animateScrollToPage(next, animationSpec = tween(600))
        }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.large),
        ) { page ->
            HeroBannerPage(banner = banners[page], onCtaClick = onCtaClick)
        }

        // Page indicator dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(banners.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (selected) 20.dp else 6.dp, 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                )
            }
        }
    }
}

@Composable
private fun HeroBannerPage(
    banner: HeroBanner,
    onCtaClick: (HeroBanner) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        AsyncImage(
            model = banner.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlay — left-to-right, darkens the left where text lives
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(banner.backgroundGradientStart).copy(alpha = 0.92f),
                            Color(banner.backgroundGradientEnd).copy(alpha = 0.5f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // Text + CTA
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp, end = 80.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = banner.title,
                style = MaterialTheme.typography.displaySmall,
                color = White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = banner.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = White70,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onCtaClick(banner) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = White,
                    contentColor = Color(banner.backgroundGradientStart),
                ),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = banner.ctaLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
