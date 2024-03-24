package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.entities.DepartmentMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;

public interface DepartmentMembershipRepository extends JpaRepository<DepartmentMembership, Integer> {
    Collection<DepartmentMembership> findAllByDepartmentId(Integer departmentId);

    Collection<DepartmentMembership> findAllByUserId(String userId);

    Collection<DepartmentMembership> findAllByRole(UserRole userRole);

    Collection<DepartmentMembership> findAllByDepartmentIdAndUserId(Integer departmentId, String userId);

    Collection<DepartmentMembership> findAllByDepartmentIdAndRole(Integer departmentId, UserRole userRole);

    Collection<DepartmentMembership> findAllByUserIdAndRole(String userId, UserRole userRole);

    Collection<DepartmentMembership> findAllByDepartmentIdAndUserIdAndRole(Integer departmentId, String userId, UserRole userRole);

    boolean existsByDepartmentIdAndUserId(Integer departmentId, String userId);

    boolean existsByDepartmentIdAndUserIdAndRole(Integer departmentId, String userId, UserRole userRole);

    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM forms form JOIN submissions sub ON form.id = sub.form_id WHERE sub.assignee_id = ?1 AND sub.archived is null AND (responsible_department_id = ?2 OR managing_department_id = ?2))",
            nativeQuery = true
    )
    boolean existsActiveSubmissionByUserIdAndDepartmentId(String userId, Integer departmentId);
}
