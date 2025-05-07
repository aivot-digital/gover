package de.aivot.GoverBackend.identity.repositories;

import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IdentityProviderRepository extends JpaRepository<IdentityProviderEntity, String>, JpaSpecificationExecutor<IdentityProviderEntity> {
}
