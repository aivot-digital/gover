package de.aivot.prosuna.backend.search.repositories;

import de.aivot.prosuna.backend.core.repositories.ReadOnlyRepository;
import de.aivot.prosuna.backend.search.entities.SearchItemEntity;
import de.aivot.prosuna.backend.search.entities.SearchItemEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SearchEntityRepository extends ReadOnlyRepository<SearchItemEntity, SearchItemEntityId>, JpaSpecificationExecutor<SearchItemEntity> {
}
