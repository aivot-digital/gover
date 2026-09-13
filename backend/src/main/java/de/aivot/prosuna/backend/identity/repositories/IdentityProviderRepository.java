package de.aivot.prosuna.backend.identity.repositories;

import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IdentityProviderRepository extends JpaRepository<IdentityProviderEntity, UUID>, JpaSpecificationExecutor<IdentityProviderEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select identityProvider from IdentityProviderEntity identityProvider where identityProvider.key = :key")
    Optional<IdentityProviderEntity> findByKeyForUpdate(@Param("key") UUID key);
}
