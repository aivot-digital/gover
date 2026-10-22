package de.aivot.prosuna.backend.process.utils;

import de.aivot.prosuna.backend.markdown.utils.MarkdownTemplateValues;
import de.aivot.prosuna.backend.process.enums.TemplateOutputFormat;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.regex.Pattern;

/** Formats process data for insertion into a template of a given {@link TemplateOutputFormat}. */
public final class TemplateValueFormatter {
    private static final Pattern LINE_BREAKS = Pattern.compile("\\R+");

    private TemplateValueFormatter() {
    }

    /** Formats the value of a {@code {{ ... }}} tag. */
    @Nonnull
    public static String formatEscaped(@Nonnull TemplateOutputFormat format, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return switch (format) {
            case Html -> escapeHtmlAndJavascript(value);
            case Markdown -> MarkdownTemplateValues.escapeText(value);
            case PlainText -> value;
            case SingleLineText -> joinLines(value);
        };
    }

    /**
     * Formats the value of a {@code {! ... !}} tag. A whole field value taken from process data or code is formatted
     * the same way, because it is inserted without any surrounding template.
     */
    @Nonnull
    public static String formatRaw(@Nonnull TemplateOutputFormat format, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return switch (format) {
            case Html, PlainText -> value;
            case Markdown -> MarkdownTemplateValues.revealLinkTargets(value);
            // The text may end up in a mail header, where a line break would start a new header.
            case SingleLineText -> joinLines(value);
        };
    }

    @Nonnull
    private static String joinLines(@Nonnull String value) {
        return LINE_BREAKS.matcher(value).replaceAll(" ");
    }

    /**
     * Escapes both HTML and JavaScript-sensitive characters, because HTML templates may embed values in scripts as
     * well as in markup.
     */
    @Nonnull
    private static String escapeHtmlAndJavascript(@Nonnull String value) {
        var builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            var c = value.charAt(i);
            switch (c) {
                case '&' -> builder.append("&amp;");
                case '<' -> builder.append("&lt;");
                case '>' -> builder.append("&gt;");
                case '"' -> builder.append("&quot;");
                case '\'' -> builder.append("&#39;");
                case '\\' -> builder.append("\\\\");
                case '`' -> builder.append("\\`");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20 || c == ' ' || c == ' ') {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.toString();
    }
}
