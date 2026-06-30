package de.aivot.gover.backend.identity.repositories;

import de.aivot.gover.backend.identity.entities.IdentityProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface IdentityProviderRepository extends JpaRepository<IdentityProviderEntity, UUID>, JpaSpecificationExecutor<IdentityProviderEntity> {
}
