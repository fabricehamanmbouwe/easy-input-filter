package io.github.fabricehamanmbouwe.inputfilter.aop;

import io.github.fabricehamanmbouwe.inputfilter.annotation.NoPhone;
import io.github.fabricehamanmbouwe.inputfilter.core.FilterEngine;
import io.github.fabricehamanmbouwe.inputfilter.exception.InputFilterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = FilterAspectIntegrationTest.TestConfig.class)
class FilterAspectIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Configuration
    @EnableWebMvc
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        FilterEngine filterEngine() {
            return new FilterEngine();
        }

        @Bean
        FilterAspect filterAspect(FilterEngine filterEngine) {
            return new FilterAspect(filterEngine);
        }

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/test-phone-param")
        String checkPhone(@RequestParam @NoPhone String phone) {
            return "ok:" + phone;
        }
    }

    @Test
    void requestParamAnnotatedWithNoPhone_throwsInputFilterException_whenPhoneDetected() {
        // InputFilterException propagates from the aspect and is wrapped by the Servlet container
        assertThatThrownBy(() ->
            mockMvc.perform(get("/test-phone-param").param("phone", "06 12 34 56 78")).andReturn()
        ).hasRootCauseInstanceOf(InputFilterException.class);
    }

    @Test
    void requestParamAnnotatedWithNoPhone_passes_whenNoPhoneNumber() throws Exception {
        mockMvc.perform(get("/test-phone-param").param("phone", "hello world"))
                .andExpect(status().isOk());
    }
}
