package de.aivot.GoverBackend.department.dtos;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitEntity;
import de.aivot.GoverBackend.department.entities.OrganizationalUnitMembershipEntity;
import de.aivot.GoverBackend.department.entities.VOrganizationalUnitMembershipWithDetailsEntity;
import de.aivot.GoverBackend.user.entities.UserEntity;

import javax.annotation.Nonnull;

public record DepartmentMembershipResponseDTO(
        @Nonnull
        Integer membershipId,
        @Nonnull
        Integer orgUnitId,
        @Nonnull
        String orgUnitName,
        @Nonnull
        String userId,
        @Nonnull
        String userFullName,
        @Nonnull
        String userEmail
) {
    public static DepartmentMembershipResponseDTO fromEntity(VOrganizationalUnitMembershipWithDetailsEntity membership) {
        return new DepartmentMembershipResponseDTO(
                membership.getMembershipId(),
                membership.getOrganizationalUnitId(),
                membership.getOrganizationalUnitName(),
                membership.getUserId(),
                membership.getUserFullName(),
                membership.getUserEmail()
        );
    }

    public static DepartmentMembershipResponseDTO fromEntity(OrganizationalUnitMembershipEntity membership, OrganizationalUnitEntity department, UserEntity user) {
        return new DepartmentMembershipResponseDTO(
                membership.getId(),
                department.getId(),
                department.getName(),
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );
    }
}
