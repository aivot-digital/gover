package de.aivot.gover.backend.department.repositories;

import de.aivot.gover.backend.department.entities.DepartmentMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DepartmentMembershipRepository extends JpaRepository<DepartmentMembershipEntity, Integer>, JpaSpecificationExecutor<DepartmentMembershipEntity> {
    @Query("SELECT m.id FROM DepartmentMembershipEntity m WHERE m.departmentId IN :departmentIds")
    List<Integer> findIdsByDepartmentIdIn(@Param("departmentIds") List<Integer> departmentIds);

}
