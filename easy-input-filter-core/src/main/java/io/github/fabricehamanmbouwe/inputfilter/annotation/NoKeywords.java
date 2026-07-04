package io.github.fabricehamanmbouwe.inputfilter.annotation;

import io.github.fabricehamanmbouwe.inputfilter.strategy.FilterStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field as forbidden from containing any of the given keywords.
 * <p>
 * Matching uses an Aho-Corasick trie under the hood, so checking against
 * hundreds of keywords stays O(n) regardless of the list size.
 * <p>
 * Example:
 * <pre>{@code
 * @NoKeywords({"whatsapp", "telegram", "contact me directly"})
 * private String description;
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoKeywords {

    /**
     * The list of forbidden keywords or phrases.
     */
    String[] value();

    /**
     * Whether matching is case-sensitive. Defaults to false (case-insensitive).
     */
    boolean caseSensitive() default false;

    FilterStrategy strategy() default FilterStrategy.REJECT;

    String message() default "This field contains a forbidden keyword";

    /**
     * <strong>Pro feature.</strong> When {@code > 0}, uses Levenshtein distance
     * to detect obfuscated variants of each keyword (e.g. {@code wh4tsapp} with
     * tolerance 2 would match {@code whatsapp}).
     * <p>
     * Requires an active Pro licence — without one, this attribute is silently
     * ignored and only exact matching is performed. A WARN is logged at detection
     * time when a non-zero value is encountered without a Pro licence.
     */
    int fuzzyTolerance() default 0;
}
