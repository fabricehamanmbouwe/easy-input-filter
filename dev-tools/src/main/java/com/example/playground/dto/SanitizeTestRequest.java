package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.Sanitize;

public class SanitizeTestRequest {

    @Sanitize
    private String value;

    public SanitizeTestRequest() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
