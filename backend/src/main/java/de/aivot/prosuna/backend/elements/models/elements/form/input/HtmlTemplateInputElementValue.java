package de.aivot.prosuna.backend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class HtmlTemplateInputElementValue implements Serializable {
    @Nullable
    private String assetKey;

    @Nullable
    private Map<String, String> slots;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HtmlTemplateInputElementValue that = (HtmlTemplateInputElementValue) o;
        return Objects.equals(assetKey, that.assetKey) && Objects.equals(slots, that.slots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetKey, slots);
    }

    @Nullable
    public String getAssetKey() {
        return assetKey;
    }

    public HtmlTemplateInputElementValue setAssetKey(@Nullable String assetKey) {
        this.assetKey = assetKey;
        return this;
    }

    @Nullable
    public Map<String, String> getSlots() {
        return slots;
    }

    public HtmlTemplateInputElementValue setSlots(@Nullable Map<String, String> slots) {
        this.slots = slots;
        return this;
    }
}
