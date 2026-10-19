package de.aivot.prosuna.backend.ai.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

public record AiChatTraceContext(
        @Nonnull String userId,
        @Nonnull String sessionId,
        @Nonnull String turnId
) {
    public static final String CONTEXT_KEY = AiChatTraceContext.class.getName();

    @Nullable
    public static AiChatTraceContext from(@Nonnull Map<String, ?> context) {
        var value = context.get(CONTEXT_KEY);
        return value instanceof AiChatTraceContext traceContext ? traceContext : null;
    }
}
