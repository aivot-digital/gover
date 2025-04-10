package de.aivot.GoverBackend.form.models;

import de.aivot.GoverBackend.elements.models.BaseElement;

import javax.annotation.Nonnull;
import java.util.Map;

public record FormState(
        @Nonnull Map<String, Boolean> visibilities,
        @Nonnull Map<String, Object> values,
        @Nonnull Map<String, String> errors,
        @Nonnull Map<String, BaseElement> overrides
) {
}