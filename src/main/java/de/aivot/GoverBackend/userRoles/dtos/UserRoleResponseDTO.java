package de.aivot.GoverBackend.userRoles.dtos;

import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;

import java.time.LocalDateTime;

public record UserRoleResponseDTO(
        Integer id,
        String name,
        String description,
        Boolean orgUnitMemberPermissionEdit,
        Boolean teamMemberPermissionEdit,
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
    public static UserRoleResponseDTO fromEntity(UserRoleEntity entity) {
        return new UserRoleResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getOrgUnitMemberPermissionEdit(),
                entity.getTeamMemberPermissionEdit(),
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
