package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailDetectorTest {

    private final EmailDetector detector = new EmailDetector();

    @BeforeEach
    void clearPatternCache() {
        PatternCache.clear();
    }

    @Test
    void detectsStandardEmail() {
        FilterResult result = detector.check("Ecrivez-moi a jean.dupont@gmail.com pour negocier");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void detectsEmailWithPlusTag() {
        FilterResult result = detector.check("contact+vente@monsite.io");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void doesNotMatchTextWithoutAt() {
        FilterResult result = detector.check("Prix negociable, voir description complete");
        assertThat(result.matched()).isFalse();
    }
}
