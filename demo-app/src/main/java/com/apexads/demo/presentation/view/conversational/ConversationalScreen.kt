package com.apexads.demo.presentation.view.conversational

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.conversational.ConversationalAd
import com.apexads.sdk.conversational.ConversationalAdListener
import com.apexads.sdk.conversational.SponsoredSuggestion
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.IntentContext
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
 * Demo of the Conversational Sponsored Suggestion format: a mock in-app trip
 * assistant whose organic messages stay untouched while an Apex suggestion card
 * renders between them. The ad never merges into assistant text (answer
 * independence) and only structured [IntentContext] taxonomy is sent — never
 * the chat content itself.
 */
@Composable
fun ConversationalScreen(viewModel: AdViewModel) {
    val state by viewModel.conversationalState.collectAsStateWithLifecycle()
    var conversationalAd by remember { mutableStateOf<ConversationalAd?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Conversational Sponsored Suggestion", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Native 1.2 assets re-flowed for an assistant surface · surface=\"assistant\" · executable action · answer independence",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AnimatedVisibility(visible = state is AdViewModel.AdState.Loading) {
            LinearProgressIndicator(modifier = Modifier.width(240.dp))
        }

        Button(
            onClick = {
                viewModel.onConversationalLoading()
                viewModel.log("Conversational", "Loading sponsored suggestion…")
                ConversationalAd.Builder("demo-assistant-inline")
                    .intentContext(
                        IntentContext.builder("apex-commerce-1", "travel.hotel")
                            .journeyStage(IntentContext.JourneyStage.READY_TO_ACT)
                            .displayLabel("Relevant to your hotel search")
                            .supports(IntentContext.ActionType.SAVE_TO_WALLET)
                            .build(),
                    )
                    .listener(object : ConversationalAdListener {
                        override fun onSuggestionReady(ad: ConversationalAd) {
                            conversationalAd = ad
                            viewModel.onConversationalLoaded()
                            viewModel.log(
                                "Conversational",
                                "onSuggestionReady: title='${ad.suggestion?.title}'",
                            )
                        }
                        override fun onSuggestionFailed(error: AdError) {
                            viewModel.onConversationalError(error)
                            viewModel.log("Conversational", "onSuggestionFailed: ${error.message}")
                        }
                        override fun onSuggestionClicked() =
                            viewModel.log("Conversational", "onSuggestionClicked")
                        override fun onActionCompleted() =
                            viewModel.log("Conversational", "onActionCompleted")
                        override fun onActionCancelled() =
                            viewModel.log("Conversational", "onActionCancelled")
                        override fun onActionFailed() =
                            viewModel.log("Conversational", "onActionFailed")
                    })
                    .build()
                    .load()
            },
            enabled = state !is AdViewModel.AdState.Loading,
        ) {
            Text("Load Sponsored Suggestion")
        }

        MockAssistantThread(
            ad = conversationalAd.takeIf { state is AdViewModel.AdState.Loaded },
        )
    }
}

@Composable
private fun MockAssistantThread(ad: ConversationalAd?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Trip assistant",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            UserBubble("Weekend stay in Bengaluru, 6–8 Sep, under ₹4,000?")
            AssistantBubble(
                "Found 12 stays near Bengaluru. Olive Hotel, Koramangala is the best fit — 4.5 (812), ₹3,299 per night.",
            )

            val suggestion = ad?.suggestion
            if (ad != null && suggestion != null) {
                SponsoredSuggestionCard(ad = ad, suggestion = suggestion)
            }

            AssistantBubble("Want me to compare Olive Hotel with two similar stays?")
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp, 14.dp, 2.dp, 14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .widthIn(max = 300.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(14.dp, 14.dp, 14.dp, 2.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/**
 * The Sponsored Suggestion card: structurally distinct from chat bubbles (full
 * border + tinted disclosure header), rendered between organic messages.
 */
@Composable
private fun SponsoredSuggestionCard(ad: ConversationalAd, suggestion: SponsoredSuggestion) {
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
                RoundedCornerShape(14.dp),
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable { ad.handleClick(context) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${suggestion.disclosure} · ${suggestion.advertiserName ?: "Advertiser"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Why this ad?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Column(
            modifier = Modifier.padding(10.dp),
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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Row(verticalAlignment = Alignment.Top) {
                thumbBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Suggestion thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 84.dp, height = 64.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
    Spacer(Modifier.height(0.dp))
}
