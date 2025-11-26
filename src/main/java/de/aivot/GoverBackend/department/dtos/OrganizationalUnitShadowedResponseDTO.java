package de.aivot.GoverBackend.department.dtos;

import de.aivot.GoverBackend.department.entities.VOrganizationalUnitShadowedEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;

public record OrganizationalUnitShadowedResponseDTO(
        @Nonnull Integer id,
        @Nullable Integer parentOrgUnitId,
        @Nonnull Integer depth,
        @Nonnull String name,
        @Nullable String address,
        @Nullable String imprint,
        @Nullable String commonPrivacy,
        @Nullable String commonAccessibility,
        @Nullable String technicalSupportAddress,
        @Nullable String technicalSupportPhone,
        @Nullable String technicalSupportInfo,
        @Nullable String specialSupportAddress,
        @Nullable String specialSupportPhone,
        @Nullable String specialSupportInfo,
        @Nullable String additionalInfo,
        @Nullable String departmentMail,
        @Nullable Integer themeId,
        @Nonnull List<String> parentNames,
        @Nonnull List<Integer> parentIds,
        @Nonnull LocalDateTime created,
        @Nonnull LocalDateTime updated
) {
    public static OrganizationalUnitShadowedResponseDTO fromEntity(VOrganizationalUnitShadowedEntity entity) {
        return new OrganizationalUnitShadowedResponseDTO(
                entity.getId(),
                entity.getParentOrgUnitId(),
                entity.getDepth(),
                entity.getName(),
                entity.getAddress(),
                entity.getImprint(),
                entity.getCommonPrivacy(),
                entity.getCommonAccessibility(),
                entity.getTechnicalSupportAddress(),
                entity.getTechnicalSupportPhone(),
                entity.getTechnicalSupportInfo(),
                entity.getSpecialSupportAddress(),
                entity.getSpecialSupportPhone(),
                entity.getSpecialSupportInfo(),
                entity.getAdditionalInfo(),
                entity.getDepartmentMail(),
                entity.getThemeId(),
                entity.getParentNames(),
                entity.getParentIds(),
                entity.getCreated(),
                entity.getUpdated()
        );
    }
}
