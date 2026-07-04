package com.example.playground.dto;

import io.github.fabricehamanmbouwe.inputfilter.annotation.NoPhone;

public class ParentWithPhone {

    @NoPhone
    private String value;

    public ParentWithPhone() {}

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
