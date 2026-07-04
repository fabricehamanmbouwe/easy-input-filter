package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.Honeypot;

public class HoneypotTestRequest {

    private String realField;

    @Honeypot
    private String website;

    public HoneypotTestRequest() {}

    public String getRealField() { return realField; }
    public void setRealField(String realField) { this.realField = realField; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
