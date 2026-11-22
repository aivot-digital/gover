package de.aivot.prosuna.backend.userRoles.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * The complete role set of a department or team membership, based on the membership's lock version.
 */
public record MembershipRolesUpdateRequestDTO(
        @NotNull(message = "Die Version der Mitgliedschaft muss angegeben werden.")
        Long lockVersion,

        @NotNull(message = "Die Rollen müssen angegeben werden.")
        List<Integer> roleIds
) {
}
