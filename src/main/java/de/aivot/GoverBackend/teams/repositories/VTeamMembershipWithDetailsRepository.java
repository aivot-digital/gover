package de.aivot.GoverBackend.teams.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VTeamMembershipWithDetailsRepository extends ReadOnlyRepository<VTeamMembershipWithDetailsEntity, Integer>, JpaSpecificationExecutor<VTeamMembershipWithDetailsEntity> {
}
