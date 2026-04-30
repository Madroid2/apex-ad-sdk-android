package com.apexads.demo.ui.appopen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.apexads.sdk.appopen.AppOpenAd

/**
 * Demonstrates App Open Ads.
 *
 * The ad itself is wired globally in DemoApplication — this fragment simply shows
 * the current state and lets QA toggle the feature on/off without restarting the app.
 *
 * To trigger the ad: tap "Background App" button, then switch back to the app.
 * The AppOpenAdManager detects the foreground transition and shows the fullscreen ad.
 */
class AppOpenFragment : Fragment() {

    private lateinit var tvStatus: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var switchEnabled: Switch
    private lateinit var btnRefreshStatus: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = buildLayout(requireContext())

    private fun buildLayout(ctx: android.content.Context): View {
        val root = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(16))
        }

        val tvTitle = TextView(ctx).apply {
            text = "App Open Ads"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(4))
        }

        val tvSubtitle = TextView(ctx).apply {
            text = "Only Google AdMob offers App Open as a first-class ad format.\nApexAds brings the same capability — natively, via OpenRTB."
            textSize = 13f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 0, 0, dp(24))
        }

        tvStatus = TextView(ctx).apply {
            textSize = 15f
            setPadding(0, 0, 0, dp(16))
        }

        tvInstructions = TextView(ctx).apply {
            text = "How to trigger:\n1. Ensure status shows \"Ready\"\n2. Press the Home button (or Recent Apps)\n3. Tap the app icon to return — the ad appears automatically"
            textSize = 13f
            setTextColor(0xFF444444.toInt())
            setBackgroundColor(0xFFF5F5F5.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        switchEnabled = Switch(ctx).apply {
            text = "App Open Ads enabled"
            isChecked = true
            setPadding(0, dp(20), 0, dp(8))
            setOnCheckedChangeListener { _, checked ->
                AppOpenAd.setEnabled(checked)
                refreshStatus()
            }
        }

        btnRefreshStatus = Button(ctx).apply {
            text = "Refresh Status"
            setOnClickListener { refreshStatus() }
        }

        root.addView(tvTitle)
        root.addView(tvSubtitle)
        root.addView(tvStatus)
        root.addView(tvInstructions)
        root.addView(switchEnabled)
        root.addView(btnRefreshStatus)
        return root
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val ready = AppOpenAd.isAdReady()
        tvStatus.text = if (ready) "Status: Ad ready ✓ — background the app to trigger" else "Status: Preloading… (ad fetched on first launch)"
        tvStatus.setTextColor(if (ready) 0xFF2E7D32.toInt() else 0xFFE65100.toInt())
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
