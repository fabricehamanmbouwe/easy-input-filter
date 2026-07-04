package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.NoPhone;

public class WebhookTestRequest {

    @NoPhone
    private String value;

    public WebhookTestRequest() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
