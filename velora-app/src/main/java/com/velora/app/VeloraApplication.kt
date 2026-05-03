package com.velora.app

import android.app.Application
import com.apexads.sdk.ApexAds
import com.apexads.sdk.ApexAdsConfig
import com.apexads.sdk.appopen.AppOpenAd
import com.apexads.sdk.core.di.ServiceLocator
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.network.MockAdExchange
import com.apexads.sdk.core.utils.AdLog
import com.apexads.sdk.wallet.WalletAdExtension
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VeloraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initAdSdk()
    }

    private fun initAdSdk() {
        val config = ApexAdsConfig.Builder(APP_TOKEN)
            .debugLogging(true)
            .testMode(true)
            .cacheTtlSeconds(120)
            .build()

        ApexAds.init(this, config)

        // Mock exchange — no live server needed for the portfolio demo.
        ServiceLocator.register(
            com.apexads.sdk.core.network.AdNetworkClient::class.java,
            MockAdExchange(),
        )

        // Google Wallet pass CTAs inside Interstitial and MRECT Banner ads.
        WalletAdExtension.install()

        // App Open Ad — shows a fullscreen ad when the user returns from background.
        AppOpenAd.initialize(this, PLACEMENT_APP_OPEN, object : AppOpenAd.Listener {
            override fun onAppOpenAdLoaded() {
                AdLog.i("Velora: App Open Ad preloaded ✓")
            }
            override fun onAppOpenAdFailedToLoad(error: AdError) {
                AdLog.w("Velora: App Open Ad failed to load — %s", error.message)
            }
            override fun onAppOpenAdImpression() {
                AdLog.d("Velora: App Open Ad impression")
            }
            override fun onAppOpenAdDismissed() {
                AdLog.d("Velora: App Open Ad dismissed")
            }
        })
        AppOpenAd.setAdExpiryMinutes(30)
    }

    override fun onTerminate() {
        AppOpenAd.destroy()
        super.onTerminate()
    }

    companion object {
        private const val APP_TOKEN = "velora-app-token-001"
        const val PLACEMENT_APP_OPEN = "velora-appopen"
        const val PLACEMENT_HOME_BANNER = "velora-home-banner"
        const val PLACEMENT_HOME_NATIVE = "velora-home-native"
        const val PLACEMENT_CART_REWARDED = "velora-cart-rewarded"
        const val PLACEMENT_CHECKOUT_INTERSTITIAL = "velora-checkout-interstitial"
    }
}
