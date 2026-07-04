package io.github.fabricehamanmbouwe.inputfilter.autoconfigure;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookNotifierTest {

    private HttpServer mockServer;
    private String baseUrl;
    private final AtomicReference<String> capturedBody = new AtomicReference<>();
    private final AtomicReference<String> capturedMethod = new AtomicReference<>();
    private final AtomicReference<String> capturedContentType = new AtomicReference<>();
    private CountDownLatch latch;

    @BeforeEach
    void startMockServer() throws IOException {
        latch = new CountDownLatch(1);
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/hook", exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] body = exchange.getRequestBody().readAllBytes();
            capturedBody.set(new String(body, StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            latch.countDown();
        });
        mockServer.start();
        int port = mockServer.getAddress().getPort();
        baseUrl = "http://localhost:" + port + "/hook";
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop(0);
    }

    @Test
    void notifyAsync_sendsPostWithExpectedJsonPayload() throws InterruptedException {
        WebhookNotifier notifier = new WebhookNotifier(baseUrl, "my-app", List.of("REJECT"));

        notifier.notifyAsync("description", "phone", "Phone numbers are not allowed").join();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedMethod.get()).isEqualTo("POST");
        assertThat(capturedContentType.get()).contains("application/json");

        String body = capturedBody.get();
        assertThat(body).contains("\"field\":\"description\"");
        assertThat(body).contains("\"detector\":\"phone\"");
        assertThat(body).contains("\"message\":\"Phone numbers are not allowed\"");
        assertThat(body).contains("\"applicationName\":\"my-app\"");
        assertThat(body).contains("\"timestamp\":");
    }

    @Test
    void notifyAsync_withNullApplicationName_sendsNullInPayload() throws InterruptedException {
        WebhookNotifier notifier = new WebhookNotifier(baseUrl, null, List.of("REJECT"));

        notifier.notifyAsync("field", "email", "msg").join();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedBody.get()).contains("\"applicationName\":null");
    }

    @Test
    void notifyAsync_invalidUrl_completesWithoutThrowing() {
        WebhookNotifier notifier = new WebhookNotifier("http://localhost:1/nonexistent",
                null, List.of("REJECT"));

        // Must complete (possibly exceptionally) without propagating the exception to the caller
        notifier.notifyAsync("f", "d", "m").join();
    }
}
