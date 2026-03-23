package de.aivot.GoverBackend.teams.repositories;

import de.aivot.GoverBackend.teams.entities.TeamEntity;
import de.aivot.GoverBackend.teams.entities.TeamMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeamMembershipRepository extends JpaRepository<TeamMembershipEntity, Integer>, JpaSpecificationExecutor<TeamMembershipEntity> {
}
