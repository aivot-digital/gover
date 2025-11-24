package de.aivot.GoverBackend.teams.dtos;

import de.aivot.GoverBackend.lib.RequestDTO;
import de.aivot.GoverBackend.teams.entities.TeamEntity;

public record TeamRequestDTO(
        String name
) implements RequestDTO<TeamEntity> {
    @Override
    public TeamEntity toEntity() {
        return new TeamEntity()
                .setName(this.name);
    }
}
