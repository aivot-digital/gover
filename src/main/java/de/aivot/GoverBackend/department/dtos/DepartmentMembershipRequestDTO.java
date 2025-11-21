package de.aivot.GoverBackend.department.dtos;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitMembershipEntity;
import de.aivot.GoverBackend.lib.RequestDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import javax.annotation.Nonnull;

public record DepartmentMembershipRequestDTO(
        @Nonnull
        @NotNull(message = "Organizational unit id cannot be null")
        Integer organizationalUnitId,

        @Nonnull
        @NotNull(message = "User id cannot be null")
        @NotBlank(message = "User id cannot be blank")
        @Length(min = 36, max = 36)
        String userId
) implements RequestDTO<OrganizationalUnitMembershipEntity> {
    @Override
    public OrganizationalUnitMembershipEntity toEntity() {
        return new OrganizationalUnitMembershipEntity()
                .setOrganizationalUnitId(organizationalUnitId)
                .setUserId(userId);
    }
}
