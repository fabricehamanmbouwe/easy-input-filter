package io.github.fabricehamanmbouwe.inputfilter.annotation;

import io.github.fabricehamanmbouwe.inputfilter.strategy.FilterStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects or sanitizes input containing a character repeated more than
 * {@code max} times in a row (e.g. "aaaaaaa", "!!!!!!!!").
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxRepeatChar {

    /**
     * Maximum number of consecutive identical characters allowed.
     */
    int value() default 3;

    FilterStrategy strategy() default FilterStrategy.SANITIZE;

    String message() default "This field contains too many repeated characters";
}
