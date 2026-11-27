package de.aivot.prosuna.backend.permissions.repositories;

import de.aivot.prosuna.backend.permissions.entities.VUserDepartmentPermissionEntity;
import de.aivot.prosuna.backend.permissions.entities.VUserDepartmentPermissionEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VUserDepartmentPermissionRepository extends JpaRepository<VUserDepartmentPermissionEntity, VUserDepartmentPermissionEntityId>, JpaSpecificationExecutor<VUserDepartmentPermissionEntity> {
    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_department_permissions p WHERE p.user_id = :userId AND p.department_id = :departmentId AND p.department_id IS NOT NULL AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasPermission(@Param("userId") String userId,
                          @Param("departmentId") Integer departmentId,
                          @Param("permission") String permission);

    // The permission view also contains system-derived rows without a concrete department.
    // Resource-scoped list filters must only receive real department IDs.
    @Query(
            value = "SELECT p.department_id FROM v_user_department_permissions p WHERE p.user_id = :userId AND p.department_id IS NOT NULL AND p.permissions::text[]  @> ARRAY[:permission]",
            nativeQuery = true
    )
    List<Integer> getDepartmentsWithPermission(@Param("userId") String userId,
                                               @Param("permission") String permission);

    @Query(
            value = "SELECT * FROM v_user_department_permissions p WHERE p.user_id = :userId AND p.department_id IS NOT NULL AND array_length(p.permissions, 1) > 0",
            nativeQuery = true
    )
    List<VUserDepartmentPermissionEntity> findAllByUserId(@Param("userId") String userId);

    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_department_permissions p WHERE p.user_id = :userId AND p.department_id IS NOT NULL AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasPermissionInAnyDepartment(@Param("userId") String userId,
                                         @Param("permission") String permission);
}
