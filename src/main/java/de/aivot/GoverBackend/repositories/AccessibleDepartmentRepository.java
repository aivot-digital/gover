package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.entities.AccessibleDepartment;

import java.util.Collection;

public interface AccessibleDepartmentRepository extends ReadOnlyRepository<AccessibleDepartment, Integer> {
    Collection<AccessibleDepartment> findAllByUserId(Integer userId);
    Collection<AccessibleDepartment> findAllByUserIdAndRole(Integer userId, UserRole role);
    Collection<AccessibleDepartment> findAllByRole(UserRole role);
    boolean existsByDepartmentIdAndUserId(Integer departmentId, Integer userId);
    boolean existsByDepartmentIdAndUserIdAndRole(Integer departmentId, Integer userId, UserRole role);
}
