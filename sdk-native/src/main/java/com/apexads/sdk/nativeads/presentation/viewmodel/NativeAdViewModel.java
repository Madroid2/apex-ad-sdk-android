package com.apexads.sdk.nativeads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.models.NativeAdPayload;
import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.presentation.mvvm.AdViewModel;
import com.apexads.sdk.core.utils.AdLog;

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

    @Nullable
    public NativeAdPayload getNativePayload() {
        return nativePayload;
    }

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
