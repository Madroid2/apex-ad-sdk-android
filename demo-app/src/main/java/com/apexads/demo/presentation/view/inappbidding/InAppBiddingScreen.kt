package com.apexads.demo.presentation.view.inappbidding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.inappbidding.mock.MockMediationPlatform

/**
 * Demonstrates the in-app bidding (header bidding) flow:
 *  1. [AdViewModel.fetchBid] wraps [ApexInAppBidder.fetchBidToken] in a
 *     [kotlinx.coroutines.suspendCancellableCoroutine] and runs it inside
 *     [androidx.lifecycle.viewModelScope].
 *  2. [AdViewModel.bidState] emits [AdViewModel.BidUiState] updates — the UI
 *     collects them via [collectAsStateWithLifecycle].
 *  3. [MockMediationPlatform] simulates the MAX/LevelPlay waterfall auction result.
 */
@Composable
fun InAppBiddingScreen(viewModel: AdViewModel) {
    val bidState by viewModel.bidState.collectAsStateWithLifecycle()
    // MockMediationPlatform is demo-only; survive recompositions with remember.
    val mockPlatform = remember { MockMediationPlatform() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("In-App Bidding (Header Bidding)", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "ApexAds fetches a price signal before the mediation SDK runs its waterfall.\n" +
                "Compatible with AppLovin MAX and Unity LevelPlay.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Step 1 — Fetch bid token
        Button(
            onClick = { viewModel.fetchBid() },
            enabled = bidState !is AdViewModel.BidUiState.Fetching,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("1.  Fetch ApexAds Bid Token")
        }

        AnimatedVisibility(visible = bidState is AdViewModel.BidUiState.Fetching) {
            LinearProgressIndicator(modifier = Modifier.width(240.dp))
        }

        // Step 2 — Simulate auction (enabled only once bid is ready)
        OutlinedButton(
            onClick = {
                val ready = bidState as? AdViewModel.BidUiState.Ready ?: return@OutlinedButton
                viewModel.simulateAuction(ready.token, mockPlatform)
            },
            enabled = bidState is AdViewModel.BidUiState.Ready,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("2.  Simulate Mediation Auction")
        }

        // Result card — transitions between bid states
        AnimatedContent(targetState = bidState, label = "bid-state") { state ->
            when (state) {
                is AdViewModel.BidUiState.Idle -> Unit

                is AdViewModel.BidUiState.Fetching ->
                    InfoCard("Fetching bid from ApexAds…")

                is AdViewModel.BidUiState.Ready ->
                    InfoCard(
                        "Bid ready!\n" +
                            "CPM:    \$${"%,.3f".format(state.token.cpmUsd)}\n" +
                            "Token:  ${state.token.token.take(28)}…\n\n" +
                            "Tap step 2 to simulate the mediation auction.",
                        highlight = true,
                    )

                is AdViewModel.BidUiState.Failed ->
                    InfoCard("Bid failed: ${state.message}\n\nMediation waterfall proceeds without ApexAds price signal.")

                is AdViewModel.BidUiState.AuctionResult ->
                    InfoCard(
                        "Auction result\n" +
                            "Winner:  ${state.winner}\n" +
                            "CPM:     \$${"%,.3f".format(state.cpm)}\n\n" +
                            "(In production MAX/LevelPlay runs this server-side.)",
                        highlight = true,
                    )
            }
        }
    }
}

@Composable
private fun InfoCard(text: String, highlight: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (highlight)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}
