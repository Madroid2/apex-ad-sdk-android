package com.apexads.sdk.core.device;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Collects device signals for the OpenRTB 2.6 {@code Device} object.
 *
 * All properties are derived synchronously from Android APIs — no async GAID
 * fetch here so init never blocks the main thread. Advertising ID is surfaced
 * as {@code null} when limit-ad-tracking is set; publishers may supply it via
 * {@link com.apexads.sdk.ApexAdsConfig}.
 */
public final class DeviceInfoProvider {

    // OpenRTB 2.6 connectiontype values
    public static final int CONNECTION_UNKNOWN  = 0;
    public static final int CONNECTION_ETHERNET = 1;
    public static final int CONNECTION_WIFI     = 2;
    public static final int CONNECTION_CELLULAR = 3;

    public static final class DeviceInfo {
        public final String manufacturer;
        public final String model;
        public final String osVersion;
        public final String userAgent;
        public final String language;
        public final int    screenWidth;
        public final int    screenHeight;
        public final float  density;
        public final boolean isTablet;
        public final int    connectionType;
        public final String packageName;
        public final String appName;
        public final String appVersion;
        public final boolean limitAdTracking;
        @Nullable public final String advertisingId;

        DeviceInfo(Builder b) {
            manufacturer  = b.manufacturer;
            model         = b.model;
            osVersion     = b.osVersion;
            userAgent     = b.userAgent;
            language      = b.language;
            screenWidth   = b.screenWidth;
            screenHeight  = b.screenHeight;
            density       = b.density;
            isTablet      = b.isTablet;
            connectionType = b.connectionType;
            packageName   = b.packageName;
            appName       = b.appName;
            appVersion    = b.appVersion;
            limitAdTracking = b.limitAdTracking;
            advertisingId = b.advertisingId;
        }

        static final class Builder {
            String manufacturer, model, osVersion, userAgent, language;
            int screenWidth, screenHeight, connectionType;
            float density;
            boolean isTablet, limitAdTracking;
            String packageName, appName, appVersion;
            String advertisingId;
        }
    }

    private final Context context;

    public DeviceInfoProvider(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    public DeviceInfo getDeviceInfo() {
        DisplayMetrics metrics = getDisplayMetrics();
        String[] appInfo = getAppInfo();

        DeviceInfo.Builder b = new DeviceInfo.Builder();
        b.manufacturer  = Build.MANUFACTURER;
        b.model         = Build.MODEL;
        b.osVersion     = Build.VERSION.RELEASE;
        b.userAgent     = buildUserAgent();
        b.language      = Locale.getDefault().getLanguage();
        b.screenWidth   = metrics.widthPixels;
        b.screenHeight  = metrics.heightPixels;
        b.density       = metrics.density;
        b.isTablet      = context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
        b.connectionType = getConnectionType();
        b.packageName   = context.getPackageName();
        b.appName       = appInfo[0];
        b.appVersion    = appInfo[1];
        b.limitAdTracking = false; // Real apps: populate via Google Play Services AdvertisingIdClient
        b.advertisingId = null;
        return new DeviceInfo(b);
    }

    private String buildUserAgent() {
        return "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE +
               "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) " +
               "Chrome/120.0.0.0 Mobile Safari/537.36";
    }

    @SuppressWarnings("deprecation")
    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    private int getConnectionType() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        if (network == null) return CONNECTION_UNKNOWN;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) return CONNECTION_UNKNOWN;
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))     return CONNECTION_WIFI;
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return CONNECTION_CELLULAR;
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return CONNECTION_ETHERNET;
        return CONNECTION_UNKNOWN;
    }

    private String[] getAppInfo() {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), 0);
            String label = pm.getApplicationLabel(ai).toString();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return new String[]{label, pi.versionName != null ? pi.versionName : "0.0.0"};
        } catch (PackageManager.NameNotFoundException e) {
            return new String[]{"Unknown", "0.0.0"};
        }
    }
}
