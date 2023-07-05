package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.DepartmentMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface DepartmentMembershipRepository extends JpaRepository<DepartmentMembership, Integer> {
    Collection<DepartmentMembership> findAllByDepartmentId(Integer departmentId);

    Collection<DepartmentMembership> findAllByUserId(Integer userId);

    Collection<DepartmentMembership> findAllByUserIdAndDepartmentId(Integer userId, Integer departmentId);
    
    boolean existsByUserIdAndDepartmentId(Integer userId, Integer departmentId);

}
