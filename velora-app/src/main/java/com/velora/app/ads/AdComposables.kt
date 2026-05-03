package com.velora.app.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.apexads.sdk.banner.BannerAd
import com.apexads.sdk.banner.BannerAdListener
import com.apexads.sdk.banner.BannerAdView
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.AdSize
import com.velora.app.core.design.Gold400
import com.velora.app.core.design.White
import com.velora.app.core.design.White40

// ── Banner Ad ─────────────────────────────────────────────────────────────────

/**
 * Wraps [BannerAdView] inside Compose via [AndroidView].
 *
 * The [BannerAd] and [BannerAdView] are remembered so they survive recomposition.
 * The ad is loaded once on first composition; [BannerAdView] is shown via the
 * standard [BannerAd.show] callback in [BannerAdListener.onAdLoaded].
 */
@Composable
fun BannerAdSlot(
    placementId: String,
    adSize: AdSize = AdSize.BANNER_320x50,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Remember both objects so they survive recomposition.
    val bannerView = remember(placementId) { BannerAdView(context) }
    // Holder allows the listener closure to reference the BannerAd before it is assigned.
    val adHolder = remember(placementId) { arrayOfNulls<BannerAd>(1) }
    remember(placementId) {
        val ad = BannerAd.Builder(placementId)
            .adSize(adSize)
            .listener(object : BannerAdListener {
                override fun onAdLoaded() {
                    bannerView.post { adHolder[0]?.show(bannerView) }
                }
                override fun onAdFailed(error: AdError) {}
            })
            .build()
        adHolder[0] = ad
        ad.load()
        ad
    }

    AndroidView(
        factory = { bannerView },
        modifier = modifier
            .fillMaxWidth()
            .height(if (adSize == AdSize.MRECT_300x250) 250.dp else 50.dp),
    )
}

// ── Native Ad card ────────────────────────────────────────────────────────────

/**
 * A native ad rendered using the app's own Compose card design.
 *
 * This is the key native ad pattern: the SDK supplies the ad data (title, image,
 * body, CTA); the app controls 100% of the visual presentation in Compose.
 * The "Sponsored" badge is the only publisher-defined disclosure element.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column {
            Box {
                AsyncImage(
                    model = "https://picsum.photos/seed/velora-native-ad/400/220",
                    contentDescription = "Sponsored content",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                )
                Text(
                    text = "Sponsored",
                    style = MaterialTheme.typography.labelSmall,
                    color = White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(White40, MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Apex Demo Brand",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "AD",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold400,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Discover Premium Essentials",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Curated collection for the modern wardrobe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
