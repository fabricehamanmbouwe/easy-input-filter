package io.github.fabricehamanmbouwe.inputfilter.autoconfigure;

import io.github.fabricehamanmbouwe.inputfilter.exception.InputFilterException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates {@link InputFilterException} into a RFC 7807 Problem Details
 * response. Registered with the highest precedence so it does not get
 * shadowed by an application's own {@code @ControllerAdvice}.
 * <p>
 * After building the response, fires a fire-and-forget webhook notification
 * if {@code easy-input-filter.webhook.enabled=true} and a
 * {@link WebhookNotifier} bean is present.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InputFilterExceptionHandler {

    /** Null when webhook is disabled (no bean registered). */
    private final WebhookNotifier webhookNotifier;

    public InputFilterExceptionHandler(WebhookNotifier webhookNotifier) {
        this.webhookNotifier = webhookNotifier;
    }

    @ExceptionHandler(InputFilterException.class)
    public ResponseEntity<FilterErrorResponse> handle(InputFilterException ex) {
        FilterErrorResponse body = FilterErrorResponse.of(
                ex.getMessage(),
                ex.getFieldName(),
                ex.getDetectorName()
        );
        if (webhookNotifier != null) {
            webhookNotifier.notifyAsync(ex.getFieldName(), ex.getDetectorName(), ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
