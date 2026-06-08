package com.apexads.sdk.inappbidding;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;

public interface InAppBidListener {

    void onBidReady(@NonNull BidToken token);

    void onBidFailed(@NonNull AdError error);
}
