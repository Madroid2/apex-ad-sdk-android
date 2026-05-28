package com.apexads.demo.ui.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.video.VideoAd
import com.apexads.sdk.video.VideoAdListener

@Composable
fun VideoScreen(viewModel: AdViewModel) {
    val state by viewModel.videoState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var videoAd by remember { mutableStateOf<VideoAd?>(null) }
    var rewardEarned by remember { mutableStateOf(false) }

    // Reset reward badge whenever a new load starts.
    LaunchedEffect(state) {
        if (state is AdViewModel.AdState.Loading) rewardEarned = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Rewarded Video (VAST 4.0)", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "VAST 4.0 XML parsing · ExoPlayer · quartile trackers · reward on completion",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AnimatedVisibility(visible = state is AdViewModel.AdState.Loading) {
            LinearProgressIndicator(modifier = Modifier.width(240.dp))
        }

        Text(
            text = when (val s = state) {
                is AdViewModel.AdState.Idle -> "Tap Load to fetch a rewarded video"
                is AdViewModel.AdState.Loading -> "Loading VAST 4.0 video ad…"
                is AdViewModel.AdState.Loaded -> "Rewarded video ready ✓ — tap Show"
                is AdViewModel.AdState.Shown -> "Video playing…"
                is AdViewModel.AdState.Error -> "Error: ${s.error.message}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    rewardEarned = false
                    viewModel.onVideoLoading()
                    viewModel.log("Video", "Loading VAST 4.0 rewarded video…")
                    videoAd = VideoAd.Builder("demo-video-placement")
                        .listener(object : VideoAdListener {
                            override fun onVideoAdLoaded() {
                                viewModel.onVideoLoaded()
                                viewModel.log("Video", "onVideoAdLoaded ✓")
                            }
                            override fun onVideoAdFailed(error: AdError) {
                                viewModel.onVideoError(error)
                                viewModel.log("Video", "onVideoAdFailed: ${error.message}")
                            }
                            override fun onVideoAdStarted() {
                                viewModel.onVideoShown()
                                viewModel.log("Video", "onVideoAdStarted — START tracker fired")
                            }
                            override fun onVideoAdCompleted() =
                                viewModel.log("Video", "onVideoAdCompleted — COMPLETE tracker fired")
                            override fun onVideoAdSkipped() =
                                viewModel.log("Video", "onVideoAdSkipped")
                            override fun onRewardEarned() {
                                rewardEarned = true
                                viewModel.log("Video", "onRewardEarned 🎉")
                            }
                        })
                        .build()
                        .also { it.load() }
                },
                enabled = state !is AdViewModel.AdState.Loading,
            ) {
                Text("Load")
            }

            OutlinedButton(
                onClick = { videoAd?.show(context) },
                enabled = state is AdViewModel.AdState.Loaded,
            ) {
                Text("Show")
            }
        }

        AnimatedVisibility(visible = rewardEarned) {
            Text(
                text = "🎉 Reward granted!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
