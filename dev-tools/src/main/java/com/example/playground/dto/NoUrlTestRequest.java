package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.NoUrl;

public class NoUrlTestRequest {

    @NoUrl
    private String value;

    public NoUrlTestRequest() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
