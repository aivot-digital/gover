package de.aivot.prosuna.backend.userRoles.dtos;

import jakarta.annotation.Nullable;

public record DeleteSystemRoleResponseDto(
        int migratedUsersCount,
        boolean defaultSystemRoleForAutomaticImportsUpdated,
        @Nullable Integer newDefaultSystemRoleId,
        boolean mostPrivilegedSystemRoleUpdated,
        @Nullable Integer newMostPrivilegedSystemRoleId
) {
}
