package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntity;
import de.aivot.GoverBackend.form.entities.FormWithMembershipEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record FormListResponseDTO(
        @Nonnull
        Integer id,
        @Nonnull
        String slug,
        @Nonnull
        String internalTitle,
        @Nonnull
        Integer developingDepartmentId,
        @Nonnull
        LocalDateTime created,
        @Nonnull
        LocalDateTime updated,
        @Nullable
        Integer publishedVersion,
        @Nullable
        Integer draftedVersion,
        @Nonnull
        Integer versionCount
) {
    public static FormListResponseDTO fromEntity(FormEntity form) {
        return new FormListResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getInternalTitle(),
                form.getDevelopingDepartmentId(),
                form.getCreated(),
                form.getUpdated(),
                form.getPublishedVersion(),
                form.getDraftedVersion(),
                form.getVersionCount()
        );
    }

    public static FormListResponseDTO fromEntity(FormVersionWithDetailsEntity form) {
        return new FormListResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getInternalTitle(),
                form.getDevelopingDepartmentId(),
                form.getCreated(),
                form.getUpdated(),
                form.getPublishedVersion(),
                form.getDraftedVersion(),
                form.getVersionCount()
        );
    }

    public static FormListResponseDTO fromEntity(FormVersionWithMembershipEntity form) {
        return new FormListResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getInternalTitle(),
                form.getDevelopingDepartmentId(),
                form.getCreated(),
                form.getUpdated(),
                form.getPublishedVersion(),
                form.getDraftedVersion(),
                form.getVersionCount()
        );
    }

    public static FormListResponseDTO fromEntity(FormWithMembershipEntity formWithMembershipEntity) {
        return new FormListResponseDTO(
                formWithMembershipEntity.getId(),
                formWithMembershipEntity.getSlug(),
                formWithMembershipEntity.getInternalTitle(),
                formWithMembershipEntity.getDevelopingDepartmentId(),
                formWithMembershipEntity.getCreated(),
                formWithMembershipEntity.getUpdated(),
                formWithMembershipEntity.getPublishedVersion(),
                formWithMembershipEntity.getDraftedVersion(),
                formWithMembershipEntity.getVersionCount()
        );
    }
}
