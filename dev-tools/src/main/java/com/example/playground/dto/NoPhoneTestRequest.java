package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.NoPhone;

public class NoPhoneTestRequest {

    @NoPhone
    private String value;

    public NoPhoneTestRequest() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
