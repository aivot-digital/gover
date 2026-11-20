package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.models.AiChatAttachmentContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

@Service
public class AiChatAttachmentTools {
    public static final String TOOL_NAME = "lese-chat-anhang";

    @Nonnull
    @Tool(name = TOOL_NAME, description = """
            Liest einen Bereich der nur für die aktuelle Anfrage hochgeladenen Datei blockweise.
            Verwende einen Bereich aus dem anfänglichen Dateikontext und folge nextOffset nur bei Bedarf.
            Pro Aufruf werden höchstens zwei Blöcke zurückgegeben.
            """)
    public Object readAttachment(
            @ToolParam(description = "Bereichsname aus dem anfänglichen Dateikontext", required = true)
            @Nonnull String section,
            @ToolParam(description = "Blockoffset, Standard 0", required = false)
            @Nullable Integer offset,
            @ToolParam(description = "Anzahl Blöcke, Standard 1 und höchstens 2", required = false)
            @Nullable Integer limit,
            @Nonnull ToolContext toolContext
    ) {
        var context = AiChatAttachmentContext.fromToolContext(toolContext);
        var blocks = context.sections().get(section);
        if (blocks == null) {
            return "Unbekannter Bereich. Verfügbar sind: " + String.join(", ", context.sections().keySet());
        }

        var effectiveOffset = offset == null ? 0 : Math.max(0, offset);
        var effectiveLimit = Math.min(2, limit == null ? 1 : Math.max(1, limit));
        var end = Math.min(blocks.size(), effectiveOffset + effectiveLimit);
        if (effectiveOffset >= blocks.size()) {
            effectiveOffset = blocks.size();
            end = blocks.size();
        }

        var result = new LinkedHashMap<String, Object>();
        result.put("section", section);
        result.put("offset", effectiveOffset);
        result.put("items", blocks.subList(effectiveOffset, end));
        result.put("nextOffset", end < blocks.size() ? end : null);
        result.put("total", blocks.size());
        return result;
    }
}
