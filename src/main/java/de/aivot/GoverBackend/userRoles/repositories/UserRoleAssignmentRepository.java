package de.aivot.GoverBackend.userRoles.repositories;

import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignmentEntity, Integer>, JpaSpecificationExecutor<UserRoleAssignmentEntity> {
}
