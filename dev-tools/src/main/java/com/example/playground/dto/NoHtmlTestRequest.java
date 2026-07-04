package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.NoHtml;

public class NoHtmlTestRequest {

    @NoHtml
    private String value;

    public NoHtmlTestRequest() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
