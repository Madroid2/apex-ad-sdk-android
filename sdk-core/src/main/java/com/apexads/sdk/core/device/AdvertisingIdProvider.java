package com.apexads.sdk.core.device;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.apexads.sdk.core.utils.AdLog;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resolves the Google advertising ID (GAID) and limit-ad-tracking state without any
 * Play Services library on the classpath, preserving the SDK's zero-dependency core.
 *
 * <p>Primary path binds to the Play Services advertising-ID service and speaks its
 * stable AIDL wire protocol over a raw {@link IBinder}. Fallback path reads the
 * Amazon Fire OS {@link Settings.Secure} keys. Results are cached in a volatile
 * snapshot so bid-request assembly never blocks on IPC; callers trigger
 * {@link #refreshAsync(Executor)} at init and read {@link #get()} thereafter.</p>
 */
public final class AdvertisingIdProvider {

    /** Immutable advertising-ID snapshot. A zeroed/absent ID is normalized to null. */
    public static final class Info {
        @Nullable public final String id;
        public final boolean limitAdTracking;

        Info(@Nullable String id, boolean limitAdTracking) {
            this.id = id;
            this.limitAdTracking = limitAdTracking;
        }
    }

    private static final Info EMPTY = new Info(null, false);

    private static final String ZEROED_ID = "00000000-0000-0000-0000-000000000000";

    private static final String GMS_PACKAGE = "com.google.android.gms";
    private static final String GMS_SERVICE_ACTION =
            "com.google.android.gms.ads.identifier.service.START";
    private static final String GMS_DESCRIPTOR =
            "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService";
    private static final int TRANSACTION_GET_ID = 1;
    private static final int TRANSACTION_IS_LAT = 2;
    private static final long BIND_TIMEOUT_MS = 10_000L;

    private static final String FIRE_OS_ADVERTISING_ID = "advertising_id";
    private static final String FIRE_OS_LIMIT_AD_TRACKING = "limit_ad_tracking";

    private final Context context;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private volatile Info cached = EMPTY;
    private volatile long bindTimeoutMs = BIND_TIMEOUT_MS;

    public AdvertisingIdProvider(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /** Latest resolved snapshot; {@link #EMPTY} until a refresh has completed. */
    @NonNull
    public Info get() {
        return cached;
    }

    /** Refreshes the snapshot off-thread; concurrent calls collapse into one refresh. */
    public void refreshAsync(@NonNull Executor executor) {
        if (!refreshing.compareAndSet(false, true)) return;
        executor.execute(() -> {
            try {
                refreshBlocking();
            } finally {
                refreshing.set(false);
            }
        });
    }

    @WorkerThread
    @VisibleForTesting
    @NonNull
    Info refreshBlocking() {
        Info info = normalize(fetchFromGooglePlayServices());
        if (info.id == null && !info.limitAdTracking) {
            // GMS absent or answered with nothing usable — try the Fire OS settings.
            Info fallback = normalize(fetchFromDeviceSettings());
            if (fallback.id != null || fallback.limitAdTracking) {
                info = fallback;
            }
        }
        cached = info;
        return info;
    }

    @VisibleForTesting
    void setBindTimeoutForTesting(long timeoutMs) {
        bindTimeoutMs = timeoutMs;
    }

    /** A zeroed or blank ID means the user opted out — never send it as a live ifa. */
    @VisibleForTesting
    @NonNull
    static Info normalize(@Nullable Info raw) {
        if (raw == null) return EMPTY;
        String id = raw.id != null ? raw.id.trim() : null;
        if (id == null || id.isEmpty()) {
            return new Info(null, raw.limitAdTracking);
        }
        if (ZEROED_ID.equals(id)) {
            return new Info(null, true);
        }
        return new Info(id, raw.limitAdTracking);
    }

    @Nullable
    private Info fetchFromGooglePlayServices() {
        final BlockingQueue<IBinder> binderQueue = new LinkedBlockingQueue<>(1);
        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder service) {
                binderQueue.offer(service);
            }
            @Override public void onServiceDisconnected(ComponentName name) {}
        };

        Intent intent = new Intent(GMS_SERVICE_ACTION);
        intent.setPackage(GMS_PACKAGE);

        boolean bound = false;
        try {
            bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            if (!bound) return null;

            IBinder binder = binderQueue.poll(bindTimeoutMs, TimeUnit.MILLISECONDS);
            if (binder == null) return null;

            String id = transactGetId(binder);
            boolean lat = transactIsLimitAdTracking(binder);
            return new Info(id, lat);
        } catch (Throwable t) {
            AdLog.d("AdvertisingIdProvider: GMS lookup unavailable — %s", t.getMessage());
            return null;
        } finally {
            if (bound) {
                try {
                    context.unbindService(connection);
                } catch (Throwable ignored) {
                    // Service may already be dead; nothing to release.
                }
            }
        }
    }

    private static String transactGetId(IBinder binder) throws Exception {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(GMS_DESCRIPTOR);
            binder.transact(TRANSACTION_GET_ID, data, reply, 0);
            reply.readException();
            return reply.readString();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private static boolean transactIsLimitAdTracking(IBinder binder) throws Exception {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(GMS_DESCRIPTOR);
            data.writeInt(1);
            binder.transact(TRANSACTION_IS_LAT, data, reply, 0);
            reply.readException();
            return reply.readInt() != 0;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Nullable
    private Info fetchFromDeviceSettings() {
        try {
            int lat = Settings.Secure.getInt(context.getContentResolver(), FIRE_OS_LIMIT_AD_TRACKING);
            String id = Settings.Secure.getString(context.getContentResolver(), FIRE_OS_ADVERTISING_ID);
            return new Info(id, lat != 0);
        } catch (Settings.SettingNotFoundException e) {
            return null;
        } catch (Throwable t) {
            AdLog.d("AdvertisingIdProvider: settings lookup failed — %s", t.getMessage());
            return null;
        }
    }
}
