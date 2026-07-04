package io.github.fabricehamanmbouwe.inputfilter.annotation;

import io.github.fabricehamanmbouwe.inputfilter.strategy.FilterStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field as forbidden from containing a phone number.
 * <p>
 * Detection covers international formats (E.164) as well as common local
 * formats for the configured countries (defaults to a generic international pattern).
 * <p>
 * Example:
 * <pre>{@code
 * public class ProductRequest {
 *     @NoPhone
 *     private String description;
 * }
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoPhone {

    /**
     * What to do when a phone number is detected.
     * Defaults to the global strategy configured in application.yml.
     */
    FilterStrategy strategy() default FilterStrategy.REJECT;

    /**
     * Restrict detection to specific ISO country codes (e.g. "FR", "BE", "CA").
     * Empty array means "detect any plausible international phone number".
     */
    String[] countries() default {};

    /**
     * Custom error message returned when strategy = REJECT.
     */
    String message() default "Phone numbers are not allowed in this field";

    /**
     * <strong>Pro feature.</strong> When {@code true} and a
     * {@link io.github.fabricehamanmbouwe.inputfilter.core.VerifiedUserContext} bean is
     * present and {@code isVerified()} returns {@code true}, detection is skipped for
     * verified users.
     * <p>
     * Requires an active Pro licence — without one, this attribute is silently ignored
     * and detection proceeds normally.
     */
    boolean allowIfVerified() default false;
}
