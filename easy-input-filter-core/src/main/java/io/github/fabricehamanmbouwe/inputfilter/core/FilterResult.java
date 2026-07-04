package io.github.fabricehamanmbouwe.inputfilter.core;

import io.github.fabricehamanmbouwe.inputfilter.strategy.FilterStrategy;

/**
 * Outcome of running one detector against one field value.
 *
 * @param matched      whether the forbidden pattern was found
 * @param detectorName which detector produced this result (e.g. "phone", "email")
 * @param strategy     the strategy that should be applied if matched
 * @param sanitizedValue the cleaned value, only meaningful when strategy = SANITIZE
 * @param message      human readable explanation, used when strategy = REJECT
 */
public record FilterResult(
        boolean matched,
        String detectorName,
        FilterStrategy strategy,
        String sanitizedValue,
        String message
) {

    public static FilterResult clean(String detectorName) {
        return new FilterResult(false, detectorName, null, null, null);
    }

    public static FilterResult matched(String detectorName, FilterStrategy strategy, String message) {
        return new FilterResult(true, detectorName, strategy, null, message);
    }

    public static FilterResult sanitized(String detectorName, String sanitizedValue) {
        return new FilterResult(true, detectorName, FilterStrategy.SANITIZE, sanitizedValue, null);
    }
}
