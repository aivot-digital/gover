package de.aivot.GoverBackend.teams.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.teams.entities.VTeamUserRoleAssignmentWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VTeamUserRoleAssignmentWithDetailsRepository extends ReadOnlyRepository<VTeamUserRoleAssignmentWithDetailsEntity, Integer>, JpaSpecificationExecutor<VTeamUserRoleAssignmentWithDetailsEntity> {
    // Add custom query methods if needed
}

