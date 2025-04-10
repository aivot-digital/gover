package de.aivot.GoverBackend.elements.models.form.input;

import de.aivot.GoverBackend.utils.MapUtils;
import java.util.Map;
import java.util.Objects;

public class TextPattern {
    private String regex;
    private String message;

    public TextPattern(Map<String, Object> data) {
        this.regex = MapUtils.getString(data, "regex");
        this.message = MapUtils.getString(data, "message");
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TextPattern that = (TextPattern) o;
        return Objects.equals(regex, that.regex) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regex, message);
    }
}
