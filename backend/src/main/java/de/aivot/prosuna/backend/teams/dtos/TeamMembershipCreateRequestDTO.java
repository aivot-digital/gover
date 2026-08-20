package de.aivot.prosuna.backend.teams.dtos;

import de.aivot.prosuna.backend.lib.RequestDTO;
import de.aivot.prosuna.backend.teams.entities.TeamMembershipEntity;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TeamMembershipCreateRequestDTO(
        @NotNull
        Integer teamId,

        @NotNull
        String userId,

        @Nullable
        List<Integer> roleIds
) implements RequestDTO<TeamMembershipEntity> {
    @Override
    public TeamMembershipEntity toEntity() {
        return new TeamMembershipEntity()
                .setTeamId(teamId)
                .setUserId(userId);
    }

    public List<Integer> roleIdsOrEmpty() {
        return roleIds != null ? roleIds : List.of();
    }
}
