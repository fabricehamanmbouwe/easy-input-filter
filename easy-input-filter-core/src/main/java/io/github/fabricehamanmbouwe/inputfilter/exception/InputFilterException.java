package io.github.fabricehamanmbouwe.inputfilter.exception;

/**
 * Thrown when a field annotated with a REJECT strategy matches a forbidden pattern.
 * <p>
 * In the Spring Boot starter, this is translated into a 400 response following
 * the RFC 7807 "Problem Details" format by the autoconfigure module's exception handler.
 */
public class InputFilterException extends RuntimeException {

    private final String fieldName;
    private final String detectorName;

    public InputFilterException(String fieldName, String detectorName, String message) {
        super(message);
        this.fieldName = fieldName;
        this.detectorName = detectorName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDetectorName() {
        return detectorName;
    }
}
