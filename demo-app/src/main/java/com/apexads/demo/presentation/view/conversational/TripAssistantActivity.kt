package com.apexads.demo.presentation.view.conversational

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apexads.demo.ui.theme.ApexTheme
import com.apexads.sdk.conversational.ConversationalAd
import com.apexads.sdk.conversational.ConversationalAdListener
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.IntentContext
import kotlinx.coroutines.delay

/**
 * Production-shaped reference integration of the Conversational Sponsored
 * Suggestion format: a publisher's in-app AI assistant ("Trip assistant")
 * rendering an Apex suggestion card *between* organic messages.
 *
 * What this demonstrates, exactly as a publisher would ship it:
 *  - The assistant conversation is fully owned by the app; the SDK never
 *    sees message text. When the journey reaches a commercial category the
 *    app declares a coarse [IntentContext] and loads a [ConversationalAd].
 *  - On fill, the card is inserted into the thread as its own item —
 *    disclosed, bordered, visually distinct (answer independence).
 *  - On no-fill or failure the thread simply continues; nothing renders.
 */
class TripAssistantActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApexTheme {
                TripAssistantChatScreen(onBack = { finish() })
            }
        }
    }
}

private sealed interface ChatItem {
    data class User(val text: String) : ChatItem
    data class Assistant(val text: String) : ChatItem
    object Typing : ChatItem
    data class Sponsored(val ad: ConversationalAd) : ChatItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripAssistantChatScreen(onBack: () -> Unit) {
    val items = remember { mutableStateListOf<ChatItem>() }
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var adInFlight by remember { mutableStateOf<ConversationalAd?>(null) }

    // Scripted opening conversation, then the production ad flow: once the
    // journey hits a commercial category, declare the coarse intent and load.
    LaunchedEffect(Unit) {
        delay(500)
        items += ChatItem.User("Weekend stay in Bengaluru, 6–8 Sep, under ₹4,000?")
        items += ChatItem.Typing
        delay(1200)
        items.remove(ChatItem.Typing)
        items += ChatItem.Assistant(
            "Found 12 stays near Bengaluru. Olive Hotel, Koramangala is the best fit — " +
                "4.5 (812), ₹3,299 per night.",
        )

        val ad = ConversationalAd.Builder("assistant-inline-1")
            .intentContext(
                IntentContext.builder("apex-commerce-1", "travel.hotel")
                    .journeyStage(IntentContext.JourneyStage.READY_TO_ACT)
                    .displayLabel("Relevant to your hotel search")
                    .supports(IntentContext.ActionType.SAVE_TO_WALLET)
                    .build(),
            )
            .listener(object : ConversationalAdListener {
                override fun onSuggestionReady(readyAd: ConversationalAd) {
                    // Insert the disclosed card between organic messages.
                    items += ChatItem.Sponsored(readyAd)
                }
                override fun onSuggestionFailed(error: AdError) {
                    // Production behaviour on no-fill: the conversation just
                    // continues — the surface is never degraded by an ad slot.
                }
            })
            .build()
        adInFlight = ad
        ad.load()

        delay(2200)
        items += ChatItem.Assistant("Want me to compare Olive Hotel with two similar stays?")
    }

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) listState.animateScrollToItem(items.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Luggage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Trip assistant", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "TravelApp",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Ask about your trip…") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotEmpty()) {
                            items += ChatItem.User(text)
                            items += ChatItem.Assistant(
                                "I can help with that — in this demo the thread is " +
                                    "scripted, but the suggestion card above is a live " +
                                    "Apex Conversational ad.",
                            )
                            input = ""
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items) { item ->
                when (item) {
                    is ChatItem.User -> UserBubble(item.text)
                    is ChatItem.Assistant -> AssistantBubble(item.text)
                    ChatItem.Typing -> AssistantBubble("…")
                    is ChatItem.Sponsored -> {
                        val suggestion = item.ad.suggestion
                        if (suggestion != null) {
                            SponsoredSuggestionCard(ad = item.ad, suggestion = suggestion)
                        }
                    }
                }
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { adInFlight?.destroy() }
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
                .widthIn(max = 300.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
