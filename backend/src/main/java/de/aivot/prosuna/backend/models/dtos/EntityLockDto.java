package de.aivot.prosuna.backend.models.dtos;

import de.aivot.prosuna.backend.enums.EntityLockState;

public record EntityLockDto(
        EntityLockState state,
        String lockedBy
) {

}
