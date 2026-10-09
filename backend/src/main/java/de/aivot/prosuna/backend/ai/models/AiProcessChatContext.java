package de.aivot.prosuna.backend.ai.models;

import jakarta.annotation.Nonnull;
import org.springframework.ai.chat.model.ToolContext;

public record AiProcessChatContext(@Nonnull String userId, @Nonnull String sessionId,
                                   @Nonnull Integer processId, @Nonnull Integer processVersion) {
    public static final String KEY = AiProcessChatContext.class.getName();

    @Nonnull
    public static AiProcessChatContext from(@Nonnull ToolContext context) {
        if (context.getContext().get(KEY) instanceof AiProcessChatContext scope) return scope;
        throw new IllegalArgumentException("Es ist kein Prozessbearbeitungskontext verfügbar.");
    }
}
