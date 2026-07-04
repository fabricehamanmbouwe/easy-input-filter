package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * Detects and/or strips HTML markup using the OWASP Java HTML Sanitizer.
 * <p>
 * When used as a pure detector ({@code @NoHtml} with REJECT/WARN), it flags
 * any input where sanitizing would change the text. When used for sanitizing
 * ({@code @NoHtml} with SANITIZE, or {@code @Sanitize}), the cleaned value is
 * returned in {@link FilterResult#sanitizedValue()}.
 *
 * @implNote Thread-safe and stateless: {@code POLICY} is a static immutable
 *           {@link org.owasp.html.PolicyFactory}. A single instance may be shared
 *           across threads without synchronization.
 */
public final class HtmlDetector implements Detector {

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    @Override
    public FilterResult check(String value) {
        String cleaned = POLICY.sanitize(value);
        if (!cleaned.equals(value)) {
            return FilterResult.sanitized(name(), cleaned);
        }
        return FilterResult.clean(name());
    }

    /**
     * Strips all HTML tags entirely (stricter than {@link #check(String)}), used by
     * {@code @Sanitize(stripHtml = true)}.
     */
    public String stripAll(String value) {
        return value.replaceAll("<[^>]*>", "");
    }

    @Override
    public String name() {
        return "html";
    }
}
