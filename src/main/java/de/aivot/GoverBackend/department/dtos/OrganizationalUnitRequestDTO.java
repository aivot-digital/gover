package de.aivot.GoverBackend.department.dtos;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitEntity;
import de.aivot.GoverBackend.lib.RequestDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import jakarta.annotation.Nullable;

public record OrganizationalUnitRequestDTO(
        @NotNull(message = "Name cannot be null")
        @NotBlank(message = "Name cannot be blank")
        @Length(min = 3, max = 96)
        String name,

        @Nullable
        @Length(max = 255)
        String address,

        @Nullable
        String imprint,

        @Nullable
        String commonPrivacy,

        @Nullable
        String commonAccessibility,

        @Nullable
        @Length(max = 255)
        String technicalSupportAddress,

        @Nullable
        @Length(max = 96)
        String technicalSupportPhone,

        @Nullable
        String technicalSupportInfo,

        @Nullable
        @Length(max = 255)
        String specialSupportAddress,

        @Nullable
        @Length(max = 96)
        String specialSupportPhone,

        @Nullable
        String specialSupportInfo,

        @Nullable
        String additionalInfo,

        @Nullable
        @Length(max = 255)
        String departmentMail,

        @Nullable
        Integer themeId,

        @Nullable
        Integer parentOrgUnitId
) implements RequestDTO<OrganizationalUnitEntity> {
    @Override
    public OrganizationalUnitEntity toEntity() {
        return new OrganizationalUnitEntity()
                .setId(null)
                .setName(name)
                .setAddress(address)
                .setImprint(imprint)
                .setCommonPrivacy(commonPrivacy)
                .setCommonAccessibility(commonAccessibility)
                .setTechnicalSupportAddress(technicalSupportAddress)
                .setTechnicalSupportPhone(technicalSupportPhone)
                .setTechnicalSupportInfo(technicalSupportInfo)
                .setSpecialSupportAddress(specialSupportAddress)
                .setSpecialSupportPhone(specialSupportPhone)
                .setSpecialSupportInfo(specialSupportInfo)
                .setAdditionalInfo(additionalInfo)
                .setDepartmentMail(departmentMail)
                .setThemeId(themeId)
                .setParentOrgUnitId(parentOrgUnitId);
    }
}
