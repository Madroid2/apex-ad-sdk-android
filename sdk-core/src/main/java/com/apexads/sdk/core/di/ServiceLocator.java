package com.apexads.sdk.core.di;

import androidx.annotation.NonNull;

import java.util.concurrent.ConcurrentHashMap;

public final class ServiceLocator {

    private static final ConcurrentHashMap<Class<?>, Object> registry = new ConcurrentHashMap<>();

    private ServiceLocator() {}

    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> T get(@NonNull Class<T> clazz) {
        Object instance = registry.get(clazz);
        if (instance == null) {
            throw new IllegalStateException(
                    "No binding registered for " + clazz.getSimpleName() +
                    ". Ensure ApexAds.init() was called before accessing SDK services.");
        }
        return (T) instance;
    }

    public static <T> void register(@NonNull Class<T> clazz, @NonNull T instance) {
        registry.put(clazz, instance);
    }

    public static void reset() {
        registry.clear();
    }

    public static boolean isRegistered(@NonNull Class<?> clazz) {
        return registry.containsKey(clazz);
    }
}
