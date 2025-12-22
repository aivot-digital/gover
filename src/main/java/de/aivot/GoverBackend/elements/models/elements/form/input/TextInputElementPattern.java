package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class TextInputElementPattern implements Serializable {
    @Nullable
    private String regex;

    @Nullable
    private String message;

    public static TextInputElementPattern of(String regex, String message) {
        return new TextInputElementPattern()
                .setRegex(regex)
                .setMessage(message);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        TextInputElementPattern that = (TextInputElementPattern) o;
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

    public TextInputElementPattern setRegex(@Nullable String regex) {
        this.regex = regex;
        return this;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public TextInputElementPattern setMessage(@Nullable String message) {
        this.message = message;
        return this;
    }
}
