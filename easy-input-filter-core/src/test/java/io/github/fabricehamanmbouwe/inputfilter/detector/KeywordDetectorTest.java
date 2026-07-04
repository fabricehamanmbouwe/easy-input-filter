package io.github.fabricehamanmbouwe.inputfilter.detector;

import io.github.fabricehamanmbouwe.inputfilter.core.FilterResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordDetectorTest {

    @Test
    void detectsKeywordCaseInsensitiveByDefault() {
        KeywordDetector detector = new KeywordDetector(List.of("whatsapp", "telegram"), false);
        FilterResult result = detector.check("Ecrivez-moi sur WhatsApp directement");
        assertThat(result.matched()).isTrue();
    }

    @Test
    void caseSensitiveModeDoesNotMatchDifferentCase() {
        KeywordDetector detector = new KeywordDetector(List.of("WhatsApp"), true);
        FilterResult result = detector.check("ecrivez-moi sur whatsapp directement");
        assertThat(result.matched()).isFalse();
    }

    @Test
    void doesNotMatchUnrelatedText() {
        KeywordDetector detector = new KeywordDetector(List.of("whatsapp", "telegram"), false);
        FilterResult result = detector.check("Produit en parfait etat, jamais utilise");
        assertThat(result.matched()).isFalse();
    }

    @Test
    void handlesLargeKeywordListEfficiently() {
        List<String> manyKeywords = List.of(
                "whatsapp", "telegram", "signal", "viber", "wechat",
                "skype", "facetime", "messenger", "snapchat", "line"
        );
        KeywordDetector detector = new KeywordDetector(manyKeywords, false);
        FilterResult result = detector.check("Disponible aussi sur Snapchat");
        assertThat(result.matched()).isTrue();
    }
}
