package de.aivot.GoverBackend.userRoles.dtos;

import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;

import java.time.LocalDateTime;
import java.util.List;

public record UserRoleResponseDTO(
        Integer id,
        String name,
        String description,
        List<String> permissions,
        LocalDateTime created,
        LocalDateTime updated
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
