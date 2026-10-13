package de.aivot.prosuna.backend.process.models;

import jakarta.annotation.Nonnull;

import java.util.List;

/** Authoring ports and recoverable problems, so invalid drafts remain editable. */
public record ProcessNodePortResolution(
        @Nonnull List<ProcessNodePort> ports,
        @Nonnull List<String> problems,
        @Nonnull List<ProcessNodeDeadlineDefinition> deadlineDefinitions
) {
    public ProcessNodePortResolution(List<ProcessNodePort> ports, List<String> problems) {
        this(ports, problems, List.of());
    }

    public ProcessNodePortResolution {
        ports = List.copyOf(ports);
        problems = List.copyOf(problems);
        deadlineDefinitions = List.copyOf(deadlineDefinitions);
    }
}
