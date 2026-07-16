package com.apexads.sdk.core.device;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.apexads.sdk.core.utils.AdLog;

import java.util.Locale;
import java.util.concurrent.Executor;

public final class DeviceInfoProvider {

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
        public final int    ppi;
        public final boolean isTablet;
        public final int    connectionType;
        public final String packageName;
        public final String appName;
        public final String appVersion;
        public final boolean limitAdTracking;
        @Nullable public final String advertisingId;
        @Nullable public final String carrier;
        @Nullable public final String mccmnc;
        @Nullable public final String geoCountry;
        @NonNull public final String deviceRisk;
        public final boolean emulatorSuspected;
        public final int trustSignalsVersion;

        DeviceInfo(Builder b) {
            manufacturer  = b.manufacturer;
            model         = b.model;
            osVersion     = b.osVersion;
            userAgent     = b.userAgent;
            language      = b.language;
            screenWidth   = b.screenWidth;
            screenHeight  = b.screenHeight;
            density       = b.density;
            ppi           = b.ppi;
            isTablet      = b.isTablet;
            connectionType = b.connectionType;
            packageName   = b.packageName;
            appName       = b.appName;
            appVersion    = b.appVersion;
            limitAdTracking = b.limitAdTracking;
            advertisingId = b.advertisingId;
            carrier       = b.carrier;
            mccmnc        = b.mccmnc;
            geoCountry    = b.geoCountry;
            deviceRisk    = b.deviceRisk;
            emulatorSuspected = b.emulatorSuspected;
            trustSignalsVersion = b.trustSignalsVersion;
        }

        static final class Builder {
            String manufacturer, model, osVersion, userAgent, language;
            int screenWidth, screenHeight, connectionType, ppi;
            float density;
            boolean isTablet, limitAdTracking;
            String packageName, appName, appVersion;
            String advertisingId, carrier, mccmnc, geoCountry;
            String deviceRisk;
            boolean emulatorSuspected;
            int trustSignalsVersion;
        }
    }

    private final Context context;
    private final AdvertisingIdProvider advertisingIdProvider;
    private volatile String cachedUserAgent;

    public DeviceInfoProvider(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.advertisingIdProvider = new AdvertisingIdProvider(this.context);
    }

    /**
     * Pre-resolves the slow signals (advertising ID over IPC, WebView user agent) so
     * the first bid request reads a warm cache instead of blocking on them.
     */
    public void warmUp(@NonNull Executor executor) {
        advertisingIdProvider.refreshAsync(executor);
        executor.execute(this::resolveUserAgent);
    }

    @NonNull
    public DeviceInfo getDeviceInfo() {
        DisplayMetrics metrics = getDisplayMetrics();
        String[] appInfo = getAppInfo();
        AdvertisingIdProvider.Info adId = advertisingIdProvider.get();

        DeviceInfo.Builder b = new DeviceInfo.Builder();
        b.manufacturer  = Build.MANUFACTURER;
        b.model         = Build.MODEL;
        b.osVersion     = Build.VERSION.RELEASE;
        b.userAgent     = resolveUserAgent();
        b.language      = Locale.getDefault().getLanguage();
        b.screenWidth   = metrics.widthPixels;
        b.screenHeight  = metrics.heightPixels;
        b.density       = metrics.density;
        b.ppi           = metrics.densityDpi;
        b.isTablet      = context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
        b.connectionType = getConnectionType();
        b.packageName   = context.getPackageName();
        b.appName       = appInfo[0];
        b.appVersion    = appInfo[1];
        b.limitAdTracking = adId.limitAdTracking;
        b.advertisingId = adId.id;
        DeviceTrustSignals.Result trust = DeviceTrustSignals.evaluate();
        b.deviceRisk = trust.risk;
        b.emulatorSuspected = trust.emulatorSuspected;
        b.trustSignalsVersion = DeviceTrustSignals.VERSION;
        fillTelephony(b);
        return new DeviceInfo(b);
    }

    /**
     * The real WebView UA — the one ad creatives will actually render under. A
     * fabricated UA that mismatches the device is a standard IVT flag at exchanges.
     * Falls back to the HTTP-stack UA, then a minimal platform string, when no
     * WebView is installed.
     */
    private String resolveUserAgent() {
        String ua = cachedUserAgent;
        if (ua != null) return ua;
        try {
            ua = WebSettings.getDefaultUserAgent(context);
        } catch (Throwable t) {
            AdLog.d("DeviceInfoProvider: WebView UA unavailable — %s", t.getMessage());
            ua = null;
        }
        if (ua == null || ua.isEmpty()) ua = System.getProperty("http.agent");
        if (ua == null || ua.isEmpty()) {
            ua = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ")";
        }
        cachedUserAgent = ua;
        return ua;
    }

    private void fillTelephony(DeviceInfo.Builder b) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String localeCountry = Locale.getDefault().getCountry();
        if (tm == null) {
            b.geoCountry = emptyToNull(localeCountry != null ? localeCountry.toUpperCase(Locale.US) : null);
            return;
        }
        b.carrier = emptyToNull(tm.getNetworkOperatorName());
        b.mccmnc = formatMccMnc(tm.getNetworkOperator());

        String country = emptyToNull(tm.getNetworkCountryIso());
        if (country == null) country = emptyToNull(tm.getSimCountryIso());
        if (country == null) country = emptyToNull(localeCountry);
        b.geoCountry = country != null ? country.toUpperCase(Locale.US) : null;
    }

    /** OpenRTB {@code device.mccmnc} wants "mcc-mnc"; TelephonyManager returns "mccmnc". */
    @VisibleForTesting
    @Nullable
    static String formatMccMnc(@Nullable String operator) {
        if (operator == null || operator.length() < 5 || !operator.matches("\\d+")) return null;
        return operator.substring(0, 3) + "-" + operator.substring(3);
    }

    @Nullable
    private static String emptyToNull(@Nullable String s) {
        return s == null || s.isEmpty() ? null : s;
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
