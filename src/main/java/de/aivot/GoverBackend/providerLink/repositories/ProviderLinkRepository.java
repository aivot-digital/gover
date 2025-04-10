package de.aivot.GoverBackend.providerLink.repositories;

import de.aivot.GoverBackend.providerLink.entities.ProviderLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderLinkRepository extends JpaRepository<ProviderLink, Integer>, JpaSpecificationExecutor<ProviderLink> {
}
