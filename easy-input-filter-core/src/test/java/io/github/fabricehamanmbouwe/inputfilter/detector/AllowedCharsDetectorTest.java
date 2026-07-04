package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllowedCharsDetectorTest {

    @BeforeEach
    void clearPatternCache() {
        PatternCache.clear();
    }

    // ── Pure digit class ─────────────────────────────────────────

    @Test
    void acceptsValueWithOnlyAllowedDigits() {
        AllowedCharsDetector detector = new AllowedCharsDetector("0-9");
        assertThat(detector.check("123456").matched()).isFalse();
    }

    @Test
    void rejectsFirstCharOutsideAllowedSet() {
        AllowedCharsDetector detector = new AllowedCharsDetector("0-9");
        FilterResult result = detector.check("a123");
        assertThat(result.matched()).isTrue();
        assertThat(result.message()).contains("a");
    }

    @Test
    void rejectsCharInMiddleOutsideAllowedSet() {
        AllowedCharsDetector detector = new AllowedCharsDetector("0-9");
        assertThat(detector.check("12x4").matched()).isTrue();
    }

    // ── Phone char class with escaped dash ───────────────────────

    @Test
    void acceptsValidPhoneChars() {
        AllowedCharsDetector detector = new AllowedCharsDetector("0-9+\\-\\s");
        assertThat(detector.check("+33 6 12-34-56-78").matched()).isFalse();
    }

    @Test
    void rejectsLetterInPhoneCharClass() {
        AllowedCharsDetector detector = new AllowedCharsDetector("0-9+\\-\\s");
        assertThat(detector.check("+33 ext.6").matched()).isTrue();
    }

    @Test
    void rejectsDotInPhoneCharClass() {
        AllowedCharsDetector detector = new AllowedCharsDetector("0-9+\\-\\s");
        assertThat(detector.check("06.12.34.56.78").matched()).isTrue();
    }

    // ── Char class with surrounding brackets stripped ─────────────

    @Test
    void handlesCharClassWithExplicitBrackets() {
        AllowedCharsDetector detector = new AllowedCharsDetector("[0-9]");
        assertThat(detector.check("123").matched()).isFalse();
        assertThat(detector.check("12a3").matched()).isTrue();
    }

    // ── Empty / single-char inputs ───────────────────────────────

    @Test
    void acceptsSingleAllowedChar() {
        AllowedCharsDetector detector = new AllowedCharsDetector("a-z");
        assertThat(detector.check("a").matched()).isFalse();
    }

    @Test
    void rejectsSingleDisallowedChar() {
        AllowedCharsDetector detector = new AllowedCharsDetector("a-z");
        assertThat(detector.check("A").matched()).isTrue();
    }
}
