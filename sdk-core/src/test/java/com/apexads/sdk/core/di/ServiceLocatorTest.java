package com.apexads.sdk;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.apexads.sdk.core.di.ServiceLocator;

import org.junit.After;
import org.junit.Test;

public class ServiceLocatorTest {

    @After
    public void tearDown() {
        ServiceLocator.reset();
    }

    @Test
    public void register_and_get_returns_correct_instance() {
        ServiceLocator.register(String.class, "hello");
        assertThat(ServiceLocator.get(String.class)).isEqualTo("hello");
    }

    @Test
    public void get_throws_for_unregistered_type() {
        assertThrows(IllegalStateException.class, () -> ServiceLocator.get(Integer.class));
    }

    @Test
    public void reset_clears_all_registrations() {
        ServiceLocator.register(String.class, "hello");
        ServiceLocator.reset();
        assertThat(ServiceLocator.isRegistered(String.class)).isFalse();
    }

    @Test
    public void register_overwrites_previous_binding() {
        ServiceLocator.register(String.class, "original");
        ServiceLocator.register(String.class, "updated");
        assertThat(ServiceLocator.get(String.class)).isEqualTo("updated");
    }

    @Test
    public void isRegistered_returns_false_before_registration() {
        assertThat(ServiceLocator.isRegistered(Double.class)).isFalse();
    }
}
