package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.entities.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record FormResponseDTO(
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
    public static FormResponseDTO fromEntity(FormEntity form) {
        return new FormResponseDTO(
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

    public static FormResponseDTO fromEntity(VFormWithPermissionEntity form) {
        return new FormResponseDTO(
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

    public static FormResponseDTO fromEntity(FormVersionWithDetailsEntity form) {
        return new FormResponseDTO(
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

    public static FormResponseDTO fromEntity(FormVersionWithMembershipEntity form) {
        return new FormResponseDTO(
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

    public static FormResponseDTO fromEntity(FormWithMembershipEntity formWithMembershipEntity) {
        return new FormResponseDTO(
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
