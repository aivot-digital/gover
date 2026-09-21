package de.aivot.prosuna.backend.ai.permissions;

import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AiChatPermissionProvider implements PermissionProvider {
    public static final String AI_CHAT_USE = "ai_chat.use";

    @Override
    public String getContextLabel() {
        return "KI-Chat";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(AI_CHAT_USE, "KI-Chat verwenden", "Erlaubt die Verwendung des KI-Chats."),
        };
    }
}
