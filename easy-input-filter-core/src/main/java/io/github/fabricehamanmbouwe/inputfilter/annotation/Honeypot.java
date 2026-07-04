package io.github.fabricehamanmbouwe.inputfilter.annotation;

import java.lang.annotation.*;

/**
 * Marks a field as a honeypot trap for bot detection.
 * <p>
 * The annotated field must be hidden from real users (e.g. via CSS
 * {@code display:none}). Any non-empty value triggers an
 * {@link io.github.fabricehamanmbouwe.inputfilter.exception.InputFilterException}
 * regardless of any other strategy configured on the same object — bots that
 * blindly fill all form fields will always be rejected.
 * <p>
 * Available in the <strong>Free</strong> tier.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Honeypot {}
