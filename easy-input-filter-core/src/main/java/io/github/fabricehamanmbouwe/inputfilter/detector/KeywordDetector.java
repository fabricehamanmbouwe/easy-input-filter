package io.github.fabricehamanmbouwe.inputfilter.detector;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Detects any of a configured list of forbidden keywords inside free text.
 * <p>
 * Backed by an Aho-Corasick trie: matching against N keywords stays O(text length)
 * regardless of how large N is, instead of the naive O(text length x N) of a
 * loop of {@code String.contains}.
 * <p>
 * Tries are built once per distinct keyword list and cached, since the same
 * annotation on a given field always carries the same keyword list for the
 * lifetime of the application.
 *
 * @implNote Thread-safe after construction: {@code keywords} is treated as immutable
 *           (only read after construction) and {@code caseSensitive} is a primitive.
 *           {@code TRIE_CACHE} is a class-level Caffeine cache, which is thread-safe.
 */
public final class KeywordDetector implements Detector {

    private static final Cache<String, Trie> TRIE_CACHE = Caffeine.newBuilder()
            .maximumSize(200)
            .build();

    private final List<String> keywords;
    private final boolean caseSensitive;

    public KeywordDetector(List<String> keywords, boolean caseSensitive) {
        this.keywords = keywords;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public FilterResult check(String value) {
        Trie trie = TRIE_CACHE.get(cacheKey(), k -> buildTrie());

        Collection<Emit> emits = trie.parseText(value);
        if (!emits.isEmpty()) {
            String firstMatch = emits.iterator().next().getKeyword();
            return FilterResult.matched(name(), null, "Detected forbidden keyword: " + firstMatch);
        }
        return FilterResult.clean(name());
    }

    @Override
    public String name() {
        return "keywords";
    }

    private Trie buildTrie() {
        Trie.TrieBuilder builder = Trie.builder();
        if (!caseSensitive) {
            builder = builder.ignoreCase();
        }
        builder = builder.addKeywords(keywords);
        return builder.build();
    }

    private String cacheKey() {
        return (caseSensitive ? "cs:" : "ci:") + String.join(",", keywords.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList()));
    }
}
