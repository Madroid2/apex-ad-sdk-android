package com.apexads.demo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.apexads.sdk.core.error.AdError

/**
 * Shared ViewModel for the demo screens.
 *
 * Follows the MVVM pattern: UI state is exposed via [LiveData], fragments
 * observe state changes and never hold business logic themselves.
 *
 * Single ViewModel shared across all ad format fragments via the Activity scope
 * to demonstrate cross-fragment state sharing (e.g., request latency log).
 */
class AdViewModel : ViewModel() {

    // --- State sealed class ---
    sealed class AdState {
        object Idle : AdState()
        object Loading : AdState()
        object Loaded : AdState()
        object Shown : AdState()
        data class Error(val error: AdError) : AdState()
    }

    private val _bannerState = MutableLiveData<AdState>(AdState.Idle)
    val bannerState: LiveData<AdState> = _bannerState

    private val _interstitialState = MutableLiveData<AdState>(AdState.Idle)
    val interstitialState: LiveData<AdState> = _interstitialState

    private val _nativeState = MutableLiveData<AdState>(AdState.Idle)
    val nativeState: LiveData<AdState> = _nativeState

    private val _videoState = MutableLiveData<AdState>(AdState.Idle)
    val videoState: LiveData<AdState> = _videoState

    private val _walletState = MutableLiveData<AdState>(AdState.Idle)
    val walletState: LiveData<AdState> = _walletState

    private val _logEntries = MutableLiveData<List<LogEntry>>(emptyList())
    val logEntries: LiveData<List<LogEntry>> = _logEntries

    // --- Banner ---
    fun onBannerLoading() = _bannerState.postValue(AdState.Loading)
    fun onBannerLoaded() = _bannerState.postValue(AdState.Loaded)
    fun onBannerError(error: AdError) = _bannerState.postValue(AdState.Error(error))
    fun onBannerShown() = _bannerState.postValue(AdState.Shown)

    // --- Interstitial ---
    fun onInterstitialLoading() = _interstitialState.postValue(AdState.Loading)
    fun onInterstitialLoaded() = _interstitialState.postValue(AdState.Loaded)
    fun onInterstitialError(error: AdError) = _interstitialState.postValue(AdState.Error(error))
    fun onInterstitialShown() = _interstitialState.postValue(AdState.Shown)

    // --- Native ---
    fun onNativeLoading() = _nativeState.postValue(AdState.Loading)
    fun onNativeLoaded() = _nativeState.postValue(AdState.Loaded)
    fun onNativeError(error: AdError) = _nativeState.postValue(AdState.Error(error))

    // --- Video ---
    fun onVideoLoading() = _videoState.postValue(AdState.Loading)
    fun onVideoLoaded() = _videoState.postValue(AdState.Loaded)
    fun onVideoError(error: AdError) = _videoState.postValue(AdState.Error(error))
    fun onVideoShown() = _videoState.postValue(AdState.Shown)

    // --- Wallet ---
    fun onWalletLoading() = _walletState.postValue(AdState.Loading)
    fun onWalletLoaded() = _walletState.postValue(AdState.Loaded)
    fun onWalletError(error: AdError) = _walletState.postValue(AdState.Error(error))
    fun onWalletShown() = _walletState.postValue(AdState.Shown)

    // --- Event log ---
    fun log(tag: String, message: String) {
        val current = _logEntries.value.orEmpty().toMutableList()
        current.add(0, LogEntry(tag, message, System.currentTimeMillis()))
        if (current.size > MAX_LOG_ENTRIES) current.removeAt(current.lastIndex)
        _logEntries.postValue(current)
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
