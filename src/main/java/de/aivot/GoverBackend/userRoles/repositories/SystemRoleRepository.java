package de.aivot.GoverBackend.userRoles.repositories;

import de.aivot.GoverBackend.userRoles.entities.SystemRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemRoleRepository extends JpaRepository<SystemRoleEntity, Integer>, JpaSpecificationExecutor<SystemRoleEntity> {
    @Query(
            value = """
                    SELECT * FROM system_roles
                    WHERE array_length(permissions, 1) = (
                        SELECT MAX(array_length(permissions, 1)) FROM system_roles
                    )
                    """,
            nativeQuery = true
    )
    Optional<SystemRoleEntity> findByMaxPermissions();
}

