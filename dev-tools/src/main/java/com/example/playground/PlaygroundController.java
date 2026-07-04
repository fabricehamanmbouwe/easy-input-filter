package com.example.playground;

import com.example.playground.dto.*;
import io.github.fabricehamanmbouwe.inputfilter.annotation.NoPhone;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * One POST endpoint per annotation, each using a single annotation in isolation.
 * If the filter lets the request through, the DTO is returned as-is so you can
 * observe the sanitized value directly in the JSON response.
 *
 * Base URL: http://localhost:8081/playground
 */
@RestController
@RequestMapping("/playground")
public class PlaygroundController {

    @PostMapping("/no-phone")
    public NoPhoneTestRequest noPhone(@RequestBody NoPhoneTestRequest req) {
        return req;
    }

    @PostMapping("/no-email")
    public NoEmailTestRequest noEmail(@RequestBody NoEmailTestRequest req) {
        return req;
    }

    @PostMapping("/no-url")
    public NoUrlTestRequest noUrl(@RequestBody NoUrlTestRequest req) {
        return req;
    }

    @PostMapping("/no-keywords")
    public NoKeywordsTestRequest noKeywords(@RequestBody NoKeywordsTestRequest req) {
        return req;
    }

    @PostMapping("/no-html")
    public NoHtmlTestRequest noHtml(@RequestBody NoHtmlTestRequest req) {
        return req;
    }

    @PostMapping("/sanitize")
    public SanitizeTestRequest sanitize(@RequestBody SanitizeTestRequest req) {
        return req;
    }

    @PostMapping("/max-repeat-char")
    public MaxRepeatCharTestRequest maxRepeatChar(@RequestBody MaxRepeatCharTestRequest req) {
        return req;
    }

    @PostMapping("/allowed-chars")
    public AllowedCharsTestRequest allowedChars(@RequestBody AllowedCharsTestRequest req) {
        return req;
    }

    @PostMapping("/honeypot")
    public HoneypotTestRequest honeypot(@RequestBody HoneypotTestRequest req) {
        return req;
    }

    /**
     * Tests @RequestParam + filter annotation directly on a method parameter (no DTO).
     * FAIL: ?phone=0612345678  →  400
     * PASS: ?phone=Bonjour     →  200 {"phone":"Bonjour"}
     */
    @PostMapping("/request-param")
    public Map<String, String> requestParam(@RequestParam("phone") @NoPhone String phone) {
        return Map.of("phone", phone);
    }

    /**
     * Tests that @NoPhone declared on a parent class field is detected on a child DTO
     * (regression for inherited-field support).
     */
    @PostMapping("/inherited-field")
    public ChildRequest inheritedField(@RequestBody ChildRequest req) {
        return req;
    }

    /**
     * Identical to /no-phone but named explicitly for the webhook scenario.
     * Configure easy-input-filter.webhook.url in application.yml to see the
     * webhook fire when this endpoint rejects a request.
     */
    @PostMapping("/webhook-trigger")
    public WebhookTestRequest webhookTrigger(@RequestBody WebhookTestRequest req) {
        return req;
    }
}
