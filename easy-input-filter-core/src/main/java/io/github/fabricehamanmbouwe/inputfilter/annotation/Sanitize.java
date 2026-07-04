package io.github.fabricehamanmbouwe.inputfilter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Always cleans the field silently (strips HTML, trims, collapses whitespace),
 * regardless of any strategy configured on other annotations. Never rejects.
 * <p>
 * Useful when you want the request to always succeed but stored data to stay clean.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sanitize {

    /**
     * Strip HTML tags during sanitization.
     */
    boolean stripHtml() default true;

    /**
     * Collapse repeated whitespace into a single space and trim.
     */
    boolean normalizeWhitespace() default true;
}
