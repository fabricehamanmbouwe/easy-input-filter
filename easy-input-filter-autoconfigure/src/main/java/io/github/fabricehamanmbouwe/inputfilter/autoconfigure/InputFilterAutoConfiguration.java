package io.github.fabricehamanmbouwe.inputfilter.autoconfigure;

import io.github.fabricehamanmbouwe.inputfilter.aop.FilterAspect;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterEngine;
import io.github.fabricehamanmbouwe.inputfilter.core.VerifiedUserContext;
import io.github.fabricehamanmbouwe.inputfilter.license.ProFeatureGate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;

/**
 * Auto-configures everything needed to use easy-input-filter with zero
 * manual bean declaration. Activates automatically as soon as the starter
 * is on the classpath, unless explicitly disabled via:
 * <pre>{@code easy-input-filter.enabled=false}</pre>
 *
 * <p>All beans are protected with {@link ConditionalOnMissingBean} so that
 * applications can override any individual component without conflicts.
 */
@AutoConfiguration
@EnableConfigurationProperties(InputFilterProperties.class)
@ConditionalOnProperty(prefix = "easy-input-filter", name = "enabled", matchIfMissing = true)
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class InputFilterAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(InputFilterAutoConfiguration.class);

    private final InputFilterProperties properties;

    public InputFilterAutoConfiguration(InputFilterProperties properties) {
        this.properties = properties;
    }

    /**
     * Core filtering engine. Override by registering your own {@link FilterEngine} bean
     * before this auto-configuration runs.
     */
    @Bean
    @ConditionalOnMissingBean
    public FilterEngine filterEngine(ObjectProvider<VerifiedUserContext> verifiedUserContextProvider) {
        ProFeatureGate.setProInfoUrl(properties.getProInfoUrl());
        GlobalKeywordLoader loader = new GlobalKeywordLoader(
                properties.getKeywords().getLocales(),
                properties.getProInfoUrl());
        return new FilterEngine(loader.getKeywords(), verifiedUserContextProvider.getIfAvailable());
    }

    /**
     * AOP aspect that intercepts Spring MVC controller methods.
     * Override by registering your own {@link FilterAspect} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public FilterAspect filterAspect(FilterEngine filterEngine) {
        return new FilterAspect(filterEngine);
    }

    /**
     * Webhook notifier bean — created only when
     * {@code easy-input-filter.webhook.enabled=true}.
     * Override by registering your own {@link WebhookNotifier} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "easy-input-filter.webhook", name = "enabled", havingValue = "true")
    public WebhookNotifier webhookNotifier(Environment environment) {
        String appName = environment.getProperty("spring.application.name");
        InputFilterProperties.Webhook webhook = properties.getWebhook();
        return new WebhookNotifier(webhook.getUrl(), appName, webhook.getOnStrategies());
    }

    /**
     * RFC 7807 exception handler. Override by registering your own
     * {@link InputFilterExceptionHandler} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public InputFilterExceptionHandler inputFilterExceptionHandler(
            ObjectProvider<WebhookNotifier> webhookNotifierProvider) {
        return new InputFilterExceptionHandler(webhookNotifierProvider.getIfAvailable());
    }

    /** D2 — startup banner listing version, active detectors, and Pro features. */
    @Bean
    public ApplicationRunner inputFilterStartupBanner() {
        return args -> {
            String version = resolveVersion();
            log.info("[easy-input-filter] v{} (FREE) — "
                    + "Active detectors: phone, email, url, html, keywords, repeatChar, allowedChars",
                    version);
            log.info("[easy-input-filter] Pro features (not active): "
                    + "whitelist-verified, fuzzy-matching, extended-locales, "
                    + "database-rules, dashboard — see {}",
                    properties.getProInfoUrl());
        };
    }

    private String resolveVersion() {
        Package pkg = getClass().getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        return "0.1.0-SNAPSHOT";
    }
}
