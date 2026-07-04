package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneDetectorTest {

    private final PhoneDetector detector = new PhoneDetector();

    @BeforeEach
    void clearPatternCache() {
        PatternCache.clear();
    }

    @Test
    void detectsFrenchMobileNumberWithSpaces() {
        FilterResult result = detector.check("Contactez-moi au 06 12 34 56 78 pour plus d'infos");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void detectsInternationalNumberWithPlusPrefix() {
        FilterResult result = detector.check("Mon whatsapp: +33 6 12 34 56 78");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void detectsNumberWithDotsAsSeparators() {
        FilterResult result = detector.check("06.12.34.56.78");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void doesNotMatchPlainText() {
        FilterResult result = detector.check("Ce produit est en excellent etat, livraison rapide");
        assertThat(result.matched()).isFalse();
    }

    @Test
    void doesNotMatchShortNumericSequences() {
        FilterResult result = detector.check("Taille 42, reference 12-34");
        assertThat(result.matched()).isFalse();
    }
}
