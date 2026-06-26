package de.aivot.gover.backend.models.dtos;

import de.aivot.gover.backend.enums.EntityLockState;

public record EntityLockDto(
        EntityLockState state,
        String lockedBy
) {

}
