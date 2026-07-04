package io.github.fabricehamanmbouwe.inputfilter.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls access to Pro-tier features of easy-input-filter.
 *
 * <p>The FREE tier includes:
 * <ul>
 *   <li>Basic obfuscation normalization (spaced characters, separator injection)</li>
 *   <li>Exact keyword matching via Aho-Corasick</li>
 * </ul>
 *
 * <p>The PRO tier additionally includes advanced obfuscation detection that catches
 * sophisticated bypass attempts invisible to basic string matching. Pro features
 * activate automatically when a valid license key is present.
 *
 * <p>See <a href="https://github.com/fabricehamanmbouwe/easy-input-filter">
 * easy-input-filter.io/pro</a> for licensing details.
 *
 * @since 0.1.0
 */
public final class ProFeatureGate {

    private static final Logger log = LoggerFactory.getLogger(ProFeatureGate.class);

    private static volatile String proInfoUrl = "https://github.com/fabricehamanmbouwe/easy-input-filter";

    private ProFeatureGate() {}

    /**
     * Extension point: the Pro module replaces this via its own initializer.
     * Always returns {@code false} in the open-source build.
     */
    public static boolean isProActive() {
        return false;
    }

    /**
     * Overrides the URL shown in Pro-feature warnings. Called once at startup.
     */
    public static void setProInfoUrl(String url) {
        proInfoUrl = url;
    }

    public static String getProInfoUrl() {
        return proInfoUrl;
    }

    /**
     * Logs a WARN if {@code featureName} requires a Pro licence that is not active.
     * Never throws — callers must fall back to Free behaviour after this returns.
     *
     * @param featureName human-readable feature identifier used in the log message
     */
    public static void requirePro(String featureName) {
        if (!isProActive()) {
            log.warn("[easy-input-filter] Feature '{}' is available in the Pro tier only. "
                    + "See {} to unlock it.", featureName, proInfoUrl);
        }
    }
}
