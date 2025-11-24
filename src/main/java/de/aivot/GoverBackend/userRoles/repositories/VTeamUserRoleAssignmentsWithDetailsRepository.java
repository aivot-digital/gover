package de.aivot.GoverBackend.userRoles.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.userRoles.entities.VTeamUserRoleAssignmentsWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VTeamUserRoleAssignmentsWithDetailsRepository extends ReadOnlyRepository<VTeamUserRoleAssignmentsWithDetailsEntity, Integer>, JpaSpecificationExecutor<VTeamUserRoleAssignmentsWithDetailsEntity> {
    // Add custom query methods if needed
}

