package de.aivot.prosuna.backend.user.repositories;

import de.aivot.prosuna.backend.user.entities.UserApiKeyEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserApiKeyRepository extends JpaRepository<UserApiKeyEntity, Integer> {
    /**
     * Finds the key only if its user is enabled and not deleted in the IdP. Keycloak refuses tokens of such users, so
     * the API key has to enforce this itself.
     */
    @Nonnull
    @Query("""
            SELECT k
            FROM UserApiKeyEntity k, UserEntity u
            WHERE k.keyHash = :keyHash
              AND u.id = k.userId
              AND u.enabled = TRUE
              AND u.deletedInIdp = FALSE
            """)
    Optional<UserApiKeyEntity> findByKeyHashOfActiveUser(@Nonnull @Param("keyHash") String keyHash);
}
