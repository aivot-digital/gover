package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.ProviderLink;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "providerLinks", path = "provider-links")
public interface ProviderLinkRepository extends PagingAndSortingRepository<ProviderLink, Long> {
}