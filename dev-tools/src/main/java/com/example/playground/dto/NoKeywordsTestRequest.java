package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.NoKeywords;

public class NoKeywordsTestRequest {

    @NoKeywords({"whatsapp", "telegram"})
    private String value;

    public NoKeywordsTestRequest() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
