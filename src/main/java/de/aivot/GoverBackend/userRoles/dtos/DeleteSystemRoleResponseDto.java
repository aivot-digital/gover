package de.aivot.GoverBackend.userRoles.dtos;

import jakarta.annotation.Nullable;

public record DeleteSystemRoleResponseDto(
        int migratedUsersCount,
        boolean defaultSystemRoleForAutomaticImportsUpdated,
        @Nullable Integer newDefaultSystemRoleId
) {
}
