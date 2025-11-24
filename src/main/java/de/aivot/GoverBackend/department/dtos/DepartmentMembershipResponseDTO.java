package de.aivot.GoverBackend.department.dtos;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitEntity;
import de.aivot.GoverBackend.department.entities.OrganizationalUnitMembershipEntity;
import de.aivot.GoverBackend.department.entities.VOrganizationalUnitMembershipWithDetailsEntity;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record DepartmentMembershipResponseDTO(
        @Nonnull
        Integer membershipId,
        @Nonnull
        Integer orgUnitId,
        @Nonnull
        String orgUnitName,
        @Nullable
        Integer organizationalUnitParentOrgUnitId,
        @Nonnull
        Integer organizationalUnitDepth,
        @Nonnull
        String userId,
        @Nonnull
        String userFirstName,
        @Nonnull
        String userLastName,
        @Nonnull
        String userFullName,
        @Nonnull
        String userEmail,
        @Nonnull
        Boolean userEnabled,
        @Nonnull
        Boolean userVerified,
        @Nonnull
        Boolean userGlobalAdmin,
        @Nonnull
        Boolean userDeletedInIdp
) {
    public static DepartmentMembershipResponseDTO fromEntity(VOrganizationalUnitMembershipWithDetailsEntity membership) {
        return new DepartmentMembershipResponseDTO(
                membership.getMembershipId(),
                membership.getOrganizationalUnitId(),
                membership.getOrganizationalUnitName(),
                membership.getOrganizationalUnitParentOrgUnitId(),
                membership.getOrganizationalUnitDepth(),
                membership.getUserId(),
                membership.getUserFirstName(),
                membership.getUserLastName(),
                membership.getUserFullName(),
                membership.getUserEmail(),
                membership.getUserEnabled(),
                membership.getUserVerified(),
                membership.getUserGlobalAdmin(),
                membership.getUserDeletedInIdp()
        );
    }

    public static DepartmentMembershipResponseDTO fromEntity(OrganizationalUnitMembershipEntity membership, OrganizationalUnitEntity department, UserEntity user) {
        return new DepartmentMembershipResponseDTO(
                membership.getId(),
                department.getId(),
                department.getName(),
                department.getParentOrgUnitId(),
                department.getDepth(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getEmail(),
                user.getEnabled(),
                user.getVerified(),
                user.getGlobalAdmin(),
                user.getDeletedInIdp()
        );
    }
}
