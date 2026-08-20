package de.aivot.prosuna.backend.providerLink.repositories;

import de.aivot.prosuna.backend.providerLink.entities.ProviderLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProviderLinkRepository extends JpaRepository<ProviderLink, Integer>, JpaSpecificationExecutor<ProviderLink> {
}
