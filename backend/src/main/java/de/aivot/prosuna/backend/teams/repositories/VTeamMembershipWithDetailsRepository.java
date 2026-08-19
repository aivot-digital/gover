package de.aivot.prosuna.backend.teams.repositories;

import de.aivot.prosuna.backend.core.repositories.ReadOnlyRepository;
import de.aivot.prosuna.backend.teams.entities.VTeamMembershipWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VTeamMembershipWithDetailsRepository extends ReadOnlyRepository<VTeamMembershipWithDetailsEntity, Integer>, JpaSpecificationExecutor<VTeamMembershipWithDetailsEntity> {
}
