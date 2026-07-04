package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.MaxRepeatChar;

public class MaxRepeatCharTestRequest {

    @MaxRepeatChar(3)
    private String value;

    public MaxRepeatCharTestRequest() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
