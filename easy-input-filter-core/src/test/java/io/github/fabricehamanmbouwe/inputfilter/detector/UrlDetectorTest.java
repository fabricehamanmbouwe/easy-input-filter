package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlDetectorTest {

    private final UrlDetector detector = new UrlDetector();

    @BeforeEach
    void clearPatternCache() {
        PatternCache.clear();
    }

    @Test
    void detectsFullHttpsUrl() {
        FilterResult result = detector.check("Voir plus de photos sur https://monsite.com/produit");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void detectsWhatsAppBypassLink() {
        FilterResult result = detector.check("Contactez-moi directement sur wa.me/33612345678");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void detectsTelegramBypassLink() {
        FilterResult result = detector.check("Mon telegram: t.me/myusername");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void doesNotMatchPlainText() {
        FilterResult result = detector.check("Magnifique table en bois, etat neuf");
        assertThat(result.matched()).isFalse();
    }

    @Test
    void bareDomainsIgnoredWhenDisabled() {
        UrlDetector strictHttpOnly = new UrlDetector(false);
        FilterResult result = strictHttpOnly.check("wa.me/33612345678");
        assertThat(result.matched()).isFalse();
    }
}
