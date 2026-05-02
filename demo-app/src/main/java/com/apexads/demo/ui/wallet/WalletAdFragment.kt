package com.apexads.demo.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.apexads.demo.R
import com.apexads.sdk.banner.BannerAd
import com.apexads.sdk.banner.BannerAdListener
import com.apexads.sdk.banner.BannerAdView
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.AdSize
import com.apexads.sdk.interstitial.InterstitialAd
import com.apexads.sdk.interstitial.InterstitialAdListener

/**
 * Demonstrates the wallet CTA feature embedded inside existing ad formats.
 *
 * No dedicated WalletAd API is needed — the "Save to Google Wallet" panel appears
 * automatically inside Interstitial ads and MRECT Banner ads when:
 *  1. [com.apexads.sdk.wallet.WalletAdExtension.install] was called in DemoApplication.
 *  2. The bid response carries ext.wallet data (simulated by MockAdExchange for any
 *     request that includes wallet_supported=true in imp.ext, which the SDK sets
 *     automatically for Interstitial and MRECT Banner when sdk-wallet is installed).
 *
 * Replace the placeholder JWT in MockAdExchange.buildWalletExt() with a real signed
 * Google Wallet pass JWT to exercise the full end-to-end save flow.
 */
class WalletAdFragment : Fragment() {

    // ── Interstitial ──────────────────────────────────────────────────────────
    private var interstitialAd: InterstitialAd? = null
    private lateinit var btnLoadInterstitial: Button
    private lateinit var btnShowInterstitial: Button
    private lateinit var tvInterstitialStatus: TextView
    private lateinit var progressInterstitial: ProgressBar
    private lateinit var tvInterstitialWalletResult: TextView

    // ── MRECT Banner ──────────────────────────────────────────────────────────
    private var bannerAd: BannerAd? = null
    private lateinit var btnLoadMrect: Button
    private lateinit var mrectContainer: FrameLayout
    private lateinit var tvMrectStatus: TextView
    private lateinit var progressMrect: ProgressBar
    private lateinit var tvMrectWalletResult: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnLoadInterstitial = view.findViewById(R.id.btn_load_wallet_interstitial)
        btnShowInterstitial = view.findViewById(R.id.btn_show_wallet_interstitial)
        tvInterstitialStatus = view.findViewById(R.id.tv_wallet_interstitial_status)
        progressInterstitial = view.findViewById(R.id.progress_wallet_interstitial)
        tvInterstitialWalletResult = view.findViewById(R.id.tv_wallet_interstitial_result)

        btnLoadMrect = view.findViewById(R.id.btn_load_wallet_mrect)
        mrectContainer = view.findViewById(R.id.wallet_mrect_container)
        tvMrectStatus = view.findViewById(R.id.tv_wallet_mrect_status)
        progressMrect = view.findViewById(R.id.progress_wallet_mrect)
        tvMrectWalletResult = view.findViewById(R.id.tv_wallet_mrect_result)

        btnShowInterstitial.isEnabled = false
        tvInterstitialWalletResult.visibility = View.GONE
        tvMrectWalletResult.visibility = View.GONE

        btnLoadInterstitial.setOnClickListener { loadWalletInterstitial() }
        btnShowInterstitial.setOnClickListener { interstitialAd?.show(requireContext()) }
        btnLoadMrect.setOnClickListener { loadWalletMrect() }
    }

    // ── Interstitial with wallet CTA ──────────────────────────────────────────

    private fun loadWalletInterstitial() {
        tvInterstitialWalletResult.visibility = View.GONE
        progressInterstitial.visibility = View.VISIBLE
        btnLoadInterstitial.isEnabled = false
        btnShowInterstitial.isEnabled = false
        tvInterstitialStatus.text = "Loading wallet interstitial…"

        interstitialAd = InterstitialAd.Builder("demo-wallet-interstitial")
            .listener(object : InterstitialAdListener {
                override fun onInterstitialLoaded() {
                    progressInterstitial.visibility = View.GONE
                    btnLoadInterstitial.isEnabled = true
                    btnShowInterstitial.isEnabled = true
                    tvInterstitialStatus.text = "Ready ✓ — tap Show (wallet CTA will appear)"
                }
                override fun onInterstitialFailed(error: AdError) {
                    progressInterstitial.visibility = View.GONE
                    btnLoadInterstitial.isEnabled = true
                    tvInterstitialStatus.text = "Failed: ${error.message}"
                }
                override fun onInterstitialShown() {
                    btnShowInterstitial.isEnabled = false
                    tvInterstitialStatus.text = "Interstitial showing…"
                }
                override fun onInterstitialClosed() {
                    tvInterstitialStatus.text = "Closed — tap Load to fetch another"
                }
                override fun onWalletPassSaved() {
                    tvInterstitialWalletResult.text = "🎉 Pass saved to Google Wallet!"
                    tvInterstitialWalletResult.visibility = View.VISIBLE
                }
                override fun onWalletPassCancelled() {
                    tvInterstitialWalletResult.text = "Wallet save cancelled"
                    tvInterstitialWalletResult.visibility = View.VISIBLE
                }
                override fun onWalletPassFailed() {
                    tvInterstitialWalletResult.text =
                        "⚠ Wallet save failed — replace the placeholder JWT in MockAdExchange with a real signed JWT to test."
                    tvInterstitialWalletResult.visibility = View.VISIBLE
                }
            })
            .build()
        interstitialAd?.load()
    }

    // ── MRECT Banner with wallet CTA ──────────────────────────────────────────

    private fun loadWalletMrect() {
        tvMrectWalletResult.visibility = View.GONE
        progressMrect.visibility = View.VISIBLE
        btnLoadMrect.isEnabled = false
        tvMrectStatus.text = "Loading wallet MRECT banner…"
        mrectContainer.removeAllViews()

        val bannerAdView = BannerAdView(requireContext())
        mrectContainer.addView(bannerAdView)

        bannerAd = BannerAd.Builder("demo-wallet-mrect")
            .adSize(AdSize.MRECT_300x250)
            .listener(object : BannerAdListener {
                override fun onAdLoaded() {
                    bannerAd?.show(bannerAdView)
                    progressMrect.visibility = View.GONE
                    btnLoadMrect.isEnabled = true
                    tvMrectStatus.text = "MRECT ready ✓ (wallet CTA strip at the bottom)"
                }
                override fun onAdFailed(error: AdError) {
                    progressMrect.visibility = View.GONE
                    btnLoadMrect.isEnabled = true
                    tvMrectStatus.text = "Failed: ${error.message}"
                }
                override fun onAdClicked() {
                    tvMrectStatus.text = "Banner clicked"
                }
                override fun onWalletPassSaved() {
                    tvMrectWalletResult.text = "🎉 Pass saved to Google Wallet!"
                    tvMrectWalletResult.visibility = View.VISIBLE
                }
                override fun onWalletPassCancelled() {
                    tvMrectWalletResult.text = "Wallet save cancelled"
                    tvMrectWalletResult.visibility = View.VISIBLE
                }
                override fun onWalletPassFailed() {
                    tvMrectWalletResult.text =
                        "⚠ Wallet save failed — replace the placeholder JWT in MockAdExchange with a real signed JWT to test."
                    tvMrectWalletResult.visibility = View.VISIBLE
                }
            })
            .build()
        bannerAd?.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerAd = null
        interstitialAd = null
    }
}
