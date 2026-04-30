package com.apexads.demo.ui.native

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.apexads.demo.R
import com.apexads.demo.viewmodel.AdViewModel
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.nativeads.NativeAd
import com.apexads.sdk.nativeads.NativeAdListener
import com.apexads.sdk.nativeads.NativeAdView

class NativeFragment : Fragment() {

    private val viewModel: AdViewModel by activityViewModels()
    private var nativeAd: NativeAd? = null

    private lateinit var nativeAdView: NativeAdView
    private lateinit var nativeCard: View
    private lateinit var loadButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    // Native ad views — publisher controls these
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvCta: TextView
    private lateinit var tvSponsor: TextView
    private lateinit var ivMainImage: ImageView
    private lateinit var ivIcon: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_native, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nativeAdView = view.findViewById(R.id.native_ad_view)
        nativeCard = view.findViewById(R.id.native_card)
        loadButton = view.findViewById(R.id.btn_load_native)
        statusText = view.findViewById(R.id.tv_status)
        progressBar = view.findViewById(R.id.progress_bar)

        // Register publisher-controlled views with the SDK's NativeAdView container
        tvTitle = view.findViewById(R.id.tv_native_title)
        tvDescription = view.findViewById(R.id.tv_native_description)
        tvCta = view.findViewById(R.id.tv_native_cta)
        tvSponsor = view.findViewById(R.id.tv_native_sponsor)
        ivMainImage = view.findViewById(R.id.iv_native_main)
        ivIcon = view.findViewById(R.id.iv_native_icon)

        nativeAdView.setTitleView(tvTitle)
        nativeAdView.setDescriptionView(tvDescription)
        nativeAdView.setCtaView(tvCta)
        nativeAdView.setAdvertiserView(tvSponsor)
        nativeAdView.setMainImageView(ivMainImage)
        nativeAdView.setIconView(ivIcon)

        nativeCard.visibility = View.GONE

        viewModel.nativeState.observe(viewLifecycleOwner) { state ->
            when (state) {
                AdViewModel.AdState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    loadButton.isEnabled = false
                    nativeCard.visibility = View.GONE
                    statusText.text = "Loading native ad…"
                }
                AdViewModel.AdState.Loaded -> {
                    progressBar.visibility = View.GONE
                    loadButton.isEnabled = true
                    nativeCard.visibility = View.VISIBLE
                    statusText.text = "Native ad loaded ✓"
                    bindNativeAd()
                }
                is AdViewModel.AdState.Error -> {
                    progressBar.visibility = View.GONE
                    loadButton.isEnabled = true
                    statusText.text = "Error: ${state.error.message}"
                }
                AdViewModel.AdState.Idle -> {
                    statusText.text = "Tap Load to fetch a native ad"
                }
                else -> {}
            }
        }

        loadButton.setOnClickListener { loadNativeAd() }
    }

    private fun loadNativeAd() {
        viewModel.onNativeLoading()
        viewModel.log("Native", "Loading native ad…")

        nativeAd = NativeAd.Builder("demo-native-placement")
            .listener(object : NativeAdListener {
                override fun onNativeAdLoaded(ad: NativeAd) {
                    nativeAd = ad
                    viewModel.onNativeLoaded()
                    viewModel.log("Native", "onNativeAdLoaded: title='${ad.title}'")
                }
                override fun onNativeAdFailed(error: AdError) {
                    viewModel.onNativeError(error)
                    viewModel.log("Native", "onNativeAdFailed: ${error.message}")
                }
                override fun onNativeAdClicked() = viewModel.log("Native", "onNativeAdClicked")
            })
            .build()

        nativeAd?.load()
    }

    private fun bindNativeAd() {
        val ad = nativeAd ?: return
        ad.bindTo(nativeAdView)

        // Load images via simple URL tag (in production, use Coil/Glide)
        tvTitle.text = ad.title
        tvDescription.text = ad.description
        tvCta.text = ad.ctaText
        tvSponsor.text = ad.advertiserName ?: "Sponsored"

        // For the demo, load image from tag set by NativeAdView.bind()
        loadImageFromUrl(ivMainImage, ad.imageUrl)
        loadImageFromUrl(ivIcon, ad.iconUrl)
    }

    private fun loadImageFromUrl(imageView: ImageView, url: String?) {
        if (url == null) return
        // Simple async load without Glide to keep dependencies minimal
        Thread {
            try {
                val conn = java.net.URL(url).openConnection().apply { connect() }
                val drawable = Drawable.createFromStream(conn.getInputStream(), null)
                imageView.post { imageView.setImageDrawable(drawable) }
            } catch (_: Exception) {}
        }.start()
    }
}
