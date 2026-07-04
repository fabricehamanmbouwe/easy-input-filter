package io.github.fabricehamanmbouwe.inputfilter.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads global marketplace-bypass keyword lists from classpath resources at
 * startup. Two locales are included in the <strong>Free</strong> tier: {@code fr}
 * and {@code en}. Any other locale requested via
 * {@code easy-input-filter.keywords.locales} is silently ignored and triggers a
 * WARN pointing to the Pro upgrade page.
 *
 * <p>Keyword files are plain text ({@code keywords/keywords-{locale}.txt}),
 * one keyword per line. Lines starting with {@code #} and blank lines are ignored.
 *
 * <h2>Pro extension point (C3)</h2>
 * The Pro module can register additional locale files (ES, PT-BR, DE, etc.) by
 * placing {@code keywords/keywords-{locale}.txt} files on the classpath and
 * adding the corresponding locale to {@code easy-input-filter.keywords.locales}.
 * The loading logic in {@link #loadLocale(String)} is intentionally generic and
 * will pick them up automatically once a valid Pro licence is detected by
 * {@link io.github.fabricehamanmbouwe.inputfilter.license.ProFeatureGate}.
 */
public class GlobalKeywordLoader {

    private static final Logger log = LoggerFactory.getLogger(GlobalKeywordLoader.class);

    /** Locales bundled in the Free tier. All others require a Pro licence. */
    private static final Set<String> FREE_LOCALES = Set.of("fr", "en");

    private final List<String> keywords;

    /**
     * @param configuredLocales locales requested via {@code easy-input-filter.keywords.locales}
     * @param proInfoUrl        URL shown in the warning when unsupported locales are requested
     */
    public GlobalKeywordLoader(List<String> configuredLocales, String proInfoUrl) {
        List<String> unsupported = configuredLocales.stream()
                .filter(l -> !FREE_LOCALES.contains(l))
                .collect(Collectors.toList());

        if (!unsupported.isEmpty()) {
            log.warn("[easy-input-filter] Locales {} are not available in the Free tier "
                    + "(only 'fr' and 'en' are included). "
                    + "See {} to unlock extended locales (ES, PT-BR, DE, and more).",
                    unsupported, proInfoUrl);
        }

        List<String> loaded = new ArrayList<>();
        for (String locale : configuredLocales) {
            if (FREE_LOCALES.contains(locale)) {
                loaded.addAll(loadLocale(locale));
            }
        }
        this.keywords = List.copyOf(loaded);
    }

    /** Returns the combined keyword list for all supported locales that were configured. */
    public List<String> getKeywords() {
        return keywords;
    }

    private List<String> loadLocale(String locale) {
        String resourcePath = "keywords/keywords-" + locale + ".txt";
        try (InputStream is = GlobalKeywordLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("[easy-input-filter] Keyword file not found on classpath: {}", resourcePath);
                return List.of();
            }
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("[easy-input-filter] Failed to load keyword file: {}", resourcePath, e);
            return List.of();
        }
    }
}
