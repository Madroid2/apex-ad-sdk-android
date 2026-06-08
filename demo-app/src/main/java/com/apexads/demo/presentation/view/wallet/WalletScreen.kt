package com.apexads.demo.presentation.view.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.apexads.sdk.banner.BannerAd
import com.apexads.sdk.banner.BannerAdListener
import com.apexads.sdk.banner.BannerAdView
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.AdSize
import com.apexads.sdk.interstitial.InterstitialAd
import com.apexads.sdk.interstitial.InterstitialAdListener

/**
 * Demonstrates the Google Wallet pass CTA embedded inside existing ad formats.
 *
 * No dedicated WalletAd API is needed — the "Save to Google Wallet" panel appears
 * automatically inside Interstitial and MRECT Banner when:
 *  1. [WalletAdExtension.install] was called in DemoApplication.
 *  2. The bid response carries an ext.wallet block (MockAdExchange sets this automatically
 *     for any request that includes wallet_supported=true, which the SDK signals for
 *     Interstitial and MRECT Banner when sdk-wallet is installed).
 */
@Composable
fun WalletScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Google Wallet Pass Ads", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "The \"Save to Google Wallet\" CTA is embedded inside existing Interstitial " +
                "and MRECT Banner formats. No separate WalletAd API needed — wallet_supported=true " +
                "is signalled automatically when sdk-wallet is installed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        WalletInterstitialSection()

        HorizontalDivider()

        WalletMrectSection()
    }
}

// ── Section 1: Interstitial + Wallet CTA ──────────────────────────────────────

private data class WalletInterstitialState(
    val statusText: String = "Tap Load to fetch",
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val walletResult: String? = null,
)

@Composable
private fun WalletInterstitialSection() {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf(WalletInterstitialState()) }
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    DisposableEffect(Unit) {
        onDispose { interstitialAd = null }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Interstitial + Wallet CTA", style = MaterialTheme.typography.titleMedium)

        AnimatedVisibility(visible = uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.width(240.dp))
        }

        Text(uiState.statusText, style = MaterialTheme.typography.bodyMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    uiState = WalletInterstitialState(statusText = "Loading…", isLoading = true)
                    interstitialAd = InterstitialAd.Builder("demo-wallet-interstitial")
                        .listener(object : InterstitialAdListener {
                            override fun onInterstitialLoaded() {
                                uiState = WalletInterstitialState(
                                    statusText = "Ready ✓ — tap Show (wallet CTA will appear)",
                                    isReady = true,
                                )
                            }
                            override fun onInterstitialFailed(error: AdError) {
                                uiState = WalletInterstitialState(
                                    statusText = "Failed: ${error.message}",
                                )
                            }
                            override fun onInterstitialShown() {
                                uiState = uiState.copy(statusText = "Interstitial showing…", isReady = false)
                            }
                            override fun onInterstitialClosed() {
                                uiState = uiState.copy(statusText = "Closed — tap Load to fetch another")
                            }
                            override fun onWalletPassSaved() {
                                uiState = uiState.copy(walletResult = "🎉 Pass saved to Google Wallet!")
                            }
                            override fun onWalletPassCancelled() {
                                uiState = uiState.copy(walletResult = "Wallet save cancelled")
                            }
                            override fun onWalletPassFailed() {
                                uiState = uiState.copy(
                                    walletResult = "⚠ Wallet save failed — replace the placeholder JWT " +
                                        "in MockAdExchange with a real signed JWT to test.",
                                )
                            }
                        })
                        .build()
                        .also { it.load() }
                },
                enabled = !uiState.isLoading,
            ) { Text("Load") }

            OutlinedButton(
                onClick = { interstitialAd?.show(context) },
                enabled = uiState.isReady,
            ) { Text("Show") }
        }

        AnimatedVisibility(visible = uiState.walletResult != null) {
            uiState.walletResult?.let { result ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }
}

// ── Section 2: MRECT Banner (300×250) + Wallet CTA ───────────────────────────

private data class WalletMrectState(
    val statusText: String = "Tap Load to fetch",
    val isLoading: Boolean = false,
    val walletResult: String? = null,
)

@Composable
private fun WalletMrectSection() {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf(WalletMrectState()) }
    var bannerAd by remember { mutableStateOf<BannerAd?>(null) }
    val bannerView = remember { BannerAdView(context) }

    DisposableEffect(Unit) {
        onDispose {
            bannerView.destroy()
            bannerAd = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("MRECT Banner (300×250) + Wallet CTA", style = MaterialTheme.typography.titleMedium)

        AnimatedVisibility(visible = uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.width(240.dp))
        }

        Text(uiState.statusText, style = MaterialTheme.typography.bodyMedium)

        Button(
            onClick = {
                uiState = WalletMrectState(statusText = "Loading MRECT banner…", isLoading = true)
                // Build first, then assign, then load — so onAdLoaded() can safely call show().
                val ad = BannerAd.Builder("demo-wallet-mrect")
                    .adSize(AdSize.MRECT_300x250)
                    .listener(object : BannerAdListener {
                        override fun onAdLoaded() {
                            bannerAd?.show(bannerView)
                            uiState = WalletMrectState(
                                statusText = "MRECT ready ✓ (wallet CTA strip at the bottom)",
                            )
                        }
                        override fun onAdFailed(error: AdError) {
                            uiState = WalletMrectState(statusText = "Failed: ${error.message}")
                        }
                        override fun onAdClicked() {
                            uiState = uiState.copy(statusText = "Banner clicked")
                        }
                        override fun onWalletPassSaved() {
                            uiState = uiState.copy(walletResult = "🎉 Pass saved to Google Wallet!")
                        }
                        override fun onWalletPassCancelled() {
                            uiState = uiState.copy(walletResult = "Wallet save cancelled")
                        }
                        override fun onWalletPassFailed() {
                            uiState = uiState.copy(
                                walletResult = "⚠ Wallet save failed — replace the placeholder JWT " +
                                    "in MockAdExchange with a real signed JWT to test.",
                            )
                        }
                    })
                    .build()
                bannerAd = ad
                ad.load()
            },
            enabled = !uiState.isLoading,
        ) { Text("Load MRECT") }

        // Embed the 300×250 BannerAdView (WebView) inside the Compose layout.
        AndroidView(
            factory = { bannerView },
            modifier = Modifier
                .width(300.dp)
                .height(250.dp),
        )

        AnimatedVisibility(visible = uiState.walletResult != null) {
            uiState.walletResult?.let { result ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }
}
