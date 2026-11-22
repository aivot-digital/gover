package de.aivot.prosuna.backend.userRoles.repositories;

import de.aivot.prosuna.backend.userRoles.entities.UserRoleAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignmentEntity, Integer>, JpaSpecificationExecutor<UserRoleAssignmentEntity> {
    List<UserRoleAssignmentEntity> findAllByDepartmentMembershipId(Integer departmentMembershipId);

    List<UserRoleAssignmentEntity> findAllByTeamMembershipId(Integer teamMembershipId);
}
