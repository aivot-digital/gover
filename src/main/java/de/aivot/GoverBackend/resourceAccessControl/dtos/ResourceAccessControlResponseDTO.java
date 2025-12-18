package de.aivot.GoverBackend.resourceAccessControl.dtos;

import de.aivot.GoverBackend.resourceAccessControl.entities.ResourceAccessControlEntity;

import java.time.LocalDateTime;
import java.util.List;

public record ResourceAccessControlResponseDTO(
        Integer id,
        Integer sourceTeamId,
        Integer sourceOrgUnitId,
        Integer targetProcessId,
        Integer targetProcessInstanceId,
        Integer targetProcessInstanceTaskId,
        List<String> permissions,
        LocalDateTime created,
        LocalDateTime updated
) {
    public static ResourceAccessControlResponseDTO fromEntity(ResourceAccessControlEntity entity) {
        return new ResourceAccessControlResponseDTO(
                entity.getId(),
                entity.getSourceTeamId(),
                entity.getSourceDepartmentId(),
                entity.getTargetProcessId(),
                entity.getTargetProcessInstanceId(),
                entity.getTargetProcessInstanceTaskId(),
                entity.getPermissions(),
                entity.getCreated(),
                entity.getUpdated()
        );
    }
}

