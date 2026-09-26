package de.aivot.prosuna.backend.teams.repositories;

import de.aivot.prosuna.backend.teams.entities.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeamRepository extends JpaRepository<TeamEntity, Integer>, JpaSpecificationExecutor<TeamEntity> {
}
