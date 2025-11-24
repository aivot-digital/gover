package de.aivot.GoverBackend.resourceAccessControl.dtos;

import de.aivot.GoverBackend.resourceAccessControl.entities.ResourceAccessControlEntity;

import java.time.LocalDateTime;

public record ResourceAccessControlResponseDTO(
    Integer id,
    Integer sourceTeamId,
    Integer sourceOrgUnitId,
    Integer targetFormId,
    Integer targetProcessId,
    Integer targetProcessInstanceId,
    Boolean formPermissionCreate,
    Boolean formPermissionRead,
    Boolean formPermissionEdit,
    Boolean formPermissionDelete,
    Boolean formPermissionAnnotate,
    Boolean formPermissionPublish,
    Boolean processPermissionCreate,
    Boolean processPermissionRead,
    Boolean processPermissionEdit,
    Boolean processPermissionDelete,
    Boolean processPermissionAnnotate,
    Boolean processPermissionPublish,
    Boolean processInstancePermissionCreate,
    Boolean processInstancePermissionRead,
    Boolean processInstancePermissionEdit,
    Boolean processInstancePermissionDelete,
    Boolean processInstancePermissionAnnotate,
    LocalDateTime created,
    LocalDateTime updated
) {
    public static ResourceAccessControlResponseDTO fromEntity(ResourceAccessControlEntity entity) {
        return new ResourceAccessControlResponseDTO(
            entity.getId(),
            entity.getSourceTeamId(),
            entity.getSourceOrgUnitId(),
            entity.getTargetFormId(),
            entity.getTargetProcessId(),
            entity.getTargetProcessInstanceId(),
            entity.getFormPermissionCreate(),
            entity.getFormPermissionRead(),
            entity.getFormPermissionEdit(),
            entity.getFormPermissionDelete(),
            entity.getFormPermissionAnnotate(),
            entity.getFormPermissionPublish(),
            entity.getProcessPermissionCreate(),
            entity.getProcessPermissionRead(),
            entity.getProcessPermissionEdit(),
            entity.getProcessPermissionDelete(),
            entity.getProcessPermissionAnnotate(),
            entity.getProcessPermissionPublish(),
            entity.getProcessInstancePermissionCreate(),
            entity.getProcessInstancePermissionRead(),
            entity.getProcessInstancePermissionEdit(),
            entity.getProcessInstancePermissionDelete(),
            entity.getProcessInstancePermissionAnnotate(),
            entity.getCreated(),
            entity.getUpdated()
        );
    }
}

