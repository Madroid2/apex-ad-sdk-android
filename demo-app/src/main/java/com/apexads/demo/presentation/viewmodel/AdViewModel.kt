package com.apexads.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.core.models.AdFormat
import com.apexads.sdk.inappbidding.ApexInAppBidder
import com.apexads.sdk.inappbidding.BidToken
import com.apexads.sdk.inappbidding.InAppBidListener
import com.apexads.sdk.inappbidding.mock.MockMediationPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Shared ViewModel for all demo screens.
 *
 * UI state is exposed as [StateFlow] — consumers call [StateFlow.collect] or
 * [androidx.lifecycle.compose.collectAsStateWithLifecycle] in Compose.
 *
 * Long-running operations (e.g. bid token fetch) run inside [viewModelScope]
 * and wrap the SDK's callback-based APIs via [suspendCancellableCoroutine].
 */
class AdViewModel : ViewModel() {

    // ── UI state sealed class ──────────────────────────────────────────────────

    sealed class AdState {
        object Idle : AdState()
        object Loading : AdState()
        object Loaded : AdState()
        object Shown : AdState()
        data class Error(val error: AdError) : AdState()
    }

    sealed class BidUiState {
        object Idle : BidUiState()
        object Fetching : BidUiState()
        data class Ready(val token: BidToken) : BidUiState()
        data class Failed(val message: String) : BidUiState()
        data class AuctionResult(val winner: String, val cpm: Double) : BidUiState()
    }

    // ── Per-format state flows ─────────────────────────────────────────────────

    private val _bannerState = MutableStateFlow<AdState>(AdState.Idle)
    val bannerState: StateFlow<AdState> = _bannerState.asStateFlow()

    private val _interstitialState = MutableStateFlow<AdState>(AdState.Idle)
    val interstitialState: StateFlow<AdState> = _interstitialState.asStateFlow()

    private val _nativeState = MutableStateFlow<AdState>(AdState.Idle)
    val nativeState: StateFlow<AdState> = _nativeState.asStateFlow()

    private val _videoState = MutableStateFlow<AdState>(AdState.Idle)
    val videoState: StateFlow<AdState> = _videoState.asStateFlow()

    private val _walletState = MutableStateFlow<AdState>(AdState.Idle)
    val walletState: StateFlow<AdState> = _walletState.asStateFlow()

    private val _bidState = MutableStateFlow<BidUiState>(BidUiState.Idle)
    val bidState: StateFlow<BidUiState> = _bidState.asStateFlow()

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()

    // ── Banner ─────────────────────────────────────────────────────────────────

    fun onBannerLoading() { _bannerState.value = AdState.Loading }
    fun onBannerLoaded() { _bannerState.value = AdState.Loaded }
    fun onBannerError(error: AdError) { _bannerState.value = AdState.Error(error) }
    fun onBannerShown() { _bannerState.value = AdState.Shown }

    // ── Interstitial ───────────────────────────────────────────────────────────

    fun onInterstitialLoading() { _interstitialState.value = AdState.Loading }
    fun onInterstitialLoaded() { _interstitialState.value = AdState.Loaded }
    fun onInterstitialError(error: AdError) { _interstitialState.value = AdState.Error(error) }
    fun onInterstitialShown() { _interstitialState.value = AdState.Shown }

    // ── Native ─────────────────────────────────────────────────────────────────

    fun onNativeLoading() { _nativeState.value = AdState.Loading }
    fun onNativeLoaded() { _nativeState.value = AdState.Loaded }
    fun onNativeError(error: AdError) { _nativeState.value = AdState.Error(error) }

    // ── Video ──────────────────────────────────────────────────────────────────

    fun onVideoLoading() { _videoState.value = AdState.Loading }
    fun onVideoLoaded() { _videoState.value = AdState.Loaded }
    fun onVideoError(error: AdError) { _videoState.value = AdState.Error(error) }
    fun onVideoShown() { _videoState.value = AdState.Shown }

    // ── Wallet ─────────────────────────────────────────────────────────────────

    fun onWalletLoading() { _walletState.value = AdState.Loading }
    fun onWalletLoaded() { _walletState.value = AdState.Loaded }
    fun onWalletError(error: AdError) { _walletState.value = AdState.Error(error) }
    fun onWalletShown() { _walletState.value = AdState.Shown }

    // ── In-App Bidding (coroutine-based) ───────────────────────────────────────

    /**
     * Fetches a bid token from ApexAds using a [suspendCancellableCoroutine] wrapper
     * around the SDK's callback API. Runs inside [viewModelScope] so the fetch is
     * automatically cancelled if the Activity is destroyed.
     */
    fun fetchBid() {
        viewModelScope.launch {
            _bidState.value = BidUiState.Fetching
            log("Bidding", "Fetching bid token from ApexAds…")
            try {
                val token = suspendCancellableCoroutine { cont ->
                    ApexInAppBidder.fetchBidToken(
                        "demo-inappbidding-placement",
                        AdFormat.INTERSTITIAL,
                        object : InAppBidListener {
                            override fun onBidReady(token: BidToken) {
                                if (cont.isActive) cont.resume(token)
                            }
                            override fun onBidFailed(error: AdError) {
                                if (cont.isActive) cont.resumeWithException(Exception(error.message))
                            }
                        }
                    )
                }
                _bidState.value = BidUiState.Ready(token)
                log("Bidding", "Bid ready — CPM: ${"%.3f".format(token.cpmUsd)}")
            } catch (e: Exception) {
                _bidState.value = BidUiState.Failed(e.message ?: "Unknown error")
                log("Bidding", "Bid failed: ${e.message}")
            }
        }
    }

    /**
     * Passes the winning bid token to the mock mediation platform and simulates
     * a waterfall auction — the real equivalent of MAX/LevelPlay's server-side auction.
     */
    fun simulateAuction(token: BidToken, platform: MockMediationPlatform) {
        platform.setApexBidToken(token)
        platform.simulateImpression { winner, cpm ->
            _bidState.value = BidUiState.AuctionResult(winner, cpm)
            log("Bidding", "Auction result — Winner: $winner  CPM: ${"%.3f".format(cpm)}")
        }
    }

    // ── Event log ──────────────────────────────────────────────────────────────

    fun log(tag: String, message: String) {
        _logEntries.update { current ->
            buildList {
                add(LogEntry(tag, message, System.currentTimeMillis()))
                addAll(current.take(MAX_LOG_ENTRIES - 1))
            }
        }
    }

    data class LogEntry(
        val tag: String,
        val message: String,
        val timestampMs: Long,
    )

    private companion object {
        const val MAX_LOG_ENTRIES = 100
    }
}
