package de.aivot.prosuna.backend.department.dtos;

import de.aivot.prosuna.backend.department.entities.DepartmentMembershipEntity;
import de.aivot.prosuna.backend.lib.RequestDTO;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DepartmentMembershipCreateRequestDTO(
        @NotNull
        Integer departmentId,

        @NotNull
        String userId,

        @Nullable
        List<Integer> roleIds
) implements RequestDTO<DepartmentMembershipEntity> {
    @Override
    public DepartmentMembershipEntity toEntity() {
        return new DepartmentMembershipEntity()
                .setDepartmentId(departmentId)
                .setUserId(userId);
    }

    public List<Integer> roleIdsOrEmpty() {
        return roleIds != null ? roleIds : List.of();
    }
}
