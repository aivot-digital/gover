package de.aivot.prosuna.backend.user.repositories;

import de.aivot.prosuna.backend.user.entities.UserApiKeyEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserApiKeyRepository extends JpaRepository<UserApiKeyEntity, Integer> {
    @Nonnull
    Optional<UserApiKeyEntity> findByKeyHash(@Nonnull String keyHash);
}
