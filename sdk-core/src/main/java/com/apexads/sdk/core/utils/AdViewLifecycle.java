package com.apexads.sdk.core.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;

public final class AdViewLifecycle {

    private AdViewLifecycle() {}

    public static boolean isTerminalDetach(@NonNull View view) {
        return isHostFinishing(view.getContext()) || isLifecycleDestroyed(view);
    }

    public static boolean isHostFinishing(@Nullable Context context) {
        Activity activity = findActivity(context);
        return activity != null
                && (activity.isFinishing()
                || activity.isDestroyed()
                || activity.isChangingConfigurations());
    }

    @Nullable
    private static Activity findActivity(@Nullable Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return current instanceof Activity ? (Activity) current : null;
    }

    private static boolean isLifecycleDestroyed(@NonNull View view) {
        try {
            Class<?> ownerClass = Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner");
            Method getOwner = ownerClass.getMethod("get", View.class);
            Object owner = getOwner.invoke(null, view);
            if (owner == null) return false;

            Class<?> lifecycleOwnerClass = Class.forName("androidx.lifecycle.LifecycleOwner");
            Method getLifecycle = lifecycleOwnerClass.getMethod("getLifecycle");
            Object lifecycle = getLifecycle.invoke(owner);
            if (lifecycle == null) return false;

            Class<?> lifecycleClass = Class.forName("androidx.lifecycle.Lifecycle");
            Method getCurrentState = lifecycleClass.getMethod("getCurrentState");
            Object state = getCurrentState.invoke(lifecycle);
            return state != null && "DESTROYED".equals(state.toString());
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (Exception e) {
            AdLog.w(e, "AdViewLifecycle: lifecycle lookup failed");
            return false;
        }
    }
}
