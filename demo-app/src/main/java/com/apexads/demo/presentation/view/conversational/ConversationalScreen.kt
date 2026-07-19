package com.apexads.demo.presentation.view.conversational

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.conversational.ConversationalAd
import com.apexads.sdk.conversational.ConversationalAdListener
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.IntentContext

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
    val context = LocalContext.current
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

        OutlinedButton(
            onClick = {
                context.startActivity(Intent(context, TripAssistantActivity::class.java))
            },
        ) {
            Text("Open Trip Assistant — production integration")
        }

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
