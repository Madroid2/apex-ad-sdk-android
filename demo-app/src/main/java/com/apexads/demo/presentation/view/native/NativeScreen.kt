package com.apexads.demo.presentation.view.native

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.nativeads.NativeAd
import com.apexads.sdk.nativeads.NativeAdListener
import com.apexads.sdk.nativeads.NativeAdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Loads a [Bitmap] from [url] asynchronously using [produceState] + [Dispatchers.IO].
 * Returns [State] of the Bitmap (null while loading or on error).
 */
@Composable
private fun rememberBitmapFromUrl(url: String?): State<Bitmap?> = produceState<Bitmap?>(
    initialValue = null,
    key1 = url,
) {
    if (url == null) return@produceState
    withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection().apply { connect() }
            value = BitmapFactory.decodeStream(conn.getInputStream())
        } catch (_: Exception) { /* leave null */ }
    }
}

@Composable
fun NativeScreen(viewModel: AdViewModel) {
    val state by viewModel.nativeState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Native Ad (IAB Native 1.2)", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Publisher-controlled layout · OpenRTB native request · org.json asset parsing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AnimatedVisibility(visible = state is AdViewModel.AdState.Loading) {
            LinearProgressIndicator(modifier = Modifier.width(240.dp))
        }

        Text(
            text = when (val s = state) {
                is AdViewModel.AdState.Idle -> "Tap Load to fetch a native ad"
                is AdViewModel.AdState.Loading -> "Loading native ad…"
                is AdViewModel.AdState.Loaded -> "Native ad loaded ✓"
                is AdViewModel.AdState.Error -> "Error: ${s.error.message}"
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = {
                viewModel.onNativeLoading()
                viewModel.log("Native", "Loading native ad…")
                NativeAd.Builder("demo-native-placement")
                    .listener(object : NativeAdListener {
                        override fun onNativeAdLoaded(ad: NativeAd) {
                            nativeAd = ad
                            viewModel.onNativeLoaded()
                            viewModel.log("Native", "onNativeAdLoaded: title='${ad.title}'")
                        }
                        override fun onNativeAdFailed(error: AdError) {
                            viewModel.onNativeError(error)
                            viewModel.log("Native", "onNativeAdFailed: ${error.message}")
                        }
                        override fun onNativeAdClicked() =
                            viewModel.log("Native", "onNativeAdClicked")
                    })
                    .build()
                    .load()
            },
            enabled = state !is AdViewModel.AdState.Loading,
        ) {
            Text("Load Native Ad")
        }

        // Render the native ad card in pure Compose once loaded.
        // Images are loaded asynchronously via produceState + Dispatchers.IO (no Glide/Coil).
        val ad = nativeAd
        AnimatedVisibility(visible = ad != null && state is AdViewModel.AdState.Loaded) {
            if (ad != null) {
                NativeAdCard(ad = ad, context = context)
            }
        }
    }
}

@Composable
private fun NativeAdCard(ad: NativeAd, context: android.content.Context) {
    val mainBitmap by rememberBitmapFromUrl(ad.imageUrl)
    val iconBitmap by rememberBitmapFromUrl(ad.iconUrl)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            // Main creative image
            mainBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Ad main image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Icon + title row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    iconBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Ad icon",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = ad.title ?: "",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = ad.advertiserName ?: "Sponsored",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    text = ad.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Button(
                    onClick = { /* handled by SDK via NativeAdView binding below */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(ad.ctaText ?: "Learn More")
                }
            }
        }
    }

    // Zero-size AndroidView that binds the NativeAdView for SDK click/impression tracking.
    // The Compose card above handles display; the SDK view handles event registration.
    val trackingView = remember(ad) {
        NativeAdView(context).also { nv -> ad.bindTo(nv) }
    }
    AndroidView(
        factory = { trackingView },
        modifier = Modifier.height(0.dp),
    )
}
