package io.github.fabricehamanmbouwe.inputfilter.autoconfigure;

import io.github.fabricehamanmbouwe.inputfilter.aop.FilterAspect;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterEngine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class InputFilterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InputFilterAutoConfiguration.class));

    @Test
    void registersFilterEngineAndAspectByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FilterEngine.class);
            assertThat(context).hasSingleBean(FilterAspect.class);
            assertThat(context).hasSingleBean(InputFilterExceptionHandler.class);
        });
    }

    @Test
    void disablesEverythingWhenPropertySetToFalse() {
        contextRunner
                .withPropertyValues("easy-input-filter.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FilterEngine.class);
                    assertThat(context).doesNotHaveBean(FilterAspect.class);
                });
    }

    @Test
    void doesNotCreateFilterEngineWhenUserBeanIsAlreadyPresent() {
        FilterEngine userBean = new FilterEngine();
        contextRunner
                .withBean(FilterEngine.class, () -> userBean)
                .run(context -> {
                    assertThat(context).hasSingleBean(FilterEngine.class);
                    assertThat(context.getBean(FilterEngine.class)).isSameAs(userBean);
                });
    }

    @Test
    void doesNotCreateExceptionHandlerWhenUserBeanIsAlreadyPresent() {
        contextRunner
                .withBean(InputFilterExceptionHandler.class,
                        () -> new InputFilterExceptionHandler(null))
                .run(context -> assertThat(context).hasSingleBean(InputFilterExceptionHandler.class));
    }

    @Test
    void webhookNotifierNotCreatedByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(WebhookNotifier.class));
    }

    @Test
    void webhookNotifierCreatedWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "easy-input-filter.webhook.enabled=true",
                        "easy-input-filter.webhook.url=http://localhost:9999/hook")
                .run(context -> assertThat(context).hasSingleBean(WebhookNotifier.class));
    }

    @Test
    void startupBannerRunnerExecutesWithoutError() {
        contextRunner.run(context -> {
            var runners = context.getBeansOfType(ApplicationRunner.class);
            assertThat(runners).isNotEmpty();
            runners.values().forEach(r -> {
                try {
                    r.run(null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }
}
