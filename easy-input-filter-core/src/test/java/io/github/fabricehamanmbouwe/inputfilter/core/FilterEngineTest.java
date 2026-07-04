package io.github.fabricehamanmbouwe.inputfilter.core;

import io.github.fabricehamanmbouwe.inputfilter.annotation.*;
import io.github.fabricehamanmbouwe.inputfilter.cache.PatternCache;
import io.github.fabricehamanmbouwe.inputfilter.exception.InputFilterException;
import io.github.fabricehamanmbouwe.inputfilter.strategy.FilterStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterEngineTest {

    private final FilterEngine engine = new FilterEngine();

    @BeforeEach
    void clearCaches() {
        PatternCache.clear();
    }

    // ── Fixtures ──────────────────────────────────────────────────

    static class MarketplaceProduct {
        @NoPhone
        @NoEmail
        @NoUrl
        @NoKeywords({"whatsapp", "telegram"})
        String description;

        MarketplaceProduct(String description) {
            this.description = description;
        }
    }

    static class SanitizedComment {
        @MaxRepeatChar(value = 3, strategy = FilterStrategy.SANITIZE)
        String comment;

        SanitizedComment(String comment) {
            this.comment = comment;
        }
    }

    static class GenericSanitizeField {
        @Sanitize
        String note;

        GenericSanitizeField(String note) {
            this.note = note;
        }
    }

    static class PhoneOnlyField {
        @AllowedChars(value = "0-9+\\-\\s", strategy = FilterStrategy.REJECT)
        String phone;

        PhoneOnlyField(String phone) {
            this.phone = phone;
        }
    }

    // ── The core marketplace scenario ────────────────────────────

    @Test
    void rejectsDescriptionContainingPhoneNumber() {
        MarketplaceProduct product = new MarketplaceProduct(
                "Magnifique canape, contactez-moi au 06 12 34 56 78");

        assertThatThrownBy(() -> engine.process(product))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("phone");
    }

    @Test
    void rejectsDescriptionContainingWhatsAppBypassLink() {
        // Use an alphabetic username so the phone detector doesn't fire first
        MarketplaceProduct product = new MarketplaceProduct(
                "Disponible immediatement, contact direct sur wa.me/johndoe");

        assertThatThrownBy(() -> engine.process(product))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("url");
    }

    @Test
    void rejectsDescriptionContainingForbiddenKeyword() {
        MarketplaceProduct product = new MarketplaceProduct(
                "Pour eviter les frais, ajoutez-moi sur Telegram");

        assertThatThrownBy(() -> engine.process(product))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("keywords");
    }

    @Test
    void allowsCleanDescriptionThrough() {
        MarketplaceProduct product = new MarketplaceProduct(
                "Magnifique canape en cuir, etat impeccable, livraison possible");

        engine.process(product);

        assertThat(product.description)
                .isEqualTo("Magnifique canape en cuir, etat impeccable, livraison possible");
    }

    // ── Sanitize strategies ───────────────────────────────────────

    @Test
    void sanitizeCollapsesRepeatedCharacters() {
        SanitizedComment comment = new SanitizedComment("Suuuuuuper produit !!!!!!!");

        engine.process(comment);

        assertThat(comment.comment).doesNotContain("uuuuuu");
        assertThat(comment.comment).doesNotContain("!!!!!!!");
    }

    @Test
    void genericSanitizeStripsHtmlAndNormalizesWhitespace() {
        GenericSanitizeField field = new GenericSanitizeField(
                "  <b>Bonjour</b>    le    monde  ");

        engine.process(field);

        assertThat(field.note).isEqualTo("Bonjour le monde");
    }

    // ── Inherited fields ──────────────────────────────────────────

    static class ParentWithPhone {
        @NoPhone
        String phone;

        ParentWithPhone(String phone) {
            this.phone = phone;
        }
    }

    static class ChildDto extends ParentWithPhone {
        String childDescription;

        ChildDto(String phone, String childDescription) {
            super(phone);
            this.childDescription = childDescription;
        }
    }

    @Test
    void processesAnnotatedFieldsInheritedFromParentClass() {
        ChildDto dto = new ChildDto("06 12 34 56 78", "some text");

        assertThatThrownBy(() -> engine.process(dto))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("phone");
    }

    // ── AllowedChars on a genuine phone field ────────────────────

    @Test
    void allowedCharsRejectsLettersInPhoneField() {
        PhoneOnlyField field = new PhoneOnlyField("+33 6 12 34 56 78 ext.42");

        assertThatThrownBy(() -> engine.process(field))
                .isInstanceOf(InputFilterException.class);
    }

    @Test
    void allowedCharsAcceptsValidPhoneField() {
        PhoneOnlyField field = new PhoneOnlyField("+33 6 12 34 56 78");

        engine.process(field);

        assertThat(field.phone).isEqualTo("+33 6 12 34 56 78");
    }

    // ── B1: @Honeypot ─────────────────────────────────────────────

    static class FormWithHoneypot {
        @Honeypot
        String hiddenField;

        String name;

        FormWithHoneypot(String hiddenField, String name) {
            this.hiddenField = hiddenField;
            this.name = name;
        }
    }

    @Test
    void honeypot_emptyValue_passes() {
        FormWithHoneypot form = new FormWithHoneypot("", "Alice");

        engine.process(form);  // must not throw

        assertThat(form.name).isEqualTo("Alice");
    }

    @Test
    void honeypot_nullValue_passes() {
        FormWithHoneypot form = new FormWithHoneypot(null, "Alice");

        engine.process(form);

        assertThat(form.name).isEqualTo("Alice");
    }

    @Test
    void honeypot_nonEmptyValue_rejectsRegardlessOfOtherAnnotations() {
        FormWithHoneypot form = new FormWithHoneypot("bot filled this", "Alice");

        assertThatThrownBy(() -> engine.process(form))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("honeypot");
    }

    // ── C1: allowIfVerified — Pro gate (no licence = no bypass) ──

    static class ContactField {
        @NoPhone(allowIfVerified = true)
        String contact;

        ContactField(String contact) {
            this.contact = contact;
        }
    }

    @Test
    void allowIfVerified_withoutProLicence_detectionAlwaysFires_evenWhenUserIsVerified() {
        // VerifiedUserContext returns true — but without Pro licence the bypass must NOT happen
        VerifiedUserContext alwaysVerified = () -> true;
        FilterEngine engineWithVerifiedUser = new FilterEngine(List.of(), alwaysVerified);

        ContactField dto = new ContactField("06 12 34 56 78");

        assertThatThrownBy(() -> engineWithVerifiedUser.process(dto))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("phone");
    }

    @Test
    void allowIfVerified_withoutVerifiedUserContext_detectionFires() {
        FilterEngine engineWithoutContext = new FilterEngine(List.of(), null);

        ContactField dto = new ContactField("06 12 34 56 78");

        assertThatThrownBy(() -> engineWithoutContext.process(dto))
                .isInstanceOf(InputFilterException.class);
    }

    // ── C2: fuzzyTolerance — Pro gate (no licence = exact match only) ──

    static class KeywordField {
        @NoKeywords(value = {"whatsapp"}, fuzzyTolerance = 2)
        String text;

        KeywordField(String text) {
            this.text = text;
        }
    }

    @Test
    void fuzzyTolerance_withoutProLicence_obfuscatedVariantIsNotDetected() {
        // "wh4tsapp" is a fuzzy variant of "whatsapp" with distance 2
        // Without Pro, fuzzyTolerance is ignored → exact match only → no detection
        KeywordField dto = new KeywordField("contact me via wh4tsapp");

        engine.process(dto);  // must not throw — fuzzy is not active

        assertThat(dto.text).isEqualTo("contact me via wh4tsapp");
    }

    @Test
    void fuzzyTolerance_exactMatchStillWorks_withoutPro() {
        // Exact match "whatsapp" must still be detected even when fuzzyTolerance is configured
        KeywordField dto = new KeywordField("contact me via whatsapp");

        assertThatThrownBy(() -> engine.process(dto))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("keywords");
    }

    // ── Global keywords (B3) ──────────────────────────────────────

    static class DescriptionOnly {
        String description;

        DescriptionOnly(String description) {
            this.description = description;
        }
    }

    @Test
    void globalKeywords_detectedOnUnannotatedField() {
        FilterEngine engineWithGlobal = new FilterEngine(
                List.of("contact me directly"), null);
        DescriptionOnly dto = new DescriptionOnly("Please contact me directly for the deal");

        assertThatThrownBy(() -> engineWithGlobal.process(dto))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("keywords");
    }

    @Test
    void globalKeywords_cleanValue_passes() {
        FilterEngine engineWithGlobal = new FilterEngine(
                List.of("contact me directly"), null);
        DescriptionOnly dto = new DescriptionOnly("Nice product in good condition");

        engineWithGlobal.process(dto);

        assertThat(dto.description).isEqualTo("Nice product in good condition");
    }

    // ── Obfuscation normalization ─────────────────────────────────

    static class ObfuscationTarget {
        String text;

        ObfuscationTarget(String text) {
            this.text = text;
        }
    }

    @Test
    void normalizeObfuscation_spacedLettersCollapsed() {
        assertThat(FilterEngine.normalizeObfuscation("m o n n u m é r o")).isEqualTo("monnuméro");
        assertThat(FilterEngine.normalizeObfuscation("w h a t s a p p")).isEqualTo("whatsapp");
    }

    @Test
    void normalizeObfuscation_dottedAndDashedLettersCollapsed() {
        assertThat(FilterEngine.normalizeObfuscation("w.h.a.t.s.a.p.p")).isEqualTo("whatsapp");
        assertThat(FilterEngine.normalizeObfuscation("w-h-a-t-s-a-p-p")).isEqualTo("whatsapp");
    }

    @Test
    void detectsSpacedLetterObfuscation_monnuméro() {
        FilterEngine engineWithGlobal = new FilterEngine(List.of("monnuméro"), null);

        assertThatThrownBy(() -> engineWithGlobal.process(new ObfuscationTarget("m o n n u m é r o")))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("keywords");
    }

    @Test
    void detectsDottedLetterObfuscation_whatsapp() {
        FilterEngine engineWithGlobal = new FilterEngine(List.of("whatsapp"), null);

        assertThatThrownBy(() -> engineWithGlobal.process(new ObfuscationTarget("w.h.a.t.s.a.p.p")))
                .isInstanceOf(InputFilterException.class)
                .extracting(e -> ((InputFilterException) e).getDetectorName())
                .isEqualTo("keywords");
    }
}
