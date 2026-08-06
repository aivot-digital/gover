package de.aivot.gover.backend.user.repositories;

import de.aivot.gover.backend.user.entities.UserEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {
    Integer countAllByDeletedInIdpIsFalseAndEnabledIsTrue();

    Boolean existsBySystemRoleId(Integer systemRoleId);

    @Query("""
            SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END
            FROM UserEntity u
            WHERE u.systemRoleId = :systemRoleId
              AND u.enabled = TRUE
              AND u.deletedInIdp = FALSE
            """)
    boolean existsActiveUserBySystemRoleId(@Param("systemRoleId") Integer systemRoleId);

    boolean existsByEmail(String email);

    List<UserEntity> findAllBySystemRoleIdOrderByFullNameAsc(Integer systemRoleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UserEntity u
            SET u.systemRoleId = :replacementSystemRoleId
            WHERE u.systemRoleId = :currentSystemRoleId
            """)
    int reassignSystemRoleId(
            @Param("currentSystemRoleId") Integer currentSystemRoleId,
            @Param("replacementSystemRoleId") Integer replacementSystemRoleId
    );

    @Query("""
            SELECT DISTINCT u.id
            FROM UserEntity u
            WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    List<String> findIdsByFullNameContaining(@Param("query") String query);
}
