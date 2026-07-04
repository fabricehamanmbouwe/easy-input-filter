package io.github.fabricehamanmbouwe.inputfilter.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Caches compiled {@link Pattern} instances so regexes are compiled exactly once
 * per process, no matter how many requests go through the filter chain.
 * <p>
 * This is the L1 cache (in-memory, Caffeine). A Redis-backed L2 cache can be
 * layered on top of this in the Pro module without changing the public API.
 */
public final class PatternCache {

    private static final Cache<String, Pattern> CACHE = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    private PatternCache() {
    }

    /**
     * Returns the compiled pattern for the given regex, compiling and caching it
     * on first access.
     *
     * @param key   a stable cache key (e.g. "phone-fr", "email-rfc5322")
     * @param regex the regex to compile if not already cached
     */
    public static Pattern get(String key, String regex) {
        return CACHE.get(key, k -> Pattern.compile(regex));
    }

    /**
     * Returns the compiled pattern for the given key, using the supplied
     * compiler function if not already cached. Useful when flags are needed.
     */
    public static Pattern get(String key, Function<String, Pattern> compiler, String regex) {
        return CACHE.get(key, k -> compiler.apply(regex));
    }

    /**
     * Clears all cached patterns. Mostly useful for tests.
     */
    public static void clear() {
        CACHE.invalidateAll();
    }

    public static long size() {
        return CACHE.estimatedSize();
    }
}
