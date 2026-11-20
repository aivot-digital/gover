package de.aivot.prosuna.backend.ai.models;

import de.aivot.prosuna.backend.elements.enums.InputMode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.tool.annotation.ToolParam;

public record AiProcessConfigurationChange(
        @ToolParam(description = "Exakter valuePath aus der Feldliste")
        @Nonnull String valuePath,
        @ToolParam(description = "Literal, Variable, NoCode oder LowCode")
        @Nonnull InputMode mode,
        @ToolParam(description = "Literal: Rohwert; Variable: Referenz; NoCode: Operand; LowCode: JavaScript-Text")
        @Nullable Object value
) {
}
