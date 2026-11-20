package de.aivot.prosuna.backend.ai.models;

import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.model.ToolContext;

import java.util.HashMap;
import java.util.Map;

public record ChatContextModel(
        @Nonnull
        String sessionId,
        @Nullable
        ElementType targetRootType,
        @Nullable
        Integer processId,
        @Nullable
        Integer processVersion
) {
    public enum AppContext {
        FormEditor,
        ProcessEditor,
        GeneralChat,
    }

    public Map<String, Object> toMap() {
        var res = new HashMap<String, Object>();
        res.put("sessionId", sessionId);

        if (targetRootType != null) {
            res.put("targetRootType", targetRootType);
        }

        if (processId != null) {
            res.put("processId", processId);
        }

        if (processVersion != null) {
            res.put("processVersion", processVersion);
        }

        return res;
    }

    public static ChatContextModel fromToolContext(ToolContext toolContext) {
        var map = toolContext.getContext();
        return new ChatContextModel(
                (String) map.get("sessionId"),
                (ElementType) map.get("targetRootType"),
                (Integer) map.get("processId"),
                (Integer) map.get("processVersion")
        );
    }

    public AppContext getAppContext() {
        if (targetRootType != null) {
            return AppContext.FormEditor;
        } else if (processId != null) {
            return AppContext.ProcessEditor;
        } else {
            return AppContext.GeneralChat;
        }
    }
}
