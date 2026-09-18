package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.core.repositories.ReadOnlyRepository;
import de.aivot.prosuna.backend.process.entities.VPotentialProcessInstanceAccessEntity;
import de.aivot.prosuna.backend.process.entities.VPotentialProcessInstanceAccessEntityId;
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
                        p.user_is_direct_member,
                        p.user_direct_permissions,
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

    @Query(
            value = """
                    SELECT
                        p.department_id,
                        CASE
                            WHEN dpt.id IS NULL THEN p.department_name
                            WHEN cardinality(dpt.parent_names) > 0 THEN array_to_string(dpt.parent_names, ' › ') || ' › ' || dpt.name
                            ELSE dpt.name
                        END AS department_label,
                        dpt.depth AS department_depth,
                        p.team_id,
                        coalesce(tm.name, p.team_name) AS team_label,
                        p.user_id,
                        p.user_is_enabled,
                        coalesce(
                            nullif(concat_ws(', ', nullif(trim(usr.last_name), ''), nullif(trim(usr.first_name), '')), ''),
                            nullif(trim(usr.full_name), ''),
                            nullif(trim(p.user_full_name), '')
                        ) AS user_label,
                        usr.email AS user_sub_label,
                        p.user_via_department_id,
                        p.user_via_team_id,
                        p.user_is_direct_member,
                        p.permissions
                    FROM v_potential_process_instance_access p
                             LEFT JOIN v_departments_shadowed dpt
                                       ON p.department_id = dpt.id
                             LEFT JOIN teams tm
                                       ON p.team_id = tm.id
                             LEFT JOIN users usr
                                       ON p.user_id = usr.id
                    WHERE p.process_id = :processId
                      AND p.process_version = :processVersion
                    """,
            nativeQuery = true
    )
    List<Object[]> findSelectableRowsByProcessIdAndProcessVersion(
            @Nonnull @Param("processId") Integer processId,
            @Nonnull @Param("processVersion") Integer processVersion
    );
}
