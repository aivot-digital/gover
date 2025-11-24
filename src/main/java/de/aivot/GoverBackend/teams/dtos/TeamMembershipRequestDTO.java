package de.aivot.GoverBackend.teams.dtos;

import de.aivot.GoverBackend.lib.RequestDTO;
import de.aivot.GoverBackend.teams.entities.TeamMembershipEntity;

public record TeamMembershipRequestDTO(
        Integer teamId,
        String userId
) implements RequestDTO<TeamMembershipEntity> {

    @Override
    public TeamMembershipEntity toEntity() {
        TeamMembershipEntity entity = new TeamMembershipEntity();
        entity.setTeamId(this.teamId);
        entity.setUserId(this.userId);
        return entity;
    }
}
