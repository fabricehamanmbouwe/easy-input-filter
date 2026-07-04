package io.github.fabricehamanmbouwe.inputfilter.annotation;

import io.github.fabricehamanmbouwe.inputfilter.strategy.FilterStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts the field to only the characters matched by the given regex character class.
 * <p>
 * Example: {@code @AllowedChars("[0-9+\\-\\s]")} only allows digits, +, - and whitespace,
 * which is typically applied to a real phone field (as opposed to @NoPhone, which forbids it).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedChars {

    /**
     * A regex character class expression. Surrounding brackets are optional:
     * both {@code "0-9+\\-\\s"} and {@code "[0-9+\\-\\s]"} are accepted.
     */
    String value();

    FilterStrategy strategy() default FilterStrategy.REJECT;

    String message() default "This field contains characters that are not allowed";
}
