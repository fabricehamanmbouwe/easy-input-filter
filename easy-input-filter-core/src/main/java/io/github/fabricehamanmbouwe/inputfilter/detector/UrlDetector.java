package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects URLs and common messaging deep-links embedded in free text.
 * <p>
 * This is the detector that catches the classic marketplace-bypass pattern:
 * a seller writes "contact me on wa.me/33612345678" or "t.me/myusername"
 * instead of a full https:// link.
 *
 * @implNote Thread-safe after construction: only {@code detectBareDomains} (a primitive
 *           boolean) is stored as instance state. Compiled patterns live in the shared,
 *           thread-safe {@link io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache}.
 */
public final class UrlDetector implements Detector {

    private static final String CACHE_KEY_FULL = "url-full";
    private static final String CACHE_KEY_BARE = "url-bare-domains";

    private static final String FULL_URL_REGEX =
            "\\b(?:https?://|www\\.)[\\w\\-.]+\\.[a-z]{2,}(?:/\\S*)?";

    /**
     * Known messaging / social bypass domains, matched even without a scheme.
     */
    private static final String BARE_DOMAINS_REGEX =
            "\\b(?:wa\\.me|t\\.me|m\\.me|telegram\\.me|instagram\\.com|signal\\.me)/\\S+";

    private final boolean detectBareDomains;

    public UrlDetector() {
        this(true);
    }

    public UrlDetector(boolean detectBareDomains) {
        this.detectBareDomains = detectBareDomains;
    }

    @Override
    public FilterResult check(String value) {
        Pattern fullUrl = PatternCache.get(CACHE_KEY_FULL, FULL_URL_REGEX);
        Matcher fullMatcher = fullUrl.matcher(value);
        if (fullMatcher.find()) {
            return FilterResult.matched(name(), null, "Detected a URL: " + fullMatcher.group());
        }

        if (detectBareDomains) {
            Pattern bareUrl = PatternCache.get(CACHE_KEY_BARE, BARE_DOMAINS_REGEX);
            Matcher bareMatcher = bareUrl.matcher(value);
            if (bareMatcher.find()) {
                return FilterResult.matched(name(), null,
                        "Detected a messaging bypass link: " + bareMatcher.group());
            }
        }

        return FilterResult.clean(name());
    }

    @Override
    public String name() {
        return "url";
    }
}
