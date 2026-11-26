package de.aivot.prosuna.backend.userRoles.dtos;

import de.aivot.prosuna.backend.lib.RequestDTO;
import de.aivot.prosuna.backend.userRoles.entities.UserRoleEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.annotation.Nullable;

import java.util.List;

public record UserRoleRequestDTO(
    @NotNull(message = "Geben Sie einen Namen für die Domänenrolle ein.")
    @NotBlank(message = "Geben Sie einen Namen für die Domänenrolle ein.")
    @Size(max = 64, message = "Der Name der Domänenrolle darf höchstens 64 Zeichen lang sein.")
    String name,

    @Nullable
    String description,

    @NotNull(message = "Die Berechtigungen der Domänenrolle müssen angegeben werden.")
    List<String> permissions,

    @Nullable
    Long lockVersion
) implements RequestDTO<UserRoleEntity> {
    @Override
    public UserRoleEntity toEntity() {
        UserRoleEntity entity = new UserRoleEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setPermissions(permissions);
        entity.setLockVersion(lockVersion);
        return entity;
    }
}
