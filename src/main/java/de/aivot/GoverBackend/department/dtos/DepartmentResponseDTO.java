package de.aivot.GoverBackend.department.dtos;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.entities.DepartmentWithMembershipEntity;
import de.aivot.GoverBackend.department.entities.ShadowedOrganizationalUnitEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDateTime;

public record DepartmentResponseDTO(
        @Nonnull
        Integer id,
        @Nullable
        String name,
        @Nullable
        String address,
        @Nullable
        String imprint,
        @Nullable
        String privacy,
        @Nullable
        String accessibility,
        @Nullable
        String technicalSupportAddress,
        @Nullable
        String specialSupportAddress,
        @Nullable
        String departmentMail,
        @Nullable
        Integer themeId,
        @Nullable
        String contactLegal,
        @Nullable
        String contactTechnical,
        @Nullable
        String additionalInfo,
        @Nonnull
        Integer depth,
        @Nullable
        Integer parentOrgUnitId,
        @Nonnull
        LocalDateTime created,
        @Nonnull
        LocalDateTime updated
) {
    public static DepartmentResponseDTO fromEntity(DepartmentEntity department) {
        return new DepartmentResponseDTO(
                department.getId(),
                department.getName(),
                department.getAddress(),
                department.getImprint(),
                department.getPrivacy(),
                department.getAccessibility(),
                department.getTechnicalSupportAddress(),
                department.getSpecialSupportAddress(),
                department.getDepartmentMail(),
                department.getThemeId(),
                null,
                null,
                null,
                0,
                0,
                department.getCreated(),
                department.getUpdated()
        );
    }

    public static DepartmentResponseDTO fromEntity(DepartmentWithMembershipEntity department) {
        return new DepartmentResponseDTO(
                department.getId(),
                department.getName(),
                department.getAddress(),
                department.getImprint(),
                department.getPrivacy(),
                department.getAccessibility(),
                department.getTechnicalSupportAddress(),
                department.getSpecialSupportAddress(),
                department.getDepartmentMail(),
                department.getThemeId(),
                null,
                null,
                null,
                0,
                0,
                department.getCreated(),
                department.getUpdated()
        );
    }

    public static DepartmentResponseDTO fromEntity(ShadowedOrganizationalUnitEntity department) {
        return new DepartmentResponseDTO(
                department.getId(),
                department.getName(),
                department.getAddress(),
                department.getImprint(),
                department.getPrivacy(),
                department.getAccessibility(),
                department.getTechnicalSupportAddress(),
                department.getSpecialSupportAddress(),
                department.getDepartmentMail(),
                department.getThemeId(),
                department.getContactLegal(),
                department.getContactTechnical(),
                department.getAdditionalInfo(),
                department.getDepth(),
                department.getParentOrgUnitId(),
                department.getCreated(),
                department.getUpdated()
        );
    }
}
