package io.github.fabricehamanmbouwe.inputfilter.annotation;

import io.github.fabricehamanmbouwe.inputfilter.strategy.FilterStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field as forbidden from containing an email address.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoEmail {

    FilterStrategy strategy() default FilterStrategy.REJECT;

    String message() default "Email addresses are not allowed in this field";

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
