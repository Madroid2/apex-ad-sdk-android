package com.apexads.demo.presentation.view.conversational

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apexads.sdk.conversational.ConversationalAd
import com.apexads.sdk.conversational.SponsoredSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

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

/**
 * The production render of a Conversational Sponsored Suggestion.
 *
 * This is the exact card contract publishers ship: structurally distinct from
 * organic chat bubbles (full border + tinted disclosure header), rendered
 * *between* messages — never inside an assistant answer. All fields come from
 * [SponsoredSuggestion]; clicks and the executable action route through the
 * SDK so tracking fires correctly.
 */
@Composable
fun SponsoredSuggestionCard(ad: ConversationalAd, suggestion: SponsoredSuggestion) {
    val context = LocalContext.current
    val thumbBitmap by rememberBitmapFromUrl(suggestion.thumbnailUrl)
    val iconBitmap by rememberBitmapFromUrl(suggestion.iconUrl)
    LaunchedEffect(ad) { ad.recordSuggestionRendered() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { ad.handleClick(context) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${suggestion.disclosure} · ${suggestion.advertiserName ?: "Advertiser"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Why this ad?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestion.relevanceLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }

            Row(verticalAlignment = Alignment.Top) {
                thumbBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Suggestion thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 88.dp, height = 66.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        iconBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Advertiser icon",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = listOfNotNull(suggestion.advertiserName, suggestion.badgeText)
                                .joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = suggestion.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = {
                    if (suggestion.hasAction) ad.performAction(context)
                    else ad.handleClick(context)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(suggestion.actionCtaText ?: suggestion.fallbackCtaText)
            }
            if (suggestion.hasAction) {
                Text(
                    text = suggestion.fallbackCtaText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { ad.handleClick(context) }
                        .padding(4.dp),
                )
            }
            Text(
                text = "Offer terms apply",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
