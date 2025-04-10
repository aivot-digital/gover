package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.entities.FormWithMembership;
import de.aivot.GoverBackend.form.enums.FormType;

import java.time.LocalDateTime;

public record FormListResponseDTO(
        Integer id,
        String slug,
        String version,
        String title,
        FormStatus status,
        FormType type,
        Integer developingDepartmentId,
        Integer managingDepartmentId,
        Integer responsibleDepartmentId,
        Integer themeId,
        LocalDateTime created,
        LocalDateTime updated,
        Boolean bundIdEnabled,
        Boolean bayernIdEnabled,
        Boolean shIdEnabled,
        Boolean mukEnabled,
        String paymentProvider
) {
    public static FormListResponseDTO fromEntity(Form form) {
        return new FormListResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getVersion(),
                form.getTitle(),
                form.getStatus(),
                form.getType(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getThemeId(),
                form.getCreated(),
                form.getUpdated(),
                form.getBundIdEnabled(),
                form.getBayernIdEnabled(),
                form.getShIdEnabled(),
                form.getMukEnabled(),
                form.getPaymentProvider()
        );
    }

    public static FormListResponseDTO fromEntity(FormWithMembership form) {
        return new FormListResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getVersion(),
                form.getTitle(),
                form.getStatus(),
                form.getType(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getThemeId(),
                form.getCreated(),
                form.getUpdated(),
                form.getBundIdEnabled(),
                form.getBayernIdEnabled(),
                form.getShIdEnabled(),
                form.getMukEnabled(),
                form.getPaymentProvider()
        );
    }
}
