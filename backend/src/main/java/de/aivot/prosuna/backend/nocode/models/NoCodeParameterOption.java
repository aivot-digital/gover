package de.aivot.prosuna.backend.nocode.models;

import jakarta.annotation.Nullable;

public record NoCodeParameterOption(String label, String value, @Nullable String singularLabel) {
    public NoCodeParameterOption(String label, String value) {
        this(label, value, null);
    }
}
