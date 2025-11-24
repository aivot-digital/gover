package de.aivot.GoverBackend.teams.dtos;

import de.aivot.GoverBackend.teams.entities.TeamMembershipEntity;

public record TeamMembershipResponseDTO(
        Integer id,
        Integer teamId,
        String userId,
        String created,
        String updated
) {
    public static TeamMembershipResponseDTO fromEntity(TeamMembershipEntity entity) {
        return new TeamMembershipResponseDTO(
                entity.getId(),
                entity.getTeamId(),
                entity.getUserId(),
                entity.getCreated().toString(),
                entity.getUpdated().toString()
        );
    }
}

