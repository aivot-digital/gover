package de.aivot.gover.backend.teams.repositories;

import de.aivot.gover.backend.teams.entities.TeamMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeamMembershipRepository extends JpaRepository<TeamMembershipEntity, Integer>, JpaSpecificationExecutor<TeamMembershipEntity> {
}
