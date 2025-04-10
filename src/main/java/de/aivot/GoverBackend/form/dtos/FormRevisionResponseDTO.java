package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.entities.FormRevision;
import de.aivot.GoverBackend.models.lib.DiffItem;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collection;

public record FormRevisionResponseDTO(
        BigInteger id,
        Integer formId,
        String userId,
        LocalDateTime timestamp,
        Collection<DiffItem> diff
) {
    public static FormRevisionResponseDTO fromEntity(FormRevision formRevision) {
        return new FormRevisionResponseDTO(
                formRevision.getId(),
                formRevision.getFormId(),
                formRevision.getUserId(),
                formRevision.getTimestamp(),
                formRevision.getDiff()
        );
    }
}
