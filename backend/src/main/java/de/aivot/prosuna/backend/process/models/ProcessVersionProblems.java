package de.aivot.prosuna.backend.process.models;

import jakarta.annotation.Nonnull;

import java.util.List;

public record ProcessVersionProblems(
        @Nonnull List<String> versionProblems,
        @Nonnull List<ProcessNodeProblems> nodeProblems
) {
    public boolean hasAnyProblems() {
        return !versionProblems.isEmpty() || nodeProblems.stream().anyMatch(ProcessNodeProblems::hasAnyProblems);
    }
}
