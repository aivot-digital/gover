package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.permissions.entities.VUserDepartmentPermissionEntity;
import de.aivot.GoverBackend.permissions.entities.VUserDepartmentPermissionEntityId;
import de.aivot.GoverBackend.process.entities.VPotentialProcessInstanceAccessEntity;
import de.aivot.GoverBackend.process.entities.VPotentialProcessInstanceAccessEntityId;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VPotentialProcessInstanceAccessRepository extends
        ReadOnlyRepository<VPotentialProcessInstanceAccessEntity, VPotentialProcessInstanceAccessEntityId>,
        JpaSpecificationExecutor<VPotentialProcessInstanceAccessEntity> {

    @Query(
            value = "SELECT * FROM v_potential_process_instance_access p WHERE p.permissions::text[] @> ARRAY[:permission, '*']",
            nativeQuery = true
    )
    boolean findAllByPermission(@Nonnull @Param("permission") String permission);

}
