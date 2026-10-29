package de.aivot.prosuna.backend.javascript.enums;

import jakarta.annotation.Nonnull;

/**
 * Describes where JavaScript code comes from, so the {@link de.aivot.prosuna.backend.javascript.services.JavascriptEngine}
 * only provides the function providers that are permitted there.
 */
public enum JavascriptExecutionContext {
    /**
     * Code that is part of a process or form definition, such as node configurations and form logic. Only staff with
     * permission to edit the process can change it, so all function providers are available.
     */
    Default("in diesem Kontext"),

    /**
     * HTML templates loaded from Dateien &amp; Medien. Their content can be changed with the asset permissions alone,
     * independent of the permission to edit the processes that use them. Only function providers that opt in are
     * available.
     */
    AssetTemplate("in HTML-Vorlagen aus Dateien & Medien");

    private final String description;

    JavascriptExecutionContext(@Nonnull String description) {
        this.description = description;
    }

    /**
     * Returns the German phrase that completes messages such as „Die Funktionen … sind <i>description</i> nicht
     * verfügbar.“
     */
    @Nonnull
    public String getDescription() {
        return description;
    }
}
