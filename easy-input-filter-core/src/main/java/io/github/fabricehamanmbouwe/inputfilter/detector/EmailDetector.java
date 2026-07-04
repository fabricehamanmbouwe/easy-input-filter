package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects email addresses embedded in free text.
 * <p>
 * Uses a pragmatic subset of RFC 5322 - covers the vast majority of real
 * world addresses without the full grammar complexity (which is irrelevant
 * here since we are scanning free text, not validating a dedicated field).
 *
 * @implNote Stateless and thread-safe: no mutable instance fields; compiled patterns
 *           are cached in the shared, thread-safe
 *           {@link io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache}.
 */
public final class EmailDetector implements Detector {

    private static final String CACHE_KEY = "email-rfc5322-lite";

    private static final String DEFAULT_REGEX =
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

    @Override
    public FilterResult check(String value) {
        Pattern pattern = PatternCache.get(CACHE_KEY, DEFAULT_REGEX);
        Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            return FilterResult.matched(
                    name(),
                    null,
                    "Detected an email address: " + mask(matcher.group())
            );
        }
        return FilterResult.clean(name());
    }

    @Override
    public String name() {
        return "email";
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "****" + email.substring(at);
        }
        return email.charAt(0) + "****" + email.substring(at);
    }
}
