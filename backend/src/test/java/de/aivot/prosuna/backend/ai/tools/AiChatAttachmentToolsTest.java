package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.models.AiChatAttachmentContext;
import de.aivot.prosuna.backend.ai.models.AiChatAttachmentMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatAttachmentToolsTest {
    private final AiChatAttachmentTools tools = new AiChatAttachmentTools();

    @Test
    void readsAtMostTwoBlocksAndReturnsTheNextOffset() {
        var attachment = new AiChatAttachmentContext(
                new AiChatAttachmentMetadata("file.txt", 12, "text/plain"),
                "initial",
                Map.of("text", List.of("one", "two", "three"))
        );
        var toolContext = new ToolContext(Map.of(AiChatAttachmentContext.CONTEXT_KEY, attachment));

        assertThat(tools.readAttachment("text", 0, 99, toolContext)).isEqualTo(Map.of(
                "section", "text",
                "offset", 0,
                "items", List.of("one", "two"),
                "nextOffset", 2,
                "total", 3
        ));
        assertThat(tools.readAttachment("missing", null, null, toolContext).toString())
                .contains("Unbekannter Bereich", "text");
    }
}
