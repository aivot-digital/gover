package de.aivot.prosuna.backend.ai.models;

import jakarta.annotation.Nonnull;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

public record AiChatAttachmentContext(
        @Nonnull AiChatAttachmentMetadata metadata,
        @Nonnull String initialContext,
        @Nonnull Map<String, List<String>> sections
) {
    public static final String CONTEXT_KEY = AiChatAttachmentContext.class.getName();
    public static final String MESSAGE_METADATA_KEY = "prosunaAiAttachmentContext";
    public static final String ORIGINAL_TEXT_METADATA_KEY = "prosunaAiAttachmentOriginalText";
    public static final String ATTACHMENTS_METADATA_KEY = "attachments";

    public AiChatAttachmentContext {
        sections = Map.copyOf(sections);
    }

    @Nonnull
    public static AiChatAttachmentContext fromToolContext(@Nonnull ToolContext toolContext) {
        var value = toolContext.getContext().get(CONTEXT_KEY);
        if (value instanceof AiChatAttachmentContext context) {
            return context;
        }
        throw new IllegalStateException("Der Dateianhang ist in diesem Aufruf nicht verfügbar.");
    }
}
