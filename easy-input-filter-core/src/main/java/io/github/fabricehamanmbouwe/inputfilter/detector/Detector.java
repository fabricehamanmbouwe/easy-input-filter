package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;

/**
 * Common contract for all input detectors (phone, email, url, keywords...).
 * <p>
 * Implementations must be stateless and thread-safe - a single instance is
 * reused across every request.
 */
public interface Detector {

    /**
     * Checks the given value for the pattern this detector looks for.
     *
     * @param value the field value to check, never null (callers should skip null/blank values)
     * @return a {@link FilterResult} describing whether a match was found
     */
    FilterResult check(String value);

    /**
     * Short stable name used in cache keys, logs and metrics (e.g. "phone").
     */
    String name();
}
