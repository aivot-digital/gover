package de.aivot.GoverBackend.userRoles.dtos;

import de.aivot.GoverBackend.lib.RequestDTO;
import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.annotation.Nullable;

import java.util.List;

public record UserRoleRequestDTO(
    @NotNull
    @NotBlank
    @Size(max = 64)
    String name,

    @Nullable
    String description,

    @NotNull
    List<String> permissions
) implements RequestDTO<UserRoleEntity> {
    @Override
    public UserRoleEntity toEntity() {
        UserRoleEntity entity = new UserRoleEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setPermissions(permissions);
        return entity;
    }
}
