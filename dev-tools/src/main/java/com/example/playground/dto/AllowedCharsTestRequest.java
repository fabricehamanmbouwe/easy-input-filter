package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.AllowedChars;

public class AllowedCharsTestRequest {

    @AllowedChars("0-9+\\-\\s")
    private String value;

    public AllowedCharsTestRequest() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
