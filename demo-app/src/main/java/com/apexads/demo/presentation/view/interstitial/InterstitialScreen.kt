package com.apexads.demo.presentation.view.interstitial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.interstitial.InterstitialAd
import com.apexads.sdk.interstitial.InterstitialAdListener

@Composable
fun InterstitialScreen(viewModel: AdViewModel) {
    val state by viewModel.interstitialState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Interstitial Ad", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Fullscreen HTML · MRAID 3.0 · renders in a separate Activity",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AnimatedVisibility(visible = state is AdViewModel.AdState.Loading) {
            LinearProgressIndicator(modifier = Modifier.width(240.dp))
        }

        Text(
            text = when (val s = state) {
                is AdViewModel.AdState.Idle -> "Tap Load to pre-fetch the interstitial"
                is AdViewModel.AdState.Loading -> "Loading interstitial…"
                is AdViewModel.AdState.Loaded -> "Interstitial ready ✓ — tap Show"
                is AdViewModel.AdState.Shown -> "Interstitial shown"
                is AdViewModel.AdState.Error -> "Error: ${s.error.message}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.onInterstitialLoading()
                    viewModel.log("Interstitial", "Loading fullscreen interstitial…")
                    interstitialAd = InterstitialAd.Builder("demo-interstitial-placement")
                        .listener(object : InterstitialAdListener {
                            override fun onInterstitialLoaded() {
                                viewModel.onInterstitialLoaded()
                                viewModel.log("Interstitial", "onInterstitialLoaded ✓")
                            }
                            override fun onInterstitialFailed(error: AdError) {
                                viewModel.onInterstitialError(error)
                                viewModel.log("Interstitial", "onInterstitialFailed: ${error.message}")
                            }
                            override fun onInterstitialShown() {
                                viewModel.onInterstitialShown()
                                viewModel.log("Interstitial", "onInterstitialShown")
                            }
                            override fun onInterstitialClosed() =
                                viewModel.log("Interstitial", "onInterstitialClosed")
                            override fun onInterstitialClicked() =
                                viewModel.log("Interstitial", "onInterstitialClicked")
                        })
                        .build()
                        .also { it.load() }
                },
                enabled = state !is AdViewModel.AdState.Loading,
            ) {
                Text("Load")
            }

            OutlinedButton(
                onClick = { interstitialAd?.show(context) },
                enabled = state is AdViewModel.AdState.Loaded,
            ) {
                Text("Show")
            }
        }
    }
}
