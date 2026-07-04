package io.github.fabricehamanmbouwe.inputfilter.core;

/**
 * Optional extension point for Pro-tier whitelist bypass.
 * <p>
 * Implement this interface in the host application and expose it as a Spring bean
 * to enable the {@code allowIfVerified = true} attribute on
 * {@link io.github.fabricehamanmbouwe.inputfilter.annotation.NoPhone},
 * {@link io.github.fabricehamanmbouwe.inputfilter.annotation.NoEmail}, and
 * {@link io.github.fabricehamanmbouwe.inputfilter.annotation.NoUrl}.
 * <p>
 * <strong>Without a Pro licence the result of {@link #isVerified()} is always
 * ignored</strong> — detection proceeds normally regardless.
 */
public interface VerifiedUserContext {

    /**
     * Returns {@code true} if the currently authenticated user has been verified
     * by the host application and is eligible for contact-detail detection bypass
     * (Pro licence required).
     */
    boolean isVerified();
}
