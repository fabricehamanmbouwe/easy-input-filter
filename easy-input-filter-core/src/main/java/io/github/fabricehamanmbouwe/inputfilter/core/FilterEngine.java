package io.github.fabricehamanmbouwe.inputfilter.core;

import io.github.fabricehamanmbouwe.inputfilter.annotation.AllowedChars;
import io.github.fabricehamanmbouwe.inputfilter.annotation.Honeypot;
import io.github.fabricehamanmbouwe.inputfilter.annotation.MaxRepeatChar;
import io.github.fabricehamanmbouwe.inputfilter.annotation.NoEmail;
import io.github.fabricehamanmbouwe.inputfilter.annotation.NoHtml;
import io.github.fabricehamanmbouwe.inputfilter.annotation.NoKeywords;
import io.github.fabricehamanmbouwe.inputfilter.annotation.NoPhone;
import io.github.fabricehamanmbouwe.inputfilter.annotation.NoUrl;
import io.github.fabricehamanmbouwe.inputfilter.annotation.Sanitize;
import io.github.fabricehamanmbouwe.inputfilter.detector.AllowedCharsDetector;
import io.github.fabricehamanmbouwe.inputfilter.detector.Detector;
import io.github.fabricehamanmbouwe.inputfilter.detector.EmailDetector;
import io.github.fabricehamanmbouwe.inputfilter.detector.HtmlDetector;
import io.github.fabricehamanmbouwe.inputfilter.detector.KeywordDetector;
import io.github.fabricehamanmbouwe.inputfilter.detector.PhoneDetector;
import io.github.fabricehamanmbouwe.inputfilter.detector.RepeatCharDetector;
import io.github.fabricehamanmbouwe.inputfilter.detector.UrlDetector;
import io.github.fabricehamanmbouwe.inputfilter.exception.InputFilterException;
import io.github.fabricehamanmbouwe.inputfilter.license.ProFeatureGate;
import io.github.fabricehamanmbouwe.inputfilter.strategy.FilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * Central engine that scans a target object's fields for filter annotations
 * and applies the configured detector + strategy to each one.
 *
 * <p>This class has zero Spring dependency on purpose: the
 * {@code easy-input-filter-aop} module wires it into Spring AOP, but the
 * engine itself can be unit tested or reused as-is in a future standalone
 * SaaS API or MCP server without any change.
 *
 * <p>Example (pure Java, no Spring):
 * <pre>{@code
 * FilterEngine engine = new FilterEngine();
 * engine.process(myDto);           // throws InputFilterException on REJECT match
 * String clean = engine.processValue(rawParam, annotations);
 * }</pre>
 *
 * @implNote Thread-safe: all fields are immutable after construction. Multiple
 *           threads may share a single instance safely.
 * @since 0.1.0
 */
public final class FilterEngine {

    private static final Logger log = LoggerFactory.getLogger(FilterEngine.class);

    /** Default maximum input length — guards against ReDoS on pathological inputs. */
    public static final int DEFAULT_MAX_INPUT_LENGTH = 50_000;

    private final HtmlDetector htmlDetector = new HtmlDetector();

    /** Shared detector for globally configured keywords (FR/EN by default). Null when empty. */
    private final KeywordDetector globalKeywordDetector;

    /**
     * Optional context allowing Pro-tier bypass for verified users.
     * Null when the host application has not registered a bean implementing
     * {@link VerifiedUserContext}.
     */
    private final VerifiedUserContext verifiedUserContext;

    /**
     * Maximum number of characters passed to the detection engine per field value.
     * Values longer than this limit are truncated before detection (a WARN is logged).
     * Configurable via {@code easy-input-filter.max-input-length}.
     */
    private final int maxInputLength;

    /**
     * No-arg constructor for use in unit tests and simple setups.
     * Uses {@link #DEFAULT_MAX_INPUT_LENGTH} (50 000 characters).
     */
    public FilterEngine() {
        this(List.of(), null, DEFAULT_MAX_INPUT_LENGTH);
    }

    /**
     * Constructor with global keywords and optional verified-user context.
     * Uses {@link #DEFAULT_MAX_INPUT_LENGTH} (50 000 characters).
     *
     * @param globalKeywords      keywords loaded from locale files (FR/EN bypass phrases);
     *                            empty list disables global keyword checking
     * @param verifiedUserContext optional Pro-tier context; {@code null} when unavailable
     */
    public FilterEngine(List<String> globalKeywords, VerifiedUserContext verifiedUserContext) {
        this(globalKeywords, verifiedUserContext, DEFAULT_MAX_INPUT_LENGTH);
    }

    /**
     * Full constructor used by the Spring auto-configuration.
     *
     * @param globalKeywords      keywords loaded from locale files (FR/EN bypass phrases);
     *                            empty list disables global keyword checking
     * @param verifiedUserContext optional Pro-tier context; {@code null} when unavailable
     * @param maxInputLength      maximum field value length passed to detectors; values
     *                            exceeding this are truncated with a WARN log
     */
    public FilterEngine(List<String> globalKeywords, VerifiedUserContext verifiedUserContext,
                        int maxInputLength) {
        this.globalKeywordDetector = (globalKeywords == null || globalKeywords.isEmpty())
                ? null
                : new KeywordDetector(globalKeywords, false);
        this.verifiedUserContext = verifiedUserContext;
        this.maxInputLength = maxInputLength > 0 ? maxInputLength : DEFAULT_MAX_INPUT_LENGTH;
    }

    /**
     * Scans every declared field of the given object — including fields inherited
     * from superclasses — and applies any filter annotation found.
     * Mutates SANITIZE-d fields in place via reflection.
     *
     * <p>Values longer than {@code maxInputLength} are silently truncated before
     * detection to prevent ReDoS on pathological inputs. A WARN is logged.
     *
     * @param target the object whose fields should be filtered; does nothing if null
     * @throws InputFilterException as soon as a REJECT-strategy field matches
     */
    public void process(Object target) {
        if (target == null) {
            return;
        }
        Class<?> clazz = target.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object rawValue = readField(field, target);
                if (!(rawValue instanceof String value) || value.isBlank()) {
                    continue;
                }
                String effective = capInput(field.getName(), value);
                String result = applyAnnotations(field.getName(), field.getAnnotations(), effective);
                if (!result.equals(effective)) {
                    writeField(field, target, result);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Applies filter annotations present in the given array to a bare String value.
     * Intended for {@code @RequestParam} parameters annotated directly with filter
     * annotations rather than embedded in a DTO.
     *
     * <p>Values longer than {@code maxInputLength} are silently truncated before
     * detection to prevent ReDoS on pathological inputs. A WARN is logged.
     *
     * @param value       the raw parameter value; blank values are returned as-is
     * @param annotations the annotations present on the method parameter
     * @return the (possibly sanitized) value
     * @throws InputFilterException when a REJECT-strategy annotation matches
     */
    public String processValue(String value, Annotation[] annotations) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return applyAnnotations("<param>", annotations, capInput("<param>", value));
    }

    /**
     * Returns a detection-only copy of the input with common letter-spacing obfuscation
     * removed. This copy is used exclusively for keyword matching — the original value
     * is never modified.
     *
     * <ul>
     *   <li>"w h a t s a p p" → "whatsapp"  (single spaces between isolated letters)</li>
     *   <li>"w.h.a.t.s.a.p.p" → "whatsapp"  (dots used as letter separators)</li>
     *   <li>"w-h-a-t-s-a-p-p" → "whatsapp"  (dashes used as letter separators)</li>
     * </ul>
     */
    static String normalizeObfuscation(String input) {
        String result = input.replaceAll("(?<![\\s])\\s(?![\\s])", "");
        result = result.replaceAll("(?<=\\w)[.\\-](?=\\w)", "");
        return result;
    }

    /**
     * Caps the input to {@code maxInputLength} characters. Logs a WARN if truncation
     * occurs — this should never happen with legitimate user inputs, so the warning
     * helps operators detect probing or misconfigured clients.
     */
    private String capInput(String contextName, String value) {
        if (value.length() <= maxInputLength) {
            return value;
        }
        log.warn("[easy-input-filter] field={} length={} exceeds max-input-length={}; truncated for detection",
                contextName, value.length(), maxInputLength);
        return value.substring(0, maxInputLength);
    }

    /**
     * Core annotation-dispatch: applies every recognised filter annotation in a
     * deterministic order, returning the final (possibly sanitized) value.
     * REJECT-strategy matches throw immediately.
     *
     * <p>Evaluation order:
     * <ol>
     *   <li>{@code @Honeypot} — unconditional reject when non-empty</li>
     *   <li>{@code @NoPhone}, {@code @NoEmail}, {@code @NoUrl}</li>
     *   <li>{@code @NoKeywords} (per-field list)</li>
     *   <li>{@code @NoHtml}, {@code @MaxRepeatChar}, {@code @AllowedChars}, {@code @Sanitize}</li>
     *   <li>Global keyword list (applied last as a safety net on every field)</li>
     * </ol>
     */
    private String applyAnnotations(String contextName, Annotation[] annotations, String originalValue) {
        String value = originalValue;

        // @Honeypot — unconditional reject: caller guarantees value is non-blank
        Honeypot honeypot = findAnnotation(annotations, Honeypot.class);
        if (honeypot != null) {
            throw new InputFilterException(contextName, "honeypot",
                    "Honeypot field must be empty — bot activity suspected");
        }

        // @NoPhone
        NoPhone noPhone = findAnnotation(annotations, NoPhone.class);
        if (noPhone != null) {
            if (!resolveVerifiedBypass(noPhone.allowIfVerified(), "verified-whitelist")) {
                value = handle(contextName, value, new PhoneDetector(),
                        noPhone.strategy(), noPhone.message());
            }
        }

        // @NoEmail
        NoEmail noEmail = findAnnotation(annotations, NoEmail.class);
        if (noEmail != null) {
            if (!resolveVerifiedBypass(noEmail.allowIfVerified(), "verified-whitelist")) {
                value = handle(contextName, value, new EmailDetector(),
                        noEmail.strategy(), noEmail.message());
            }
        }

        // @NoUrl
        NoUrl noUrl = findAnnotation(annotations, NoUrl.class);
        if (noUrl != null) {
            if (!resolveVerifiedBypass(noUrl.allowIfVerified(), "verified-whitelist")) {
                value = handle(contextName, value,
                        new UrlDetector(noUrl.detectBareDomains()), noUrl.strategy(), noUrl.message());
            }
        }

        // @NoKeywords — obfuscation-aware; fuzzyTolerance > 0 requires a Pro licence
        NoKeywords noKeywords = findAnnotation(annotations, NoKeywords.class);
        if (noKeywords != null) {
            if (noKeywords.fuzzyTolerance() > 0) {
                ProFeatureGate.requirePro("fuzzy-matching");
            }
            value = handleKeywords(contextName, value,
                    new KeywordDetector(Arrays.asList(noKeywords.value()), noKeywords.caseSensitive()),
                    noKeywords.strategy(), noKeywords.message());
        }

        // @NoHtml
        NoHtml noHtml = findAnnotation(annotations, NoHtml.class);
        if (noHtml != null) {
            value = handleHtml(contextName, value, noHtml.strategy(), noHtml.message());
        }

        // @MaxRepeatChar
        MaxRepeatChar maxRepeat = findAnnotation(annotations, MaxRepeatChar.class);
        if (maxRepeat != null) {
            value = handleRepeatChar(contextName, value,
                    maxRepeat.value(), maxRepeat.strategy(), maxRepeat.message());
        }

        // @AllowedChars
        AllowedChars allowedChars = findAnnotation(annotations, AllowedChars.class);
        if (allowedChars != null) {
            value = handle(contextName, value,
                    new AllowedCharsDetector(allowedChars.value()),
                    allowedChars.strategy(), allowedChars.message());
        }

        // @Sanitize
        Sanitize sanitize = findAnnotation(annotations, Sanitize.class);
        if (sanitize != null) {
            value = handleGenericSanitize(value, sanitize);
        }

        // Global keywords — obfuscation-aware, applied last as a safety net on every field
        if (globalKeywordDetector != null) {
            value = handleKeywords(contextName, value, globalKeywordDetector,
                    FilterStrategy.REJECT, "This field contains a globally forbidden keyword");
        }

        return value;
    }

    /**
     * Resolves whether a Pro-tier verified-user bypass is active for this field.
     *
     * <p>Conditions: {@code allowIfVerified=true} AND a {@link VerifiedUserContext}
     * is registered AND {@link VerifiedUserContext#isVerified()} returns {@code true}.
     * If all three are met, {@link ProFeatureGate#requirePro(String)} is called;
     * without an active Pro licence this always returns {@code false}.
     *
     * @param allowIfVerified value of the annotation attribute
     * @param featureName     Pro feature name used in the warning log
     * @return {@code true} only when Pro is active and the user is verified
     */
    private boolean resolveVerifiedBypass(boolean allowIfVerified, String featureName) {
        if (allowIfVerified && verifiedUserContext != null && verifiedUserContext.isVerified()) {
            ProFeatureGate.requirePro(featureName);
            return ProFeatureGate.isProActive();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <A extends Annotation> A findAnnotation(Annotation[] annotations, Class<A> type) {
        for (Annotation a : annotations) {
            if (type.isInstance(a)) {
                return (A) a;
            }
        }
        return null;
    }

    /**
     * Keyword-aware variant of {@link #handle}: checks the original value first, then
     * falls back to the obfuscation-normalized copy so that patterns like
     * "w.h.a.t.s.a.p.p" and "w h a t s a p p" are detected as "whatsapp".
     *
     * <p>The original value is always preserved — normalization is detection-only.
     * For SANITIZE on obfuscated matches the original is returned unchanged because
     * reconstructing the exact obfuscated span is not possible with a normalized index.
     */
    private String handleKeywords(String contextName, String value, Detector detector,
                                   FilterStrategy strategy, String message) {
        FilterResult result = detector.check(value);
        if (!result.matched()) {
            // Fallback: try on the de-obfuscated copy
            result = detector.check(normalizeObfuscation(value));
            if (!result.matched()) {
                return value;
            }
            // Obfuscated match — cannot sanitize the original form precisely
            return switch (strategy) {
                case REJECT -> throw new InputFilterException(contextName, detector.name(), message);
                case WARN -> {
                    log.warn("[easy-input-filter] field={} detector={} message={}",
                            contextName, detector.name(), result.message());
                    yield value;
                }
                case SANITIZE -> value;
            };
        }
        return switch (strategy) {
            case REJECT -> throw new InputFilterException(contextName, detector.name(), message);
            case WARN -> {
                log.warn("[easy-input-filter] field={} detector={} message={}",
                        contextName, detector.name(), result.message());
                yield value;
            }
            case SANITIZE -> result.sanitizedValue() != null ? result.sanitizedValue() : value;
        };
    }

    private String handle(String contextName, String value, Detector detector,
                          FilterStrategy strategy, String message) {
        FilterResult result = detector.check(value);
        if (!result.matched()) {
            return value;
        }
        return switch (strategy) {
            case REJECT -> throw new InputFilterException(contextName, detector.name(), message);
            case WARN -> {
                log.warn("[easy-input-filter] field={} detector={} message={}",
                        contextName, detector.name(), result.message());
                yield value;
            }
            case SANITIZE -> result.sanitizedValue() != null ? result.sanitizedValue() : value;
        };
    }

    private String handleHtml(String contextName, String value,
                              FilterStrategy strategy, String message) {
        FilterResult result = htmlDetector.check(value);
        if (!result.matched()) {
            return value;
        }
        return switch (strategy) {
            case REJECT -> throw new InputFilterException(contextName, "html", message);
            case WARN -> {
                log.warn("[easy-input-filter] field={} detector={} message={}",
                        contextName, "html", message);
                yield value;
            }
            case SANITIZE -> result.sanitizedValue();
        };
    }

    private String handleRepeatChar(String contextName, String value, int max,
                                    FilterStrategy strategy, String message) {
        RepeatCharDetector detector = new RepeatCharDetector(max);
        FilterResult result = detector.check(value);
        if (!result.matched()) {
            return value;
        }
        return switch (strategy) {
            case REJECT -> throw new InputFilterException(contextName, detector.name(), message);
            case WARN -> {
                log.warn("[easy-input-filter] field={} detector={} message={}",
                        contextName, detector.name(), result.message());
                yield value;
            }
            case SANITIZE -> detector.collapse(value);
        };
    }

    private String handleGenericSanitize(String value, Sanitize annotation) {
        String result = value;
        if (annotation.stripHtml()) {
            result = htmlDetector.stripAll(result);
        }
        if (annotation.normalizeWhitespace()) {
            result = result.trim().replaceAll("\\s+", " ");
        }
        return result;
    }

    private Object readField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to read field " + field.getName(), e);
        }
    }

    private void writeField(Field field, Object target, String value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to write field " + field.getName(), e);
        }
    }
}
