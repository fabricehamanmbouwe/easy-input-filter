package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.NoEmail;

public class NoEmailTestRequest {

    @NoEmail
    private String value;

    public NoEmailTestRequest() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
