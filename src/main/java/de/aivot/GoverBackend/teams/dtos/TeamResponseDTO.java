package de.aivot.GoverBackend.teams.dtos;

import de.aivot.GoverBackend.teams.entities.TeamEntity;

public record TeamResponseDTO(
    Integer id,
    String name,
    String created,
    String updated
) {
    public static TeamResponseDTO fromEntity(TeamEntity entity) {
        return new TeamResponseDTO(
            entity.getId(),
            entity.getName(),
            entity.getCreated().toString(),
            entity.getUpdated().toString()
        );
    }
}
