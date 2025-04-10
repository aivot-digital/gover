package de.aivot.GoverBackend.department.dtos;

import de.aivot.GoverBackend.department.entities.DepartmentMembershipEntity;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.lib.ReqeustDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import javax.annotation.Nonnull;

public record DepartmentMembershipRequestDTO(
        @Nonnull
        @NotNull(message = "Department id cannot be null")
        Integer departmentId,

        @Nonnull
        @NotNull(message = "UserId cannot be null")
        @NotBlank(message = "UserId cannot be blank")
        @Length(min = 36, max = 36)
        String userId,

        @Nonnull
        @NotNull(message = "Role cannot be null")
        UserRole role
) implements ReqeustDTO<DepartmentMembershipEntity> {
    @Override
    public DepartmentMembershipEntity toEntity() {
        var membership = new DepartmentMembershipEntity();
        membership.setDepartmentId(departmentId());
        membership.setUserId(userId());
        membership.setRole(role());
        return membership;
    }
}
