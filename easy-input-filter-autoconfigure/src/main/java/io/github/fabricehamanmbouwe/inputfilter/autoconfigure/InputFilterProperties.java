package io.github.fabricehamanmbouwe.inputfilter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the {@code easy-input-filter.*} properties from application.yml / .properties.
 * <p>
 * Example:
 * <pre>{@code
 * easy-input-filter:
 *   enabled: true
 *   default-strategy: REJECT
 *   pro-info-url: https://github.com/fabricehamanmbouwe/easy-input-filter
 *   phone-detection:
 *     enabled: true
 *   keywords:
 *     locales: [fr, en]
 *   webhook:
 *     enabled: true
 *     url: https://example.com/hooks/input-filter
 *     on-strategies: [REJECT]
 *   global-keywords:
 *     - promo
 *     - whatsapp
 * }</pre>
 */
@ConfigurationProperties(prefix = "easy-input-filter")
public class InputFilterProperties {

    /**
     * Master switch. When false, the aspect and exception handler are not
     * registered at all (zero overhead).
     */
    private boolean enabled = true;

    /**
     * Default strategy applied when an annotation does not explicitly override it.
     * Currently strategies are set per-annotation; this property is reserved
     * for a future global override mechanism.
     */
    private String defaultStrategy = "REJECT";

    /**
     * URL shown in Pro feature warnings. Override once a real Pro landing page exists.
     */
    private String proInfoUrl = "https://github.com/fabricehamanmbouwe/easy-input-filter";

    /**
     * Maximum number of characters in a single field value that will be passed to
     * the filter engine. Values longer than this limit are truncated before
     * detection to prevent ReDoS on pathologically long inputs.
     * <p>
     * The first {@code maxInputLength} characters are filtered; a WARN is logged
     * when truncation occurs. Default is 50 000 — well above any realistic
     * free-text form field while far below a ReDoS-triggering payload.
     */
    private int maxInputLength = 50_000;

    private final PhoneDetection phoneDetection = new PhoneDetection();
    private final Cache cache = new Cache();
    private final Keywords keywords = new Keywords();
    private final Webhook webhook = new Webhook();

    /**
     * Keywords forbidden across the whole application regardless of per-field
     * {@code @NoKeywords} usage. Reserved for a future global keyword pass.
     */
    private List<String> globalKeywords = List.of();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDefaultStrategy() { return defaultStrategy; }
    public void setDefaultStrategy(String defaultStrategy) { this.defaultStrategy = defaultStrategy; }

    public String getProInfoUrl() { return proInfoUrl; }
    public void setProInfoUrl(String proInfoUrl) { this.proInfoUrl = proInfoUrl; }

    public int getMaxInputLength() { return maxInputLength; }
    public void setMaxInputLength(int maxInputLength) { this.maxInputLength = maxInputLength; }

    public PhoneDetection getPhoneDetection() { return phoneDetection; }
    public Cache getCache() { return cache; }
    public Keywords getKeywords() { return keywords; }
    public Webhook getWebhook() { return webhook; }

    public List<String> getGlobalKeywords() { return globalKeywords; }
    public void setGlobalKeywords(List<String> globalKeywords) { this.globalKeywords = globalKeywords; }

    // ── Nested: PhoneDetection ────────────────────────────────────────────────

    public static class PhoneDetection {
        private boolean enabled = true;
        private List<String> countries = List.of();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public List<String> getCountries() { return countries; }
        public void setCountries(List<String> countries) { this.countries = countries; }
    }

    // ── Nested: Cache ─────────────────────────────────────────────────────────

    public static class Cache {
        /** "caffeine" (default) or "redis" (Pro module). */
        private String provider = "caffeine";
        private int ttlSeconds = 300;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public int getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    }

    // ── Nested: Keywords ─────────────────────────────────────────────────────

    public static class Keywords {
        /**
         * Locales for the built-in global keyword lists.
         * Free tier: "fr" and "en" only. Other values are ignored with a WARN
         * pointing to the Pro upgrade page (see {@code easy-input-filter.pro-info-url}).
         */
        private List<String> locales = List.of("fr", "en");

        public List<String> getLocales() { return locales; }
        public void setLocales(List<String> locales) { this.locales = locales; }
    }

    // ── Nested: Webhook ───────────────────────────────────────────────────────

    public static class Webhook {
        /** Set to true to enable webhook notifications. */
        private boolean enabled = false;

        /** Target URL for POST notifications. Required when enabled = true. */
        private String url;

        /**
         * Strategies that trigger webhook notifications.
         * Defaults to REJECT only (the only strategy currently routed through
         * the exception handler).
         */
        private List<String> onStrategies = List.of("REJECT");

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public List<String> getOnStrategies() { return onStrategies; }
        public void setOnStrategies(List<String> onStrategies) { this.onStrategies = onStrategies; }
    }
}
