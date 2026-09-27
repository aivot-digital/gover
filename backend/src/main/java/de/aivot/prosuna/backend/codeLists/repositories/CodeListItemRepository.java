package de.aivot.prosuna.backend.codeLists.repositories;

import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CodeListItemRepository extends JpaRepository<CodeListItemEntity, Long>, JpaSpecificationExecutor<CodeListItemEntity> {
    List<CodeListItemEntity> findAllByCodeListId(Integer codeListId);

    Optional<CodeListItemEntity> findByIdAndCodeListId(Long id, Integer codeListId);

    @Transactional
    void deleteAllByCodeListId(Integer codeListId);
}
