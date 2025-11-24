package de.aivot.GoverBackend.userRoles.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.userRoles.entities.VOrgUserRoleAssignmentsWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VOrgUserRoleAssignmentsWithDetailsRepository extends ReadOnlyRepository<VOrgUserRoleAssignmentsWithDetailsEntity, Integer>, JpaSpecificationExecutor<VOrgUserRoleAssignmentsWithDetailsEntity> {
    // Add custom query methods if needed
}

