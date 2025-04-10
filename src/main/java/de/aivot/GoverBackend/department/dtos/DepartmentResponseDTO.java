package de.aivot.GoverBackend.department.dtos;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.entities.DepartmentWithMembershipEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDateTime;

public record DepartmentResponseDTO(
        @Nonnull
        Integer id,
        @Nonnull
        String name,
        @Nonnull
        String address,
        @Nonnull
        String imprint,
        @Nonnull
        String privacy,
        @Nonnull
        String accessibility,
        @Nonnull
        String technicalSupportAddress,
        @Nonnull
        String specialSupportAddress,
        @Nullable
        String departmentMail,
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
                department.getCreated(),
                department.getUpdated()
        );
    }
}
