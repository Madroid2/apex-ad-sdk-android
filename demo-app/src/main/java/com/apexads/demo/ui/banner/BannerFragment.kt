package com.apexads.demo.ui.banner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.apexads.demo.MainActivity
import com.apexads.demo.R
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.banner.BannerAd
import com.apexads.sdk.banner.BannerAdListener
import com.apexads.sdk.banner.BannerAdView
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.AdSize

class BannerFragment : Fragment() {

    private val viewModel: AdViewModel by activityViewModels()
    private var bannerAd: BannerAd? = null

    private lateinit var bannerView: BannerAdView
    private lateinit var loadButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_banner, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bannerView = view.findViewById(R.id.banner_ad_view)
        loadButton = view.findViewById(R.id.btn_load_banner)
        statusText = view.findViewById(R.id.tv_status)
        progressBar = view.findViewById(R.id.progress_bar)

        viewModel.bannerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                AdViewModel.AdState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    loadButton.isEnabled = false
                    statusText.text = "Loading banner ad…"
                }
                AdViewModel.AdState.Loaded -> {
                    progressBar.visibility = View.GONE
                    statusText.text = "Banner ad loaded ✓ — showing"
                    bannerAd?.show(bannerView)
                }
                AdViewModel.AdState.Shown -> {
                    loadButton.isEnabled = true
                    statusText.text = "Banner ad displayed"
                }
                is AdViewModel.AdState.Error -> {
                    progressBar.visibility = View.GONE
                    loadButton.isEnabled = true
                    statusText.text = "Error: ${state.error.message}"
                }
                AdViewModel.AdState.Idle -> {
                    statusText.text = "Tap Load to fetch a banner ad"
                    loadButton.isEnabled = true
                }
            }
        }

        loadButton.setOnClickListener { loadBannerAd() }
    }

    private fun loadBannerAd() {
        viewModel.onBannerLoading()
        viewModel.log("Banner", "Loading 320×50 banner ad…")

        bannerAd = BannerAd.Builder("demo-banner-placement")
            .adSize(AdSize.BANNER_320x50)
            .listener(object : BannerAdListener {
                override fun onAdLoaded() {
                    viewModel.onBannerLoaded()
                    viewModel.log("Banner", "onAdLoaded ✓")
                }
                override fun onAdFailed(error: AdError) {
                    viewModel.onBannerError(error)
                    viewModel.log("Banner", "onAdFailed: ${error.message}")
                }
                override fun onAdClicked() = viewModel.log("Banner", "onAdClicked")
                override fun onAdImpression() {
                    viewModel.onBannerShown()
                    viewModel.log("Banner", "onAdImpression (MRC threshold met)")
                }
            })
            .build()

        bannerAd?.load()
    }

    override fun onDestroyView() {
        bannerView.destroy()
        super.onDestroyView()
    }
}
