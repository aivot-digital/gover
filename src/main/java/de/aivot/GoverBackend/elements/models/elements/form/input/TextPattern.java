package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.util.Objects;

public class TextPattern {
    @Nullable
    private String regex;
    @Nullable
    private String message;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        TextPattern that = (TextPattern) o;
        return Objects.equals(regex, that.regex) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(regex);
        result = 31 * result + Objects.hashCode(message);
        return result;
    }

    @Nullable
    public String getRegex() {
        return regex;
    }

    public TextPattern setRegex(@Nullable String regex) {
        this.regex = regex;
        return this;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public TextPattern setMessage(@Nullable String message) {
        this.message = message;
        return this;
    }
}
