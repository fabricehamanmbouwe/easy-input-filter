package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates that a value contains ONLY characters allowed by a given regex
 * character class, backing the {@code @AllowedChars} annotation.
 * <p>
 * Example: a character class of "0-9+\\-\\s" only allows digits, plus, dash
 * and whitespace - typically applied to a genuine phone number field.
 *
 * @implNote Thread-safe after construction: only {@code characterClass} (an immutable
 *           String) is stored as instance state. The resulting pattern is compiled once
 *           and stored in the shared, thread-safe
 *           {@link io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache}.
 */
public final class AllowedCharsDetector implements Detector {

    private final String characterClass;

    public AllowedCharsDetector(String characterClass) {
        this.characterClass = characterClass;
    }

    @Override
    public FilterResult check(String value) {
        String cacheKey = "allowed-chars:" + characterClass;
        String regex = "[^" + stripBrackets(characterClass) + "]";
        Pattern pattern = PatternCache.get(cacheKey, regex);
        Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            return FilterResult.matched(name(), null,
                    "Found a character outside the allowed set: '" + matcher.group() + "'");
        }
        return FilterResult.clean(name());
    }

    private String stripBrackets(String charClass) {
        String trimmed = charClass.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    @Override
    public String name() {
        return "allowed-chars";
    }
}
