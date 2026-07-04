package io.github.fabricehamanmbouwe.inputfilter.strategy;

/**
 * Defines what happens when a filter rule (phone, email, url, keyword...) matches the input.
 *
 * <ul>
 *   <li>{@link #REJECT} - throws an {@code InputFilterException}, request fails with 400</li>
 *   <li>{@link #SANITIZE} - the matched portion is removed/masked, the request continues</li>
 *   <li>{@link #WARN} - the match is logged and recorded as a metric, the request continues unchanged</li>
 * </ul>
 */
public enum FilterStrategy {
    REJECT,
    SANITIZE,
    WARN
}
