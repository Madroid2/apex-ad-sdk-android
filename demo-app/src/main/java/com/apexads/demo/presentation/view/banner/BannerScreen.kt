package com.apexads.demo.presentation.view.banner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.banner.BannerAdView

@Composable
fun BannerScreen(viewModel: AdViewModel) {
    val state by viewModel.bannerState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // BannerAdView is a real Android View (WebView-based). A new instance is needed after
    // Activity recreation (rotation), but the BannerAd itself lives in the ViewModel and
    // survives — so show() re-renders the retained creative without a network round-trip.
    val bannerView = remember { BannerAdView(context) }

    // First-show: fires when the ad transitions to Loaded.
    LaunchedEffect(state) {
        if (state is AdViewModel.AdState.Loaded) {
            viewModel.bannerAd?.show(bannerView)
        }
    }

    // Rotation re-attach: fires whenever bannerView is (re-)created. show() is a no-op if
    // no ad is loaded yet; if the ViewModel is already DISPLAYED it re-renders into the new view.
    LaunchedEffect(bannerView) {
        viewModel.bannerAd?.show(bannerView)
    }

    // Destroy the WebView when the screen leaves the composition permanently.
    // BannerAd itself is destroyed in AdViewModel.onCleared().
    DisposableEffect(Unit) {
        onDispose { bannerView.destroy() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Banner Ad (320×50)", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "OpenRTB 2.6 auction · MRAID 3.0 WebView · MRC viewability (50% / 1 s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AnimatedVisibility(visible = state is AdViewModel.AdState.Loading) {
            LinearProgressIndicator(modifier = Modifier.width(240.dp))
        }

        Text(
            text = when (val s = state) {
                is AdViewModel.AdState.Idle -> "Tap Load to fetch a banner ad"
                is AdViewModel.AdState.Loading -> "Loading banner ad…"
                is AdViewModel.AdState.Loaded -> "Banner ad loaded ✓ — displaying"
                is AdViewModel.AdState.Shown -> "Banner ad displayed (MRC impression fired)"
                is AdViewModel.AdState.Error -> "Error: ${s.error.message}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = { viewModel.loadBanner() },
            enabled = state !is AdViewModel.AdState.Loading,
        ) {
            Text("Load Banner")
        }

        // Embed the real Android BannerAdView (WebView) inside the Compose layout.
        AndroidView(
            factory = { bannerView },
            modifier = Modifier
                .width(320.dp)
                .height(50.dp),
        )
    }
}
