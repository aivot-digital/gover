package de.aivot.GoverBackend.department.dtos;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.lib.ReqeustDTO;
import de.aivot.GoverBackend.validation.ValidEmailList;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record DepartmentRequestDTO(
        @Nonnull
        @NotNull(message = "Name cannot be null")
        @NotBlank(message = "Name cannot be blank")
        @Length(min = 3, max = 96)
        String name,

        @Nonnull
        @NotNull(message = "Address cannot be null")
        @NotBlank(message = "Address cannot be blank")
        @Length(min = 3, max = 255)
        String address,

        @Nonnull
        // TODO: add blank validator
        @NotNull(message = "Imprint cannot be null")
        @NotBlank(message = "Imprint cannot be blank")
        String imprint,

        @Nonnull
        @NotNull(message = "Privacy cannot be null")
        @NotBlank(message = "Privacy cannot be blank")
        String privacy,

        @Nonnull
        @NotNull(message = "Accessibility cannot be null")
        @NotBlank(message = "Accessibility cannot be blank")
        String accessibility,

        @Nonnull
        @NotNull(message = "TechnicalSupportAddress cannot be null")
        @NotBlank(message = "TechnicalSupportAddress cannot be blank")
        @Email(message = "TechnicalSupportAddress must be a valid email address")
        String technicalSupportAddress,

        @Nonnull
        @NotNull(message = "SpecialSupportAddress cannot be null")
        @NotBlank(message = "SpecialSupportAddress cannot be blank")
        @Email(message = "SpecialSupportAddress must be a valid email address")
        String specialSupportAddress,

        @Nullable
        @ValidEmailList
        String departmentMail
) implements ReqeustDTO<DepartmentEntity> {
    @Override
    public DepartmentEntity toEntity() {
        var department = new DepartmentEntity();
        department.setName(name());
        department.setAddress(address());
        department.setImprint(imprint());
        department.setPrivacy(privacy());
        department.setAccessibility(accessibility());
        department.setTechnicalSupportAddress(technicalSupportAddress());
        department.setSpecialSupportAddress(specialSupportAddress());
        department.setDepartmentMail(departmentMail());
        return department;
    }
}
