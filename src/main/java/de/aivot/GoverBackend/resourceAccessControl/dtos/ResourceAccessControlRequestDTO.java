package de.aivot.GoverBackend.resourceAccessControl.dtos;

import de.aivot.GoverBackend.lib.RequestDTO;
import de.aivot.GoverBackend.resourceAccessControl.entities.ResourceAccessControlEntity;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record ResourceAccessControlRequestDTO(
    @Nullable Integer sourceTeamId,
    @Nullable Integer sourceOrgUnitId,
    @Nullable Integer targetFormId,
    @Nullable Integer targetProcessId,
    @Nullable Integer targetProcessInstanceId,
    @NotNull Boolean formPermissionCreate,
    @NotNull Boolean formPermissionRead,
    @NotNull Boolean formPermissionEdit,
    @NotNull Boolean formPermissionDelete,
    @NotNull Boolean formPermissionAnnotate,
    @NotNull Boolean formPermissionPublish,
    @NotNull Boolean processPermissionCreate,
    @NotNull Boolean processPermissionRead,
    @NotNull Boolean processPermissionEdit,
    @NotNull Boolean processPermissionDelete,
    @NotNull Boolean processPermissionAnnotate,
    @NotNull Boolean processPermissionPublish,
    @NotNull Boolean processInstancePermissionCreate,
    @NotNull Boolean processInstancePermissionRead,
    @NotNull Boolean processInstancePermissionEdit,
    @NotNull Boolean processInstancePermissionDelete,
    @NotNull Boolean processInstancePermissionAnnotate
) implements RequestDTO<ResourceAccessControlEntity> {
    @Override
    public ResourceAccessControlEntity toEntity() {
        ResourceAccessControlEntity entity = new ResourceAccessControlEntity();
        entity.setSourceTeamId(sourceTeamId);
        entity.setSourceDepartmentId(sourceOrgUnitId);
        entity.setTargetFormId(targetFormId);
        entity.setTargetProcessId(targetProcessId);
        entity.setTargetProcessInstanceId(targetProcessInstanceId);
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

