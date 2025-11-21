package de.aivot.GoverBackend.userRoles.dtos;

import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import java.time.LocalDateTime;

public record UserRoleAssignmentResponseDTO(
    Integer id,
    Integer organizationalUnitMembershipId,
    Integer teamMembershipId,
    Integer userRoleId,
    LocalDateTime created
) {
    public static UserRoleAssignmentResponseDTO fromEntity(UserRoleAssignmentEntity entity) {
        return new UserRoleAssignmentResponseDTO(
            entity.getId(),
            entity.getOrganizationalUnitMembershipId(),
            entity.getTeamMembershipId(),
            entity.getUserRoleId(),
            entity.getCreated()
        );
    }
}
