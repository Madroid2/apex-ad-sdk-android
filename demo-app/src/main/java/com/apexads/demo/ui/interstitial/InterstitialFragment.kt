package com.apexads.demo.ui.interstitial

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
import com.apexads.sdk.interstitial.InterstitialAd
import com.apexads.sdk.interstitial.InterstitialAdListener

class InterstitialFragment : Fragment() {

    private val viewModel: AdViewModel by activityViewModels()
    private var interstitialAd: InterstitialAd? = null

    private lateinit var loadButton: Button
    private lateinit var showButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_interstitial, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadButton = view.findViewById(R.id.btn_load_interstitial)
        showButton = view.findViewById(R.id.btn_show_interstitial)
        statusText = view.findViewById(R.id.tv_status)
        progressBar = view.findViewById(R.id.progress_bar)

        showButton.isEnabled = false

        viewModel.interstitialState.observe(viewLifecycleOwner) { state ->
            when (state) {
                AdViewModel.AdState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    loadButton.isEnabled = false
                    showButton.isEnabled = false
                    statusText.text = "Loading interstitial…"
                }
                AdViewModel.AdState.Loaded -> {
                    progressBar.visibility = View.GONE
                    loadButton.isEnabled = true
                    showButton.isEnabled = true
                    statusText.text = "Interstitial ready ✓ — tap Show"
                }
                AdViewModel.AdState.Shown -> {
                    showButton.isEnabled = false
                    statusText.text = "Interstitial shown"
                }
                is AdViewModel.AdState.Error -> {
                    progressBar.visibility = View.GONE
                    loadButton.isEnabled = true
                    showButton.isEnabled = false
                    statusText.text = "Error: ${state.error.message}"
                }
                AdViewModel.AdState.Idle -> {
                    statusText.text = "Tap Load to pre-fetch the interstitial"
                    loadButton.isEnabled = true
                }
            }
        }

        loadButton.setOnClickListener { loadInterstitialAd() }
        showButton.setOnClickListener {
            interstitialAd?.show(requireContext())
        }
    }

    private fun loadInterstitialAd() {
        viewModel.onInterstitialLoading()
        viewModel.log("Interstitial", "Loading fullscreen interstitial…")

        interstitialAd = InterstitialAd.Builder("demo-interstitial-placement")
            .listener(object : InterstitialAdListener {
                override fun onInterstitialLoaded() {
                    viewModel.onInterstitialLoaded()
                    viewModel.log("Interstitial", "onInterstitialLoaded ✓")
                }
                override fun onInterstitialFailed(error: AdError) {
                    viewModel.onInterstitialError(error)
                    viewModel.log("Interstitial", "onInterstitialFailed: ${error.message}")
                }
                override fun onInterstitialShown() {
                    viewModel.onInterstitialShown()
                    viewModel.log("Interstitial", "onInterstitialShown")
                }
                override fun onInterstitialClosed() {
                    viewModel.log("Interstitial", "onInterstitialClosed")
                }
                override fun onInterstitialClicked() {
                    viewModel.log("Interstitial", "onInterstitialClicked")
                }
            })
            .build()

        interstitialAd?.load()
    }
}
