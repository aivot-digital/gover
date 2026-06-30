package de.aivot.gover.backend.providerLink.repositories;

import de.aivot.gover.backend.providerLink.entities.ProviderLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProviderLinkRepository extends JpaRepository<ProviderLink, Integer>, JpaSpecificationExecutor<ProviderLink> {
}
