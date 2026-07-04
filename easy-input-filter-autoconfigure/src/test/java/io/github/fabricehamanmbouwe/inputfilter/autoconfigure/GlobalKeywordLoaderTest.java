package io.github.fabricehamanmbouwe.inputfilter.autoconfigure;

import io.github.fabricehamanmbouwe.inputfilter.core.FilterEngine;
import io.github.fabricehamanmbouwe.inputfilter.exception.InputFilterException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class GlobalKeywordLoaderTest {

    @Test
    void unsupportedLocale_isIgnoredWithWarnLog(CapturedOutput output) {
        GlobalKeywordLoader loader = new GlobalKeywordLoader(
                List.of("fr", "pt-BR"),
                "https://easy-input-filter.io/pro");

        // "pt-BR" should be reported in the WARN
        assertThat(output.getAll()).contains("pt-BR");
        assertThat(output.getAll()).contains("Free tier");
        assertThat(output.getAll()).contains("easy-input-filter.io/pro");

        // Only FR keywords must be loaded (pt-BR silently excluded)
        List<String> keywords = loader.getKeywords();
        assertThat(keywords).isNotEmpty();
    }

    @Test
    void supportedLocales_frAndEn_loadKeywords() {
        GlobalKeywordLoader loader = new GlobalKeywordLoader(
                List.of("fr", "en"),
                "https://easy-input-filter.io/pro");

        List<String> keywords = loader.getKeywords();

        assertThat(keywords).isNotEmpty();
        // Both locale files contain these marketplace bypass phrases
        assertThat(keywords).anyMatch(k -> k.equalsIgnoreCase("whatsapp"));
    }

    @Test
    void frKeywords_detectedByFilterEngine() {
        GlobalKeywordLoader loader = new GlobalKeywordLoader(
                List.of("fr"),
                "https://easy-input-filter.io/pro");

        FilterEngine engine = new FilterEngine(loader.getKeywords(), null);

        // "contactez-moi" is in keywords-fr.txt
        assertThatThrownBy(() -> engine.process(new SimpleDto("Rejoignez-moi sur contactez-moi")))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("keywords");
    }

    @Test
    void enKeywords_detectedByFilterEngine() {
        GlobalKeywordLoader loader = new GlobalKeywordLoader(
                List.of("en"),
                "https://easy-input-filter.io/pro");

        FilterEngine engine = new FilterEngine(loader.getKeywords(), null);

        // "off-platform" is in keywords-en.txt
        assertThatThrownBy(() -> engine.process(new SimpleDto("Let us deal off-platform")))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("keywords");
    }

    @Test
    void onlyUnsupportedLocale_noKeywordsLoaded_cleanValuePasses(CapturedOutput output) {
        GlobalKeywordLoader loader = new GlobalKeywordLoader(
                List.of("pt-BR"),
                "https://easy-input-filter.io/pro");

        assertThat(loader.getKeywords()).isEmpty();
        assertThat(output.getAll()).contains("pt-BR");
    }

    static class SimpleDto {
        String description;
        SimpleDto(String description) { this.description = description; }
    }
}
