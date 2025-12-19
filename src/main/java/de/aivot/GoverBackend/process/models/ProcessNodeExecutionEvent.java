package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import jakarta.annotation.Nonnull;

import java.util.Map;

public record ProcessNodeExecutionEvent(
        @Nonnull
        ProcessHistoryEventType type,
        @Nonnull
        String title,
        @Nonnull
        String message,
        @Nonnull
        Map<String, Object> details
) {
    public static ProcessNodeExecutionEvent of(
            @Nonnull ProcessHistoryEventType type,
            @Nonnull String title,
            @Nonnull String message,
            @Nonnull Map<String, Object> details
    ) {
        return new ProcessNodeExecutionEvent(type, title, message, details);
    }

    public static ProcessNodeExecutionEvent of(
            @Nonnull ProcessHistoryEventType type,
            @Nonnull String title,
            @Nonnull String message
    ) {
        return new ProcessNodeExecutionEvent(type, title, message, Map.of());
    }
}
