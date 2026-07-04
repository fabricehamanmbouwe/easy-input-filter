package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RepeatCharDetectorTest {

    // ── check() — detection ──────────────────────────────────────

    @Test
    void detectsRunExceedingMax() {
        RepeatCharDetector detector = new RepeatCharDetector(3);
        FilterResult result = detector.check("aaaaa");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void doesNotDetectRunExactlyAtMax() {
        RepeatCharDetector detector = new RepeatCharDetector(3);
        // exactly 3 consecutive chars is allowed
        FilterResult result = detector.check("aaa");
        assertThat(result.matched()).isFalse();
    }

    @Test
    void doesNotDetectRunBelowThreshold() {
        RepeatCharDetector detector = new RepeatCharDetector(3);
        FilterResult result = detector.check("aabbcc");
        assertThat(result.matched()).isFalse();
    }

    @Test
    void detectsExclamationMarkRun() {
        RepeatCharDetector detector = new RepeatCharDetector(2);
        FilterResult result = detector.check("wow!!!");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void detectsUnicodeBmpCharRepetition() {
        // BMP accented char 'é' (U+00E9) — a single char in Java
        RepeatCharDetector detector = new RepeatCharDetector(2);
        FilterResult result = detector.check("éééé");
        assertThat(result.matched()).isTrue();
    }

    // ── collapse() — sanitize strategy ───────────────────────────

    @Test
    void collapseReducesRunToExactlyMax() {
        RepeatCharDetector detector = new RepeatCharDetector(3);
        assertThat(detector.collapse("aaaaaaa")).isEqualTo("aaa");
    }

    @Test
    void collapsePreservesRunsAtOrBelowMax() {
        RepeatCharDetector detector = new RepeatCharDetector(3);
        assertThat(detector.collapse("aabbb")).isEqualTo("aabbb");
    }

    @Test
    void collapseHandlesMixedRuns() {
        RepeatCharDetector detector = new RepeatCharDetector(2);
        // "aaabbc" → "aabbc" (a run of 3 → 2, b run of 2 stays, c stays)
        assertThat(detector.collapse("aaabbc")).isEqualTo("aabbc");
    }

    @Test
    void collapseHandlesUnicodeBmpChar() {
        RepeatCharDetector detector = new RepeatCharDetector(2);
        assertThat(detector.collapse("éééé")).isEqualTo("éé");
    }
}
