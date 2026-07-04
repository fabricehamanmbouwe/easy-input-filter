package io.github.fabricehamanmbouwe.inputfilter.autoconfigure;

import java.time.Instant;

/**
 * RFC 7807 "Problem Details for HTTP APIs" response body returned when a
 * REJECT-strategy field is matched.
 */
public record FilterErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String field,
        String detector,
        Instant timestamp
) {
    public static FilterErrorResponse of(String detail, String field, String detector) {
        return new FilterErrorResponse(
                "https://github.com/fabricehamanmbouwe/easy-input-filter/errors/input-rejected",
                "Input rejected by easy-input-filter",
                400,
                detail,
                field,
                detector,
                Instant.now()
        );
    }
}
