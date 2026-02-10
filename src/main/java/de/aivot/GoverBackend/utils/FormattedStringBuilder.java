package de.aivot.GoverBackend.utils;

public class FormattedStringBuilder {
    private final StringBuilder builder;

    public FormattedStringBuilder() {
        this.builder = new StringBuilder();
    }

    public FormattedStringBuilder append(String format, Object... args) {
        builder.append(String.format(format, args));
        return this;
    }

    public FormattedStringBuilder appendLine(String format, Object... args) {
        builder.append(String.format(format, args)).append(System.lineSeparator());
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
