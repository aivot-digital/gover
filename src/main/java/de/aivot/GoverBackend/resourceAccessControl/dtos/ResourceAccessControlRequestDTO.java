package de.aivot.GoverBackend.resourceAccessControl.dtos;

import de.aivot.GoverBackend.lib.RequestDTO;
import de.aivot.GoverBackend.resourceAccessControl.entities.ResourceAccessControlEntity;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ResourceAccessControlRequestDTO(
        @Nullable Integer sourceTeamId,
        @Nullable Integer sourceOrgUnitId,
        @NotNull Integer targetProcessId,
        @Nullable Integer targetProcessInstanceId,
        @Nullable Integer targetProcessInstanceTaskId,
        @NotNull List<String> permissions
) implements RequestDTO<ResourceAccessControlEntity> {
    @Override
    public ResourceAccessControlEntity toEntity() {
        ResourceAccessControlEntity entity = new ResourceAccessControlEntity();
        entity.setSourceTeamId(sourceTeamId);
        entity.setSourceDepartmentId(sourceOrgUnitId);
        entity.setTargetProcessId(targetProcessId);
        entity.setTargetProcessInstanceId(targetProcessInstanceId);
        entity.setTargetProcessInstanceTaskId(targetProcessInstanceTaskId);
        entity.setPermissions(permissions);
        return entity;
    }
}

