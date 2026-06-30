package de.aivot.gover.backend.teams.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.teams.entities.VTeamMembershipWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VTeamMembershipWithDetailsRepository extends ReadOnlyRepository<VTeamMembershipWithDetailsEntity, Integer>, JpaSpecificationExecutor<VTeamMembershipWithDetailsEntity> {
}
