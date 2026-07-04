package io.github.fabricehamanmbouwe.inputfilter.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Sends an asynchronous HTTP POST notification when a filter detection occurs.
 * <p>
 * The call is fire-and-forget: it never blocks the current request and any
 * HTTP failure is logged as WARN only.
 * <p>
 * Enable via:
 * <pre>{@code
 * easy-input-filter:
 *   webhook:
 *     enabled: true
 *     url: https://example.com/hooks/input-filter
 * }</pre>
 *
 * <p>Payload (JSON):
 * <pre>{@code
 * {
 *   "field":           "description",
 *   "detector":        "phone",
 *   "message":         "Phone numbers are not allowed in this field",
 *   "timestamp":       "2026-06-28T12:00:00Z",
 *   "applicationName": "my-app"
 * }
 * }</pre>
 */
public class WebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifier.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final String webhookUrl;
    private final String applicationName;
    private final List<String> onStrategies;

    /**
     * @param webhookUrl      target URL for POST notifications
     * @param applicationName value of {@code spring.application.name}, or {@code null}
     * @param onStrategies    strategies that trigger the webhook (currently only REJECT
     *                        is supported via the exception handler)
     */
    public WebhookNotifier(String webhookUrl, String applicationName, List<String> onStrategies) {
        this.webhookUrl = webhookUrl;
        this.applicationName = applicationName;
        this.onStrategies = onStrategies == null ? List.of("REJECT") : List.copyOf(onStrategies);
    }

    /**
     * Sends the webhook asynchronously. Returns immediately; failures are logged as WARN.
     *
     * @param field    name of the field that triggered the filter
     * @param detector detector that matched (e.g. "phone", "email")
     * @param message  the rejection message
     * @return a future that completes when the HTTP call finishes (or fails)
     */
    public CompletableFuture<Void> notifyAsync(String field, String detector, String message) {
        String payload = buildPayload(field, detector, message);
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
        } catch (Exception e) {
            log.warn("[easy-input-filter] Invalid webhook URL '{}': {}", webhookUrl, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
        return HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(r -> (Void) null)
                .exceptionally(e -> {
                    log.warn("[easy-input-filter] Webhook notification failed: {}", e.getMessage());
                    return null;
                });
    }

    private String buildPayload(String field, String detector, String message) {
        String appNameJson = applicationName != null
                ? "\"" + jsonEscape(applicationName) + "\""
                : "null";
        return "{"
                + "\"field\":\"" + jsonEscape(field) + "\","
                + "\"detector\":\"" + jsonEscape(detector) + "\","
                + "\"message\":\"" + jsonEscape(message) + "\","
                + "\"timestamp\":\"" + Instant.now() + "\","
                + "\"applicationName\":" + appNameJson
                + "}";
    }

    private String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
