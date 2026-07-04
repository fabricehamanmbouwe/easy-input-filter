package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects phone numbers embedded in free text.
 * <p>
 * This is intentionally a heuristic detector (not a strict E.164 validator):
 * the goal is to catch phone numbers people try to slip into a description or
 * comment field, including common obfuscations like spaces, dots or dashes
 * used as separators ("06 12 34 56 78", "06.12.34.56.78", "+33 6 12 34 56 78").
 *
 * @implNote Thread-safe and stateless after construction. All state ({@code countryHints},
 *           {@code minDigits}) is final; compiled patterns are stored in the shared
 *           {@link io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache}
 *           (Caffeine-backed, thread-safe). A single instance may be shared across threads.
 */
public final class PhoneDetector implements Detector {

    private static final String CACHE_KEY = "phone-international";

    /**
     * Matches:
     *  - optional leading + or 00 international prefix
     *  - 7 to 15 digits total
     *  - digits may be separated by spaces, dots, dashes or wrapped in parentheses
     */
    private static final String DEFAULT_REGEX =
            "(?:(?:\\+|00)\\d{1,3}[\\s.-]?)?(?:\\(?\\d{1,4}\\)?[\\s.-]?){2,6}\\d{2,4}";

    private final Set<String> countryHints;
    private final int minDigits;

    public PhoneDetector() {
        this(Set.of(), 7);
    }

    /**
     * @param countryHints ISO country codes used only as documentation/metrics hint -
     *                      detection itself stays generic since free-text phone numbers
     *                      rarely include a reliable country marker.
     * @param minDigits     minimum total digit count to count as a phone-like match,
     *                      avoids false positives on short numbers (e.g. "12-34").
     */
    public PhoneDetector(Set<String> countryHints, int minDigits) {
        this.countryHints = countryHints;
        this.minDigits = minDigits;
    }

    @Override
    public FilterResult check(String value) {
        Pattern pattern = PatternCache.get(CACHE_KEY, DEFAULT_REGEX);
        Matcher matcher = pattern.matcher(value);

        while (matcher.find()) {
            String candidate = matcher.group();
            long digitCount = candidate.chars().filter(Character::isDigit).count();
            if (digitCount >= minDigits) {
                return FilterResult.matched(
                        name(),
                        null, // strategy filled by the engine from the annotation
                        "Detected a phone-like number: " + mask(candidate)
                );
            }
        }
        return FilterResult.clean(name());
    }

    @Override
    public String name() {
        return "phone";
    }

    private String mask(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
