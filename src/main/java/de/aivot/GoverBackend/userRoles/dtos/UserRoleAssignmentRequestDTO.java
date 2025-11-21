package de.aivot.GoverBackend.userRoles.dtos;

import de.aivot.GoverBackend.lib.RequestDTO;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

public record UserRoleAssignmentRequestDTO(
    @Nullable
    Integer organizationalUnitMembershipId,
    @Nullable
    Integer teamMembershipId,
    @NotNull
    Integer userRoleId
) implements RequestDTO<UserRoleAssignmentEntity> {
    @Override
    public UserRoleAssignmentEntity toEntity() {
        UserRoleAssignmentEntity entity = new UserRoleAssignmentEntity();
        entity.setOrganizationalUnitMembershipId(organizationalUnitMembershipId);
        entity.setTeamMembershipId(teamMembershipId);
        entity.setUserRoleId(userRoleId);
        return entity;
    }
}
