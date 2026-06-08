package com.apexads.demo.presentation.view.appopen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apexads.sdk.appopen.AppOpenAd

/**
 * App Open Ads are managed globally by [DemoApplication] — this screen shows the current
 * readiness status and lets QA toggle the feature without restarting the app.
 *
 * Trigger the ad: press Home → switch back to the app.
 * [AppOpenAd] detects the foreground transition and shows the fullscreen interstitial.
 */
@Composable
fun AppOpenScreen() {
    var isEnabled by remember { mutableStateOf(true) }
    var isReady by remember { mutableStateOf(AppOpenAd.isAdReady()) }

    // Refresh readiness whenever the screen resumes — equivalent to Fragment.onResume().
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isReady = AppOpenAd.isAdReady()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("App Open Ads", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Only Google AdMob offers App Open as a first-class ad format.\n" +
                "ApexAds brings the same capability — natively, via OpenRTB.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Status card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isReady)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (isReady)
                    "✓  Ad ready — background the app to trigger"
                else
                    "⏳  Preloading… (fetched on first launch)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isReady)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp),
            )
        }

        // How-to-trigger instructions
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "How to trigger:\n" +
                    "1. Confirm status shows \"Ad ready\"\n" +
                    "2. Press the Home button (or Recents)\n" +
                    "3. Tap the app icon to return — the ad appears automatically",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
            )
        }

        // Enable / disable toggle
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text("App Open Ads enabled", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = isEnabled,
                onCheckedChange = { checked ->
                    isEnabled = checked
                    AppOpenAd.setEnabled(checked)
                    isReady = AppOpenAd.isAdReady()
                },
            )
        }

        Button(onClick = { isReady = AppOpenAd.isAdReady() }) {
            Text("Refresh Status")
        }
    }
}
