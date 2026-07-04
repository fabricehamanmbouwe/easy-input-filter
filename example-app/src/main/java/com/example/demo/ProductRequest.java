package com.example.demo;

import io.github.fabricehamanmbouwe.inputfilter.annotation.*;
import io.github.fabricehamanmbouwe.inputfilter.strategy.FilterStrategy;

/**
 * A typical marketplace product listing.
 * <p>
 * Sellers should not be able to put their phone number, email or a
 * messaging link in the description to bypass the platform's commission.
 */
public class ProductRequest {

    private String title;

    @NoPhone
    @NoEmail
    @NoUrl
    @NoKeywords({"whatsapp", "telegram", "contact me directly", "hors plateforme"})
    @MaxRepeatChar(value = 4, strategy = FilterStrategy.SANITIZE)
    private String description;

    @Sanitize
    private String internalNote;

    public ProductRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInternalNote() {
        return internalNote;
    }

    public void setInternalNote(String internalNote) {
        this.internalNote = internalNote;
    }
}
