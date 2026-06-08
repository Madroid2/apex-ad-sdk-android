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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.banner.BannerAd
import com.apexads.sdk.banner.BannerAdListener
import com.apexads.sdk.banner.BannerAdView
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.AdSize

@Composable
fun BannerScreen(viewModel: AdViewModel) {
    val state by viewModel.bannerState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // BannerAdView is a real Android View (WebView-based). Persist it across recompositions
    // so the rendered creative survives state changes.
    val bannerView = remember { BannerAdView(context) }
    var bannerAd by remember { mutableStateOf<BannerAd?>(null) }

    // Show the ad exactly once per Loaded transition.
    LaunchedEffect(state) {
        if (state is AdViewModel.AdState.Loaded) {
            bannerAd?.show(bannerView)
        }
    }

    // Destroy the WebView when the screen leaves the composition.
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
            onClick = {
                viewModel.onBannerLoading()
                viewModel.log("Banner", "Loading 320×50 banner ad…")
                bannerAd = BannerAd.Builder("demo-banner-placement")
                    .adSize(AdSize.BANNER_320x50)
                    .listener(object : BannerAdListener {
                        override fun onAdLoaded() {
                            viewModel.onBannerLoaded()
                            viewModel.log("Banner", "onAdLoaded ✓")
                        }
                        override fun onAdFailed(error: AdError) {
                            viewModel.onBannerError(error)
                            viewModel.log("Banner", "onAdFailed: ${error.message}")
                        }
                        override fun onAdClicked() = viewModel.log("Banner", "onAdClicked")
                        override fun onAdImpression() {
                            viewModel.onBannerShown()
                            viewModel.log("Banner", "onAdImpression (MRC threshold met)")
                        }
                    })
                    .build()
                    .also { it.load() }
            },
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
