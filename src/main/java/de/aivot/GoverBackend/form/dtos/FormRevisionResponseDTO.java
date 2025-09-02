package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.entities.FormRevisionEntity;
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
    public static FormRevisionResponseDTO fromEntity(FormRevisionEntity formRevisionEntity) {
        return new FormRevisionResponseDTO(
                formRevisionEntity.getId(),
                formRevisionEntity.getFormId(),
                formRevisionEntity.getUserId(),
                formRevisionEntity.getTimestamp(),
                formRevisionEntity.getDiff()
        );
    }
}
