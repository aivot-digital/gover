package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.process.entities.VPotentialProcessInstanceAccessEntity;
import de.aivot.GoverBackend.process.entities.VPotentialProcessInstanceAccessEntityId;
import jakarta.annotation.Nonnull;
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

    // TODO: This should not be necessary and needs to be revisited. Also we should not include users based on deputy permission.
    //  At this moment we can not easily distinguish between users that have access via their own permissions and users that have access via deputy permissions.
    //  This is because the deputy permissions are currently stored in the same array as the normal permissions.
    @Query(
            value = """
                    SELECT
                        p.department_id,
                        p.team_id,
                        p.user_id,
                        p.user_is_enabled,
                        p.user_via_department_id,
                        p.user_via_team_id,
                        p.permissions
                    FROM v_potential_process_instance_access p
                    WHERE p.process_id = :processId
                      AND p.process_version = :processVersion
                    """,
            nativeQuery = true
    )
    List<Object[]> findRowsByProcessIdAndProcessVersion(
            @Nonnull @Param("processId") Integer processId,
            @Nonnull @Param("processVersion") Integer processVersion
    );
}
