package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;

/**
 * Detects a character repeated more than a configured number of times in a row
 * (e.g. "aaaaaaa", "!!!!!!!!!!"), backing the {@code @MaxRepeatChar} annotation.
 *
 * @implNote Thread-safe after construction: only {@code maxRepeat} (a primitive int)
 *           is stored. No shared mutable state. A single instance may be used from
 *           multiple threads concurrently.
 */
public final class RepeatCharDetector implements Detector {

    private final int maxRepeat;

    public RepeatCharDetector(int maxRepeat) {
        this.maxRepeat = maxRepeat;
    }

    @Override
    public FilterResult check(String value) {
        int run = 1;
        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) == value.charAt(i - 1)) {
                run++;
                if (run > maxRepeat) {
                    return FilterResult.matched(name(), null,
                            "Character '" + value.charAt(i) + "' repeated more than " + maxRepeat + " times");
                }
            } else {
                run = 1;
            }
        }
        return FilterResult.clean(name());
    }

    /**
     * Collapses runs longer than {@code maxRepeat} down to exactly {@code maxRepeat}
     * occurrences. Used when strategy = SANITIZE.
     */
    public String collapse(String value) {
        StringBuilder result = new StringBuilder();
        int run = 0;
        char last = '\0';
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == last) {
                run++;
            } else {
                run = 1;
                last = c;
            }
            if (run <= maxRepeat) {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Override
    public String name() {
        return "repeat-char";
    }
}
