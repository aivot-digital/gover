package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record FormListResponseDTO(
        @Nonnull
        Integer id,
        @Nonnull
        String slug,
        @Nonnull
        String title,
        @Nonnull
        String publicTitle,
        @Nonnull
        Integer developingDepartmentId,
        @Nullable
        Integer managingDepartmentId,
        @Nullable
        Integer responsibleDepartmentId,
        @Nonnull
        LocalDateTime created,
        @Nonnull
        LocalDateTime updated,
        @Nullable
        Integer publishedVersion,
        @Nullable
        Integer draftedVersion
) {
    public static FormListResponseDTO fromEntity(FormEntity form) {
        return new FormListResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getInternalTitle(),
                form.getPublicTitle(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getCreated(),
                form.getUpdated(),
                form.getPublishedVersion(),
                form.getDraftedVersion()
        );
    }

    public static FormListResponseDTO fromEntity(FormVersionWithDetailsEntity form) {
        return new FormListResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getInternalTitle(),
                form.getPublicTitle(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getCreated(),
                form.getUpdated(),
                form.getPublishedVersion(),
                form.getDraftedVersion()
        );
    }

    public static FormListResponseDTO fromEntity(FormVersionWithMembershipEntity form) {
        return new FormListResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getInternalTitle(),
                form.getPublicTitle(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getCreated(),
                form.getUpdated(),
                form.getPublishedVersion(),
                form.getDraftedVersion()
        );
    }
}
