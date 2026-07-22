package de.aivot.gover.backend.codeLists.repositories;

import de.aivot.gover.backend.codeLists.entities.VCodeListItemEntity;
import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface VCodeListItemRepository extends ReadOnlyRepository<VCodeListItemEntity, Long>, JpaSpecificationExecutor<VCodeListItemEntity> {
    Page<VCodeListItemEntity> findAllByCodeListId(Integer codeListId, Pageable pageable);

    List<VCodeListItemEntity> findAllByCodeListIdOrderByIdAsc(Integer codeListId);

    Optional<VCodeListItemEntity> findByIdAndCodeListId(Long id, Integer codeListId);
}
