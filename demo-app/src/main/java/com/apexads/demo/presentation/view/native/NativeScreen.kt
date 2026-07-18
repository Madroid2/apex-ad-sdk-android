package com.apexads.demo.presentation.view.native

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.IntentContext
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
        Text("Intent-to-Action Card", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Native 1.2 assets · coarse journey intent · executable Wallet CTA · native fallback",
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
                    .intentContext(
                        IntentContext.builder("apex-commerce-1", "travel.hotel")
                            .journeyStage(IntentContext.JourneyStage.READY_TO_ACT)
                            .displayLabel("Relevant to your hotel search")
                            .supports(IntentContext.ActionType.SAVE_TO_WALLET)
                            .build(),
                    )
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
    LaunchedEffect(ad) { ad.recordActionRendered() }

    Card(
        // Whole-card tap navigates, matching native-ad UX and the SDK's
        // NativeAdView behaviour. Clicks route through the SDK so tracking +
        // the onNativeAdClicked callback fire.
        modifier = Modifier
            .fillMaxWidth()
            .clickable { ad.handleClick(context) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${ad.disclosureText ?: "Sponsored"} · ${ad.advertiserName ?: "Advertiser"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Why this ad?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            ad.intentLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            RoundedCornerShape(50),
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Spacer(Modifier.height(10.dp))
            }

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
                            text = ad.advertiserName ?: "Advertiser",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        ad.actionBadgeText?.let { badge ->
                            Text(
                                text = "✓ $badge",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF18794E),
                            )
                        }
                    }
                }

                Text(
                    text = ad.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = ad.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Button(
                    onClick = {
                        if (ad.hasIntentAction()) ad.performAction(context)
                        else ad.handleClick(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(ad.actionCtaText ?: ad.ctaText ?: "Learn More")
                }

                Text(
                    text = "View offer",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { ad.handleClick(context) }
                        .padding(8.dp),
                )
                Text(
                    text = "Offer terms apply",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }

    // The classic View binding remains mounted for Native 1.2 impression tracking;
    // Compose renders the card and explicitly records the optional action CTA above.
    val trackingView = remember(ad) {
        NativeAdView(context).also { nv -> ad.bindTo(nv) }
    }
    AndroidView(
        factory = { trackingView },
        modifier = Modifier.height(0.dp),
    )
}
