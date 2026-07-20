package de.aivot.gover.backend.search.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.search.entities.SearchItemEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SearchEntityRepository extends ReadOnlyRepository<SearchItemEntity, String>, JpaSpecificationExecutor<SearchItemEntity> {
}
