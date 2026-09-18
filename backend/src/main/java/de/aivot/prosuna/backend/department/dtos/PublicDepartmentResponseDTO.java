package de.aivot.prosuna.backend.department.dtos;

import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;

import java.util.List;

public record PublicDepartmentResponseDTO(
        Integer id,
        String postalAddress,
        String imprint,
        String commonPrivacy,
        String commonAccessibility,
        String technicalSupportEmail,
        String technicalSupportPhone,
        String technicalSupportInfo,
        String specialSupportEmail,
        String specialSupportPhone,
        String specialSupportInfo,
        String defaultMailSignature,
        Integer themeId,
        Integer parentDepartmentId,
        List<Integer> parentIds
) {
    public static PublicDepartmentResponseDTO fromEntity(VDepartmentShadowedEntity entity) {
        return new PublicDepartmentResponseDTO(
                entity.getId(),
                entity.getPostalAddress(),
                entity.getImprint(),
                entity.getCommonPrivacy(),
                entity.getCommonAccessibility(),
                entity.getTechnicalSupportEmail(),
                entity.getTechnicalSupportPhone(),
                entity.getTechnicalSupportInfo(),
                entity.getSpecialSupportEmail(),
                entity.getSpecialSupportPhone(),
                entity.getSpecialSupportInfo(),
                entity.getDefaultMailSignature(),
                entity.getThemeId(),
                entity.getParentDepartmentId(),
                entity.getParentIds()
        );
    }
}
