package de.aivot.gover.backend.teams.repositories;

import de.aivot.gover.backend.teams.entities.TeamMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamMembershipRepository extends JpaRepository<TeamMembershipEntity, Integer>, JpaSpecificationExecutor<TeamMembershipEntity> {
    @Query("SELECT m.id FROM TeamMembershipEntity m WHERE m.teamId IN :teamIds")
    List<Integer> findIdsByTeamIdIn(@Param("teamIds") List<Integer> teamIds);
}
