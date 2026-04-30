package com.apexads.demo.ui.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.apexads.demo.R
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.video.VideoAd
import com.apexads.sdk.video.VideoAdListener

class VideoFragment : Fragment() {

    private val viewModel: AdViewModel by activityViewModels()
    private var videoAd: VideoAd? = null

    private lateinit var loadButton: Button
    private lateinit var showButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var rewardText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_video, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadButton = view.findViewById(R.id.btn_load_video)
        showButton = view.findViewById(R.id.btn_show_video)
        statusText = view.findViewById(R.id.tv_status)
        progressBar = view.findViewById(R.id.progress_bar)
        rewardText = view.findViewById(R.id.tv_reward)

        showButton.isEnabled = false
        rewardText.visibility = View.GONE

        viewModel.videoState.observe(viewLifecycleOwner) { state ->
            when (state) {
                AdViewModel.AdState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    loadButton.isEnabled = false
                    showButton.isEnabled = false
                    statusText.text = "Loading VAST video ad…"
                    rewardText.visibility = View.GONE
                }
                AdViewModel.AdState.Loaded -> {
                    progressBar.visibility = View.GONE
                    loadButton.isEnabled = true
                    showButton.isEnabled = true
                    statusText.text = "Rewarded video ready ✓ — tap Show"
                }
                AdViewModel.AdState.Shown -> {
                    showButton.isEnabled = false
                    statusText.text = "Video playing…"
                }
                is AdViewModel.AdState.Error -> {
                    progressBar.visibility = View.GONE
                    loadButton.isEnabled = true
                    showButton.isEnabled = false
                    statusText.text = "Error: ${state.error.message}"
                }
                AdViewModel.AdState.Idle -> {
                    statusText.text = "Tap Load to fetch a rewarded video"
                }
            }
        }

        loadButton.setOnClickListener { loadVideoAd() }
        showButton.setOnClickListener {
            videoAd?.show(requireContext())
        }
    }

    private fun loadVideoAd() {
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
                    viewModel.log("Video", "onVideoAdStarted — firing START tracker")
                }
                override fun onVideoAdCompleted() {
                    viewModel.log("Video", "onVideoAdCompleted — COMPLETE tracker fired")
                }
                override fun onVideoAdSkipped() {
                    viewModel.log("Video", "onVideoAdSkipped")
                }
                override fun onRewardEarned() {
                    viewModel.log("Video", "onRewardEarned 🎉")
                    rewardText.visibility = View.VISIBLE
                    rewardText.text = "🎉 Reward granted!"
                }
            })
            .build()

        videoAd?.load()
    }
}
