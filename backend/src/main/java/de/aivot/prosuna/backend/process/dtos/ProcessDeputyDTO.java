package de.aivot.prosuna.backend.process.dtos;

import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A person currently representing the assignee of an open task. Like assignee names, deputy names are display
 * metadata for readers of the task and expose no further user details.
 */
public record ProcessDeputyDTO(@Nonnull String id, @Nonnull String name) {
    @Nonnull
    public static List<ProcessDeputyDTO> of(@Nonnull Map<String, List<UserEntity>> deputies, @Nullable String assignedUserId) {
        return assignedUserId == null ? List.of() : deputies.getOrDefault(assignedUserId, List.of()).stream()
                .map(user -> new ProcessDeputyDTO(user.getId(), user.getFullName()))
                .toList();
    }
}
