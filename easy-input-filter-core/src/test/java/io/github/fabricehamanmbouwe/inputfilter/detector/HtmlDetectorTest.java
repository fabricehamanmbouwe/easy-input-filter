package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlDetectorTest {

    private final HtmlDetector detector = new HtmlDetector();

    // ── check() — detection via OWASP sanitizer ─────────────────

    @Test
    void detectsScriptTagAsHtml() {
        FilterResult result = detector.check("<script>alert('xss')</script>safe text");
        assertThat(result.matched()).isTrue();
        // sanitized value should strip the script tag
        assertThat(result.sanitizedValue()).doesNotContain("<script>");
    }

    @Test
    void detectsOnClickAttributeAsHtml() {
        FilterResult result = detector.check("<p onclick=\"evil()\">hello</p>");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void preservesAllowedFormattingTagB() {
        // OWASP FORMATTING policy keeps <b>
        FilterResult result = detector.check("<b>bold text</b>");
        assertThat(result.matched()).isFalse();
    }

    @Test
    void preservesAllowedFormattingTagI() {
        // OWASP FORMATTING policy keeps <i>
        FilterResult result = detector.check("<i>italic text</i>");
        assertThat(result.matched()).isFalse();
    }

    @Test
    void detectsIframeAsHtml() {
        FilterResult result = detector.check("<iframe src=\"evil.com\"></iframe>");
        assertThat(result.matched()).isTrue();
    }

    // ── stripAll() — removes every tag including allowed ones ────

    @Test
    void stripAllRemovesScriptTags() {
        // stripAll removes the TAG markup, not the text content between tags
        String stripped = detector.stripAll("<script>alert('xss')</script>safe");
        assertThat(stripped).isEqualTo("alert('xss')safe");
        assertThat(stripped).doesNotContain("<script>").doesNotContain("</script>");
    }

    @Test
    void stripAllRemovesAllTagsIncludingAllowed() {
        String stripped = detector.stripAll("<b>hello</b> <i>world</i>");
        assertThat(stripped).isEqualTo("hello world");
    }

    @Test
    void stripAllLeavesPlainTextUnchanged() {
        String input = "plain text without tags";
        assertThat(detector.stripAll(input)).isEqualTo(input);
    }
}
