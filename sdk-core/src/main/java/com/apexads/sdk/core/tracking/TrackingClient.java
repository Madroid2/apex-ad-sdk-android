package com.apexads.sdk.core.tracking;

import androidx.annotation.NonNull;

public interface TrackingClient {
    void fireTrackingUrl(@NonNull String url);
}
