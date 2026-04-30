package com.apexads.demo.ui.inappbidding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.AdFormat
import com.apexads.sdk.inappbidding.ApexInAppBidder
import com.apexads.sdk.inappbidding.BidToken
import com.apexads.sdk.inappbidding.InAppBidListener
import com.apexads.sdk.inappbidding.mock.MockMediationPlatform

/**
 * Demonstrates the in-app bidding (header bidding) flow:
 *  1. Fetch a bid token from ApexAds before calling the mediation SDK.
 *  2. Pass the token to the mediation platform (MAX / LevelPlay).
 *  3. MockMediationPlatform simulates the waterfall auction result.
 */
class InAppBiddingFragment : Fragment() {

    private val mockPlatform = MockMediationPlatform()

    private lateinit var btnFetchBid: Button
    private lateinit var btnSimulateAuction: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(android.R.layout.activity_list_item, container, false)
        // Build layout programmatically to avoid requiring a new XML layout resource
        return buildLayout(requireContext())
    }

    private fun buildLayout(ctx: android.content.Context): View {
        val root = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        tvStatus = TextView(ctx).apply {
            text = "Tap Fetch Bid to start the in-app bidding flow."
            textSize = 14f
            setPadding(0, 0, 0, dp(16))
        }

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
        }

        btnFetchBid = Button(ctx).apply {
            text = "1. Fetch ApexAds Bid Token"
            setOnClickListener { fetchBidToken() }
        }

        btnSimulateAuction = Button(ctx).apply {
            text = "2. Simulate Mediation Auction"
            isEnabled = false
            setOnClickListener { simulateAuction() }
        }

        root.addView(tvStatus)
        root.addView(progressBar)
        root.addView(btnFetchBid)
        root.addView(btnSimulateAuction)
        return root
    }

    private fun fetchBidToken() {
        tvStatus.text = "Fetching bid from ApexAds…"
        progressBar.visibility = View.VISIBLE
        btnFetchBid.isEnabled = false
        btnSimulateAuction.isEnabled = false

        ApexInAppBidder.fetchBidToken(
            "demo-inappbidding-placement",
            AdFormat.INTERSTITIAL,
            object : InAppBidListener {
                override fun onBidReady(token: BidToken) {
                    progressBar.visibility = View.GONE
                    btnFetchBid.isEnabled = true
                    btnSimulateAuction.isEnabled = true
                    mockPlatform.setApexBidToken(token)
                    tvStatus.text = "Bid ready!\nCPM: $%.3f\nToken: ${token.token.take(24)}…\n\nTap Simulate Auction."
                        .format(token.cpmUsd)
                }

                override fun onBidFailed(error: AdError) {
                    progressBar.visibility = View.GONE
                    btnFetchBid.isEnabled = true
                    tvStatus.text = "Bid failed: ${error.message}\n\n(Mediation waterfall will proceed without ApexAds price signal)"
                }
            }
        )
    }

    private fun simulateAuction() {
        mockPlatform.simulateImpression { winningNetwork, cpm ->
            tvStatus.text = "Auction result:\nWinner: $winningNetwork\nCPM: $%.3f\n\n(In production MAX/LevelPlay handles this server-side)".format(cpm)
            btnSimulateAuction.isEnabled = false
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
