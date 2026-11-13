package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import jakarta.annotation.Nonnull;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class AiChatSharedTools {
    @Nonnull
    @Tool(name = "hole-chatmodus", description = """
            Bestimme den aktuellen Chatmodus anhand des bereitgestellten Kontexts.
            Gibt "Formular" zurück, wenn ein Formular bearbeitet werden soll,
            "Prozess", wenn ein Prozess bearbeitet werden soll, oder
            "Allgemein", wenn es sich um eine allgemeine Chatsitzung ohne spezifischen Bearbeitungskontext handelt.
            """)
    public String getChatMode(@Nonnull ToolContext toolContext) {
        var context = ChatContextModel.fromToolContext(toolContext);

        switch (context.getAppContext()) {
            case FormEditor:
                return "Formular";
            case ProcessEditor:
                return "Prozess";
            default:
                return "Allgemein";
        }
    }
}
