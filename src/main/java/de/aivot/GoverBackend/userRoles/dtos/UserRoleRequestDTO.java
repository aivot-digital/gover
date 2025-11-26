package de.aivot.GoverBackend.userRoles.dtos;

import de.aivot.GoverBackend.lib.RequestDTO;
import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.annotation.Nullable;

public record UserRoleRequestDTO(
    @NotNull
    @NotBlank
    @Size(max = 64)
    String name,

    @Nullable
    String description,

    @NotNull
    Boolean orgUnitMemberPermissionEdit,
    @NotNull
    Boolean teamMemberPermissionEdit,
    @NotNull
    Boolean formPermissionCreate,
    @NotNull
    Boolean formPermissionRead,
    @NotNull
    Boolean formPermissionEdit,
    @NotNull
    Boolean formPermissionDelete,
    @NotNull
    Boolean formPermissionAnnotate,
    @NotNull
    Boolean formPermissionPublish,
    @NotNull
    Boolean processPermissionCreate,
    @NotNull
    Boolean processPermissionRead,
    @NotNull
    Boolean processPermissionEdit,
    @NotNull
    Boolean processPermissionDelete,
    @NotNull
    Boolean processPermissionAnnotate,
    @NotNull
    Boolean processPermissionPublish,
    @NotNull
    Boolean processInstancePermissionCreate,
    @NotNull
    Boolean processInstancePermissionRead,
    @NotNull
    Boolean processInstancePermissionEdit,
    @NotNull
    Boolean processInstancePermissionDelete,
    @NotNull
    Boolean processInstancePermissionAnnotate
) implements RequestDTO<UserRoleEntity> {
    @Override
    public UserRoleEntity toEntity() {
        UserRoleEntity entity = new UserRoleEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setDepartmentPermissionEdit(orgUnitMemberPermissionEdit);
        entity.setTeamPermissionEdit(teamMemberPermissionEdit);
        entity.setFormPermissionCreate(formPermissionCreate);
        entity.setFormPermissionRead(formPermissionRead);
        entity.setFormPermissionEdit(formPermissionEdit);
        entity.setFormPermissionDelete(formPermissionDelete);
        entity.setFormPermissionAnnotate(formPermissionAnnotate);
        entity.setFormPermissionPublish(formPermissionPublish);
        entity.setProcessPermissionCreate(processPermissionCreate);
        entity.setProcessPermissionRead(processPermissionRead);
        entity.setProcessPermissionEdit(processPermissionEdit);
        entity.setProcessPermissionDelete(processPermissionDelete);
        entity.setProcessPermissionAnnotate(processPermissionAnnotate);
        entity.setProcessPermissionPublish(processPermissionPublish);
        entity.setProcessInstancePermissionCreate(processInstancePermissionCreate);
        entity.setProcessInstancePermissionRead(processInstancePermissionRead);
        entity.setProcessInstancePermissionEdit(processInstancePermissionEdit);
        entity.setProcessInstancePermissionDelete(processInstancePermissionDelete);
        entity.setProcessInstancePermissionAnnotate(processInstancePermissionAnnotate);
        return entity;
    }
}
