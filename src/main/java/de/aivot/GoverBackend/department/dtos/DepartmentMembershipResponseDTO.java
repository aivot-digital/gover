package de.aivot.GoverBackend.department.dtos;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.entities.DepartmentMembershipEntity;
import de.aivot.GoverBackend.department.entities.DepartmentWithMembershipEntity;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.user.entities.UserEntity;

import javax.annotation.Nonnull;

public record DepartmentMembershipResponseDTO(
        @Nonnull
        Integer id,
        @Nonnull
        Integer departmentId,
        @Nonnull
        String departmentName,
        @Nonnull
        String userId,
        @Nonnull
        UserRole role,
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
        Boolean userDeletedInIdp,
        @Nonnull
        Boolean userGlobalAdmin
) {
    public static DepartmentMembershipResponseDTO fromEntity(DepartmentWithMembershipEntity membership) {
        return new DepartmentMembershipResponseDTO(
                membership.getMembershipId(),
                membership.getId(),
                membership.getName(),
                membership.getUserId(),
                membership.getMembershipRole(),
                membership.getUserFirstName(),
                membership.getUserLastName(),
                membership.getUserFullName(),
                membership.getUserEmail(),
                membership.getUserEnabled(),
                membership.getUserVerified(),
                membership.getUserDeletedInIdp(),
                membership.getUserGlobalAdmin()
        );
    }

    public static DepartmentMembershipResponseDTO fromEntity(DepartmentMembershipEntity membership, DepartmentEntity department, UserEntity user) {
        return new DepartmentMembershipResponseDTO(
                membership.getId(),
                department.getId(),
                department.getName(),
                membership.getUserId(),
                membership.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getEmail(),
                user.getEnabled(),
                user.getVerified(),
                user.getDeletedInIdp(),
                user.getGlobalAdmin()
        );
    }
}
