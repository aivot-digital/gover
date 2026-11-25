package de.aivot.prosuna.backend.process.utils;

import jakarta.annotation.Nonnull;

import java.util.Map;

/** Shared marker for staff actions outside the modeled workflow, with an instance or task as their context. */
public final class ProcessHistoryInterventions {
    public static final String DETAILS_KEY = "manualIntervention";

    private ProcessHistoryInterventions() {
    }

    @Nonnull
    public static Map<String, Object> details() {
        return Map.of(DETAILS_KEY, true);
    }
}
