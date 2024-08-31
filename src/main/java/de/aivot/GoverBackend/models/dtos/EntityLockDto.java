package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.enums.EntityLockState;

public record EntityLockDto(
        EntityLockState state,
        String lockedBy
) {

}
