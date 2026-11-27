package de.aivot.prosuna.backend.process.utils;

import jakarta.annotation.Nullable;

import java.util.regex.Pattern;

public final class ProcessNodeDataKeyUtils {
    // Node keys must also work in JavaScript dot notation (_.key).
    public static final String REGEX = "[a-zA-Z_$][a-zA-Z0-9_$]*";
    public static final String FORMAT_ERROR = "Verwenden Sie Buchstaben von A bis Z, Zahlen, Unterstriche oder $. Der Datenschlüssel darf nicht mit einer Zahl beginnen und keine Punkte oder Klammern enthalten.";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private ProcessNodeDataKeyUtils() {
    }

    @Nullable
    public static String getValidationError(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "Geben Sie einen Datenschlüssel ein.";
        }
        if (value.length() > 32) {
            return "Der Datenschlüssel darf maximal 32 Zeichen lang sein.";
        }
        return PATTERN.matcher(value).matches() ? null : FORMAT_ERROR;
    }
}
