package com.apexads.demo

import android.app.Application
import com.apexads.sdk.ApexAds
import com.apexads.sdk.ApexAdsConfig
import com.apexads.sdk.appopen.AppOpenAd
import com.apexads.sdk.core.di.ServiceLocator
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.network.MockAdExchange
import com.apexads.sdk.core.utils.AdLog

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = ApexAdsConfig.Builder(APP_TOKEN)
            .debugLogging(true)
            .testMode(true)
            .cacheTtlSeconds(120)
            .sentryDsn(SENTRY_DSN)
            .build()

        ApexAds.init(this, config)

        // Swap in the mock exchange so the demo works without any live server.
        ServiceLocator.register(
            com.apexads.sdk.core.network.AdNetworkClient::class.java,
            MockAdExchange()
        )

        // App Open Ads — loads a fullscreen interstitial that shows automatically
        // whenever the user returns the app from background. Frequency-capped to once/hour.
        AppOpenAd.initialize(this, "demo-appopen-placement", object : AppOpenAd.Listener {
            override fun onAppOpenAdLoaded() {
                AdLog.i("DemoApp: App Open Ad preloaded ✓")
            }
            override fun onAppOpenAdFailedToLoad(error: AdError) {
                AdLog.w("DemoApp: App Open Ad failed — %s", error.message)
            }
            override fun onAppOpenAdImpression() {
                AdLog.d("DemoApp: App Open Ad impression fired")
            }
            override fun onAppOpenAdDismissed() {
                AdLog.d("DemoApp: App Open Ad dismissed")
            }
        })
        AppOpenAd.setFrequencyCapHours(1)
        AppOpenAd.setAdExpiryMinutes(30)
    }

    override fun onTerminate() {
        AppOpenAd.destroy()
        super.onTerminate()
    }

    companion object {
        private const val APP_TOKEN = "demo-app-token-000"
        private const val SENTRY_DSN =
            "https://0cef346926239355fbc322cd8b31f6e9@o4511268922130432.ingest.de.sentry.io/4511268929339472"
    }
}
