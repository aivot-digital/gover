package de.aivot.gover.backend.search.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.search.entities.SearchItemEntity;
import de.aivot.gover.backend.search.entities.SearchItemEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SearchEntityRepository extends ReadOnlyRepository<SearchItemEntity, SearchItemEntityId>, JpaSpecificationExecutor<SearchItemEntity> {
}
