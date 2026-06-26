package de.aivot.GoverBackend.user.repositories;

import de.aivot.GoverBackend.user.entities.UserEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {
    Integer countAllByDeletedInIdpIsFalseAndEnabledIsTrue();

    Boolean existsBySystemRoleId(Integer globalRole);

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
