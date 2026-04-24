package de.aivot.GoverBackend.userRoles.dtos;

import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;

import java.time.Instant;
import java.util.List;

public record UserRoleResponseDTO(
        Integer id,
        String name,
        String description,
        List<String> permissions,
        Instant created,
        Instant updated
) {
    public static UserRoleResponseDTO fromEntity(UserRoleEntity entity) {
        return new UserRoleResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPermissions(),
                entity.getCreated(),
                entity.getUpdated()
        );
    }
}
