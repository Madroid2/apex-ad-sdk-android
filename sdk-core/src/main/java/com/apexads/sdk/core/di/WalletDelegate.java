package com.apexads.sdk.core.di;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.AdData;

public interface WalletDelegate extends SdkFeature {

    boolean isAvailable(@NonNull Context context);

    void attachToInterstitial(
            @NonNull Activity activity,
            @NonNull ViewGroup root,
            @NonNull String walletExtJson,
            @NonNull WalletEventCallback callback);

    boolean handleActivityResult(int requestCode, int resultCode);

    void attachToBanner(
            @NonNull Context context,
            @NonNull ViewGroup container,
            @NonNull AdData adData,
            @NonNull WalletEventCallback callback);

    interface WalletEventCallback {
        void onPassSaved();
        void onPassCancelled();
        void onPassFailed();
    }
}
