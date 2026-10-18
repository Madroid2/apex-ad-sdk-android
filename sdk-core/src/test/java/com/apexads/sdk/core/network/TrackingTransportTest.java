package com.apexads.sdk.core.network;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TrackingTransportTest {

    @Test
    public void authenticatedJsonIncludesPublisherToken() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        CountDownLatch handled = new CountDownLatch(1);
        try (ServerSocket server = new ServerSocket(0)) {
            Thread receiver = new Thread(() -> receiveOneRequest(server, token, handled));
            receiver.start();
            String endpoint = "http://127.0.0.1:" + server.getLocalPort() + "/sdk/v1/quality-report";
            assertThat(TrackingTransport.sendJson(endpoint, "{}", "publisher-token")).isTrue();
            assertThat(handled.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(token.get()).isEqualTo("publisher-token");
        }
    }

    private static void receiveOneRequest(ServerSocket server,
                                          AtomicReference<String> token,
                                          CountDownLatch handled) {
        try (Socket socket = server.accept();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.regionMatches(true, 0, "X-ApexAds-Token:", 0, "X-ApexAds-Token:".length())) {
                    token.set(line.substring(line.indexOf(':') + 1).trim());
                }
            }
            OutputStream output = socket.getOutputStream();
            output.write("HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.US_ASCII));
            output.flush();
        } catch (Exception ignored) {
            // The assertions in the test report a failed or incomplete exchange.
        } finally {
            handled.countDown();
        }
    }
}
