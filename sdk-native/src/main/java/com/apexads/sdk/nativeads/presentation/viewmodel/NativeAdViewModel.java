package com.apexads.sdk.nativeads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.models.NativeAdPayload;
import com.apexads.sdk.core.mvvm.AdRepository;
import com.apexads.sdk.core.mvvm.AdViewModel;
import com.apexads.sdk.core.utils.AdLog;

/**
 * ViewModel for {@link NativeAd}.
 *
 * <p>Overrides {@link #onAdLoaded(AdData)} to parse the IAB OpenRTB Native 1.2
 * JSON payload embedded in {@link AdData#adMarkup}. Throws
 * {@link AdError.InvalidMarkup} when parsing fails — the base class catches this
 * and transitions state to {@code FAILED}.
 *
 * <p>On success the parsed {@link NativeAdPayload} is available via
 * {@link #getNativePayload()} immediately after the {@code onNativeAdLoaded}
 * publisher callback fires.
 */
public final class NativeAdViewModel extends AdViewModel {

    private static final String TAG = "NativeAdViewModel";

    private final NativeAdParser parser;

    @Nullable private NativeAdPayload nativePayload;

    NativeAdViewModel(
            @NonNull AdRepository repository,
            @NonNull AdCache cache,
            @NonNull String placementId,
            @NonNull NativeAdParser parser) {
        super(repository, cache, AdFormat.NATIVE, AdSize.NATIVE, placementId, 0.0);
        this.parser = parser;
    }

    /** Returns the parsed native assets, or {@code null} until a successful load. */
    @Nullable
    public NativeAdPayload getNativePayload() {
        return nativePayload;
    }

    /**
     * Parses the OpenRTB Native JSON from the winning bid's ad markup.
     *
     * <p>Throws {@link AdError.InvalidMarkup} when the JSON is absent or malformed
     * so the base class can transition to {@code FAILED}.
     */
    @NonNull
    @Override
    protected AdData onAdLoaded(@NonNull AdData adData) throws AdError {
        NativeAdPayload parsed = parser.parse(adData.adMarkup);
        if (parsed == null) {
            throw new AdError.InvalidMarkup("Native JSON parse failed");
        }
        nativePayload = parsed;
        AdLog.i(TAG + ": native parsed title='%s'", parsed.title);
        return adData.withNativePayload(parsed);
    }

    @Override
    public void destroy() {
        nativePayload = null;
        super.destroy();
    }
}
